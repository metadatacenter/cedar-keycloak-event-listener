package org.metadatacenter.keycloak.provider.events;

// 5.x Fluent API
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;

// JSON mapper
import org.metadatacenter.local.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.metadatacenter.local.HttpConnectionConstants.CONNECTION_TIMEOUT;
import static org.metadatacenter.local.HttpConnectionConstants.SOCKET_TIMEOUT;
import static org.metadatacenter.local.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;
import static org.metadatacenter.local.HttpConstants.HTTP_HEADER_AUTHORIZATION;


public abstract class HttpCallExecutor {

  private HttpCallExecutor() { }

  public static int post(String url, String apiKey, Map<String, Object> map) {
    try {
      String bodyString = JsonMapper.MAPPER.writeValueAsString(map);

      // 1) Build the 5.x Fluent Request
      Request request = Request.post(url)
          .bodyString(bodyString, ContentType.APPLICATION_JSON)
          .connectTimeout(Timeout.ofMilliseconds(CONNECTION_TIMEOUT))
          .responseTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT));
      request.addHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_APIKEY_PREFIX + apiKey);

      // 2) Execute with your pooled Executor
      Executor executor = HttpClientFactory.executor();
      HttpResponse response = executor.execute(request).returnResponse();
      return response.getCode();
    } catch (IOException e) {
      e.printStackTrace();
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }
}
