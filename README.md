![Logo](docs/images/sweden-connect.png)

# Openid Federation Tooling

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.swedenconnect.oidf/oidf-tooling/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.swedenconnect.oidf/oidf-tooling)

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
| Property                                          | Value                                                | Description                                                                                                          |
|----------------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `trustAnchorEntityId`                              | `https://dev.swedenconnect.se/oidf/sc/ta`             | The entity ID of the trust anchor in the federation.                                                                  |
| `discoveryUri`                                     | `https://dev.swedenconnect.se/oidf/sc/ta/discovery`   | URI used to discover federation metadata.                                                                             |
| `resolverUri`                                      | `https://dev.swedenconnect.se/oidf/sc/ta/resolve`     | URI used to resolve entities in the federation.                                                                       |
| `ssrfProtection.enableLocalIpAddressRanges`        | `false` (default)                                     | If `true`, allows outbound calls to loopback/link-local/site-local (RFC 1918) and IPv6 unique-local addresses. **Enabling this opens a security issue** - the service can then be used to reach or scan internal networks. Only intended for local development or testing. |
| `ssrfProtection.blockHostname`                     | *(none by default)*                                   | A list of regular expressions matched against the hostname of any URL the service is about to fetch (entity ids, `jwks_uri`, `logo_uri`, SAML `metadata_endpoint`). A match denies the request.                     |

### Outbound request protection (SSRF)

The service makes outbound HTTP calls on behalf of values found in user-supplied metadata - entity ids, `jwks_uri`,
`logo_uri`, and SAML `metadata_endpoint`. By default these calls are only allowed to use HTTPS and must resolve to a
public, globally routable address; requests to loopback, link-local (including cloud metadata endpoints such as
`169.254.169.254`), site-local (RFC 1918) and IPv6 unique-local addresses are blocked. This prevents the service from
being used to scan or reach internal networks. `ssrfProtection.enableLocalIpAddressRanges` and
`ssrfProtection.blockHostname` above are the only ways to adjust this behavior.

Example configuration in application.yaml
```yaml
openid:
  federation:
    tooling:
      trustAnchorEntityId: "https://dev.swedenconnect.se/oidf/sc/ta"
      discoveryUri: "https://dev.swedenconnect.se/oidf/sc/ta/discovery"
      resolverUri: "https://dev.swedenconnect.se/oidf/sc/ta/resolve"
      ssrfProtection:
        enableLocalIpAddressRanges: false
        blockHostname:
          - "\\.internal\\.example\\.com$"
```
System environment override
```yaml
export OPENID_FEDERATION_TOOLING_TRUSTANCHORENTITYID="https://example.com/ta"
export OPENID_FEDERATION_TOOLING_DISCOVERYURI="https://example.com/ta/discovery"
export OPENID_FEDERATION_TOOLING_RESOLVERURI="https://example.com/ta/resolve"
export OPENID_FEDERATION_TOOLING_SSRFPROTECTION_ENABLELOCALIPADDRESSRANGES="false"

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