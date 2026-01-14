package im.bigs.pg.application.pg.port.out

/** TestPG 승인 요청. */
data class TestPgApproveExtra(
    val cardNumber: String = "1111-1111-1111-1111",
    val birthDate: String = "19900101",
    val expiry: String = "1227",
    val password: String = "12",
    val apiKey: String = "11111111-1111-4111-8111-111111111111",
    val ivBase64Url: String = "AAAAAAAAAAAAAAAA",
)
