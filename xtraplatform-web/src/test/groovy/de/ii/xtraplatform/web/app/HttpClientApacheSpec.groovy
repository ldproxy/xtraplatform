package de.ii.xtraplatform.web.app

import com.sun.net.httpserver.HttpServer
import jakarta.ws.rs.core.MediaType
import org.apache.hc.client5.http.impl.classic.HttpClients
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class HttpClientApacheSpec extends Specification {

    @Shared
    HttpServer server

    @Shared
    String baseUrl

    @Shared
    List<String> received = []

    // like the client built by HttpApache with the default configuration, which has no automatic
    // retries; the default Apache client would repeat a request answered with 503
    @Shared
    @AutoCleanup
    def apacheClient = HttpClients.custom().disableAutomaticRetries().build()

    def client = new HttpClientApache(apacheClient)

    def setupSpec() {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        // /status/{code} answers with that status code, the body echoes the request body
        server.createContext("/status/") { exchange ->
            def request = exchange.requestBody.bytes
            received << new String(request, StandardCharsets.UTF_8)
            int status = exchange.requestURI.path.substring("/status/".length()) as int
            byte[] response = status == 204 ? new byte[0] : ("echo " + new String(request, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length)
            if (response.length > 0) {
                exchange.responseBody.write(response)
            }
            exchange.close()
        }
        // /redirect answers with a redirect to /status/200
        server.createContext("/redirect") { exchange ->
            exchange.responseHeaders.add("Location", "${baseUrl}/status/200")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        // /headers answers with the method, two values of one header and a body of 10 bytes
        server.createContext("/headers") { exchange ->
            received << exchange.requestMethod + " " + exchange.requestHeaders.getFirst("X-Test") + " " + new String(exchange.requestBody.bytes, StandardCharsets.UTF_8)
            exchange.responseHeaders.add("X-Value", "a")
            exchange.responseHeaders.add("X-Value", "b")
            byte[] response = "0123456789".getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, response.length)
            exchange.responseBody.write(response)
            exchange.close()
        }
        server.start()
        baseUrl = "http://localhost:${server.address.port}"
    }

    def cleanupSpec() {
        server?.stop(0)
    }

    def setup() {
        received.clear()
    }

    def "a POST returns the status code #status and the body of the response"() {
        when:
        def response = client.post("${baseUrl}/status/${status}", '{"a":1}'.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_JSON_TYPE, [:])

        then:
        received == ['{"a":1}']
        response.status == status
        response.successful == successful
        new String(response.body, StandardCharsets.UTF_8) == body

        where:
        status || successful | body
        200    || true       | 'echo {"a":1}'
        204    || true       | ''
        404    || false      | 'echo {"a":1}'
        500    || false      | 'echo {"a":1}'
        503    || false      | 'echo {"a":1}'
    }

    def "a POST as input stream returns the body of the response whatever its status code: #status"() {
        when:
        def body = client.postAsInputStream("${baseUrl}/status/${status}", 'x'.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN_TYPE, [:]).text

        then:
        body == 'echo x'

        where:
        status << [200, 500]
    }

    def "a request returns a redirect instead of following it"() {
        given:
        // the default Apache client follows redirects, like the client built by HttpApache
        def following = new HttpClientApache(HttpClients.custom().disableAutomaticRetries().build())

        when:
        def response = following.send("GET", "${baseUrl}/redirect", [:], Optional.empty(), 1000)

        then:
        received == []
        response.status == 302
        response.headers["location"] == "${baseUrl}/status/200".toString()

        cleanup:
        following.httpClient.close()
    }

    def "a request sends the method, the headers and the body and returns the headers"() {
        when:
        def response = client.send("PUT", "${baseUrl}/headers", ["X-Test": "t", "Content-Type": "text/plain"], Optional.of('x'.getBytes(StandardCharsets.UTF_8)), 10)

        then:
        received == ["PUT t x"]
        response.status == 200
        response.headers["x-value"] == "a, b"
        new String(response.body, StandardCharsets.UTF_8) == "0123456789"
    }

    def "a response larger than the maximum size fails"() {
        when:
        client.send("GET", "${baseUrl}/headers", [:], Optional.empty(), 9)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("larger than 9 bytes")
    }

    def "a POST that cannot connect fails"() {
        given:
        def closed = new ServerSocket(0)
        int port = closed.localPort
        closed.close()

        when:
        client.post("http://localhost:${port}/status/200", new byte[0], MediaType.APPLICATION_JSON_TYPE, [:])

        then:
        thrown(IllegalArgumentException)
    }
}
