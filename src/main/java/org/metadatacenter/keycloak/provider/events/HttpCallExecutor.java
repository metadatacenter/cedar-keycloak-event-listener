package org.metadatacenter.keycloak.provider.events;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.metadatacenter.local.JsonMapper;

import java.util.Map;

import static org.metadatacenter.local.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;
import static org.metadatacenter.local.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public final class HttpCallExecutor {

  private HttpCallExecutor() {}

  public static int post(KeycloakSession session,
                         String url,
                         String apiKey,
                         Map<String, Object> payload) {

    try {
      String body = JsonMapper.MAPPER.writeValueAsString(payload);

      HttpPost post = new HttpPost(url);
      post.setHeader(HTTP_HEADER_AUTHORIZATION,
          HTTP_AUTH_HEADER_APIKEY_PREFIX + apiKey);
      post.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

      HttpClientProvider provider =
          session.getProvider(HttpClientProvider.class);

      HttpResponse response = provider.getHttpClient().execute(post);
      EntityUtils.consumeQuietly(response.getEntity());

      return response.getStatusLine().getStatusCode();

    } catch (Exception e) {
      e.printStackTrace();
      return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
  }
}
