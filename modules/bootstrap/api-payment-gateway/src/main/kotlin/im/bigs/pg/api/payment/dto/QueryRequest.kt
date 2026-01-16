package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

/**
 * 결제 조회 요청 파라미터.
 * - 쿼리 파라미터로 전달되는 필터 조건입니다.
 */
@Schema(description = "결제 조회 요청 파라미터")
data class QueryRequest(
    @Schema(description = "제휴사 ID (선택사항)", example = "1", required = false)
    val partnerId: Long? = null,

    @field:Pattern(regexp = "APPROVED|CANCELED", message = "status must be APPROVED or CANCELED")
    @Schema(description = "결제 상태 (선택사항)", example = "APPROVED", allowableValues = ["APPROVED", "CANCELED"], required = false)
    val status: String? = null,

    @field:DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "조회 시작 시각 (선택사항)", example = "2024-01-01 00:00:00", required = false)
    val from: LocalDateTime? = null,

    @field:DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "조회 종료 시각 (선택사항)", example = "2024-12-31 23:59:59", required = false)
    val to: LocalDateTime? = null,

    @Schema(description = "커서 (페이지네이션용, 선택사항)", example = "eyJpZCI6MSwiY3JlYXRlZEF0IjoiMjAyNC0wMS0wMSAxMjowMDowMCJ9", required = false)
    val cursor: String? = null,

    @field:Min(value = 1, message = "limit must be at least 1")
    @field:Max(value = 20, message = "limit must be at most 20")
    @Schema(description = "조회할 최대 개수 (기본값: 20, 최대: 20)", example = "20", defaultValue = "20")
    val limit: Int = 20,
)
