package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import java.math.BigDecimal

@Schema(description = "결제 생성 요청")
data class CreatePaymentRequest(
    @Schema(description = "제휴사 ID", example = "1", required = true)
    val partnerId: Long,
    
    @field:Min(1)
    @field:Digits(integer = 10, fraction = 0)
    @Schema(description = "결제 금액 (최소 1 이상, 정수만 허용)", example = "10000", required = true)
    val amount: BigDecimal,
    
    @Schema(description = "카드 BIN (선택사항)", example = "123456", required = false)
    val cardBin: String? = null,
    
    @Schema(description = "카드 마지막 4자리 (선택사항)", example = "1234", required = false)
    val cardLast4: String? = null,
    
    @Schema(description = "상품명 (선택사항)", example = "테스트 상품", required = false)
    val productName: String? = null,
)

