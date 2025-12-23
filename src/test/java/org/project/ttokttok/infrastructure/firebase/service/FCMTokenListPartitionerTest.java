package org.project.ttokttok.infrastructure.firebase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FCMTokenListPartitionerTest {

    private final String TOKEN_STRING = "token";

    private FCMTokenListPartitioner partitioner;

    @BeforeEach
    void setUp() {
        this.partitioner = new FCMTokenListPartitioner();
    }

    @Test
    @DisplayName("토큰 파티션을 나누어 반환하는데 성공한다.")
    void partitionListSuccess() {
        // given
        List<String> tokens = createTokenList(1540);

        // when
        List<List<String>> result = partitioner.partitionList(tokens);

        // then
        assertThat(result).hasSize(4); // 1540 / 500 = 3.08 -> 4개 파티션

        // 첫 번째 ~ 세 번째 파티션은 500개씩
        assertThat(result.get(0)).hasSize(500);
        assertThat(result.get(1)).hasSize(500);
        assertThat(result.get(2)).hasSize(500);

        // 마지막 파티션은 나머지 40개
        assertThat(result.get(3)).hasSize(40);

        // 전체 토큰 개수가 원래와 같은지 확인
        int totalTokens = result.stream()
                .mapToInt(List::size)
                .sum();

        assertThat(totalTokens).isEqualTo(1540);
    }

    @Test
    @DisplayName("빈 리스트인 경우 빈 파티션 리스트를 반환한다")
    void partitionEmptyListReturnsEmpty() {
        // given
        List<String> emptyTokens = List.of();

        // when
        List<List<String>> result = partitioner.partitionList(emptyTokens);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 리스트인 경우 NullPointerException이 발생한다")
    void partitionNullListThrowsNPE() {
        // given
        List<String> nullTokens = null;

        // when & then
        assertThatThrownBy(() -> partitioner.partitionList(nullTokens))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("단일 토큰인 경우 하나의 파티션이 생성된다")
    void partitionSingleTokenReturnsSinglePartition() {
        // given
        List<String> tokens = createTokenList(1);

        // when
        List<List<String>> result = partitioner.partitionList(tokens);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1);
        assertThat(result.get(0).get(0)).isEqualTo("token0");
    }

    @Test
    @DisplayName("토큰 개수가 배치 크기보다 적은 경우 하나의 파티션만 생성된다")
    void partitionSmallListReturnsSinglePartition() {
        // given
        List<String> tokens = createTokenList(300);

        // when
        List<List<String>> result = partitioner.partitionList(tokens);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(300);
        assertThat(result.get(0).get(0)).isEqualTo("token0");
        assertThat(result.get(0).get(299)).isEqualTo("token299");
    }

    @Test
    @DisplayName("토큰 개수가 정확히 배치 크기인 경우 하나의 파티션이 생성된다")
    void partitionExactBatchSizeReturnsSinglePartition() {
        // given
        List<String> tokens = createTokenList(500);

        // when
        List<List<String>> result = partitioner.partitionList(tokens);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(500);
    }

    @Test
    @DisplayName("토큰 개수가 배치 크기의 정확한 배수인 경우 나머지 없이 분할된다")
    void partitionExactMultipleOfBatchSize() {
        // given
        List<String> tokens = createTokenList(1500); // 500 * 3

        // when
        List<List<String>> result = partitioner.partitionList(tokens);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).hasSize(500);
        assertThat(result.get(1)).hasSize(500);
        assertThat(result.get(2)).hasSize(500);
    }

    // 테스트용 토큰 리스트를 생성하는 메서드
    private List<String> createTokenList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> TOKEN_STRING + i)
                .toList();
    }
}