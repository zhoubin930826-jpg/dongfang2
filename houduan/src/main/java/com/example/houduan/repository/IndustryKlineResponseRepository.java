package com.example.houduan.repository;

import com.example.houduan.entity.IndustryKlineResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndustryKlineResponseRepository extends JpaRepository<IndustryKlineResponseEntity, Long> {

    Optional<IndustryKlineResponseEntity> findTopByIndustryCodeOrderByFetchedAtDesc(String industryCode);
}
