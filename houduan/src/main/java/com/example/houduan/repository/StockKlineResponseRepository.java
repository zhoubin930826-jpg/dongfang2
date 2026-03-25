package com.example.houduan.repository;

import com.example.houduan.entity.StockKlineResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockKlineResponseRepository extends JpaRepository<StockKlineResponseEntity, Long> {

    Optional<StockKlineResponseEntity> findTopByStockCodeOrderByFetchedAtDesc(String stockCode);
}
