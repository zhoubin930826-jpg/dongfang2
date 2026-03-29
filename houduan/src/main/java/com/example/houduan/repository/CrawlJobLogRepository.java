package com.example.houduan.repository;

import com.example.houduan.entity.CrawlJobLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawlJobLogRepository extends JpaRepository<CrawlJobLogEntity, Long> {

    Optional<CrawlJobLogEntity> findTopByJobNameAndSuccessOrderByStartedAtDesc(
        String jobName,
        boolean success
    );

    Optional<CrawlJobLogEntity> findTopByJobNameAndTargetKeyAndSuccessOrderByStartedAtDesc(
        String jobName,
        String targetKey,
        boolean success
    );
}
