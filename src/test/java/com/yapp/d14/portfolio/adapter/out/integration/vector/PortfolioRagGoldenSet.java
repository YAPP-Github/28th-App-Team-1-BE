package com.yapp.d14.portfolio.adapter.out.integration.vector;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record PortfolioRagGoldenSet(String label, List<Passage> passages, List<Query> queries) {

    record Passage(String id, String text) {
    }

    record Query(String text, List<String> relevantPassageIds) {
    }

    String joinedPortfolioText() {
        return passages.stream()
                .map(Passage::text)
                .collect(Collectors.joining("\n\n"));
    }

    Map<String, String> passageTextsById() {
        return passages.stream()
                .collect(Collectors.toMap(Passage::id, Passage::text));
    }

    static PortfolioRagGoldenSet fromClasspath(String resourcePath) {
        try (InputStream in = PortfolioRagGoldenSet.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("골든셋 리소스를 찾을 수 없습니다: " + resourcePath);
            }
            return new ObjectMapper().readValue(in, PortfolioRagGoldenSet.class);
        } catch (IOException e) {
            throw new IllegalStateException("골든셋 리소스를 읽지 못했습니다: " + resourcePath, e);
        }
    }

    static PortfolioRagGoldenSet fromFile(Path path) {
        try {
            return new ObjectMapper().readValue(Files.readString(path), PortfolioRagGoldenSet.class);
        } catch (IOException e) {
            throw new IllegalStateException("골든셋 파일을 읽지 못했습니다: " + path, e);
        }
    }
}
