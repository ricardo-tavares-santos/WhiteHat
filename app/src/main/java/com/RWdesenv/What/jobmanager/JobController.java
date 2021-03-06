package com.RWdesenv.What.jobmanager;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import com.RWdesenv.What.jobmanager.persistence.ConstraintSpec;
import com.RWdesenv.What.jobmanager.persistence.DependencySpec;
import com.RWdesenv.What.jobmanager.persistence.FullSpec;
import com.RWdesenv.What.jobmanager.persistence.JobSpec;
import com.RWdesenv.What.jobmanager.persistence.JobStorage;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.util.Debouncer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manages the queue of jobs. This is the only class that should write to {@link JobStorage} to
 * ensure consistency.
 */
class JobController {

  private static final String TAG = JobController.class.getSimpleName();

  private final Application            application;
  private final JobStorage             jobStorage;
  private final JobInstantiator        jobInstantiator;
  private final ConstraintInstantiator constraintInstantiator;
  private final Data.Serializer        dataSerializer;
  private final JobTracker             jobTracker;
  private final Scheduler              scheduler;
  private final Debouncer              debouncer;
  private final Callback               callback;
  private final Map<String, Job>       runningJobs;

  JobController(@NonNull Application application,
                @NonNull JobStorage jobStorage,
                @NonNull JobInstantiator jobInstantiator,
                @NonNull ConstraintInstantiator constraintInstantiator,
                @NonNull Data.Serializer dataSerializer,
                @NonNull JobTracker jobTracker,
                @NonNull Scheduler scheduler,
                @NonNull Debouncer debouncer,
                @NonNull Callback callback)
  {
    this.application            = application;
    this.jobStorage             = jobStorage;
    this.jobInstantiator        = jobInstantiator;
    this.constraintInstantiator = constraintInstantiator;
    this.dataSerializer         = dataSerializer;
    this.jobTracker             = jobTracker;
    this.scheduler              = scheduler;
    this.debouncer              = debouncer;
    this.callback               = callback;
    this.runningJobs            = new HashMap<>();
  }

  @WorkerThread
  synchronized void init() {
    jobStorage.updateAllJobsToBePending();
    notifyAll();
  }

  synchronized void wakeUp() {
    notifyAll();
  }

  @WorkerThread
  synchronized void submitNewJobChain(@NonNull List<List<Job>> chain) {
    chain = Stream.of(chain).filterNot(List::isEmpty).toList();

    if (chain.isEmpty()) {
      Log.w(TAG, "Tried to submit an empty job chain. Skipping.");
      return;
    }

    if (chainExceedsMaximumInstances(chain)) {
      Job solo = chain.get(0).get(0);
      jobTracker.onStateChange(solo, JobTracker.JobState.IGNORED);
      Log.w(TAG, JobLogger.format(solo, "Already at the max instance count of " + solo.getParameters().getMaxInstances() + ". Skipping."));
      return;
    }

    insertJobChain(chain);
    scheduleJobs(chain.get(0));
    triggerOnSubmit(chain);
    notifyAll();
  }

  @WorkerThread
  synchronized void submitJobWithExistingDependencies(@NonNull Job job, @NonNull Collection<String> dependsOn) {
    List<List<Job>> chain = Collections.singletonList(Collections.singletonList(job));

    if (chainExceedsMaximumInstances(chain)) {
      jobTracker.onStateChange(job, JobTracker.JobState.IGNORED);
      Log.w(TAG, JobLogger.format(job, "Already at the max instance count of " + job.getParameters().getMaxInstances() + ". Skipping."));
      return;
    }

    dependsOn = Stream.of(dependsOn)
                      .filter(id -> jobStorage.getJobSpec(id) != null)
                      .toList();

    FullSpec fullSpec = buildFullSpec(job, dependsOn);
    jobStorage.insertJobs(Collections.singletonList(fullSpec));

    scheduleJobs(Collections.singletonList(job));
    triggerOnSubmit(chain);
    notifyAll();
  }

  @WorkerThread
  synchronized void cancelJob(@NonNull String id) {
    Job runningJob = runningJobs.get(id);

    if (runningJob != null) {
      Log.w(TAG, JobLogger.format(runningJob, "Canceling while running."));
      runningJob.cancel();
    } else {
      JobSpec jobSpec = jobStorage.getJobSpec(id);

      if (jobSpec != null) {
        Job job = createJob(jobSpec, jobStorage.getConstraintSpecs(id));
        Log.w(TAG, JobLogger.format(job, "Canceling while inactive."));
        Log.w(TAG, JobLogger.format(job, "Job failed."));

        job.cancel();
        job.onFailure();
        onFailure(job);
      } else {
        Log.w(TAG, "Tried to cancel JOB::" + id + ", but it could not be found.");
      }
    }
  }

