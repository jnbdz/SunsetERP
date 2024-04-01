<img src="https://avatars.githubusercontent.com/u/237008" alt="Sunset" style="width: 300px;" align="right">

# 🌇 SunsetERP 🌇

**WARNING:** This project is still in development. It is not ready for production use or any use at all. But help is always welcome!

SunsetERP is based on a heavily modified version of [OFBiz](https://ofbiz.apache.org/) to run on Quarkus.

Here are some of the features of SunsetERP/OFBiz:
- Accounting (agreements, invoicing, vendor management, general ledger)
- Asset maintenance
- Catalogue and product management
- Facility and warehouse management system (WMS)
- Manufacturing execution / manufacturing operations management (MES/MOM)
- Order processing
- Inventory management, automated stock replenishment etc.
- Content management system (CMS)
- Human resources (HR)
- People and group management
- Project management
- Sales force automation
- Work effort management
- Electronic point of sale (ePOS)
- Electronic commerce (eCommerce)
- Scrum (development) (Scrum software development support)
- *and many more to come...* 

SunsetERP benefits from OFBiz usage of standard business data models.

## Delta with OFBiz
- SunsetERP is based on Quarkus
  - Why? Quarkus is faster and makes Java projects more lightweight.
- SunsetERP is API first (REST and SOAP)
  - Why? It makes it easier to integrate with other systems and create an SPA Single Page Application.
  - That being said the UI will be made with [PatternFLy](https://www.patternfly.org/) (of [RedHat](https://www.redhat.com/)) and [React](https://reactjs.org/). Because it looks way better than OFBiz UI. Sorry.
- SunsetERP is using a reactive architecture
- SunsetERP can be used in a microservices architecture more easily
- SunsetERP is using a different authentication system
  - Why? It makes it easier to integrate with other systems, to reduce the attack surface and to have something else not to worry about.
  - It will soon be able to work out of the box with [Keycloak](https://www.keycloak.org/).
    - Keycloak supports SAML 2.0, OpenID Connect, OAuth 2.0 and LDAP. So why reinvent the wheel?
  - So for more SSO and authentication options you will have to use Keycloak or some other system.

## What stays the same
- It is important to not stray away to far from the original OFBiz architecture and data model. This is to make sure that the project stays compatible with OFBiz plugins that already exist.
- It is important that it does not feel like a different project.
- The Data Models will stay untouched, but we might add new ones if need be.
- Entity Engine will stay untouched.
- Since Quarkus is used some changes to the usage of Groovy is to be expected.

### Why keep SOAP?
- SOAP is still used in many places and is still a very valid way of communicating with other systems. It is also very easy to use with Java.
- SOAP in the world of accounting, banking and finance is still very much used. It is also used in the world of logistics and supply chain management.

## Project Status
Still in dev mode.

## Nice to have
- https://kogito.kie.org/ (BPM and BRM)
- Have a similar plugin system as Keycloak
- Adapt ofbiz-plugins to this new setup
- Adapt it for better Process Mining

## TODO
- [x] Change the name of the directories to use `sunseterp` instead of `ofbiz`
- [x] Create API for framework infos and health
- [x] Update Gradle
- [x] Update Quarkus
- [ ] Load all components from OFBiz
- [ ] Create a REST API for all components from OFBiz with the help of OpenAPI
- [ ] Error handling for Rest APIs
  - [ ] Create a custom exception handler
  - [ ] Create a custom error response
  - [ ] Centralize error handling and error messages
- [ ] Create a SOAP API for all components that might need SOAP
- [ ] Error handling for SOAP APIs
- [ ] Update code with the changes that are from the OFBiz repository
- [x] Update code to support the latest Quarkus version
- [ ] Add support for the latest Java version
- [ ] Change library `AWT` for image modification (e.g.: `net.coobird.thumbnailator`, `org.apache.xmlgraphics`, `Twelvemonkeys ImageIO`) - Quarkus might not support AWT fully, and it is a bit old
- [ ] Add Unit Tests
- [ ] Add OpenTelemetry for tracing
- [ ] Integration with Keycloak
- [ ] Add integration tests
- [ ] Add support for OpenSearch and ElasticSearch
- [ ] `.github/workflows` for CI/CD
- [ ] (Optional) Integration with Vault and other secret management systems

## Comparisons
- [Comparison of shopping cart software](https://en.wikipedia.org/wiki/Comparison_of_shopping_cart_software)
- [Comparison of accounting software](https://en.wikipedia.org/wiki/Comparison_of_accounting_software)
- [Comparison of CRM systems](https://en.wikipedia.org/wiki/Comparison_of_CRM_systems)
- [Comparison of ERP software packages](https://en.wikipedia.org/wiki/Comparison_of_ERP_software_packages)
- [Comparison of time-tracking software](https://en.wikipedia.org/wiki/Comparison_of_time-tracking_software)
- [Comparison of project management software](https://en.wikipedia.org/wiki/Comparison_of_project_management_software)

## Getting Started
This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/sunseterp-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Related Guides

- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- OpenID Connect ([guide](https://quarkus.io/guides/security-openid-connect)): Verify Bearer access tokens and authenticate users with Authorization Code Flow
- Elytron Security Properties File ([guide](https://quarkus.io/guides/security-properties)): Secure your applications using properties files
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)

## Copyright
<p>
<em>Source of profile picture: <a href="https://www.flickr.com/photos/44073224@N04/24217734339/in/photostream/">Bernal Saborio</a></em> <a href="https://creativecommons.org/licenses/by-sa/2.0/"><img src="https://raw.githubusercontent.com/jnbdz/jnbdz/main/assets/80x15-by-sa.svg" alt="by-sa"></a>
</p>
