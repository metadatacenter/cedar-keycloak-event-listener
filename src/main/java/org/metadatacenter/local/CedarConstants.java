package org.metadatacenter.local;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CedarConstants {
  public static final DateTimeFormatter xsdDateTimeFormatter = DateTimeFormatter.
      ofPattern("uuuu-MM-dd'T'HH:mm:ssZZZZZ").withZone(ZoneId.systemDefault());

  private CedarConstants() {
  }
}

