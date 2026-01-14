package im.bigs.pg.application.pg.factory

import im.bigs.pg.application.pg.port.out.BasePgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.TestPgApproveExtra
import im.bigs.pg.domain.pg.PgCode

/**
 * PG 승인 요청 팩토리.
 * - pgCode에 따라 적절한 BasePgApproveRequest 구현체를 생성합니다.
 * - 상태가 없는 순수 함수이므로 object로 구현합니다.
 */
object PgApproveRequestFactory {
    /**
     * pgCode에 따라 적절한 BasePgApproveRequest 구현체를 생성합니다.
     *
     * @param pgCode PG 코드
     * @param request 기본 승인 요청 정보
     * @return pgCode에 맞는 BasePgApproveRequest 구현체
     */
    fun create(pgCode: PgCode, request: PgApproveRequest): BasePgApproveRequest {
        return when (pgCode) {
            PgCode.TEST_PG -> BasePgApproveRequest.TestPgApproveRequest(
                partnerId = request.partnerId,
                amount = request.amount,
                cardBin = request.cardBin,
                cardLast4 = request.cardLast4,
                productName = request.productName,
                extra = TestPgApproveExtra() // 기본값 사용
            )
            PgCode.MOCK -> BasePgApproveRequest.MockPgApproveRequest(
                partnerId = request.partnerId,
                amount = request.amount,
                cardBin = request.cardBin,
                cardLast4 = request.cardLast4,
                productName = request.productName
            )
            else -> throw IllegalArgumentException("Unsupported PG code: $pgCode")
        }
    }
}
