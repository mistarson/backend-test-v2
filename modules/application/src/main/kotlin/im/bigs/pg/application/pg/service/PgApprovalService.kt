package im.bigs.pg.application.pg.service

import im.bigs.pg.application.log.LoggingPort
import im.bigs.pg.application.pg.exception.PgApprovalException
import im.bigs.pg.application.pg.exception.PgClientNotFoundException
import im.bigs.pg.application.pg.factory.PgApproveRequestFactory
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.registry.PgClientRegistry
import im.bigs.pg.application.pg.resolver.PgClientResolver
import im.bigs.pg.domain.pg.PgCode
import org.springframework.stereotype.Service

/**
 * PG 승인 처리 서비스. PG 목록을 순차적으로 시도하고 실패 시 폴백합니다.
 */
@Service
class PgApprovalService(
    private val pgClientResolver: PgClientResolver,
    private val pgClientRegistry: PgClientRegistry,
    private val logger: LoggingPort,
) {
    fun approve(request: PgApproveRequest): PgApproveResult {
        logger.debug("PG 승인 요청: partnerId={}, amount={}", request.partnerId, request.amount)

        val pgCodes = pgClientResolver.resolve(request.partnerId)
            .ifEmpty { throw PgClientNotFoundException("No supported PG found for partner") }

        logger.debug("PG 목록: {}", pgCodes.joinToString(", "))

        val failures = mutableListOf<Throwable>()
        val approvalResult = pgCodes.firstNotNullOfOrNull { pgCode ->
            tryApprove(pgCode, request, failures)
        }

        if (approvalResult == null) {
            logger.error(
                "PG 승인 실패: partnerId={}, 시도한 PG 수={}, PG 목록={}",
                request.partnerId,
                pgCodes.size,
                pgCodes.joinToString(", "),
            )
            throw PgApprovalException(
                "Failed to approve payment for partner ${request.partnerId}. Tried ${pgCodes.size} PG(s): ${pgCodes.joinToString(", ")}",
                failures.lastOrNull()
            )
        }

        logger.info(
            "PG 승인 성공: partnerId={}, approvalCode={}, status={}",
            request.partnerId,
            approvalResult.approvalCode,
            approvalResult.status,
        )
        return approvalResult
    }

    private fun tryApprove(
        pgCode: PgCode,
        request: PgApproveRequest,
        failures: MutableList<Throwable>
    ): PgApproveResult? {
        return runCatching {
            val client = pgClientRegistry.getClient(pgCode) ?: run {
                logger.warn("PG Client를 찾을 수 없음: pgCode={}", pgCode)
                return null
            }

            logger.debug("PG 승인 시도: pgCode={}, partnerId={}", pgCode, request.partnerId)
            val pgRequest = PgApproveRequestFactory.create(pgCode, request)
            client.approve(pgRequest)
        }
            .onFailure {
                logger.warn("PG 승인 실패: pgCode={}, error={}", pgCode, it.message)
                failures.add(it)
            }
            .getOrNull()
    }
}
