package im.bigs.pg.application.pg.port.out

/**
 * PG 클라이언트 인터페이스.
 * - 여러 PG사(토스페이, NHN KCP, KG이니시스 등)의 공통 승인 기능을 추상화합니다.
 * - 외부 경계 포트로서 실제 PG 클라이언트 구현체들이 이 인터페이스를 구현합니다.
 */
interface PgClient {
    /**
     * PG 승인 요청을 처리합니다.
     *
     * @param request 승인 요청 정보
     * @return 승인 결과
     */
    fun approve(request: PgApproveRequest): PgApproveResult
}
