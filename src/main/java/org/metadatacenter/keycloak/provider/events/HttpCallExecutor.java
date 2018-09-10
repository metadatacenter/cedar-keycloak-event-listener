package org.metadatacenter.keycloak.provider.events;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import static org.metadatacenter.constant.HttpConnectionConstants.CONNECTION_TIMEOUT;
import static org.metadatacenter.constant.HttpConnectionConstants.SOCKET_TIMEOUT;
import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public abstract class HttpCallExecutor {

  private HttpCallExecutor() {
  }

  public static int post(String url, String apiKey, Map<String, Object> map) {
    try {
      String bodyString = JsonMapper.MAPPER.writeValueAsString(map);
      Request proxyRequest = Request.Post(url)
          .bodyString(bodyString, ContentType.APPLICATION_JSON)
          .connectTimeout(CONNECTION_TIMEOUT)
          .socketTimeout(SOCKET_TIMEOUT);
      proxyRequest.addHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_APIKEY_PREFIX + apiKey);
      HttpResponse httpResponse = proxyRequest.execute().returnResponse();
      return httpResponse.getStatusLine().getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }
}
