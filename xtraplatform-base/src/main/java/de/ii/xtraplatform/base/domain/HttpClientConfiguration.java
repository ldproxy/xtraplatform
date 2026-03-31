/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import de.ii.xtraplatform.docs.DocFile;

/**
 * @langEn # HTTP Client
 *     <p>## HTTP Proxy
 *     <p>If the application needs to use an HTTP proxy to access external resources, it can be
 *     configured as follows.
 *     <p>In this example, the HTTP proxy URL is `http://localhost:8888`. Connections to hosts
 *     listed under `nonProxyHosts` are made directly and not through the HTTP proxy. In this
 *     example, this would be `localhost`, any IP address starting with `192.168.` and any subdomain
 *     of `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 * @langDe # HTTP Client
 *     <p>## HTTP Proxy
 *     <p>Falls die Applikation einen HTTP-Proxy verwenden muss, um auf externe Ressourcen
 *     zuzugreifen, kann dieser wie folgt konfiguriert werden.
 *     <p>In diesem Beispiel ist die HTTP-Proxy-URL `http://localhost:8888`. Verbindungen zu Hosts
 *     die unter `nonProxyHosts` augelistet sind werden direkt und nicht durch den HTTP-Proxy
 *     hergestellt. In diesem Beispiel wären das `localhost`, jede IP-Addresse die mit `192.168.`
 *     anfängt und jede Subdomain von `example.org`.
 *     <p><code>
 * ```yaml
 * httpClient:
 *   proxy:
 *     host: localhost
 *     port: 8888
 *     scheme : http
 *     nonProxyHosts:
 *       - localhost
 *       - '192.168.*'
 *       - '*.example.org'
 * ```
 * </code>
 *     <p>
 */
@DocFile(path = "application/20-configuration", name = "97-http-client.md")
public class HttpClientConfiguration extends io.dropwizard.client.HttpClientConfiguration {}
