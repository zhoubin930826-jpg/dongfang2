package com.example.houduan.repository;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndustryBaseResponseRepository extends JpaRepository<IndustryBaseResponseEntity, Long> {

    Optional<IndustryBaseResponseEntity> findTopByOrderByFetchedAtDesc();
}