  @WorkerThread
  synchronized void onRetry(@NonNull Job job) {
    int    nextRunAttempt     = job.getRunAttempt() + 1;
    long   nextRunAttemptTime = calculateNextRunAttemptTime(System.currentTimeMillis(), nextRunAttempt, job.getParameters().getMaxBackoff());
    String serializedData     = dataSerializer.serialize(job.serialize());

    jobStorage.updateJobAfterRetry(job.getId(), false, nextRunAttempt, nextRunAttemptTime, serializedData);
    jobTracker.onStateChange(job, JobTracker.JobState.PENDING);

    List<Constraint> constraints = Stream.of(jobStorage.getConstraintSpecs(job.getId()))
                                         .map(ConstraintSpec::getFactoryKey)
                                         .map(constraintInstantiator::instantiate)
                                         .toList();


    long delay = Math.max(0, nextRunAttemptTime - System.currentTimeMillis());

    Log.i(TAG, JobLogger.format(job, "Scheduling a retry in " + delay + " ms."));
    scheduler.schedule(delay, constraints);

    notifyAll();
  }

  synchronized void onJobFinished(@NonNull Job job) {
    runningJobs.remove(job.getId());
  }

  @WorkerThread
  synchronized void onSuccess(@NonNull Job job) {
    jobStorage.deleteJob(job.getId());
    jobTracker.onStateChange(job, JobTracker.JobState.SUCCESS);
    notifyAll();
  }

  /**
   * @return The list of all dependent jobs that should also be failed.
   */
  @WorkerThread
  synchronized @NonNull List<Job> onFailure(@NonNull Job job) {
    List<Job> dependents = Stream.of(jobStorage.getDependencySpecsThatDependOnJob(job.getId()))
                                 .map(DependencySpec::getJobId)
                                 .map(jobStorage::getJobSpec)
                                 .withoutNulls()
                                 .map(jobSpec -> {
                                   List<ConstraintSpec> constraintSpecs = jobStorage.getConstraintSpecs(jobSpec.getId());
                                   return createJob(jobSpec, constraintSpecs);
                                 })
                                 .toList();

    List<Job> all = new ArrayList<>(dependents.size() + 1);
    all.add(job);
    all.addAll(dependents);

    jobStorage.deleteJobs(Stream.of(all).map(Job::getId).toList());
    Stream.of(all).forEach(j -> jobTracker.onStateChange(j, JobTracker.JobState.FAILURE));

    return dependents;
  }

  /**
   * Retrieves the next job that is eligible for execution. To be 'eligible' means that the job:
   *  - Has no dependencies
   *  - Has no unmet constraints
   *
   * This method will block until a job is available.
   * When the job returned from this method has been run, you must call {@link #onJobFinished(Job)}.
   */
  @WorkerThread
  synchronized @NonNull Job pullNextEligibleJobForExecution() {
    try {
      Job job;

      while ((job = getNextEligibleJobForExecution()) == null) {
        if (runningJobs.isEmpty()) {
          debouncer.publish(callback::onEmpty);
        }

        wait();
      }

      jobStorage.updateJobRunningState(job.getId(), true);
      runningJobs.put(job.getId(), job);
      jobTracker.onStateChange(job, JobTracker.JobState.RUNNING);

      return job;
    } catch (InterruptedException e) {
      Log.e(TAG, "Interrupted.");
      throw new AssertionError(e);
    }
  }

  /**
   * Retrieves a string representing the state of the job queue. Intended for debugging.
   */
  @WorkerThread
  synchronized @NonNull String getDebugInfo() {
    List<JobSpec>        jobs         = jobStorage.getAllJobSpecs();
    List<ConstraintSpec> constraints  = jobStorage.getAllConstraintSpecs();
    List<DependencySpec> dependencies = jobStorage.getAllDependencySpecs();

    StringBuilder info = new StringBuilder();

    info.append("-- Jobs\n");
    if (!jobs.isEmpty()) {
      Stream.of(jobs).forEach(j -> info.append(j.toString()).append('\n'));
    } else {
      info.append("None\n");
    }

    info.append("\n-- Constraints\n");
    if (!constraints.isEmpty()) {
      Stream.of(constraints).forEach(c -> info.append(c.toString()).append('\n'));
    } else {
      info.append("None\n");
    }

    info.append("\n-- Dependencies\n");
    if (!dependencies.isEmpty()) {
      Stream.of(dependencies).forEach(d -> info.append(d.toString()).append('\n'));
    } else {
      info.append("None\n");
    }

    return info.toString();
  }

  @WorkerThread
  private boolean chainExceedsMaximumInstances(@NonNull List<List<Job>> chain) {
    if (chain.size() == 1 && chain.get(0).size() == 1) {
      Job solo = chain.get(0).get(0);

      if (solo.getParameters().getMaxInstances() != Job.Parameters.UNLIMITED &&
          jobStorage.getJobInstanceCount(solo.getFactoryKey()) >= solo.getParameters().getMaxInstances())
      {
        return true;
      }
    }
    return false;
  }

  @WorkerThread
  private void triggerOnSubmit(@NonNull List<List<Job>> chain) {
    Stream.of(chain)
          .forEach(list -> Stream.of(list).forEach(job -> {
            job.setContext(application);
            job.onSubmit();
          }));
  }

