package de.ii.xtraplatform.jobs.app

import de.ii.xtraplatform.base.domain.*
import de.ii.xtraplatform.jobs.domain.Job
import de.ii.xtraplatform.jobs.domain.JobQueue
import de.ii.xtraplatform.jobs.domain.JobSet
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path

class JobQueueSpec extends Specification {

    @Shared
    JobQueue jobQueue = new JobQueueImpl(null, Set.of(new JobQueueBackendLocal(new AppContext() {
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
            ModifiableAppConfiguration config = new ModifiableAppConfiguration()
            ModifiableJobsConfiguration jobsConfig = new ModifiableJobsConfiguration()
            jobsConfig.setQueue(JobsConfiguration.QUEUE.LOCAL)
            config.setJobs(jobsConfig)
            return config
        }

        @Override
        URI getUri() {
            return null
        }

        @Override
        String getInstanceName() {
            return null
        }
    }, null)))

    def 'Test push'() {
        given:
        Job job = Job.of("push", 5, "bar")

        when:
        jobQueue.push(job)

        then:
        job == jobQueue.getOpen().get("push").get(5)[0]


    }

    def 'Test take'() {
        given:
        Job job = Job.of("take", 5, "bar")

        when:
        jobQueue.push(job)
        Job jobTaken = jobQueue.take("take", "").get()

        then:
        jobTaken == jobQueue.getTaken()[0]
    }

    def 'Test untake'() {
        given:
        Job job = Job.of("untake", 5, "bar")

        when:
        jobQueue.push(job, false)
        jobQueue.take("untake", "").get()
        jobQueue.push(job, true)
        then:
        job.id == jobQueue.getOpen().get("untake").get(5)[0].id
    }

    def 'Test done'() {
        given:
        Job job = Job.of("done", 5, "bar")

        when:
        jobQueue.push(job, false)
        jobQueue.take("done", "").get()
        then:
        jobQueue.done(job.id)
    }

    def 'Test done 2'() {
        given:
        Job job = Job.of("done2", 5, "bar")

        when:
        jobQueue.push(job, false)
        then:
        !jobQueue.done(job.id)
    }

    def 'Test push JobSet'() {
        given:
        JobSet jobSet = JobSet.of("jobSet", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void init(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> initJson(Map<String, Object> params) {
                return null
            }

            @Override
            void update(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> updateJson(Map<String, Object> detailParameters) {
                return null
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
        jobQueue.push(jobSet, false)

        then:
        jobSet == jobQueue.getSets()[0]
    }

    def 'Test done JobSet'() {
        given:
        JobSet jobSet = JobSet.of("doneSet", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void init(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> initJson(Map<String, Object> params) {
                return null
            }

            @Override
            void update(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> updateJson(Map<String, Object> detailParameters) {
                return null
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
        jobQueue.push(jobSet, false)

        then:
        jobQueue.doneSet(jobSet.id)
    }

    def 'Test done JobSet 2'() {
        given:
        JobSet jobSet = JobSet.of("doneSet2", 5, "entity", "label", "description", new JobSet.JobSetDetails() {
            @Override
            void init(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> initJson(Map<String, Object> params) {
                return null
            }

            @Override
            void update(Map<String, Object> parameters) {

            }

            @Override
            Map<String, Object> updateJson(Map<String, Object> detailParameters) {
                return null
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
        jobQueue.push(jobSet, false)
        jobQueue.doneSet(jobSet.id)

        then:
        !jobQueue.doneSet(jobSet.id)
    }

    def 'Test push null'() {
        given:
        Job job = null

        when:
        jobQueue.push(job, false)

        then:
        thrown(NullPointerException)


    }

    def 'Test push empty strings'() {
        given:
        Job job = Job.of("", 5, "")

        when:
        jobQueue.push(job, false)

        then:
        job == jobQueue.getOpen().get("").get(5)[0]


    }


}
