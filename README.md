# SEAL Server

Spring Boot 4.1.1 API with JPA and Neon PostgreSQL. Requires Java 21 or newer.

## Run locally

Run commands from the project root so Spring can find `config/neon.properties`.

```sh
./mvnw spring-boot:run
```

The server uses port 8080 by default. Set the `PORT` environment variable to change it.
Open http://localhost:8080/api/health to check the existing API.

## Database configuration

`src/main/resources/application.properties` imports the ignored local file
`config/neon.properties`. The local credentials have already been configured
from `a.txt`. Neither file should be committed.

On another machine, copy `config/neon.properties.example` to
`config/neon.properties` and enter the JDBC URL, username, and password.
Alternatively, supply `NEON_DB_URL`, `NEON_DB_USERNAME`, and `NEON_DB_PASSWORD`
as environment variables. Environment variables override local file values.

A JDBC URL starts with `jdbc:postgresql://`, contains no username/password in
the URL, and uses `channelBinding` instead of Neon's `channel_binding` option.
The configured connection requires TLS and channel binding.

The pool allows up to five connections and keeps no minimum idle connections.
JPA's automatic schema changes are disabled. Add entities and an explicit
schema/migration strategy when implementing persistence.

## Verify and package

```sh
./mvnw test
./mvnw package
java -jar target/seal-server-0.0.1-SNAPSHOT.jar
```

The integration test starts the Spring context and executes `SELECT 1` through
the configured Neon connection. Tests and packaging therefore need valid
credentials and internet access. It does not create tables or exam data.

## IntelliJ

Open this folder as a Maven project and reload Maven after changing `pom.xml`.
Run `SealServerApplication` with this project root as the working directory.
JavaFX should call this backend over HTTP; database credentials stay here.

The health endpoint confirms API availability; the integration test separately
confirms database connectivity. Authentication and exam APIs are still to be built.




