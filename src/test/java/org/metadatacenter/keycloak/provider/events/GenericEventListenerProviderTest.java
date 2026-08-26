package org.metadatacenter.keycloak.provider.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.metadatacenter.local.JsonMapper;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericEventListenerProviderTest {

  private static final String CALLBACK_URL = "http://resource:9007/command/auth-user-callback";
  private static final String API_KEY = "admin-api-key";
  private static final String CLIENT_ID = "cedar-angular-app";

  private KeycloakSession session;
  private CloseableHttpClient httpClient;
  private GenericEventListenerProvider listener;

  @BeforeEach
  void setUp() throws Exception {
    session = mock(KeycloakSession.class);

    RealmProvider realmProvider = mock(RealmProvider.class);
    RealmModel realm = mock(RealmModel.class);
    when(session.realms()).thenReturn(realmProvider);
    when(realmProvider.getRealm("realm-id")).thenReturn(realm);

    UserProvider userProvider = mock(UserProvider.class);
    UserModel user = mock(UserModel.class);
    when(session.users()).thenReturn(userProvider);
    when(userProvider.getUserById(realm, "user-id")).thenReturn(user);
    when(user.getId()).thenReturn("user-id");
    when(user.getFirstName()).thenReturn("Ada");
    when(user.getLastName()).thenReturn("Lovelace");
    when(user.getEmail()).thenReturn("ada@example.org");

    HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
    httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    StatusLine statusLine = mock(StatusLine.class);
    when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
    when(httpClientProvider.getHttpClient()).thenReturn(httpClient);
    when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
    when(response.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(201);

    listener = new GenericEventListenerProvider(
        session,
        Collections.singleton(EventType.LOGIN),
        CALLBACK_URL,
        Collections.emptySet(),
        null,
        "https://metadatacenter.org/users/",
        API_KEY,
        CLIENT_ID);
  }

  @Test
  void matchingLoginPostsOneProvisioningCallbackWithExpectedRequest() throws Exception {
    listener.onEvent(event(EventType.LOGIN, CLIENT_ID));

    ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
    verify(httpClient, times(1)).execute(requestCaptor.capture());

    HttpPost request = (HttpPost) requestCaptor.getValue();
    assertEquals(CALLBACK_URL, request.getURI().toString());
    assertEquals("apiKey " + API_KEY, request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    assertTrue(request.getEntity().getContentType().getValue().startsWith("application/json"));

    JsonNode body = JsonMapper.MAPPER.readTree(EntityUtils.toString(request.getEntity()));
    assertEquals("LOGIN", body.at("/event/type").asText());
    assertEquals(CLIENT_ID, body.at("/event/clientId").asText());
    assertEquals("https://metadatacenter.org/users/user-id", body.at("/eventUser/@id").asText());
    assertEquals("Ada", body.at("/eventUser/firstName").asText());
    assertEquals("Lovelace", body.at("/eventUser/lastName").asText());
    assertEquals("ada@example.org", body.at("/eventUser/email").asText());
  }

  @Test
  void nonMatchingEventDoesNotCallTheResourceServer() throws Exception {
    listener.onEvent(event(EventType.REGISTER, CLIENT_ID));
    listener.onEvent(event(EventType.LOGIN, "another-client"));

    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  private static Event event(EventType type, String clientId) {
    Event event = new Event();
    event.setType(type);
    event.setClientId(clientId);
    event.setRealmId("realm-id");
    event.setUserId("user-id");
    return event;
  }
}
