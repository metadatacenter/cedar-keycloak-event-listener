package org.metadatacenter.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public final class JsonMapper {

  private JsonMapper() {
  }

  public static final ObjectMapper MAPPER;

  static {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class,
        new LocalDateTimeSerializer(CedarConstants.xsdDateTimeFormatter));
    javaTimeModule.addDeserializer(LocalDateTime.class,
        new LocalDateTimeDeserializer(CedarConstants.xsdDateTimeFormatter));

    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new JavaTimeModule());
    MAPPER.registerModule(javaTimeModule);
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    // Do not use, infinite loop MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
  }
}

