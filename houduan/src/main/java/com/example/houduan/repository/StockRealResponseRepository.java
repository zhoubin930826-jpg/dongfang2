package com.example.houduan.repository;

import com.example.houduan.entity.StockRealResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRealResponseRepository extends JpaRepository<StockRealResponseEntity, Long> {

    Optional<StockRealResponseEntity> findTopByStockCodeOrderByFetchedAtDesc(String stockCode);
}
