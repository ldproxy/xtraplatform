/*
 * Copyright 2025 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.blobs.infra

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class BlobSourceFsContainmentSpec extends Specification {

    Path root
    Path secret

    def setup() {
        Path base = Files.createTempDirectory("blobstore")
        root = Files.createDirectory(base.resolve("store"))
        Files.writeString(root.resolve("inside.txt"), "inside")
        // a file outside the store root
        secret = base.resolve("secret.txt")
        Files.writeString(secret, "secret")
    }

    def "legitimate keys (including ones that normalize back inside) are handled"() {
        given:
        BlobSourceFs source = new BlobSourceFs(root)

        expect:
        source.canHandle(Path.of("inside.txt"))
        source.has(Path.of("inside.txt"))
        source.canHandle(Path.of("sub/../inside.txt"))
    }

    def "traversal and absolute keys are rejected"() {
        given:
        BlobSourceFs source = new BlobSourceFs(root)

        expect:
        !source.canHandle(key)
        !source.has(key)
        source.content(key).isEmpty()
        source.size(key) == -1

        where:
        key << [
                Path.of("../secret.txt"),
                Path.of("a/../../secret.txt"),
                Path.of("/etc/passwd"),
        ]
    }

    def "a traversal delete does not remove files outside the root"() {
        given:
        BlobSourceFs source = new BlobSourceFs(root)

        when:
        source.delete(Path.of("../secret.txt"))

        then:
        Files.exists(secret)
    }

    def "a traversal put does not write files outside the root"() {
        given:
        BlobSourceFs source = new BlobSourceFs(root)
        Path target = root.resolveSibling("evil.txt")

        when:
        source.put(Path.of("../evil.txt"), new ByteArrayInputStream("x".getBytes("UTF-8")))

        then:
        !Files.exists(target)
    }
}
