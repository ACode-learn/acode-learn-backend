package gr.alexc.acodelearn.course.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SectionContentConverter implements AttributeConverter<SectionContent, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(SectionContent attribute) {
        try {
            SectionContent content = attribute == null ? SectionContent.empty() : attribute;
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not serialize section content", ex);
        }
    }

    @Override
    public SectionContent convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return SectionContent.empty();
            }
            return objectMapper.readValue(dbData, SectionContent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Could not deserialize section content", ex);
        }
    }
}
