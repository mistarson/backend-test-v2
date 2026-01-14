package im.bigs.pg.external.pg.testpg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import im.bigs.pg.application.pg.port.out.BasePgApproveRequest
import im.bigs.pg.application.pg.port.out.TestPgApproveExtra
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.testpg.config.TestPgWebClientConfig
import im.bigs.pg.external.pg.testpg.exception.TestPgException
import im.bigs.pg.external.pg.testpg.util.TestPgEncryptor
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TestPgClient 실제 API 통합 테스트.
 * - 실제 https://api-test-pg.bigs.im API에 요청을 보내서 동작을 검증합니다.
 * - TestPgClient의 실제 외부 API 연동을 검증합니다.
 * - API 문서: https://api-test-pg.bigs.im/docs/index.html
 */
@SpringJUnitConfig(TestPgClientTest.TestConfig::class)
class TestPgClientTest {

    @Configuration
    class TestConfig {
        @Bean
        fun objectMapper(): ObjectMapper {
            return ObjectMapper().apply {
                registerModule(KotlinModule.Builder().build())
                registerModule(JavaTimeModule())
            }
        }

        @Bean
        fun testPgWebClient(): WebClient {
            return TestPgWebClientConfig().testPgWebClient()
        }

        @Bean
        fun testPgEncryptor(objectMapper: ObjectMapper): TestPgEncryptor {
            return TestPgEncryptor(objectMapper)
        }

        @Bean
        fun testPgClient(
            testPgEncryptor: TestPgEncryptor,
            testPgWebClient: WebClient,
        ): TestPgClient {
            return TestPgClient(testPgEncryptor, testPgWebClient)
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var testPgClient: TestPgClient

    @Test
    @DisplayName("실제 API 호출 - 결제 승인 성공 (암호화 및 응답 데이터 검증)")
    fun `실제 API 호출 - 결제 승인 성공`() {
        // Given: API 문서에 따르면 "1111-1111-1111-1111" 카드는 성공 응답을 반환
        val request = BasePgApproveRequest.TestPgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "1111",
            cardLast4 = "1111",
            productName = "테스트 상품",
            extra = TestPgApproveExtra() // 기본값 사용
        )

        // When: 실제 API에 결제 승인 요청
        val result = testPgClient.approve(request)

        // Then: 암호화 검증 - 성공 응답이 오면 암호화가 올바르게 동작한 것
        // Then: 응답 데이터 검증 - API 문서에 명시된 모든 필드 검증
        assertTrue(result.approvalCode.isNotBlank(), "승인 코드가 비어있습니다")

        assertNotNull(result.approvedAt, "승인 시각이 null입니다")

        assertEquals("1111", result.maskedCardLast4, "API 문서에 따르면 maskedCardLast4는 '1111'이어야 함")

        assertEquals(request.amount, result.amount, "요청 금액과 응답 금액이 일치해야 함")

        assertEquals(PaymentStatus.APPROVED, result.status, "상태가 APPROVED가 아닙니다")
    }

    @Test
    @DisplayName("실제 API 호출 - 0원 결제 실패 (API 문서: amount는 1 이상)")
    fun `실제 API 호출 - 0원 결제 실패`() {
        // Given: API 문서에 따르면 "amount는 1 이상"이어야 하므로 0원은 에러 예상
        val request = BasePgApproveRequest.TestPgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("0"),
            cardBin = "1111",
            cardLast4 = "1111",
            productName = "0원 테스트",
            extra = TestPgApproveExtra() // 기본값 사용
        )

        // When & Then: 0원은 에러가 발생해야 함 (400 에러 발생)
        val exception = assertFailsWith<TestPgException> {
            testPgClient.approve(request)
        }
        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode, "0원 결제 시 400 에러가 발생해야 합니다")
    }

    @Test
    @DisplayName("실제 API 호출 - 금액 범위 테스트 (1원, 10,000원, 1,000,000원)")
    fun `실제 API 호출 - 금액 범위 테스트`() {
        // Given: 다양한 금액으로 테스트 (모두 성공 예상)
        val amounts = listOf(
            BigDecimal("1"),
            BigDecimal("10000"),
            BigDecimal("1000000"),
        )

        amounts.forEach { amount ->
            val request = BasePgApproveRequest.TestPgApproveRequest(
                partnerId = 1L,
                amount = amount,
                cardBin = "1111",
                cardLast4 = "1111",
                productName = "금액 테스트: $amount",
                extra = TestPgApproveExtra() // 기본값 사용
            )

            // When: 실제 API에 결제 승인 요청
            val result = testPgClient.approve(request)

            // Then: 성공 응답 및 금액 일치 확인
            assertTrue(result.approvalCode.isNotBlank(), "금액 ${amount}에 대한 승인 코드가 비어있습니다")
            assertNotNull(result.approvedAt, "금액 ${amount}에 대한 승인 시각이 null입니다")
            assertEquals(amount, result.amount, "금액 ${amount} 응답 금액과 일치해야 함")
            assertEquals(PaymentStatus.APPROVED, result.status, "금액 ${amount}에 대한 상태가 APPROVED가 아닙니다")
        }
    }

    @Test
    @DisplayName("실제 API 호출 - 잘못된 카드 번호로 422 응답 확인")
    fun `실제 API 호출 - 잘못된 카드 번호로 422 응답 확인`() {
        // Given: API 문서에 따르면 "2222-2222-2222-2222" 카드는 422 에러를 반환
        val request = BasePgApproveRequest.TestPgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "2222",
            cardLast4 = "2222",
            productName = "422 에러 테스트",
            extra = TestPgApproveExtra(
                cardNumber = "2222-2222-2222-2222"
            )
        )

        // When & Then: 422 에러가 발생해야 함
        val exception = assertFailsWith<TestPgException> {
            testPgClient.approve(request)
        }
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.statusCode, "2222-2222-2222-2222 카드 번호로 결제 시 422 에러가 발생해야 합니다")
    }

    @Test
    @DisplayName("실제 API 호출 - 잘못된 API key로 401 응답 확인")
    fun `실제 API 호출 - 잘못된 API key로 401 응답 확인`() {
        // Given: API 문서에 따르면 "00000000-0000-4000-8000-000000000000" API key는 401 에러를 반환
        val request = BasePgApproveRequest.TestPgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "1111",
            cardLast4 = "1111",
            productName = "401 에러 테스트",
            extra = TestPgApproveExtra(
                apiKey = "00000000-0000-4000-8000-000000000000"
            )
        )

        // When & Then: 401 에러가 발생해야 함
        val exception = assertFailsWith<TestPgException> {
            testPgClient.approve(request)
        }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode, "00000000-0000-4000-8000-000000000000 API key로 결제 시 401 에러가 발생해야 합니다")
    }

}
