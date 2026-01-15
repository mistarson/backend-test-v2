package im.bigs.pg.api.payment

import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.*
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryRequest
import im.bigs.pg.api.payment.dto.QueryResponse
import im.bigs.pg.api.payment.dto.Summary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 결제 API 진입점.
 * - POST: 결제 생성
 * - GET: 결제 조회(커서 페이지네이션 + 통계)
 */
@Tag(name = "Payment", description = "결제 관련 API")
@RestController
@RequestMapping("/api/v1/payments")
@Validated
class PaymentController(
    private val paymentUseCase: PaymentUseCase,
    private val queryPaymentsUseCase: QueryPaymentsUseCase,
) {

    /** 결제 생성 요청 페이로드(간소화된 필드). */
    

    /** API 응답을 위한 변환용 DTO. 도메인 모델을 그대로 노출하지 않습니다. */
    

    /**
     * 결제 생성.
     *
     * @param req 결제 요청 본문
     * @return 생성된 결제 요약 응답
     */
    @Operation(
        summary = "결제 생성",
        description = "새로운 결제를 생성하고 승인 처리합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "결제 생성 성공",
                content = [Content(schema = Schema(implementation = PaymentResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효성 검증 실패)"
            )
        ]
    )
    @PostMapping
    fun create(@Valid @RequestBody req: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val saved = paymentUseCase.pay(
            PaymentCommand(
                partnerId = req.partnerId,
                amount = req.amount,
                cardBin = req.cardBin,
                cardLast4 = req.cardLast4,
                productName = req.productName,
            ),
        )
        return ResponseEntity.ok(PaymentResponse.from(saved))
    }

    /** 목록 + 통계를 포함한 조회 응답. */
    

    /**
     * 결제 조회(커서 기반 페이지네이션 + 통계).
     *
     * @param request 조회 요청 파라미터
     * @return 목록/통계/커서 정보
     */
    @Operation(
        summary = "결제 조회",
        description = "커서 기반 페이지네이션을 사용하여 결제 목록을 조회하고 통계 정보를 함께 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = QueryResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효성 검증 실패)"
            )
        ]
    )
    @GetMapping
    fun query(
        @ModelAttribute @Valid request: QueryRequest,
    ): ResponseEntity<QueryResponse> {
        val res = queryPaymentsUseCase.query(
            QueryFilter(
                partnerId = request.partnerId,
                status = request.status,
                from = request.from,
                to = request.to,
                cursor = request.cursor,
                limit = request.limit,
            ),
        )
        return ResponseEntity.ok(
            QueryResponse(
                items = res.items.map { PaymentResponse.from(it) },
                summary = Summary(res.summary.count, res.summary.totalAmount, res.summary.totalNetAmount),
                nextCursor = res.nextCursor,
                hasNext = res.hasNext,
            ),
        )
    }
}
