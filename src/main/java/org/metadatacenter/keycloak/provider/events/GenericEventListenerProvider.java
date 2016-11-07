package org.metadatacenter.keycloak.provider.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenericEventListenerProvider implements EventListenerProvider {

  protected static final String EVENT = "event";
  protected static final String EVENT_USER = "eventUser";

  private final KeycloakSession session;

  private final Set<EventType> userEventList;
  private final String userEventCallbackURL;
  private final Set<ResourceType> adminResourceList;
  private final String adminResourceCallbackURL;

  private final String apiKey;
  private final String clientId;

  public GenericEventListenerProvider(KeycloakSession session, Set<EventType> userEventList,
                                      String userEventCallbackURL, Set<ResourceType> adminResourceList, String
                                          adminResourceCallbackURL, String apiKey, String clientId) {
    this.session = session;
    this.userEventList = userEventList;
    this.userEventCallbackURL = userEventCallbackURL;
    this.adminResourceList = adminResourceList;
    this.adminResourceCallbackURL = adminResourceCallbackURL;
    this.apiKey = apiKey;
    this.clientId = clientId;
  }

  @Override
  public void onEvent(Event event) {
    if (userEventList.contains(event.getType())) {
      RealmModel realm = session.realms().getRealm(event.getRealmId());
      UserModel user = session.users().getUserById(event.getUserId(), realm);
      performCall(userEventCallbackURL, apiKey, event, user);
    }
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    if (adminResourceList.contains(event.getResourceType())) {
      RealmModel realm = session.realms().getRealm(event.getRealmId());
      String representation = event.getRepresentation();
      JsonNode representationNode = null;
      try {
        representationNode = JsonMapper.MAPPER.readTree(representation);
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (representationNode != null) {
        String userId = representationNode.get("id").asText();
        UserModel user = session.users().getUserById(userId, realm);
        performCall(adminResourceCallbackURL, apiKey, event, user);
      }
    }
  }

  private void performCall(String url, String apiKey, Event event, UserModel user) {
    if (clientId.equals(event.getClientId())) {
      Map<String, Object> map = new HashMap<>();
      map.put(EVENT, JsonMapper.MAPPER.valueToTree(event));
      map.put(EVENT_USER, userToMap(user));
      HttpCallExecutor.post(url, apiKey, map);
    }
  }

  private void performCall(String url, String apiKey, AdminEvent event, UserModel user) {
    Map<String, Object> map = new HashMap<>();
    map.put(EVENT, JsonMapper.MAPPER.valueToTree(event));
    map.put(EVENT_USER, userToMap(user));
    HttpCallExecutor.post(url, apiKey, map);
  }

  private Map<String, Object> userToMap(UserModel user) {
    Map<String, Object> m = new HashMap<>();
    m.put("id", user.getId());
    m.put("firstName", user.getFirstName());
    m.put("lastName", user.getLastName());
    m.put("email", user.getEmail());
    return m;
  }

  @Override
  public void close() {
  }

}
