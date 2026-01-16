package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.factory.ApplicationTestDataFactory
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPageWithSummary
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.application.payment.util.CursorEncoder
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QueryPaymentsServiceTest {
    private val paymentOutPort = mockk<PaymentOutPort>()
    private val service = QueryPaymentsService(paymentOutPort)

    @Test
    @DisplayName("변환 로직 검증 - 복합 필터가 올바르게 변환되어 전달된다")
    fun `변환 로직 검증 - 복합 필터가 올바르게 변환되어 전달된다`() {
        // Given: 파트너 1L, APPROVED 상태, 2024-01-01 ~ 2024-01-31
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
        val filter = QueryFilter(
            partnerId = 1L,
            status = "APPROVED",
            from = from,
            to = to,
        )
        val payments = listOf(
            ApplicationTestDataFactory.createPayment(1L, 1L, BigDecimal("10000"), PaymentStatus.APPROVED, from.plusDays(5)),
        )
        val summaryProjection = PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9700"),
        )
        val paymentPage = PaymentPageWithSummary(
            items = payments,
            summary = summaryProjection,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )

        every { paymentOutPort.findPageWithSummary(any()) } returns paymentPage

        // When: 조회 실행
        service.query(filter)

        // Then: 모든 필터 조건이 올바르게 변환되어 전달되었는지 확인
        val querySlot = slot<im.bigs.pg.application.payment.port.out.PaymentQuery>()
        verify { paymentOutPort.findPageWithSummary(capture(querySlot)) }
        assertEquals(1L, querySlot.captured.partnerId)
        assertEquals(PaymentStatus.APPROVED, querySlot.captured.status)
        assertEquals(from.toInstant(ZoneOffset.UTC).epochSecond, querySlot.captured.from?.epochSecond)
        assertEquals(to.toInstant(ZoneOffset.UTC).epochSecond, querySlot.captured.to?.epochSecond)
    }

    @Test
    @DisplayName("커서 인코딩 검증 - hasNext가 true일 때 nextCursor가 올바르게 인코딩된다")
    fun `커서 인코딩 검증 - hasNext가 true일 때 nextCursor가 올바르게 인코딩된다`() {
        // Given: 첫 페이지 요청 (limit=2, 다음 페이지 존재)
        val filter = QueryFilter(limit = 2)
        val payment1 = ApplicationTestDataFactory.createPayment(1L, 1L, BigDecimal("10000"), PaymentStatus.APPROVED)
        val payment2 = ApplicationTestDataFactory.createPayment(2L, 1L, BigDecimal("20000"), PaymentStatus.APPROVED)
        val lastCreatedAt = payment2.createdAt.toInstant(ZoneOffset.UTC)
        val lastId = payment2.id!!

        val summaryProjection = PaymentSummaryProjection(
            count = 2,
            totalAmount = BigDecimal("30000"),
            totalNetAmount = BigDecimal("29100"),
        )
        val paymentPage = PaymentPageWithSummary(
            items = listOf(payment1, payment2),
            summary = summaryProjection,
            hasNext = true,
            nextCursorCreatedAt = lastCreatedAt,
            nextCursorId = lastId,
        )

        every { paymentOutPort.findPageWithSummary(any()) } returns paymentPage

        // When: 조회 실행
        val result = service.query(filter)

        // Then: nextCursor가 올바르게 인코딩됨
        assertNotNull(result.nextCursor)
        val (decodedCreatedAt, decodedId) = CursorEncoder.decode(result.nextCursor)
        assertNotNull(decodedCreatedAt)
        assertNotNull(decodedId)
        assertEquals(lastCreatedAt.epochSecond, decodedCreatedAt.epochSecond)
        assertEquals(lastId, decodedId)
    }

    @Test
    @DisplayName("커서 디코딩 검증 - 커서가 올바르게 디코딩되어 전달된다")
    fun `커서 디코딩 검증 - 커서가 올바르게 디코딩되어 전달된다`() {
        // Given: 이전 페이지의 마지막 항목
        val lastCreatedAt = Instant.parse("2024-01-15T12:00:00Z")
        val lastId = 10L
        val cursor = CursorEncoder.encode(lastCreatedAt, lastId)
        val filter = QueryFilter(cursor = cursor, limit = 2)

        val payment3 = ApplicationTestDataFactory.createPayment(3L, 1L, BigDecimal("15000"), PaymentStatus.APPROVED)
        val payment4 = ApplicationTestDataFactory.createPayment(4L, 1L, BigDecimal("25000"), PaymentStatus.APPROVED)

        val summaryProjection = PaymentSummaryProjection(
            count = 2,
            totalAmount = BigDecimal("40000"),
            totalNetAmount = BigDecimal("38800"),
        )
        val paymentPage = PaymentPageWithSummary(
            items = listOf(payment3, payment4),
            summary = summaryProjection,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )

        every { paymentOutPort.findPageWithSummary(any()) } returns paymentPage

        // When: 다음 페이지 조회
        service.query(filter)

        // Then: 커서가 올바르게 디코딩되어 전달되었는지 확인
        val querySlot = slot<im.bigs.pg.application.payment.port.out.PaymentQuery>()
        verify { paymentOutPort.findPageWithSummary(capture(querySlot)) }
        assertEquals(lastCreatedAt.epochSecond, querySlot.captured.cursorCreatedAt?.epochSecond)
        assertEquals(lastId, querySlot.captured.cursorId)
    }

    @Test
    @DisplayName("커서 null 처리 검증 - 잘못된 커서는 null로 처리되어 전달된다")
    fun `커서 null 처리 검증 - 잘못된 커서는 null로 처리되어 전달된다`() {
        // Given: 유효하지 않은 커서 문자열
        val filter = QueryFilter(cursor = "INVALID_CURSOR", limit = 20)
        val payments = listOf(
            ApplicationTestDataFactory.createPayment(1L, 1L, BigDecimal("10000"), PaymentStatus.APPROVED),
        )
        val summaryProjection = PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9700"),
        )
        val paymentPage = PaymentPageWithSummary(
            items = payments,
            summary = summaryProjection,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null,
        )

        every { paymentOutPort.findPageWithSummary(any()) } returns paymentPage

        // When: 잘못된 커서로 조회
        service.query(filter)

        // Then: 커서가 null로 전달되었는지 확인
        val querySlot = slot<im.bigs.pg.application.payment.port.out.PaymentQuery>()
        verify { paymentOutPort.findPageWithSummary(capture(querySlot)) }
        assertEquals(null, querySlot.captured.cursorCreatedAt)
        assertEquals(null, querySlot.captured.cursorId)
    }
}
