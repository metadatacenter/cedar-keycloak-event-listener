package org.metadatacenter.keycloak.provider.events;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class GenericEventListenerProviderFactory implements EventListenerProviderFactory {

  private static final Logger log = LoggerFactory.getLogger(GenericEventListenerProviderFactory.class);

  private Set<EventType> userEventList;
  private String userEventCallbackURL;
  private Set<ResourceType> adminResourceList;
  private String adminResourceCallbackURL;
  private String linkedDataUserBase;
  private String apiKey;
  private String clientId;

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return new GenericEventListenerProvider(session, userEventList, userEventCallbackURL, adminResourceList,
        adminResourceCallbackURL, linkedDataUserBase, apiKey, clientId);
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

    adminResourceList = new HashSet<>();
    String[] resourceTypes = config.getArray("adminResourceList");
    if (resourceTypes != null) {
      for (String rt : resourceTypes) {
        adminResourceList.add(ResourceType.valueOf(rt));
      }
    }
    adminResourceCallbackURL = config.get("adminResourceCallbackURL");
    linkedDataUserBase = config.get("linkedDataUserBase");

    apiKey = config.get("apiKey");
    clientId = config.get("clientId");

    log.info("***********************************************************************************************");
    log.info("GenericEventListenerProviderFactory.init()");
    log.info("userEventList           :" + userEventList);
    log.info("userEventCallbackURL    :" + userEventCallbackURL);
    log.info("adminResourceList       :" + adminResourceList);
    log.info("adminResourceCallbackURL:" + adminResourceCallbackURL);
    log.info("linkedDataUserBase      :" + linkedDataUserBase);
    log.info("clientId                :" + clientId);
    log.info("***********************************************************************************************");
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
