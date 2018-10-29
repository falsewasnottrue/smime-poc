import play.api.libs.mailer.Email

object Test extends App {

  val mailer = new Mailer()

  val clearTextEmail = new Email(
    subject = "Test",
    from = "test@test.org",
    to = List("testmail.nemo@springernature.com"),
    bodyText = Some("Dies ist ein Text")
  )

  val encryptedMail = SMimePoc.encrypt(clearTextEmail)

  mailer.send(encryptedMail)
}
