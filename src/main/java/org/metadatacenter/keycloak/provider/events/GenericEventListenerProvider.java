package org.metadatacenter.keycloak.provider.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.metadatacenter.util.json.JsonMapper;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class GenericEventListenerProvider implements EventListenerProvider {

  private final Set<EventType> userEventList;
  private final String userEventCallbackURL;
  private final Set<OperationType> adminOperationList;
  private final String adminOperationCallbackURL;
  private final String apiKey;
  private final String clientId;

  public GenericEventListenerProvider(Set<EventType> userEventList, String userEventCallbackURL, Set<OperationType>
      adminOperationList, String adminOperationCallbackURL, String apiKey, String clientId) {
    this.userEventList = userEventList;
    this.userEventCallbackURL = userEventCallbackURL;
    this.adminOperationList = adminOperationList;
    this.adminOperationCallbackURL = adminOperationCallbackURL;
    this.apiKey = apiKey;
    this.clientId = clientId;
  }

  @Override
  public void onEvent(Event event) {
    if (userEventList.contains(event.getType())) {
      performCall(userEventCallbackURL, apiKey, event, "user");
    }
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    if(adminOperationList.contains(event.getOperationType())) {
      performCall(adminOperationCallbackURL, apiKey, event, "admin");
    }
  }

  private void performCall(String url, String apiKey, Event event, String eventType) {
    if (clientId.equals(event.getClientId())) {
      Map<String, Object> map = new HashMap<>();
      map.put("eventType", eventType);
      map.put("event", JsonMapper.MAPPER.valueToTree(event));
      HttpCallExecutor.post(url, apiKey, map);
    }
  }

  private void performCall(String url, String apiKey, AdminEvent event, String eventType) {
    if (clientId.equals(event.getAuthDetails().getClientId())) {
      Map<String, Object> map = new HashMap<>();
      map.put("eventType", eventType);
      map.put("event", JsonMapper.MAPPER.valueToTree(event));
      HttpCallExecutor.post(url, apiKey, map);
    }
  }

  @Override
  public void close() {
  }

}
