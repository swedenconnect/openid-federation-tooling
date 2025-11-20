![Logo](docs/images/sweden-connect.png)

# Openid Federation Tooling

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.swedenconnect.bankid/bankid-idp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.swedenconnect.bankid/bankid-idp)

Openid Federation Tooling.

-----

## About

This is a Spring Boot application with a Vue frontend. It has three main functions that help diagnose problems in
the federation.

### Resolver View
Resolves entities and displays the result. Unresolvable entities will return an error message. This helps diagnose
problems in the federation.

### Validator
Validates entity statements by entering an entity ID, JWT, or a JWT payload.

### View
Provides a visual representation of all nodes in the federation under the defined trust anchor.

## Documentation
| Property                     | Value                                                      | Description                                           |
|-------------------------------|------------------------------------------------------------|-------------------------------------------------------|
| `trustAnchorEntityId`         | `https://dev.swedenconnect.se/oidf/sc/ta`                 | The entity ID of the trust anchor in the federation. |
| `discoveryUri`                | `https://dev.swedenconnect.se/oidf/sc/ta/discovery`       | URI used to discover federation metadata.            |
| `resolverUri`                 | `https://dev.swedenconnect.se/oidf/sc/ta/resolve`         | URI used to resolve entities in the federation.      |


Example configuration in application.yaml
```yaml
openid:
  federation:
    tooling:
      trustAnchorEntityId: "https://dev.swedenconnect.se/oidf/sc/ta"
      discoveryUri: "https://dev.swedenconnect.se/oidf/sc/ta/discovery"
      resolverUri: "https://dev.swedenconnect.se/oidf/sc/ta/resolve"
```
System environment override
```yaml
export OPENID_FEDERATION_TOOLING_TRUSTANCHORENTITYID="https://example.com/ta"
export OPENID_FEDERATION_TOOLING_DISCOVERYURI="https://example.com/ta/discovery"
export OPENID_FEDERATION_TOOLING_RESOLVERURI="https://example.com/ta/resolve"

java -jar oidf-tooling.jar 
```
Using external configuration

```yaml
java -jar oidf-tooling.jar --spring.config.location=file:./config/application.yml
```

## Contributing

Pull requests are welcome. See the [Contributor Guidelines](CONTRIBUTING.md) for details.

## License

The OIDF Tooling is Open Source software released under the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

-----

Copyright &copy; 2023-2026, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).