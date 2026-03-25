package com.example.houduan.repository;

import com.example.houduan.entity.StockPoolResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockPoolResponseRepository extends JpaRepository<StockPoolResponseEntity, Long> {

    Optional<StockPoolResponseEntity> findTopByPageNoOrderByFetchedAtDesc(Integer pageNo);
}
