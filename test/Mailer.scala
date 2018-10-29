import org.apache.commons.mail.{HtmlEmail, MultiPartEmail}
import play.api.libs.mailer.{CommonsMailer, MailerClient, SMTPConfiguration, Email}

class Mailer {

  val smtpConfiguration: SMTPConfiguration = SMTPConfiguration(
    // host = "mx3.springer.com",
    // port = 25
    host = "smtp.gmail.com",
    port = 465,
    user = Some("testmailing31@gmail.com"),
    password= Some("test!12345"),
    ssl = true,
    timeout = Some(30000),
    connectionTimeout = Some(30000)
  )

  private lazy val instance: MailerClient =
    new CommonsMailer(smtpConfiguration) {
      override def send(email: MultiPartEmail): String = email.send()
      override def createMultiPartEmail(): MultiPartEmail = new MultiPartEmail()
      override def createHtmlEmail(): HtmlEmail = new HtmlEmail()
    }

  def send(data: Email): String = {
    instance.send(data)
  }
}
