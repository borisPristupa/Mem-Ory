package com.boris.study.memory.data.converter;

import org.json.JSONObject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class StateConverter implements AttributeConverter<JSONObject, String> {
    @Override
    public String convertToDatabaseColumn(JSONObject jsonObject) {
        return jsonObject.toString();
    }

    @Override
    public JSONObject convertToEntityAttribute(String s) {
        return new JSONObject(s);
    }
}
