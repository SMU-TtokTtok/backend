package org.project.ttokttok.infrastructure.firebase.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class FCMTokenListPartitioner {

    private static final int TOKEN_BATCH_MAX_SIZE = 500;

    public List<List<String>> partitionList(List<String> tokens) {
        return IntStream.range(0, partitionListRange(tokens.size()))
                .mapToObj(i -> getTokenPartition(tokens, i))
                .collect(toList());
    }

    // 토큰 파티션 개수 계산 로직
    private int partitionListRange(int listSize) {
        return (listSize + TOKEN_BATCH_MAX_SIZE - 1) / TOKEN_BATCH_MAX_SIZE;
    }

    // 토큰 파티션 분할 로직
    private List<String> getTokenPartition(List<String> tokens, int index) {
        return tokens.subList(
                index * TOKEN_BATCH_MAX_SIZE,
                Math.min((index + 1) * TOKEN_BATCH_MAX_SIZE, tokens.size())
        );
    }
}
