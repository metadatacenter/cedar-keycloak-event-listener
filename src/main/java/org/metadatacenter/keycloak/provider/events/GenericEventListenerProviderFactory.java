package org.metadatacenter.keycloak.provider.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashSet;
import java.util.Set;

public class GenericEventListenerProviderFactory implements EventListenerProviderFactory {

  private Set<EventType> eventList;
  private String callbackURL;
  private String apiKey;
  private String clientId;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new GenericEventListenerProvider(eventList, callbackURL, apiKey, clientId);
  }

  @Override
  public void init(Config.Scope config) {
    eventList = new HashSet<>();
    String[] events = config.getArray("eventList");
    if (events != null) {
      for (String e : events) {
        eventList.add(EventType.valueOf(e));
      }
    }
    callbackURL = config.get("callbackURL");
    apiKey = config.get("apiKey");
    clientId = config.get("clientId");
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {
  }

  @Override
  public String getId() {
    return "CEDAR-event-listener";
  }

}
