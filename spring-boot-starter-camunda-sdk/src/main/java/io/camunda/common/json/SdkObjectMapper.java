/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.camunda.common.exception.SdkException;
import java.io.IOException;
import java.util.Map;

public class SdkObjectMapper implements JsonMapper {

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
      new TypeReference<>() {};

  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE_REFERENCE =
      new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  public SdkObjectMapper() {
    this(new ObjectMapper());
  }

  public SdkObjectMapper(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.objectMapper
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public <T> T fromJson(final String json, final Class<T> typeClass) {
    try {
      return objectMapper.readValue(json, typeClass);
    } catch (final IOException e) {
      throw new SdkException(
          String.format("Failed to deserialize json '%s' to class '%s'", json, typeClass), e);
    }
  }

  @Override
  public <T, U> T fromJson(
      final String json, final Class<T> resultType, final Class<U> parameterType) {
    try {
      final JavaType javaType =
          objectMapper.getTypeFactory().constructParametricType(resultType, parameterType);
      return objectMapper.readValue(json, javaType);
    } catch (final IOException e) {
      throw new SdkException(
          String.format(
              "Failed to deserialize json '%s' to class '%s' with parameter '%s",
              json, resultType, parameterType),
          e);
    }
  }

  @Override
  public Map<String, Object> fromJsonAsMap(final String json) {
    try {
      return objectMapper.readValue(json, MAP_TYPE_REFERENCE);
    } catch (final IOException e) {
      throw new SdkException(
          String.format("Failed to deserialize json '%s' to 'Map<String, Object>'", json), e);
    }
  }

  @Override
  public Map<String, String> fromJsonAsStringMap(final String json) {
    try {
      return objectMapper.readValue(json, STRING_MAP_TYPE_REFERENCE);
    } catch (final IOException e) {
      throw new SdkException(
          String.format("Failed to deserialize json '%s' to 'Map<String, String>'", json), e);
    }
  }

  @Override
  public String toJson(final Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (final JsonProcessingException e) {
      throw new SdkException(String.format("Failed to serialize object '%s' to json", value), e);
    }
  }
}
