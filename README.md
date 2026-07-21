# grails-http-client

Sample app for **Calling REST APIs with Spring HTTP Services in Grails 8** (Apache Grails `8.0.0-SNAPSHOT`, JDK 21).

The guide walks through adding a declarative HTTP client to a Grails REST API using Spring Framework HTTP Services: define a `@HttpExchange` interface, register it with `@ImportHttpServices`, inject the generated `RestClient`-backed proxy into a Grails service, and expose results through JSON views. Local `RecordLabel` data stays in GORM/PostgreSQL; album metadata comes from the iTunes Search API.

This uses the same Spring stack Grails 8 already runs on (Spring Framework 7 / Spring Boot 4). No Micronaut plugin is required. Add `spring-boot-starter-json` so the HTTP client can deserialize JSON responses.

## Layout

| Directory | What it is |
|-----------|------------|
| `initial/` | Vanilla Grails 8 REST API starter from [start.grails.org](https://start.grails.org) (`postgres`, `testcontainers`, `spock`). Work through the guide starting here. |
| `complete/` | The finished sample - `@HttpExchange` `ItunesClient`, `ItunesSearchService`, `RecordLabel` REST API, JSON views, and Spock unit + integration tests. |

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

The integration spec (`RecordLabelIntegrationSpec`) asserts GORM persistence against a **real PostgreSQL database** (Testcontainers). `ItunesClientIntegrationSpec` registers the `@HttpExchange` client, calls `search()` against a MockWebServer stub, and verifies the encoded `term` query parameter plus JSON deserialization.

Unit specs under `src/test/groovy/` cover domain constraints and service delegation to the HTTP client with mocks.

## Requirements

- **JDK 21** (Temurin recommended; the Gradle build enforces Java 21+)
- **Docker** - required for `integrationTest` (Testcontainers PostgreSQL). Unit tests (`./gradlew test`) do not need Docker.
- **PostgreSQL** on `localhost:5432` - required for `./gradlew bootRun` (default database `devDb` in `application.yml`)

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
@Import(ItunesClientConfiguration)
class Application extends GrailsAutoConfiguration { ... }

// 2. Declare a Spring HTTP service interface:
@HttpExchange
interface ItunesClient {
    @GetExchange('/search?limit=25&media=music&entity=album&term={term}')
    SearchResult search(@PathVariable('term') String term)
}

// 3. Point RestClient at itunes.base-url and accept the iTunes API's
//    text/javascript response as JSON (override the URL in tests with MockWebServer):
private static final MediaType JAVASCRIPT = MediaType.parseMediaType('text/javascript')

@Bean
RestClientHttpServiceGroupConfigurer itunesBaseUrlConfigurer(
        @Value('${itunes.base-url:https://itunes.apple.com}') String itunesBaseUrl) {
    return { groups ->
        groups.forEachClient { group, builder ->
            builder.baseUrl(itunesBaseUrl)
            builder.messageConverters { List<HttpMessageConverter<?>> converters ->
                JacksonJsonHttpMessageConverter jacksonConverter =
                        (JacksonJsonHttpMessageConverter) converters.find { HttpMessageConverter<?> converter ->
                            converter instanceof JacksonJsonHttpMessageConverter
                        }
                jacksonConverter.supportedMediaTypes = jacksonConverter.supportedMediaTypes + JAVASCRIPT
            }
        }
    }
}

// 4. Inject the client into a Grails service:
@Autowired
ItunesClient itunesClient

List<Album> searchAlbums(String searchTerm) {
    itunesClient.search(searchTerm.trim())?.results ?: []
}
```

`@ImportHttpServices` (Spring Framework 7 / Spring Boot 4) scans for `@HttpExchange` interfaces and registers a `RestClient`-backed proxy bean for each one. `spring-boot-starter-json` provides the message converter used to deserialize JSON responses; `ItunesClientConfiguration` also allows the iTunes API's `text/javascript` content type.

Controllers stay thin and delegate to services; JSON views under `grails-app/views/` shape REST responses.

## Guide prose

Published narrative lives on [grails.apache.org/guides](https://grails.apache.org/guides/) in [apache/grails-static-website](https://github.com/apache/grails-static-website) under `guides/grails-http-client/v8/`.

## CI

GitHub Actions (`.github/workflows/grails8.yml`) runs `./gradlew test` for `initial` and `complete`, and `./gradlew integrationTest` for `complete`, on pushes and PRs to the `grails8` branch.
