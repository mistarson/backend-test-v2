package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "결제 조회 응답")
data class QueryResponse(
    @Schema(description = "결제 목록")
    val items: List<PaymentResponse>,

    @Schema(description = "통계 정보")
    val summary: Summary,

    @Schema(description = "다음 페이지 커서 (null이면 다음 페이지 없음)", example = "eyJpZCI6MSwiY3JlYXRlZEF0IjoiMjAyNC0wMS0wMSAxMjowMDowMCJ9", required = false)
    val nextCursor: String?,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)

@Schema(description = "결제 통계 정보")
data class Summary(
    @Schema(description = "결제 건수", example = "100")
    val count: Long,

    @Schema(description = "총 결제 금액", example = "1000000")
    val totalAmount: BigDecimal,

    @Schema(description = "총 공제 후 금액", example = "970000")
    val totalNetAmount: BigDecimal,
)
