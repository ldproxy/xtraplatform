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
