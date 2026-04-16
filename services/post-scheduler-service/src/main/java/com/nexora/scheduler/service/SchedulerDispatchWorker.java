package com.nexora.scheduler.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerDispatchWorker {

  private final PostSchedulerService postSchedulerService;

  public SchedulerDispatchWorker(PostSchedulerService postSchedulerService) {
    this.postSchedulerService = postSchedulerService;
  }

  @Scheduled(fixedDelayString = "${nexora.scheduler.dispatch.poll-interval-ms:15000}")
  public void dispatchDueJobs() {
    postSchedulerService.dispatchDueJobs();
  }
}
