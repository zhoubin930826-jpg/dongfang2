package com.example.houduan.repository;

import com.example.houduan.entity.CrawlJobLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobLogRepository extends JpaRepository<CrawlJobLogEntity, Long> {
}
