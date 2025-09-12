package de.ii.xtraplatform.jobs.app

import de.ii.xtraplatform.base.domain.AppConfiguration
import de.ii.xtraplatform.base.domain.AppContext
import de.ii.xtraplatform.base.domain.Constants
import de.ii.xtraplatform.jobs.domain.BaseJob
import de.ii.xtraplatform.jobs.domain.Job
import de.ii.xtraplatform.jobs.domain.JobSet
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class JobQueueSimpleSpec extends Specification{

    @Shared JobQueueSimple jobQueueSimple = new JobQueueSimple(new AppContext() {
        @Override
        String getName() {
            return null
        }

        @Override
        String getVersion() {
            return null
        }

        @Override
        Constants.ENV getEnvironment() {
            return null
        }

        @Override
        Path getDataDir() {
            return null
        }

        @Override
        Path getTmpDir() {
            return null
        }

        @Override
        AppConfiguration getConfiguration() {
            return null
        }

        @Override
        URI getUri() {
            return null
        }

        @Override
        String getInstanceName() {
            return null
        }
    })

    def 'Test push'() {
        given:
        Job job = Job.of("push", 5, "bar")

        when:
        jobQueueSimple.push(job, false)

        then:
        job == jobQueueSimple.getOpen().get("push").get(5)[0]


    }
    def 'Test take'() {
        given:
        Job job = Job.of("take", 5, "bar")

        when:
        jobQueueSimple.push(job, false)
        Job jobTaken = jobQueueSimple.take("take", "").get()

        then:
        jobTaken.equals(jobQueueSimple.getTaken()[0])
    }

    def 'Test untake'() {
        given:
        Job job = Job.of("untake", 5, "bar")

        when:
        jobQueueSimple.push(job, false)
        jobQueueSimple.take("untake", "").get()
        jobQueueSimple.push(job, true)
        then:
        job.id == jobQueueSimple.getOpen().get("untake").get(5)[0].id
    }

    def 'Test done'() {
        given:
        Job job = Job.of("done", 5, "bar")

        when:
        jobQueueSimple.push(job, false)
        jobQueueSimple.take("done", "").get()
        then:
        jobQueueSimple.done(job.id)
    }

    def 'Test done 2'() {
        given:
        Job job = Job.of("done2", 5, "bar")

        when:
        jobQueueSimple.push(job, false)
        then:
        !jobQueueSimple.done(job.id)
    }

    def 'Test push JobSet'() {
        given:
        JobSet jobSet= JobSet.of("jobSet", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void update(Map<String, String> parameters) {

            }

            @Override
            void reset(Job job) {

            }

            @Override
            String getLabel() {
                return null
            }
        })

        when:
        jobQueueSimple.push(jobSet, false)

        then:
        jobSet == jobQueueSimple.getSets()[0]
    }

    def 'Test done JobSet'() {
        given:
        JobSet jobSet= JobSet.of("doneSet", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void update(Map<String, String> parameters) {

            }

            @Override
            void reset(Job job) {

            }

            @Override
            String getLabel() {
                return null
            }
        })

        when:
        jobQueueSimple.push(jobSet, false)

        then:
        jobQueueSimple.doneSet(jobSet.id)
    }

    def 'Test done JobSet 2'() {
        given:
        JobSet jobSet= JobSet.of("doneSet2", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void update(Map<String, String> parameters) {

            }

            @Override
            void reset(Job job) {

            }

            @Override
            String getLabel() {
                return null
            }
        })

        when:
        jobQueueSimple.push(jobSet, false)
        jobQueueSimple.doneSet(jobSet.id)

        then:
        !jobQueueSimple.doneSet(jobSet.id)
    }

    def 'Test push null'() {
        given:
        Job job = null

        when:
        jobQueueSimple.push(job, false)

        then:
        thrown(NullPointerException)


    }

    def 'Test push empty strings'() {
        given:
        Job job = Job.of("", 5, "")

        when:
        jobQueueSimple.push(job, false)

        then:
        job == jobQueueSimple.getOpen().get("").get(5)[0]


    }



}
