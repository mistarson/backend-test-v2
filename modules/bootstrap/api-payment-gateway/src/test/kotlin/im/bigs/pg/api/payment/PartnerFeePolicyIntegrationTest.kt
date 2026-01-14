package im.bigs.pg.api.payment

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.pg.port.out.PartnerPgSupportOutPort
import im.bigs.pg.application.pg.port.out.PaymentGatewayOutPort
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 제휴사별 수수료 정책 적용 통합 테스트.
 * - PgClient을 목킹하고, 테스트 전용 파트너 데이터를 생성하여 활용합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(PartnerFeePolicyIntegrationTest.MockTestPgClientConfiguration::class)
class PartnerFeePolicyIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var paymentRepo: PaymentJpaRepository

    @Autowired
    lateinit var paymentService: PaymentUseCase

    @Autowired
    lateinit var partnerPort: PartnerOutPort

    @Autowired
    lateinit var feePolicyPort: FeePolicyOutPort

    @Autowired
    lateinit var partnerPgSupportPort: PartnerPgSupportOutPort

    @Autowired
    lateinit var paymentGatewayPort: PaymentGatewayOutPort

    private lateinit var partner: Partner
    private lateinit var testData: TestDataFactory
    private lateinit var testPg: PaymentGateway

    @BeforeEach
    fun setup() {
        testData = TestDataFactory(partnerPort, feePolicyPort, partnerPgSupportPort, paymentGatewayPort)
        partner = testData.createPartner("E2E_PARTNER", "E2E Test Partner")
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

    // ===== 헬퍼 메서드 =====

    /**
     * 결제 요청을 생성합니다.
     */
    private fun createPaymentRequest(
        partnerId: Long,
        amount: BigDecimal,
        cardBin: String? = null,
        cardLast4: String? = null,
        productName: String? = null
    ): CreatePaymentRequest {
        return CreatePaymentRequest(
            partnerId = partnerId,
            amount = amount,
            cardBin = cardBin,
            cardLast4 = cardLast4,
            productName = productName
        )
    }

    /**
     * HTTP POST 요청을 실행하고 응답을 파싱합니다.
     */
    private fun postPayment(request: CreatePaymentRequest): PaymentResponse {
        val result = mockMvc.post("/api/v1/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
            }
            .andReturn()

        return objectMapper.readValue(result.response.contentAsString, PaymentResponse::class.java)
    }

    /**
     * 결제 응답을 검증합니다.
     */
    private fun PaymentResponse.assertPayment(
        partnerId: Long,
        amount: BigDecimal,
        expectedRate: BigDecimal,
        expectedFee: BigDecimal,
        expectedNet: BigDecimal
    ) {
        assertNotNull(this.id, "결제 ID는 null이 아니어야 합니다")
        assertEquals(partnerId, this.partnerId, "파트너 ID가 일치하지 않습니다")
        assertEquals(amount, this.amount, "결제 금액이 일치하지 않습니다")
        assertEquals(expectedRate, this.appliedFeeRate, "적용된 수수료율이 일치하지 않습니다")
        assertEquals(expectedFee, this.feeAmount, "수수료 금액이 일치하지 않습니다")
        assertEquals(expectedNet, this.netAmount, "순 금액이 일치하지 않습니다")
        assertNotNull(this.approvalCode, "승인 코드는 null이 아니어야 합니다")
        assertNotNull(this.approvedAt, "승인 시각은 null이 아니어야 합니다")
        assertNotNull(this.createdAt, "생성 시각은 null이 아니어야 합니다")
    }

    @Test
    @DisplayName("POST /api/v1/payments - 결제 생성 시 정확한 수수료 정책이 적용된다")
    fun `POST 결제 생성 시 정확한 수수료 정책이 적용된다`() {
        // Given: 수수료 정책 등록 (2.5% + 50원)
        testData.createFeePolicy(partner.id, "2024-01-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))

        // When: HTTP POST 요청
        val request = createPaymentRequest(
            partnerId = partner.id,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "E2E 테스트 상품"
        )
        val response = postPayment(request)

        // Then: 응답 검증 (수수료: 10000 * 0.025 + 50 = 300원, 순 금액: 10000 - 300 = 9700원)
        response.assertPayment(
            partnerId = partner.id,
            amount = BigDecimal("10000"),
            expectedRate = BigDecimal("0.0250"),
            expectedFee = BigDecimal("300"),
            expectedNet = BigDecimal("9700")
        )
        assertEquals("4242", response.cardLast4, "카드 마지막 4자리가 일치하지 않습니다")

        // Then: DB 저장 확인
        val savedPayment = paymentRepo.findById(response.id!!).orElse(null)
        assertNotNull(savedPayment, "DB에 결제가 저장되어야 합니다")
        assertEquals("123456", savedPayment.cardBin)
        assertEquals("4242", savedPayment.cardLast4)
    }

    @Test
    @DisplayName("POST /api/v1/payments - 여러 정책 중 최신 정책이 적용된다")
    fun `POST 여러 정책 중 최신 정책이 적용된다`() {
        // Given: 여러 정책 등록 (3% + 100원, 2.5% + 50원)
        testData.createFeePolicy(partner.id, "2020-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100"))
        testData.createFeePolicy(partner.id, "2024-01-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))

        // When: 결제 요청
        val request = createPaymentRequest(partner.id, BigDecimal("10000"))
        val response = postPayment(request)

        // Then: 최신 정책(2.5% + 50원)이 적용됨 (수수료: 300원, 순 금액: 9700원)
        response.assertPayment(
            partnerId = partner.id,
            amount = BigDecimal("10000"),
            expectedRate = BigDecimal("0.0250"),
            expectedFee = BigDecimal("300"),
            expectedNet = BigDecimal("9700")
        )
    }

    @Test
    @DisplayName("POST /api/v1/payments - 정책이 없으면 예외 발생")
    fun `POST 정책이 없으면 예외 발생`() {
        // Given: 정책 없음

        // When & Then: HTTP 500 Internal Server Error
        val request = createPaymentRequest(partner.id, BigDecimal("10000"))
        mockMvc.post("/api/v1/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isInternalServerError() }
            }
            .andReturn()
    }

    @Test
    @DisplayName("POST /api/v1/payments - 여러 제휴사의 독립적인 정책이 각각 적용된다")
    fun `POST 여러 제휴사의 독립적인 정책이 각각 적용된다`() {
        // Given: Partner 1의 정책 (2.5% + 50원)
        testData.createFeePolicy(partner.id, "2024-01-01T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))

        // Given: 두 번째 Partner와 정책 (3% + 100원)
        val partner2 = testData.createPartner("PARTNER2", "Partner 2")
        testData.createFeePolicy(partner2.id, "2024-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100"))
        testData.createPartnerPgSupport(partner2.id, testPg.id!!)

        // When: 각 Partner로 결제
        val response1 = postPayment(createPaymentRequest(partner.id, BigDecimal("10000")))
        val response2 = postPayment(createPaymentRequest(partner2.id, BigDecimal("10000")))

        // Then: 각각 다른 수수료 적용
        response1.assertPayment(partner.id, BigDecimal("10000"), BigDecimal("0.0250"), BigDecimal("300"), BigDecimal("9700")) // 2.5% + 50원
        response2.assertPayment(partner2.id, BigDecimal("10000"), BigDecimal("0.0300"), BigDecimal("400"), BigDecimal("9600")) // 3% + 100원
    }

    @Test
    @DisplayName("수수료 정책 변경 시나리오 - 기존 결제는 유지, 새 결제는 새 정책 적용")
    fun `수수료 정책 변경 시나리오`() {
        // Given: Partner와 기존 정책 (3% + 100원)
        val changePartner = testData.createPartner("CHANGE_PARTNER", "Change Partner")
        testData.createFeePolicy(changePartner.id, "2024-01-01T00:00:00Z", BigDecimal("0.0300"), BigDecimal("100"))
        testData.createPartnerPgSupport(changePartner.id, testPg.id!!)

        // When: 기존 정책으로 10개 결제 완료 (UseCase 직접 호출로 성능 최적화)
        repeat(10) {
            paymentService.pay(PaymentCommand(partnerId = changePartner.id, amount = BigDecimal("10000")))
        }

        // Then: 모든 결제가 기존 정책(3% + 100원) 적용 (수수료: 400원)
        val allPayments = paymentRepo.findAll().filter { it.partnerId == changePartner.id }
        assertEquals(10, allPayments.size, "10개 결제가 저장되어야 합니다")
        allPayments.forEach { payment ->
            assertEquals(BigDecimal("0.0300"), payment.appliedFeeRate, "수수료율이 3%여야 합니다")
            assertEquals(BigDecimal("400"), payment.feeAmount, "수수료가 400원이어야 합니다")
        }

        // When: 새로운 정책 추가 (2.5% + 50원, 과거 시점으로 추가하여 즉시 적용되도록)
        // Note: 현재 시점 기준으로 과거 시점의 정책을 추가하면, 가장 최근 정책으로 적용됩니다
        testData.createFeePolicy(changePartner.id, "2024-01-02T00:00:00Z", BigDecimal("0.0250"), BigDecimal("50"))

        // When: 새로운 결제 시도
        val newPayment = paymentService.pay(PaymentCommand(partnerId = changePartner.id, amount = BigDecimal("10000")))

        // Then: 새 결제는 새로운 정책(2.5% + 50원) 적용, 기존 결제들은 기존 정책 유지
        assertEquals(BigDecimal("0.0250"), newPayment.appliedFeeRate, "새 정책(2.5%)이 적용되어야 합니다")
        assertEquals(BigDecimal("300"), newPayment.feeAmount, "수수료가 300원이어야 합니다 (10000 * 0.025 + 50)")
        assertEquals(11, paymentRepo.findAll().filter { it.partnerId == changePartner.id }.size, "총 11개 결제가 있어야 합니다")
        
        // Then: 기존 10개 결제는 여전히 기존 정책 유지
        val existingPayments = paymentRepo.findAll().filter { it.partnerId == changePartner.id && it.id != newPayment.id }
        assertEquals(10, existingPayments.size, "기존 10개 결제가 유지되어야 합니다")
        existingPayments.forEach { payment ->
            assertEquals(BigDecimal("0.0300"), payment.appliedFeeRate, "기존 결제는 3% 정책이 유지되어야 합니다")
            assertEquals(BigDecimal("400"), payment.feeAmount, "기존 결제의 수수료는 400원이어야 합니다")
        }
    }

    @Test
    @DisplayName("대량 결제 시나리오 - 여러 Partner의 동시 결제")
    fun `대량 결제 시나리오`() {
        // Given: 10개 Partner와 각각 다른 정책 생성 (0.01% ~ 0.10%, 고정수수료 10원 ~ 100원)
        val partners = (1..10).map { i ->
            val p = testData.createPartner("BULK_PARTNER_$i", "Bulk Partner $i")
            val rate = BigDecimal("0.0${i}00") // 0.01, 0.02, ..., 0.10
            val fixedFee = BigDecimal(i * 10) // 10, 20, ..., 100
            testData.createFeePolicy(p.id, "2024-01-01T00:00:00Z", rate, fixedFee)
            testData.createPartnerPgSupport(p.id, testPg.id!!)
            p
        }

        // When: 1000개의 결제를 처리 (각 결제는 순환적으로 Partner 선택)
        // UseCase 직접 호출로 성능 최적화 (HTTP 요청 1000개는 너무 느림)
        repeat(1000) { i ->
            val selectedPartner = partners[i % 10]
            paymentService.pay(PaymentCommand(partnerId = selectedPartner.id, amount = BigDecimal("10000")))
        }

        // Then: 모든 결제가 올바른 정책 적용
        val allPayments = paymentRepo.findAll()
        val bulkPayments = allPayments.filter { payment -> partners.any { it.id == payment.partnerId } }
        assertEquals(1000, bulkPayments.size, "1000개 결제가 저장되어야 합니다")

        // 각 Partner별로 결제가 올바른 정책 적용되었는지 확인
        partners.forEachIndexed { index, partner ->
            val partnerPayments = bulkPayments.filter { it.partnerId == partner.id }
            val expectedRate = BigDecimal("0.0${index + 1}00")
            val expectedFixed = BigDecimal((index + 1) * 10)
            val expectedFee = BigDecimal("10000")
                .multiply(expectedRate)
                .setScale(0, java.math.RoundingMode.HALF_UP) + expectedFixed

            partnerPayments.forEach { payment ->
                assertEquals(expectedRate, payment.appliedFeeRate, "Partner ${partner.code}의 수수료율이 일치해야 합니다")
                assertEquals(expectedFee, payment.feeAmount, "Partner ${partner.code}의 수수료 금액이 일치해야 합니다")
            }
        }
    }
}

