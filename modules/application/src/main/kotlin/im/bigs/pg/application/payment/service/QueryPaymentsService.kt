package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.*
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.application.payment.util.CursorEncoder
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset

/**
 * 결제 이력 조회 유스케이스 구현체.
 * - 커서 토큰은 createdAt/id를 안전하게 인코딩해 전달/복원합니다.
 * - 통계는 조회 조건과 동일한 집합을 대상으로 계산됩니다.
 */
@Service
class QueryPaymentsService(
    private val paymentOutPort: PaymentOutPort,
) : QueryPaymentsUseCase {
    /**
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * 현재 구현은 과제용 목업으로, 빈 결과를 반환합니다.
     * 지원자는 커서 기반 페이지네이션과 통계 집계를 완성하세요.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {
        // Step 1: 커서 디코딩 및 PaymentQuery 생성
        val (cursorCreatedAt, cursorId) = CursorEncoder.decode(filter.cursor)
        val paymentPage = paymentOutPort.findBy(
            filter.toPaymentQuery(cursorCreatedAt, cursorId)
        )

        // Step 2: 통계 조회 (동일한 필터 조건 사용, 커서 제외)
        val summary = paymentOutPort.summary(filter.toPaymentSummaryFilter())
            .toPaymentSummary()

        // Step 3: 결과 변환 및 커서 인코딩
        return QueryResult(
            items = paymentPage.items,
            summary = summary,
            nextCursor = paymentPage.toNextCursor(),
            hasNext = paymentPage.hasNext,
        )
    }

}

/** QueryFilter를 PaymentQuery로 변환합니다. */
private fun QueryFilter.toPaymentQuery(cursorCreatedAt: Instant?, cursorId: Long?): PaymentQuery {
    return PaymentQuery(
        partnerId = this.partnerId,
        status = this.status?.let { PaymentStatus.valueOf(it) },
        from = this.from?.toInstant(ZoneOffset.UTC),
        to = this.to?.toInstant(ZoneOffset.UTC),
        limit = this.limit,
        cursorCreatedAt = cursorCreatedAt,
        cursorId = cursorId,
    )
}

/** QueryFilter를 PaymentSummaryFilter로 변환합니다. (커서 제외) */
private fun QueryFilter.toPaymentSummaryFilter(): PaymentSummaryFilter {
    return PaymentSummaryFilter(
        partnerId = this.partnerId,
        status = this.status?.let { PaymentStatus.valueOf(it) },
        from = this.from,
        to = this.to,
    )
}

/** PaymentSummaryProjection을 PaymentSummary로 변환합니다. */
private fun PaymentSummaryProjection.toPaymentSummary(): PaymentSummary {
    return PaymentSummary(
        count = this.count,
        totalAmount = this.totalAmount,
        totalNetAmount = this.totalNetAmount,
    )
}

/** PaymentPage에서 다음 페이지 커서를 생성합니다. */
private fun PaymentPage.toNextCursor(): String? =
    takeIf { hasNext }?.let {
        CursorEncoder.encode(nextCursorCreatedAt, nextCursorId)
    }
