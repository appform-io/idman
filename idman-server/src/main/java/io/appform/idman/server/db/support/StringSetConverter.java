package io.appform.idman.server.db.support;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Set;

/**
 *
 */
@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
    private static final String SPLIT_CHAR = ",";

    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        return Joiner.on(SPLIT_CHAR).join(attribute);
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        return Sets.newHashSet(Splitter.on(SPLIT_CHAR).split(dbData));
    }
}
