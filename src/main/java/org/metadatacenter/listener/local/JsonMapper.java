package org.metadatacenter.listener.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

//TODO: This is duplicated code. Handle this
public final class JsonMapper {
  public static final ObjectMapper MAPPER;
  public static final ObjectMapper PRETTY_MAPPER;

  private JsonMapper() {
  }

  static {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(CedarConstants.xsdDateTimeFormatter));
    javaTimeModule.addDeserializer(LocalDateTime.class,
        new LocalDateTimeDeserializer(CedarConstants.xsdDateTimeFormatter));
    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new JavaTimeModule());
    MAPPER.registerModule(javaTimeModule);
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    PRETTY_MAPPER = new ObjectMapper();
    PRETTY_MAPPER.registerModule(new JavaTimeModule());
    PRETTY_MAPPER.registerModule(javaTimeModule);
    PRETTY_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    PRETTY_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
  }
}

