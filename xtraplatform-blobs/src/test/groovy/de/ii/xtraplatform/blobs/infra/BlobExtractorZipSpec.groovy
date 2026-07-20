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
import java.util.function.Predicate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BlobExtractorZipSpec extends Specification {

    def "a zip entry with .. is not written outside the target directory"() {
        given:
        Path base = Files.createTempDirectory("ziptest")
        Path targetRoot = Files.createDirectory(base.resolve("out"))
        Path zipFile = base.resolve("archive.zip")
        new ZipOutputStream(Files.newOutputStream(zipFile)).withCloseable { zos ->
            zos.putNextEntry(new ZipEntry("good.txt"))
            zos.write("good".getBytes("UTF-8"))
            zos.closeEntry()
            zos.putNextEntry(new ZipEntry("../evil.txt"))
            zos.write("evil".getBytes("UTF-8"))
            zos.closeEntry()
        }

        when:
        new BlobExtractorZip().extract(zipFile, Path.of("/"), { true } as Predicate, targetRoot, true)

        then:
        Files.exists(targetRoot.resolve("good.txt"))
        !Files.exists(base.resolve("evil.txt"))
    }
}
