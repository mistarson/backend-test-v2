package im.bigs.pg.application.pg.port.out

import java.math.BigDecimal

/**
 * PG 승인 요청 공통 데이터 sealed class.
 */
sealed class BasePgApproveRequest(
    val partnerId: Long,
    val amount: BigDecimal,
    val cardBin: String?,
    val cardLast4: String?,
    val productName: String?
) {
    /**
     * TestPG 승인 요청.
     */
    class TestPgApproveRequest(
        partnerId: Long,
        amount: BigDecimal,
        cardBin: String?,
        cardLast4: String?,
        productName: String?,
        val extra: TestPgApproveExtra
    ) : BasePgApproveRequest(partnerId, amount, cardBin, cardLast4, productName)

    /**
     * MockPG 승인 요청.
     */
    class MockPgApproveRequest(
        partnerId: Long,
        amount: BigDecimal,
        cardBin: String?,
        cardLast4: String?,
        productName: String?
    ) : BasePgApproveRequest(partnerId, amount, cardBin, cardLast4, productName)
}
