package im.bigs.pg.application.pg.service

import im.bigs.pg.application.pg.port.out.PgClient
import im.bigs.pg.domain.pg.PgCode
import org.springframework.stereotype.Component

/**
 * PG 클라이언트 레지스트리.
 * - 등록된 모든 PgClient를 PG 코드별로 매핑하여 관리합니다.
 */
@Component
class PgClientRegistry(
    private val pgClients: List<PgClient>,
) {
    private val clientMap: Map<PgCode, PgClient> = run {
        pgClients.associateBy { extractPgCode(it) }
    }

    fun getClient(pgCode: PgCode): PgClient? {
        return clientMap[pgCode]
    }

    private fun extractPgCode(client: PgClient): PgCode {        // 클라이언트 구현체의 클래스명이나 어노테이션으로 PG 코드 추출
        return when (client::class.simpleName) {
            "MockPgClient" -> PgCode.MOCK
            "TossPayClient" -> PgCode.TOSSPAY
            "NhnKcpClient" -> PgCode.NHN_KCP
            "KgInicisClient" -> PgCode.KG_INICIS
            else -> throw IllegalArgumentException("Unknown PG client: ${client::class.simpleName}")
        }
    }
}

