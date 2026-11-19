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
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.metadatacenter.local.JsonMapper;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;

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
  private StatusLine statusLine;
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
    statusLine = mock(StatusLine.class);
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
    JsonNode expected = callbackFixture();
    assertEquals(names(expected), names(body), "the resource server refuses a body holding anything else");
    assertEquals(expected.get("eventUser"), body.get("eventUser"),
        "the resource server reads the user strictly, so it refuses any other property");
    for (String property : List.of("type", "clientId", "realmId", "userId")) {
      assertEquals(expected.at("/event/" + property), body.at("/event/" + property), property);
    }
  }

  /**
   * Keycloak finds the listener through the service file, by the class it names, and identifies it by
   * the factory's identifier, which the realm's event configuration names.
   */
  @Test
  void keycloakFindsTheFactoryThroughItsServiceFile() {
    List<EventListenerProviderFactory> factories = ServiceLoader.load(EventListenerProviderFactory.class).stream()
        .filter(provider -> provider.type().getName().startsWith("org.metadatacenter."))
        .map(ServiceLoader.Provider::get)
        .toList();

    assertEquals(1, factories.size(), factories::toString);
    assertEquals("CEDAR-event-listener", factories.get(0).getId());
  }

  /**
   * The body the resource server's login callback accepts: exactly {@code event} and {@code eventUser}.
   * The resource server reads the event tolerantly, since Keycloak defines its properties, and the user
   * strictly, as the four properties shown.
   */
  private static JsonNode callbackFixture() throws Exception {
    try (InputStream in = GenericEventListenerProviderTest.class.getResourceAsStream("/auth-user-callback.json")) {
      return JsonMapper.MAPPER.readTree(in);
    }
  }

  private static Set<String> names(JsonNode object) {
    Set<String> names = new TreeSet<>();
    object.fieldNames().forEachRemaining(names::add);
    return names;
  }

  @Test
  void nonMatchingEventDoesNotCallTheResourceServer() throws Exception {
    listener.onEvent(event(EventType.REGISTER, CLIENT_ID));
    listener.onEvent(event(EventType.LOGIN, "another-client"));

    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /** A login still succeeds when CEDAR refuses the callback, or cannot be reached; the failure is only logged. */
  @Test
  void aFailedCallbackDoesNotStopTheLogin() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(503);
    listener.onEvent(event(EventType.LOGIN, CLIENT_ID));

    when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(new java.io.IOException("connection refused"));
    listener.onEvent(event(EventType.LOGIN, CLIENT_ID));

    verify(httpClient, times(2)).execute(any(HttpUriRequest.class));
  }

  @Test
  void administrativeEventsAreIgnored() throws Exception {
    listener.onEvent(new org.keycloak.events.admin.AdminEvent(), true);
    listener.close();

    verify(httpClient, never()).execute(any(HttpUriRequest.class));
  }

  /**
   * Keycloak builds the listener through its factory, which takes the resource server's address and the
   * administrator's API key from the environment, and answers logins to CEDAR's web client.
   */
  @Test
  void theFactoryBuildsAListenerFromTheEnvironment() throws Exception {
    java.util.Map<String, String> environment = java.util.Map.of("CEDAR_RESOURCE_SERVER_HOST", "resource",
        "CEDAR_RESOURCE_HTTP_PORT", "9007", "CEDAR_ADMIN_USER_API_KEY", API_KEY);
    GenericEventListenerProviderFactory factory = new GenericEventListenerProviderFactory() {
      @Override
      protected String environment(String name) {
        return environment.get(name);
      }
    };
    factory.init(null);
    factory.postInit(null);
    assertEquals("CEDAR-event-listener", factory.getId());

    factory.create(session).onEvent(event(EventType.LOGIN, CLIENT_ID));
    factory.close();

    ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
    verify(httpClient, times(1)).execute(requestCaptor.capture());
    assertEquals(CALLBACK_URL, requestCaptor.getValue().getURI().toString());
    assertEquals("apiKey " + API_KEY, requestCaptor.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
  }

  @Test
  void aFactoryWithoutTheResourceServersAddressStillStarts() {
    GenericEventListenerProviderFactory factory = new GenericEventListenerProviderFactory() {
      @Override
      protected String environment(String name) {
        return null;
      }
    };
    factory.init(null);
    assertTrue(factory.create(session) instanceof GenericEventListenerProvider);
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
