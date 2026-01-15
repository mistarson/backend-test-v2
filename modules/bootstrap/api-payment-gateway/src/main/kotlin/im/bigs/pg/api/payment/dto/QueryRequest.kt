package im.bigs.pg.api.payment.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

/**
 * 결제 조회 요청 파라미터.
 * - 쿼리 파라미터로 전달되는 필터 조건입니다.
 */
data class QueryRequest(
    val partnerId: Long? = null,
    
    @field:Pattern(regexp = "APPROVED|CANCELED", message = "status must be APPROVED or CANCELED")
    val status: String? = null,
    
    @field:DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val from: LocalDateTime? = null,
    
    @field:DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val to: LocalDateTime? = null,
    
    val cursor: String? = null,
    
    @field:Min(value = 1, message = "limit must be at least 1")
    @field:Max(value = 20, message = "limit must be at most 20")
    val limit: Int = 20,
)
