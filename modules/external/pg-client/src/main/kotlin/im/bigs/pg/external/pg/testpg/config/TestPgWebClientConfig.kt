package im.bigs.pg.external.pg.testpg.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/**
 * TestPG WebClient 설정.
 */
@Configuration
class TestPgWebClientConfig {
    companion object {
        const val BASE_URL = "https://api-test-pg.bigs.im"
    }

    @Bean
    fun testPgWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
