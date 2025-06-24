package org.metadatacenter.keycloak.provider.events;


import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;

public class HttpClientFactory {

  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int SOCKET_TIMEOUT_MS = 20_000;
  private static final int CONNECTION_REQUEST_TIMEOUT_MS = 5_000;
  private static final int VALIDATE_AFTER_INACTIVITY_MS = 1_000;

  private static final PoolingHttpClientConnectionManager connManager;
  private static final CloseableHttpClient httpClient;
  private static final Executor executor;

  static {
    // 1) Pooling + stale‐validation
    connManager = new PoolingHttpClientConnectionManager();
    connManager.setMaxTotal(50);
    connManager.setDefaultMaxPerRoute(10);
    connManager.setValidateAfterInactivity(Timeout.ofMilliseconds(VALIDATE_AFTER_INACTIVITY_MS));

    // 2) Timeouts
    RequestConfig defaultConfig = RequestConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT_MS))
        .setResponseTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT_MS))
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECTION_REQUEST_TIMEOUT_MS))
        .build();

    // 3) Build the shared client
    httpClient = HttpClients.custom()
        .setConnectionManager(connManager)
        .setDefaultRequestConfig(defaultConfig)
        .build();

    // 4) Prepare the fluent Executor
    executor = Executor.newInstance(httpClient);
  }

  public static Executor executor() {
    return executor;
  }

  public static CloseableHttpClient client() {
    return httpClient;
  }

  private HttpClientFactory() {
  }
}
