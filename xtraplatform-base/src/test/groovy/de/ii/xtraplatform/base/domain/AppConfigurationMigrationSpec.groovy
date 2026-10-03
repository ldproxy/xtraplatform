/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain

import de.ii.xtraplatform.base.domain.Constants.ENV
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AppConfigurationMigrationSpec extends Specification {

    static AppConfiguration load(String userCfg) {
        return new ConfigurationReader(Map.of()).loadMergedConfig(
                Map.of("cfg.yml", new ByteArrayInputStream(userCfg.getBytes(StandardCharsets.UTF_8))),
                ENV.DEVELOPMENT)
    }

    def "the default job concurrency is 1"() {
        when:
        AppConfiguration cfg = load("substitutions: {}\n")

        then:
        cfg.getJobs().getMaxConcurrent() == 1
        cfg.getJobConcurrency() == 1
    }

    def "jobs.maxConcurrent is applied"() {
        when:
        AppConfiguration cfg = load("jobs:\n  maxConcurrent: 3\n")

        then:
        cfg.getJobs().getMaxConcurrent() == 3
        cfg.getJobConcurrency() == 3
    }

    def "the deprecated backgroundTasks.maxThreads is migrated to jobs.maxConcurrent"() {
        when:
        AppConfiguration cfg = load("backgroundTasks:\n  maxThreads: 4\n")

        then:
        cfg.getJobs().getMaxConcurrent() == 4
        cfg.getJobConcurrency() == 4
    }

    def "an explicit jobs.maxConcurrent wins over the deprecated backgroundTasks.maxThreads"() {
        when:
        AppConfiguration cfg = load("backgroundTasks:\n  maxThreads: 4\njobs:\n  maxConcurrent: 2\n")

        then:
        cfg.getJobs().getMaxConcurrent() == 2
        cfg.getJobConcurrency() == 2
    }
}
