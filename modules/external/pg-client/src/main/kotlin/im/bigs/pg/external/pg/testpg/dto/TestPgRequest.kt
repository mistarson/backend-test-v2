package im.bigs.pg.external.pg.testpg.dto

/**
 * TestPG API 요청 데이터 모델 (암호화 전 평문).
 */
data class TestPgRequest(
    val cardNumber: String,
    val birthDate: String,
    val expiry: String,
    val password: String,
    val amount: Long,
)

