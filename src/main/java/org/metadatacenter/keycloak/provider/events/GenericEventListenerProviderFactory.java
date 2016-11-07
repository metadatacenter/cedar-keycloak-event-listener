package org.metadatacenter.keycloak.provider.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashSet;
import java.util.Set;

public class GenericEventListenerProviderFactory implements EventListenerProviderFactory {

  private Set<EventType> userEventList;
  private String userEventCallbackURL;
  private Set<OperationType> adminOperationList;
  private String adminOperationCallbackURL;
  private String apiKey;
  private String clientId;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new GenericEventListenerProvider(userEventList, userEventCallbackURL,
        adminOperationList, adminOperationCallbackURL,
        apiKey, clientId);
  }

  @Override
  public void init(Config.Scope config) {
    userEventList = new HashSet<>();
    String[] events = config.getArray("userEventList");
    if (events != null) {
      for (String e : events) {
        userEventList.add(EventType.valueOf(e));
      }
    }
    userEventCallbackURL = config.get("userEventCallbackURL");

    adminOperationList = new HashSet<>();
    String[] operations = config.getArray("adminOperationList");
    if (operations != null) {
      for (String op : operations) {
        adminOperationList.add(OperationType.valueOf(op));
      }
    }
    adminOperationCallbackURL = config.get("adminOperationCallbackURL");

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
