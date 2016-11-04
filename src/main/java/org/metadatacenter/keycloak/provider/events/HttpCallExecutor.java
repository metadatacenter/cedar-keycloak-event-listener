package org.metadatacenter.keycloak.provider.events;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.metadatacenter.constant.HttpConnectionConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

public abstract class HttpCallExecutor {

  private HttpCallExecutor() {
  }

  public static int post(String url, String apiKey, Map<String, Object> map) {
    try {
      String bodyString = JsonMapper.MAPPER.writeValueAsString(map);
      Request proxyRequest = Request.Post(url)
          .bodyString(bodyString, ContentType.APPLICATION_JSON)
          .connectTimeout(HttpConnectionConstants.CONNECTION_TIMEOUT)
          .socketTimeout(HttpConnectionConstants.SOCKET_TIMEOUT);
      proxyRequest.addHeader(HttpHeaders.AUTHORIZATION, HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX + apiKey);
      HttpResponse httpResponse = proxyRequest.execute().returnResponse();
      return httpResponse.getStatusLine().getStatusCode();
    } catch (IOException e) {
      e.printStackTrace();
      //TODO : put a constant here
      return 500;
    }
  }
}
