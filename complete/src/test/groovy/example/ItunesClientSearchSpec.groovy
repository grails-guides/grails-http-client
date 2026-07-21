package example

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import spock.lang.Specification

class ItunesClientSearchSpec extends Specification {

    MockWebServer mockWebServer = new MockWebServer()

    def setup() {
        mockWebServer.start()
    }

    def cleanup() {
        mockWebServer.shutdown()
    }

    private ItunesClient client() {
        String baseUrl = mockWebServer.url('/').toString()
        if (baseUrl.endsWith('/')) {
            baseUrl = baseUrl[0..-2]
        }
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .messageConverters { converters ->
                    converters.add(0, new JacksonJsonHttpMessageConverter())
                }
                .build()
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(ItunesClient)
    }

    void 'search binds the term query parameter and deserializes albums'() {
        given:
        String searchTerm = 'U2 & Friends'
        mockWebServer.enqueue(new MockResponse()
                .setHeader('Content-Type', 'application/json')
                .setBody('''{"resultCount":1,"results":[{"artistName":"U2","collectionName":"The Joshua Tree","collectionViewUrl":"https://example.com/album"}]}'''))
        ItunesClient itunesClient = client()

        when:
        SearchResult result = itunesClient.search(searchTerm)

        then:
        result.resultCount == 1
        result.results.size() == 1
        result.results[0].artistName == 'U2'
        result.results[0].collectionName == 'The Joshua Tree'
        result.results[0].collectionViewUrl == 'https://example.com/album'

        and:
        RecordedRequest request = mockWebServer.takeRequest()
        request.method == 'GET'
        request.path.contains('/search?')
        request.requestUrl.queryParameter('term') == searchTerm
    }
}
