package im.bigs.pg.application.pg.port.out

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/** PG 승인 결과 요약. */
data class PgApproveResult(
    val approvalCode: String,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val approvedAt: LocalDateTime,
    val status: PaymentStatus, // TestPG 응답의 status를 그대로 반영하기 위해 기본값 제거
    val maskedCardLast4: String? = null,
    val amount: BigDecimal? = null,
)
