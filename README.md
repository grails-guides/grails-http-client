# grails-http-client

Sample app for **Calling REST APIs with Spring HTTP Services in Grails 8** (Apache Grails `8.0.0-SNAPSHOT`, JDK 21).

The guide walks through adding a declarative HTTP client to a Grails REST API using Spring Boot's built-in HTTP Services support: define a `@HttpExchange` interface, register it with `@ImportHttpServices`, inject the generated `RestClient` proxy into a Grails service, and expose results through JSON views. Local `RecordLabel` data stays in GORM/PostgreSQL; album metadata comes from the iTunes Search API.

This approach uses the same Spring stack Grails 8 already runs on. No Micronaut plugin or extra HTTP client dependency is required.

## Layout

| Directory | What it is |
|-----------|------------|
| `initial/` | Vanilla Grails 8 REST API starter from [start.grails.org](https://start.grails.org) (`postgres`, `testcontainers`, `spock`). Work through the guide starting here. |
| `complete/` | The finished sample — `@HttpExchange` `ItunesClient`, `ItunesSearchService`, `RecordLabel` REST API, JSON views, and Spock unit + integration tests. |

## Running

```bash
git clone -b grails8 https://github.com/grails-guides/grails-http-client.git
cd grails-http-client/complete
./gradlew test integrationTest
```

To follow the guide step by step, start from `initial/`:

```bash
cd grails-http-client/initial
./gradlew test
```

Run the finished app:

```bash
cd grails-http-client/complete
./gradlew bootRun
```

Example endpoints: `GET /api/recordLabels`, `GET /api/search?q=U2`.

The integration spec (`RecordLabelIntegrationSpec`) asserts GORM persistence against a **real PostgreSQL database** (Testcontainers). `ItunesClientIntegrationSpec` verifies the `@HttpExchange` client is registered as a Spring bean.

Unit specs under `src/test/groovy/` cover domain constraints and service delegation to the HTTP client with mocks.

## Requirements

- **JDK 21** (Temurin recommended; the Gradle build enforces Java 21+)
- **Docker** running locally — required for `integrationTest` (Testcontainers PostgreSQL). Unit tests (`./gradlew test`) do not need Docker.
- **PostgreSQL** on `localhost:5432` — required for `./gradlew bootRun` (default database `devDb` in `application.yml`)

If Gradle reports *"Run this build using a Java 21 or newer JVM"*, your shell or IDE is still on an older JDK:

```bash
sdk install java 21.0.6-tem
sdk default java 21.0.6-tem
java -version   # should show 21.x
```

If `integrationTest` fails with a Testcontainers / `docker.sock` error, start Docker Desktop (or your local Docker daemon) and retry.

## The pattern, in one place

```groovy
// 1. Register HTTP service interfaces on the application class:
@ImportHttpServices(basePackages = 'example')
class Application extends GrailsAutoConfiguration { ... }

// 2. Declare a Spring HTTP service interface:
@HttpExchange(url = 'https://itunes.apple.com')
interface ItunesClient {
    @GetExchange('/search?limit=25&media=music&entity=album&term={term}')
    SearchResult search(String term)
}

// 3. Inject the client into a Grails service:
@Autowired
ItunesClient itunesClient

List<Album> searchAlbums(String searchTerm) {
    itunesClient.search(searchTerm.trim())?.results ?: []
}
```

Spring Boot auto-configures a `RestClient` proxy for the interface. No extra HTTP client dependency is required beyond the standard Grails `rest-api` profile.

Controllers stay thin and delegate to services; JSON views under `grails-app/views/` shape REST responses.

## Guide prose

Published narrative lives on [grails.apache.org/guides](https://grails.apache.org/guides/) in [apache/grails-static-website](https://github.com/apache/grails-static-website) under `guides/grails-http-client/v8/`.

## CI

GitHub Actions (`.github/workflows/grails8.yml`) runs `./gradlew test` for `initial` and `complete`, and `./gradlew integrationTest` for `complete`, on pushes and PRs to the `grails8` branch.
