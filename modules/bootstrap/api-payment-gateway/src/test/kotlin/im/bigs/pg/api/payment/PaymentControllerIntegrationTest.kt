package im.bigs.pg.api.payment

import im.bigs.pg.api.factory.ApiTestDataFactory
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.BaseIntegrationTest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PaymentGateway
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 결제 생성 API 통합 테스트
 * - PgClient을 목킹하고, 테스트 전용 파트너 데이터를 생성하여 활용합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PaymentControllerIntegrationTest.MockTestPgClientConfiguration::class)
class PaymentControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var paymentRepository: PaymentJpaRepository

    @Autowired
    lateinit var testData: ApiTestDataFactory

    private lateinit var partner: Partner
    private lateinit var testPg: PaymentGateway

    @BeforeEach
    fun setup() {
        partner = testData.createPartner("TEST_PARTNER_001", "Test Partner for Integration")
        testData.createFeePolicy(partner.id, "2020-01-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))
        testPg = testData.createPaymentGateway("TEST_PG")
        testData.createPartnerPgSupport(partner.id, testPg.id!!)
    }

    /**
     * 기본 테스트 설정: PgClientRegistry Bean을 테스트용으로 교체합니다.
     * - @Primary를 사용하여 실제 구현체 대신 테스트용 PgClientRegistry를 주입받도록 합니다.
     * - testpgclient를 spy로 만들어 approve 메서드의 반환값을 제어할 수 있게 합니다.
     * - 이 설정은 클래스 레벨에서 @Import로 적용됩니다.
     */
    @TestConfiguration
    class MockTestPgClientConfiguration {
        @Bean
        @Primary
        fun pgClientRegistry(
            testPgClient: im.bigs.pg.external.pg.testpg.TestPgClient,
            mockPgClient: im.bigs.pg.external.pg.mock.MockPgClient
        ): im.bigs.pg.application.pg.registry.PgClientRegistry {
            val spiedTestPgClient = spyk(testPgClient)
            every { spiedTestPgClient.approve(any()) } returns PgApproveResult(
                approvalCode = "APPROVAL-20240115-001234",
                approvedAt = LocalDateTime.of(2024, 1, 15, 14, 30, 0),
                status = PaymentStatus.APPROVED
            )
            return im.bigs.pg.application.pg.registry.PgClientRegistry(listOf(spiedTestPgClient, mockPgClient))
        }
    }

    @Test
    @DisplayName("POST /api/v1/payments - 기본 PG(testpgclient) 결제 성공")
    fun `기본 PG(testpgclient) 결제 성공`() {
            // Given: 활성화된 파트너로 결제 요청
            val request = CreatePaymentRequest(
                partnerId = partner.id,
                amount = BigDecimal("10000"),
                cardBin = "123456",
                cardLast4 = "4242",
                productName = "테스트 상품"
            )

            // When: 결제 생성 API 호출
            val result = mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }
                .andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, PaymentResponse::class.java)

            // Then: HTTP 200 응답 및 응답 필드 정확성 확인
            assertNotNull(response.id)
            assertEquals(partner.id, response.partnerId)
            assertEquals(BigDecimal("10000"), response.amount)
            assertEquals(BigDecimal("0.0250"), response.appliedFeeRate)
            assertEquals(BigDecimal("300"), response.feeAmount) // 10000 * 0.025 + 50
            assertEquals(BigDecimal("9700"), response.netAmount) // 10000 - 300
            assertEquals("4242", response.cardLast4)
            assertEquals("APPROVAL-20240115-001234", response.approvalCode)
            assertEquals(PaymentStatus.APPROVED, response.status)

            // Then: Payment 엔티티가 DB에 정상 저장되었는지 확인
            val savedPayment = paymentRepository.findById(response.id!!).orElse(null)
            assertNotNull(savedPayment)
            assertEquals(PaymentStatus.APPROVED, PaymentStatus.valueOf(savedPayment.status))
        }

    @Test
    @DisplayName("POST /api/v1/payments - 존재하지 않는 파트너")
    fun `존재하지 않는 파트너`() {
            // Given: 존재하지 않는 partnerId
            val nonExistentPartnerId = 99999L
            val request = CreatePaymentRequest(
                partnerId = nonExistentPartnerId,
                amount = BigDecimal("10000")
            )

            // When & Then: HTTP 400 Bad Request 또는 404 Not Found
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }
                .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 비활성화된 파트너")
    fun `비활성화된 파트너`() {
            // Given: 비활성화된 파트너 생성
            val inactivePartner = testData.createPartner("INACTIVE_PARTNER", "Inactive Partner", active = false)
            testData.createFeePolicy(inactivePartner.id, "2020-01-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))
            testData.createPartnerPgSupport(inactivePartner.id, testPg.id!!)

            val request = CreatePaymentRequest(
                partnerId = inactivePartner.id,
                amount = BigDecimal("10000")
            )

            // When & Then: HTTP 400 Bad Request
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                }
                .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 결제 금액(amount) 유효성 실패 - amount가 0")
    fun `결제 금액(amount) 유효성 실패 - amount가 0`() {
            // Given: amount = 0
            val request = CreatePaymentRequest(
                partnerId = partner.id,
                amount = BigDecimal("0")
            )

            // When & Then: HTTP 400 Bad Request
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                }
                .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 결제 금액(amount) 유효성 실패 - amount가 음수")
    fun `결제 금액(amount) 유효성 실패 - amount가 음수`() {
            // Given: amount = -1000
            val request = CreatePaymentRequest(
                partnerId = partner.id,
                amount = BigDecimal("-1000")
            )

            // When & Then: HTTP 400 Bad Request
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                }
                .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 필수 필드 누락 - partnerId 누락")
    fun `필수 필드 누락 - partnerId 누락`() {
            // Given: partnerId가 누락된 요청
            val requestJson = """
                {
                    "amount": 10000
                }
            """.trimIndent()

            // When & Then: HTTP 400 Bad Request
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = requestJson
            }
                .andExpect {
                    status { isBadRequest() }
                }
                .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 필수 필드 누락 - amount 누락")
    fun `필수 필드 누락 - amount 누락`() {
            // Given: amount가 누락된 요청
            val requestJson = """
                {
                    "partnerId": ${partner.id}
                }
            """.trimIndent()

            // When & Then: HTTP 400 Bad Request
            mockMvc.post("/api/v1/payments") {
                contentType = MediaType.APPLICATION_JSON
                content = requestJson
            }
                .andExpect {
                    status { isBadRequest() }
                }
                .andReturn()
    }
}
