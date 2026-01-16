package im.bigs.pg.api

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.api.payment.dto.QueryResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * 통합 테스트를 위한 공통 베이스 클래스
 * 상속을 통해 공통 유틸리티 메서드를 제공합니다.
 */
abstract class BaseIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    /**
     * GET 요청을 보내고 QueryResponse를 파싱하여 반환합니다.
     *
     * @param url 요청 URL
     * @return 파싱된 QueryResponse
     */
    protected fun getQueryResponse(url: String): QueryResponse {
        val result = mockMvc.get(url)
            .andExpect {
                status { isOk() }
                content { contentType("application/json") }
            }
            .andReturn()

        return objectMapper.readValue(result.response.contentAsString, QueryResponse::class.java)
    }
}
