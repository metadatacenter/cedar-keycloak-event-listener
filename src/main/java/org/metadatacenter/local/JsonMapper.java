package org.metadatacenter.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The mapper the listener serializes Keycloak events with. It handles {@code java.time} values through
 * one {@link JavaTimeModule}; Jackson ignores a second module with the same type id, so a customised
 * copy registered after a stock one would never take effect.
 */
public final class JsonMapper {

  private JsonMapper() {
  }

  public static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new JavaTimeModule());
    MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    // Do not use, infinite loop MAPPER.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
  }
}
