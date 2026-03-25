package com.example.houduan.service;

import com.example.houduan.entity.IndustryBaseResponseEntity;
import com.example.houduan.entity.IndustryKlineResponseEntity;
import com.example.houduan.entity.StockKlineResponseEntity;
import com.example.houduan.entity.StockPoolResponseEntity;
import com.example.houduan.entity.StockRealResponseEntity;
import com.example.houduan.repository.IndustryBaseResponseRepository;
import com.example.houduan.repository.IndustryKlineResponseRepository;
import com.example.houduan.repository.StockKlineResponseRepository;
import com.example.houduan.repository.StockPoolResponseRepository;
import com.example.houduan.repository.StockRealResponseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class EastMoneyStorageService {

    private final ObjectMapper objectMapper;
    private final IndustryBaseResponseRepository industryBaseResponseRepository;
    private final StockPoolResponseRepository stockPoolResponseRepository;
    private final IndustryKlineResponseRepository industryKlineResponseRepository;
    private final StockRealResponseRepository stockRealResponseRepository;
    private final StockKlineResponseRepository stockKlineResponseRepository;

    public EastMoneyStorageService(
        ObjectMapper objectMapper,
        IndustryBaseResponseRepository industryBaseResponseRepository,
        StockPoolResponseRepository stockPoolResponseRepository,
        IndustryKlineResponseRepository industryKlineResponseRepository,
        StockRealResponseRepository stockRealResponseRepository,
        StockKlineResponseRepository stockKlineResponseRepository
    ) {
        this.objectMapper = objectMapper;
        this.industryBaseResponseRepository = industryBaseResponseRepository;
        this.stockPoolResponseRepository = stockPoolResponseRepository;
        this.industryKlineResponseRepository = industryKlineResponseRepository;
        this.stockRealResponseRepository = stockRealResponseRepository;
        this.stockKlineResponseRepository = stockKlineResponseRepository;
    }

    public IndustryBaseResponseEntity saveIndustryBase(String rawJson) {
        IndustryBaseResponseEntity entity = new IndustryBaseResponseEntity();
        entity.setFetchedAt(LocalDateTime.now());
        entity.setItemCount(extractArrayCount(rawJson, "data", "diff"));
        entity.setRawJson(rawJson);
        return industryBaseResponseRepository.save(entity);
    }

    public StockPoolResponseEntity saveStockPool(int pageNo, String rawJson) {
        StockPoolResponseEntity entity = new StockPoolResponseEntity();
        entity.setPageNo(pageNo);
        entity.setFetchedAt(LocalDateTime.now());
        entity.setItemCount(extractArrayCount(rawJson, "data", "diff"));
        entity.setRawJson(rawJson);
        return stockPoolResponseRepository.save(entity);
    }

    public IndustryKlineResponseEntity saveIndustryKline(String industryCode, String rawJson) {
        IndustryKlineResponseEntity entity = new IndustryKlineResponseEntity();
        entity.setIndustryCode(industryCode);
        entity.setFetchedAt(LocalDateTime.now());
        entity.setRawJson(rawJson);
        return industryKlineResponseRepository.save(entity);
    }

    public StockRealResponseEntity saveStockReal(String stockCode, String rawJson) {
        StockRealResponseEntity entity = new StockRealResponseEntity();
        entity.setStockCode(stockCode);
        entity.setFetchedAt(LocalDateTime.now());
        entity.setRawJson(rawJson);
        return stockRealResponseRepository.save(entity);
    }

    public StockKlineResponseEntity saveStockKline(String stockCode, String rawJson) {
        StockKlineResponseEntity entity = new StockKlineResponseEntity();
        entity.setStockCode(stockCode);
        entity.setFetchedAt(LocalDateTime.now());
        entity.setRawJson(rawJson);
        return stockKlineResponseRepository.save(entity);
    }

    public Optional<IndustryBaseResponseEntity> findLatestIndustryBase() {
        return industryBaseResponseRepository.findTopByOrderByFetchedAtDesc();
    }

    public Optional<StockPoolResponseEntity> findLatestStockPool(int pageNo) {
        return stockPoolResponseRepository.findTopByPageNoOrderByFetchedAtDesc(pageNo);
    }

    public Optional<IndustryKlineResponseEntity> findLatestIndustryKline(String industryCode) {
        return industryKlineResponseRepository.findTopByIndustryCodeOrderByFetchedAtDesc(industryCode);
    }

    public Optional<StockRealResponseEntity> findLatestStockReal(String stockCode) {
        return stockRealResponseRepository.findTopByStockCodeOrderByFetchedAtDesc(stockCode);
    }

    public Optional<StockKlineResponseEntity> findLatestStockKline(String stockCode) {
        return stockKlineResponseRepository.findTopByStockCodeOrderByFetchedAtDesc(stockCode);
    }

    public List<String> extractIndustryCodes(String rawJson) {
        JsonNode diffNode = readPath(rawJson, "data", "diff");
        if (diffNode == null || !diffNode.isArray()) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        for (JsonNode item : diffNode) {
            JsonNode codeNode = item.get("f12");
            if (codeNode != null && !codeNode.asText().isBlank()) {
                codes.add(codeNode.asText());
            }
        }
        return codes;
    }

    public List<String> extractStockCodes(String rawJson) {
        JsonNode diffNode = readPath(rawJson, "data", "diff");
        if (diffNode == null) {
            return Collections.emptyList();
        }
        List<String> codes = new ArrayList<>();
        if (diffNode.isArray()) {
            for (JsonNode item : diffNode) {
                addStockCode(codes, item);
            }
        } else if (diffNode.isObject()) {
            diffNode.elements().forEachRemaining(item -> addStockCode(codes, item));
        }
        return codes;
    }

    private void addStockCode(List<String> codes, JsonNode item) {
        JsonNode codeNode = item.get("f12");
        if (codeNode != null && !codeNode.asText().isBlank()) {
            codes.add(codeNode.asText());
        }
    }

    private int extractArrayCount(String rawJson, String... path) {
        JsonNode node = readPath(rawJson, path);
        if (node == null) {
            return 0;
        }
        if (node.isArray() || node.isObject()) {
            return node.size();
        }
        return 0;
    }

    private JsonNode readPath(String rawJson, String... path) {
        try {
            JsonNode current = objectMapper.readTree(rawJson);
            for (String part : path) {
                if (current == null) {
                    return null;
                }
                current = current.get(part);
            }
            return current;
        } catch (IOException e) {
            return null;
        }
    }
}
