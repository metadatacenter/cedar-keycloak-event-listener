package org.metadatacenter.keycloak.provider.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.metadatacenter.local.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GenericEventListenerProvider implements EventListenerProvider {

  private static final Logger log = LoggerFactory.getLogger(GenericEventListenerProvider.class);

  protected static final String EVENT = "event";
  protected static final String EVENT_USER = "eventUser";
  private final KeycloakSession session;
  private final Set<EventType> userEventList;
  private final String userEventCallbackURL;
  private final Set<ResourceType> adminResourceList;
  private final String adminResourceCallbackURL;
  private final String linkedDataUserBase;
  private final String apiKey;
  private final String clientId;

  public GenericEventListenerProvider(KeycloakSession session, Set<EventType> userEventList, String
      userEventCallbackURL, Set<ResourceType> adminResourceList, String adminResourceCallbackURL,
                                      String linkedDataUserBase, String apiKey, String clientId) {
    this.session = session;
    this.userEventList = userEventList;
    this.userEventCallbackURL = userEventCallbackURL;
    this.adminResourceList = adminResourceList;
    this.adminResourceCallbackURL = adminResourceCallbackURL;
    this.linkedDataUserBase = linkedDataUserBase;
    this.apiKey = apiKey;
    this.clientId = clientId;
  }

  @Override
  public void onEvent(Event event) {
    log.info("KeycloakUserEvent:" + event.getType() + ":" + event.getClientId());
    if (userEventList.contains(event.getType()) && clientId.equals(event.getClientId())) {
      log.info("keycloak event matches conditions:" + userEventCallbackURL);
      RealmModel realm = session.realms().getRealm(event.getRealmId());
      UserModel user = session.users().getUserById(realm, event.getUserId());
      performCall(userEventCallbackURL, apiKey, event, user);
    }
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
  }

  private void performCall(String url, String apiKey, Event event, UserModel user) {
    Map<String, Object> map = new HashMap<>();
    map.put(EVENT, JsonMapper.MAPPER.valueToTree(event));
    map.put(EVENT_USER, userToMap(user));
    HttpCallExecutor.post(url, apiKey, map);
  }

  private Map<String, Object> userToMap(UserModel user) {
    Map<String, Object> m = new HashMap<>();
    m.put("@id", linkedDataUserBase + user.getId());
    m.put("firstName", user.getFirstName());
    m.put("lastName", user.getLastName());
    m.put("email", user.getEmail());
    return m;
  }

  @Override
  public void close() {
  }

}