  @WorkerThread
  private void insertJobChain(@NonNull List<List<Job>> chain) {
    List<FullSpec> fullSpecs = new LinkedList<>();
    List<String>   dependsOn = Collections.emptyList();

    for (List<Job> jobList : chain) {
      for (Job job : jobList) {
        fullSpecs.add(buildFullSpec(job, dependsOn));
      }
      dependsOn = Stream.of(jobList).map(Job::getId).toList();
    }

    jobStorage.insertJobs(fullSpecs);
  }

  @WorkerThread
  private @NonNull FullSpec buildFullSpec(@NonNull Job job, @NonNull Collection<String> dependsOn) {
    job.setRunAttempt(0);

    JobSpec jobSpec = new JobSpec(job.getId(),
                                  job.getFactoryKey(),
                                  job.getParameters().getQueue(),
                                  System.currentTimeMillis(),
                                  job.getNextRunAttemptTime(),
                                  job.getRunAttempt(),
                                  job.getParameters().getMaxAttempts(),
                                  job.getParameters().getMaxBackoff(),
                                  job.getParameters().getLifespan(),
                                  job.getParameters().getMaxInstances(),
                                  dataSerializer.serialize(job.serialize()),
                                  false);

    List<ConstraintSpec> constraintSpecs = Stream.of(job.getParameters().getConstraintKeys())
                                                 .map(key -> new ConstraintSpec(jobSpec.getId(), key))
                                                 .toList();

    List<DependencySpec> dependencySpecs = Stream.of(dependsOn)
                                                 .map(depends -> new DependencySpec(job.getId(), depends))
                                                 .toList();

    return new FullSpec(jobSpec, constraintSpecs, dependencySpecs);
  }

  @WorkerThread
  private void scheduleJobs(@NonNull List<Job> jobs) {
    for (Job job : jobs) {
      List<Constraint> constraints = Stream.of(job.getParameters().getConstraintKeys())
                                           .map(key -> new ConstraintSpec(job.getId(), key))
                                           .map(ConstraintSpec::getFactoryKey)
                                           .map(constraintInstantiator::instantiate)
                                           .toList();

      scheduler.schedule(0, constraints);
    }
  }

  @WorkerThread
  private @Nullable Job getNextEligibleJobForExecution() {
    List<JobSpec> jobSpecs = jobStorage.getPendingJobsWithNoDependenciesInCreatedOrder(System.currentTimeMillis());

    for (JobSpec jobSpec : jobSpecs) {
      List<ConstraintSpec> constraintSpecs = jobStorage.getConstraintSpecs(jobSpec.getId());
      List<Constraint>     constraints     = Stream.of(constraintSpecs)
                                                   .map(ConstraintSpec::getFactoryKey)
                                                   .map(constraintInstantiator::instantiate)
                                                   .toList();

      if (Stream.of(constraints).allMatch(Constraint::isMet)) {
        return createJob(jobSpec, constraintSpecs);
      }
    }

    return null;
  }

  private @NonNull Job createJob(@NonNull JobSpec jobSpec, @NonNull List<ConstraintSpec> constraintSpecs) {
    Job.Parameters parameters = buildJobParameters(jobSpec, constraintSpecs);

    try {
      Data data = dataSerializer.deserialize(jobSpec.getSerializedData());
      Job  job  = jobInstantiator.instantiate(jobSpec.getFactoryKey(), parameters, data);

      job.setRunAttempt(jobSpec.getRunAttempt());
      job.setNextRunAttemptTime(jobSpec.getNextRunAttemptTime());
      job.setContext(application);

      return job;
    } catch (RuntimeException e) {
      Log.e(TAG, "Failed to instantiate job! Failing it and its dependencies without calling Job#onFailure. Crash imminent.");

      List<String> failIds = Stream.of(jobStorage.getDependencySpecsThatDependOnJob(jobSpec.getId()))
                                   .map(DependencySpec::getJobId)
                                   .toList();

      jobStorage.deleteJob(jobSpec.getId());
      jobStorage.deleteJobs(failIds);

      Log.e(TAG, "Failed " + failIds.size() + " dependent jobs.");

      throw e;
    }
  }

  private @NonNull Job.Parameters buildJobParameters(@NonNull JobSpec jobSpec, @NonNull List<ConstraintSpec> constraintSpecs) {
    return new Job.Parameters.Builder(jobSpec.getId())
                  .setCreateTime(jobSpec.getCreateTime())
                  .setLifespan(jobSpec.getLifespan())
                  .setMaxAttempts(jobSpec.getMaxAttempts())
                  .setQueue(jobSpec.getQueueKey())
                  .setConstraints(Stream.of(constraintSpecs).map(ConstraintSpec::getFactoryKey).toList())
                  .setMaxBackoff(jobSpec.getMaxBackoff())
                  .build();
  }

  private long calculateNextRunAttemptTime(long currentTime, int nextAttempt, long maxBackoff) {
    int  boundedAttempt     = Math.min(nextAttempt, 30);
    long exponentialBackoff = (long) Math.pow(2, boundedAttempt) * 1000;
    long actualBackoff      = Math.min(exponentialBackoff, maxBackoff);

    return currentTime + actualBackoff;
  }

  interface Callback {
    void onEmpty();
  }
}
