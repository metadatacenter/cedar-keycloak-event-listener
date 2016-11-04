package org.metadatacenter.keycloak.provider.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.metadatacenter.util.json.JsonMapper;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class GenericEventListenerProvider implements EventListenerProvider {

  private final Set<EventType> eventList;
  private final String callbackURL;
  private final String apiKey;
  private final String clientId;

  public GenericEventListenerProvider(Set<EventType> eventList, String callbackURL, String apiKey, String clientId) {
    this.eventList = eventList;
    this.callbackURL = callbackURL;
    this.apiKey = apiKey;
    this.clientId = clientId;
  }

  @Override
  public void onEvent(Event event) {
    performCall(callbackURL, apiKey, event, "user");
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    performCall(callbackURL, apiKey, event, "admin");
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
