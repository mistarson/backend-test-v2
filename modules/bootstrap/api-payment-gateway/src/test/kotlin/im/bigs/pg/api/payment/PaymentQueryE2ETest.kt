package im.bigs.pg.api.payment

import im.bigs.pg.api.factory.ApiTestDataFactory
import im.bigs.pg.api.BaseIntegrationTest
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.partner.Partner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 결제 내역 조회 API 통합 테스트.
 * - 커서 기반 페이지네이션 및 통계 집계를 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentQueryE2ETest : BaseIntegrationTest() {

    @Autowired
    lateinit var testDataFactory: ApiTestDataFactory

    private lateinit var partner1: Partner

    @BeforeEach
    fun setup() {
        partner1 = testDataFactory.createPartner("PARTNER1", "Partner 1", true)
        testDataFactory.createFeePolicy(
            partnerId = partner1.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0300"),
            fixedFee = BigDecimal.ZERO,
        )
    }

    @Test
    @DisplayName("/api/v1/payments - 필터 없이 전체 결제 내역을 조회하고 통계를 집계한다")
    fun `필터 없이 전체 결제 내역을 조회하고 통계를 집계한다`() {
        // Given: partner2 생성 및 결제 3개 생성
        val partner2 = testDataFactory.createPartner("PARTNER2", "Partner 2", true)
        testDataFactory.createFeePolicy(
            partnerId = partner2.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0250"),
            fixedFee = BigDecimal("50"),
        )
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), paymentTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), paymentTime.plusSeconds(1))
        testDataFactory.createPayment(partner2.id, BigDecimal("15000"), paymentTime.plusSeconds(2))

        // When: 필터 없이 조회
        val response = getQueryResponse("/api/v1/payments")

        // Then: 모든 결제 반환, 통계 집계
        assertEquals(3, response.items.size)
        assertEquals(3, response.summary.count)
        assertEquals(BigDecimal("45000"), response.summary.totalAmount)
    }

    @Test
    @DisplayName("/api/v1/payments - 파트너 ID로 필터링하여 해당 파트너의 결제만 조회한다")
    fun `파트너 ID로 필터링하여 해당 파트너의 결제만 조회한다`() {
        // Given: partner2 생성 및 파트너별 결제 생성
        val partner2 = testDataFactory.createPartner("PARTNER2", "Partner 2", true)
        testDataFactory.createFeePolicy(
            partnerId = partner2.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0250"),
            fixedFee = BigDecimal("50"),
        )
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), paymentTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), paymentTime.plusSeconds(1))
        testDataFactory.createPayment(partner2.id, BigDecimal("15000"), paymentTime.plusSeconds(2))

        // When: 파트너 1로 필터링
        val response = getQueryResponse("/api/v1/payments?partnerId=${partner1.id}")

        // Then: 파트너 1의 결제만 반환
        assertEquals(2, response.items.size)
        response.items.forEach { assertEquals(partner1.id, it.partnerId) }
        assertEquals(2, response.summary.count)
        assertEquals(BigDecimal("30000"), response.summary.totalAmount)
    }

    @Test
    @DisplayName("/api/v1/payments - 상태로 필터링하여 APPROVED 상태의 결제만 조회한다")
    fun `상태로 필터링하여 APPROVED 상태의 결제만 조회한다`() {
        // Given: APPROVED와 CANCELED 상태의 결제 생성
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), paymentTime, PaymentStatus.APPROVED)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), paymentTime.plusSeconds(1), PaymentStatus.APPROVED)
        testDataFactory.createPayment(partner1.id, BigDecimal("15000"), paymentTime.plusSeconds(2), PaymentStatus.CANCELED)

        // When: APPROVED 상태로 필터링
        val response = getQueryResponse("/api/v1/payments?status=APPROVED")

        // Then: APPROVED 상태만 반환 (CANCELED는 제외됨)
        assertEquals(2, response.items.size)
        response.items.forEach { assertEquals("APPROVED", it.status.name) }
        assertEquals(2, response.summary.count)
    }

    @Test
    @DisplayName("/api/v1/payments - 기간으로 필터링하여 특정 기간 내의 결제만 조회한다")
    fun `기간으로 필터링하여 특정 기간 내의 결제만 조회한다`() {
        // Given: partner2 생성 및 기간별 결제 생성
        val partner2 = testDataFactory.createPartner("PARTNER2", "Partner 2", true)
        testDataFactory.createFeePolicy(
            partnerId = partner2.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0250"),
            fixedFee = BigDecimal("50"),
        )
        val earlyTime = Instant.parse("2024-01-10T00:00:00Z")
        val midTime = Instant.parse("2024-01-15T12:00:00Z")
        val toTime = Instant.parse("2024-01-20T23:59:59Z")
        val outOfRangeTime = Instant.parse("2024-02-01T00:00:00Z")

        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), earlyTime) // 포함
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), midTime) // 포함
        testDataFactory.createPayment(partner1.id, BigDecimal("15000"), toTime) // 포함 (2024-01-20 23:59:59 < 2024-01-21 00:00:00)
        testDataFactory.createPayment(partner2.id, BigDecimal("5000"), outOfRangeTime) // 제외

        // When: 2024-01-10 ~ 2024-01-21 00:00:00 기간으로 필터링
        val response = getQueryResponse(
            "/api/v1/payments?from=2024-01-10 00:00:00&to=2024-01-21 00:00:00"
        )

        // Then: 기간 내 결제만 반환
        assertEquals(3, response.items.size)
        assertEquals(3, response.summary.count)
        assertEquals(BigDecimal("45000"), response.summary.totalAmount)
    }

    @Test
    @DisplayName("/api/v1/payments - 복합 필터(파트너+상태+기간)를 적용하여 조회한다")
    fun `복합 필터를 적용하여 조회한다`() {
        // Given: partner2 생성 및 다양한 조건의 결제 생성
        val partner2 = testDataFactory.createPartner("PARTNER2", "Partner 2", true)
        testDataFactory.createFeePolicy(
            partnerId = partner2.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0250"),
            fixedFee = BigDecimal("50"),
        )
        val midTime = Instant.parse("2024-01-15T12:00:00Z")
        val outOfRangeTime = Instant.parse("2024-02-01T00:00:00Z")

        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), midTime) // 파트너1, 기간내
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), outOfRangeTime) // 파트너1, 기간외
        testDataFactory.createPayment(partner2.id, BigDecimal("15000"), midTime) // 파트너2, 기간내

        // When: 파트너1, APPROVED, 기간 필터 적용
        val response = getQueryResponse(
            "/api/v1/payments?partnerId=${partner1.id}&status=APPROVED&from=2024-01-10 00:00:00&to=2024-01-31 23:59:59"
        )

        // Then: 모든 조건을 만족하는 결제만 반환
        assertEquals(1, response.items.size)
        assertEquals(partner1.id, response.items[0].partnerId)
        assertEquals("APPROVED", response.items[0].status.name)
        assertEquals(1, response.summary.count)
    }

    @Test
    @DisplayName("/api/v1/payments - 커서 기반 페이지네이션: 첫 페이지에서 nextCursor를 받아 다음 페이지를 조회한다")
    fun `커서 기반 페이지네이션 첫 페이지에서 nextCursor를 받아 다음 페이지를 조회한다`() {
        // Given: 4개의 결제 생성 (limit=2로 페이지네이션)
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        repeat(4) { i ->
            testDataFactory.createPayment(partner1.id, BigDecimal("${(i + 1) * 10000}"), paymentTime.plusSeconds(i.toLong()))
        }

        // When: 첫 페이지 조회 (limit=2)
        val firstPage = getQueryResponse("/api/v1/payments?limit=2")

        // Then: 2개 항목, 다음 페이지 존재, nextCursor 반환
        assertEquals(2, firstPage.items.size)
        assertTrue(firstPage.hasNext)
        assertNotNull(firstPage.nextCursor)

        // When: 다음 페이지 조회
        val secondPage = getQueryResponse("/api/v1/payments?limit=2&cursor=${firstPage.nextCursor}")

        // Then: 다음 2개 항목 반환
        assertEquals(2, secondPage.items.size)
        // 첫 페이지와 중복되지 않음 (커서 기반 페이지네이션)
        assertTrue(firstPage.items.map { it.id }.intersect(secondPage.items.map { it.id }).isEmpty())
    }

    @Test
    @DisplayName("/api/v1/payments - 커서 기반 페이지네이션: 마지막 페이지에서는 hasNext가 false이고 nextCursor가 null이다")
    fun `커서 기반 페이지네이션 마지막 페이지에서는 hasNext가 false이고 nextCursor가 null이다`() {
        // Given: 3개의 결제 생성 (limit=2로 마지막 페이지는 1개)
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        repeat(3) { i ->
            testDataFactory.createPayment(partner1.id, BigDecimal("${(i + 1) * 10000}"), paymentTime.plusSeconds(i.toLong()))
        }

        // When: 첫 페이지 조회
        val firstPage = getQueryResponse("/api/v1/payments?limit=2")
        assertTrue(firstPage.hasNext)
        assertNotNull(firstPage.nextCursor)

        // When: 마지막 페이지 조회
        val lastPage = getQueryResponse("/api/v1/payments?limit=2&cursor=${firstPage.nextCursor}")

        // Then: 마지막 페이지 표시
        assertEquals(1, lastPage.items.size)
        assertEquals(false, lastPage.hasNext)
        assertNull(lastPage.nextCursor)
    }

    @Test
    @DisplayName("/api/v1/payments - 통계 집계 검증: summary는 items와 동일한 필터 조건으로 집계된다")
    fun `통계 집계 검증 summary는 items와 동일한 필터 조건으로 집계된다`() {
        // Given: partner2 생성 및 파트너별 결제 생성
        val partner2 = testDataFactory.createPartner("PARTNER2", "Partner 2", true)
        testDataFactory.createFeePolicy(
            partnerId = partner2.id,
            effectiveFrom = "2024-01-01T00:00:00Z",
            percentage = BigDecimal("0.0250"),
            fixedFee = BigDecimal("50"),
        )
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), paymentTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), paymentTime.plusSeconds(1))
        testDataFactory.createPayment(partner1.id, BigDecimal("15000"), paymentTime.plusSeconds(2))
        testDataFactory.createPayment(partner2.id, BigDecimal("5000"), paymentTime.plusSeconds(3))

        // When: 파트너1로 필터링하여 첫 페이지 조회 (limit=2)
        val response = getQueryResponse("/api/v1/payments?partnerId=${partner1.id}&limit=2")

        // Then: items는 2개 (페이지네이션)이지만 summary는 필터 조건(파트너1)으로 전체 집계
        assertEquals(2, response.items.size)
        assertEquals(3, response.summary.count) // 파트너1의 전체 결제 개수
        assertEquals(BigDecimal("45000"), response.summary.totalAmount) // 파트너1의 전체 금액
    }

    @Test
    @DisplayName("/api/v1/payments - 잘못된 커서 처리: 유효하지 않은 커서는 무시하고 첫 페이지를 반환한다")
    fun `잘못된 커서 처리 유효하지 않은 커서는 무시하고 첫 페이지를 반환한다`() {
        // Given: 결제 생성
        val paymentTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), paymentTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), paymentTime.plusSeconds(1))

        // When: 유효하지 않은 커서로 조회
        val response = getQueryResponse("/api/v1/payments?cursor=INVALID_CURSOR")

        // Then: 첫 페이지 반환 (커서가 무시됨)
        assertEquals(2, response.items.size)
    }

    @Test
    @DisplayName("/api/v1/payments - 정렬 검증: createdAt desc, id desc 순서로 정렬된다")
    fun `정렬 검증 createdAt desc, id desc 순서로 정렬된다`() {
        // Given: 같은 시간에 여러 결제 생성 (ID로 구분)
        val sameTime = Instant.parse("2024-01-15T12:00:00Z")
        testDataFactory.createPayment(partner1.id, BigDecimal("10000"), sameTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("20000"), sameTime)
        testDataFactory.createPayment(partner1.id, BigDecimal("15000"), sameTime)

        // When: 조회
        val response = getQueryResponse("/api/v1/payments?partnerId=${partner1.id}")

        // Then: 최신순(ID 내림차순)으로 정렬
        assertTrue(response.items.size >= 3)
        val ids = response.items.take(3).mapNotNull { it.id }
        // ID가 내림차순인지 확인 (최신이 먼저)
        for (i in 0 until ids.size - 1) {
            assertTrue(ids[i]!! >= ids[i + 1]!!, "ID는 내림차순이어야 합니다")
        }
    }



}
