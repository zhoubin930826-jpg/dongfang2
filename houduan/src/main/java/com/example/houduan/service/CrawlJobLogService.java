package com.example.houduan.service;

import com.example.houduan.entity.CrawlJobLogEntity;
import com.example.houduan.repository.CrawlJobLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CrawlJobLogService {

    private final CrawlJobLogRepository crawlJobLogRepository;

    public CrawlJobLogService(CrawlJobLogRepository crawlJobLogRepository) {
        this.crawlJobLogRepository = crawlJobLogRepository;
    }

    public void log(String jobName, String targetKey, LocalDateTime startedAt, boolean success, Integer recordCount, String message) {
        CrawlJobLogEntity entity = new CrawlJobLogEntity();
        entity.setJobName(jobName);
        entity.setTargetKey(targetKey);
        entity.setStartedAt(startedAt);
        entity.setFinishedAt(LocalDateTime.now());
        entity.setSuccess(success);
        entity.setRecordCount(recordCount);
        entity.setMessage(message);
        crawlJobLogRepository.save(entity);
    }
}
