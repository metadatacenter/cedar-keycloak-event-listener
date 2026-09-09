package org.metadatacenter.local;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class JsonMapperTest {

  @Test
  public void javaTimeValuesAreWrittenAsIsoStrings() throws Exception {
    Assertions.assertEquals("\"2026-09-02T10:30:15\"",
        JsonMapper.MAPPER.writeValueAsString(LocalDateTime.of(2026, 9, 2, 10, 30, 15)));
    Assertions.assertEquals("\"2026-09-02T10:30:15-07:00\"",
        JsonMapper.MAPPER.writeValueAsString(OffsetDateTime.of(2026, 9, 2, 10, 30, 15, 0, ZoneOffset.ofHours(-7))));
  }

  @Test
  public void javaTimeValuesReadBack() throws Exception {
    Assertions.assertEquals(LocalDateTime.of(2026, 9, 2, 10, 30, 15),
        JsonMapper.MAPPER.readValue("\"2026-09-02T10:30:15\"", LocalDateTime.class));
  }
}
