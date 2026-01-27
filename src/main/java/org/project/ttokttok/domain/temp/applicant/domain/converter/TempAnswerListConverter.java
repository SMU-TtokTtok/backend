package org.project.ttokttok.domain.temp.applicant.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import java.util.Map;
import org.project.ttokttok.domain.temp.applicant.controller.dto.request.TempAnswer;

@Converter
public class TempAnswerListConverter implements AttributeConverter<Map<String, Object>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting map to JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            Map<String, Object> result = dbData == null ? null : objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});

            if (result != null && result.containsKey("answers")) {
                // 강제 캐스팅을 피하기 위해 convertValue로 타입을 보정한다.
                List<TempAnswer> answers = objectMapper.convertValue(
                        result.get("answers"), new TypeReference<List<TempAnswer>>() {});
                result.put("answers", answers);
            }

            return result;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to map", e);
        }
    }
}