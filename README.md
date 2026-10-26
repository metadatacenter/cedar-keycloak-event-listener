# cedar-keycloak-event-listener

[![CI](https://github.com/metadatacenter/cedar-keycloak-event-listener/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/metadatacenter/cedar-keycloak-event-listener/actions/workflows/ci.yml)

Event listener for Keycloak

The listener runs inside Keycloak and packages only its own classes. Its service registration
activates `cedar-parent`'s `keycloak-provider` profile, so provided dependencies follow the
Keycloak platform rather than the CEDAR microservice platform. Build plugins, JUnit and Mockito
remain shared with the other CEDAR projects.

After installing the current parent with `cedarcli build parent`, check the dependency boundary:

```bash
python3 scripts/check_keycloak_platform.py
```

CI runs this check after Maven verification. It compares the complete provided dependency graph
with a standalone project importing `keycloak-parent` at the configured Keycloak version. A
changed version, missing library or additional library fails the check. Reports are retained in
`target/keycloak-platform/`; `--settings <path>` uses the same Maven settings as the build.
