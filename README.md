# IDMan

## _OAuth Server_

[![Build Status](https://github.com/appform-io/idman/actions/workflows/github-ci.yml/badge.svg?branch=master)](https://github.com/appform-io/idman/actions/workflows/github-ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=appform-io_idman&metric=alert_status)](https://sonarcloud.io/dashboard?id=appform-io_idman)

IDMan is an all in one authentication and authorisation service.

* Provides centralised authorisation and authentication mechanism for multiple services
* Built in console to configure services, roles users etc
* Supports user management and Role based access control for tiered access to reosurces in other services
* Provides java client library as well as Dropwizard bundle for easy integration.
  Provides [declarative security](https://docs.oracle.com/cd/E19798-01/821-1841/gjgcq/index.html) annotations as
  supported by *dropwizard-auth* bundle
* Built for easy deployment with minimal configuration
* Simple single api that can be used to integrate with clients written in any language
* Minimal configuration on client side

## Entities

The following entities comprise of the data model in IDMan

* *service* - A service is a represntation of a client that wants to use IDMan for RBAC.
* *role* - Different roles of the service, For example Administrator, Maintainer, User, Guest etc. A *role* is inclusive
  to a *service*.
* *user* - A user for client services. This service was
* *user-role* - Role of a *user* in the *service*. Same *user* can have different *role* in different *service*s

All these can be configured on the IDMan ui.

## Setup

To setup IDMan on your local dev environment follow the following steps:

* clone the git repo

```shell
 git clone git@github.com:appform-io/idman.git
```

* build

```shell
mvn clean package
```

* expose idman to internet

If you are using cookies for establisihing identity, it is important to note that cookies are not tracked at host:port
level, but at host level. Hence, in order to save a lot of wasted time debugging around this, please use something
like [ngrok](https://ngrok.com/) to expose the local IDMan endpoint to the internet and use that in client services for
testing. Point ngrok to 8080.

```shell
ngrok http 8080
```

Note the endpoint reported by ngrok. It will come in handy in the next steps.

* generate secret key for HS512 JWT signing

```shell
openssl rand -base64 172 | tr -d '\n'
```

* create environment file with required variables in a file called env.sh

```shell
#!/bin/bash
export SERVER_ENDPOINT="<NGROK ENDPOINT FROM ABOVE>"
export JWT_PRIVATE_KEY="<OUTPUT OF OPENSSL COMMAND ABOVE>"
```

* load env variables into current shell

```shell
. ./env.sh
```

* start idman

```shell
cd idman-server
docker-compose up --build
```

* first time setup - point your browser to `http://localhost:8080/setup` to setup the service with admin username and
  password

## Integration

IDman provides a raw client in java that can be used to assert on a token and get the corresponding user profile. It
also provides a bundle for easy integration with dropwizard.

## Base steps

Before you start authenticating and authorising users to your service, you need to set it up in IDMan.

* Add users to the system. These users will be able to only see details on idman once they login to idman. _Note_: The
  password you set in this step will be changed once they log in.
* Setup your service from homepage using the *Create New Service* button. Remember to put in the endpoint properly. The
  format will be `http(s)://host:port/idman/auth/callback`. The `host` will be the host for your service, so `localhoat`
  for your test service before deployment. `port` will be the port for your service, so it depends on which port you are
  running your service on.
* Once done, you will be sent to the service details page. Define the required Roles there.
* Map the user(s) created in first step to different roles on this service

That's it. You are ready for integration.

### Dropwizard integration

To use the bundle use the following maven dependency

```xml

<dependency>
    <groupId>io.appform.idman</groupId>
    <artifactId>idman-auth-bundle</artifactId>
    <version>${idman-auth-bundle.version}</version>
</dependency>
```

To initialize the bundle you will need to provide the configuration object. This can come from your config file (
typically) or by whatever other means. The integration is similar to any standard dropwizard bundle.

* Add config object to your config class

```java
//You application
public class AppConfig extends Configuration {
    @NotNull
    @Valid
    private IdManHttpClientConfig idman = new IdManHttpClientConfig();
    // Getters setters etc....
}
```

* Add and initialise bundle in Application class

```java
public class App extends Application<AppConfig> {
    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        //...
        bootstrap.addBundle(new IdmanAuthBundle<AppConfig>() {
            @Override
            public IdManHttpClientConfig clientConfig(AppConfig config) {
                return config.getIdman();
            }
        });
        //...
    }

    @Override
    public void run(AppConfig configuration, Environment environment) throws Exception {
        //...
    }
}
```

* Annotate resources with role ids available from IDMan console as parameters to `@RolesAllowed` or `@PermitAll` as need
  be

```java

@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class MyResource {
    @GET
    @RolesAllowed("TESTSERVICE_ADMIN")
    //The role name is a combination of service name "testservice" and role "admin". It can be found on idman console
    public Response hello() {
        return Response.ok(new MyView("Hello World")).build();
    }
}
```

* Update config file to point to IDMan endpoint and provide a secret key obtained from console

```yaml
# Other stuff, http port etc as usual
idman:
  serviceId: CAMPAIGN_MANAGER
  authEndpoint: "<NGROK ENDPOINT FROM SETUP SECTION>"
  authSecret: "<SECRET KEY FROM ABOVE>"
```

That's it .. time to rock n' roll. Point your brwoser to your service. It will redirect to IDMan endpoint for
credentials. Once credentials are entered, it will redirect back to your service and to the page you intended to go to
originally.

## Static Token Authorisation

Static tokens are menat to secure system to system communication. So the owner of the serivce will provide a system
token to the calling service/script and need to be passed in the Authorization header as follows:

```
Authorization: Bearer <put the token here>
```

- Create a system user from the console (only system users are allowed to use static tokens)
- Map user to appropriate role on the target service
- Go back to user details screen for this user
- Create a new session by clicking the appropriate button. You will be shown a JWT, use this to authenticate.
- All static tokens are bound by a session id. Delete that session from user details screen to invalidate the token.

## Technologies

- Dropwizard
- Dropwizard Guicy and GSP
- Bootstrap 4.0
- HandlebarsJS

## License

Apache License 2.0

### Logo from

[PNGIMG](http://pngimg.com/image/67817)
