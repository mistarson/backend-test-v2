package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.util.CursorEncoder
import org.springframework.stereotype.Service

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
        val pageWithSummary = paymentOutPort.findPageWithSummary(
            filter.toPaymentQuery(cursorCreatedAt, cursorId)
        )

        // Step 2: 결과 변환 및 커서 인코딩
        return QueryResult(
            items = pageWithSummary.items,
            summary = pageWithSummary.summary.toPaymentSummary(),
            nextCursor = pageWithSummary.toNextCursor(),
            hasNext = pageWithSummary.hasNext,
        )
    }
}
