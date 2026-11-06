package com.mapa.domain.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class IntegerMatrixConverter implements AttributeConverter<List<List<Integer>>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<List<Integer>> attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException jsonException) {
            throw new IllegalStateException("Não foi possível serializar a grade do mapa ECU", jsonException);
        }
    }

    @Override
    public List<List<Integer>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<List<Integer>>>() { });
        } catch (JsonProcessingException jsonException) {
            throw new IllegalStateException("Não foi possível interpretar a grade do mapa ECU", jsonException);
        }
    }
}
