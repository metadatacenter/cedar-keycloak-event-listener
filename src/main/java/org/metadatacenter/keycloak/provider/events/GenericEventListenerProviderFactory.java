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
    String resourceServerURL = "http://" + System.getenv("CEDAR_NET_GATEWAY") + ":" + System.getenv("CEDAR_RESOURCE_HTTP_PORT");

    userEventList = new HashSet<>();
    userEventList.add(EventType.LOGIN);

    userEventCallbackURL = resourceServerURL + "/command/auth-user-callback";

    adminResourceList = new HashSet<>();
    adminResourceList.add(ResourceType.USER);

    adminResourceCallbackURL = resourceServerURL + "/command/auth-admin-callback";

    linkedDataUserBase = "https://metadatacenter.org/users/";

    apiKey = System.getenv("CEDAR_ADMIN_USER_API_KEY");
    clientId = "cedar-angular-app";

    log.info("***********************************************************************************************");
    log.info("************************** GenericEventListenerProviderFactory.init() *************************");
    log.info("adminResourceCallbackURL:" + adminResourceCallbackURL);
    log.info("adminResourceList       :" + adminResourceList);
    log.info("apiKey                  :" + (linkedDataUserBase != null && !linkedDataUserBase.isEmpty() ? "found" :
        "not found"));
    log.info("clientId                :" + clientId);
    log.info("linkedDataUserBase      :" + linkedDataUserBase);
    log.info("userEventCallbackURL    :" + userEventCallbackURL);
    log.info("userEventList           :" + userEventList);
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
