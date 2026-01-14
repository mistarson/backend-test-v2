package im.bigs.pg.common.exception

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.application.exception.CustomException
import im.bigs.pg.application.exception.ErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * 전역 예외 핸들러.
 * - 애플리케이션 계층에서 발생한 예외를 적절한 HTTP 상태 코드로 변환합니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 잘못된 요청 파라미터 처리 (IllegalArgumentException)
     * - 존재하지 않는 파트너, 비활성 파트너 등
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status)
            .body(
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = status.value(),
                    message = e.message ?: "Bad Request"
                )
            )
    }

    /**
     * 잘못된 상태 처리 (IllegalStateException)
     * - 수수료 정책이 없는 경우 등
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status)
            .body(
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = status.value(),
                    message = e.message ?: "Internal Server Error"
                )
            )
    }

    /**
     * Validation 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val errors = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(status)
            .body(
                ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = status.value(),
                    message = "Validation failed: $errors"
                )
            )
    }

    /**
     * 커스텀 예외 처리 (CustomException 상속 예외)
     */
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse.of(e.errorCode, e.message)
        return ResponseEntity.status(errorResponse.status).body(errorResponse)
    }

    /**
     * 에러 응답 DTO
     */
    data class ErrorResponse(
        @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        val timestamp: LocalDateTime,
        val status: Int,
        val message: String
    ) {
        companion object {
            fun of(errorCode: ErrorCode, message: String? = null): ErrorResponse {
                return ErrorResponse(
                    timestamp = LocalDateTime.now(),
                    status = errorCode.status,
                    message = message ?: errorCode.message
                )
            }
        }
    }
}
