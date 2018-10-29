import java.io.{BufferedInputStream, ByteArrayOutputStream}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.util.Base64

import javax.mail.internet.{MimeBodyPart, MimeMessage}
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute
import org.bouncycastle.asn1.smime.SMIMECapability
import org.bouncycastle.asn1.smime.SMIMECapabilityVector
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cms.CMSAlgorithm
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.mail.smime.{SMIMEEnvelopedGenerator, SMIMEStreamingProcessor}
import play.api.libs.mailer.Email

object SMimePoc {

  import org.bouncycastle.jce.provider.BouncyCastleProvider
  import java.security.Security

  Security.addProvider(new BouncyCastleProvider)

  val cf = CertificateFactory.getInstance("X.509")
  val is = getClass.getResourceAsStream("certificates/tnp1511.pem")
  val caInput = new BufferedInputStream(is)

  val cert: X509Certificate = try {
    cf.generateCertificate(caInput).asInstanceOf[X509Certificate]
  } finally caInput.close()

  // encrypt message body
  val capabilities = new SMIMECapabilityVector
  capabilities.addCapability(SMIMECapability.dES_EDE3_CBC)
  capabilities.addCapability(SMIMECapability.rC2_CBC, 128)
  capabilities.addCapability(SMIMECapability.dES_CBC)

  val attributes = new ASN1EncodableVector()
  val issuerAndSerialNumber = new IssuerAndSerialNumber(
    new X500Name(cert.getIssuerDN.getName),
    cert.getSerialNumber
  )
  attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(issuerAndSerialNumber))
  attributes.add(new SMIMECapabilitiesAttribute(capabilities))

  val encrypter = new SMIMEEnvelopedGenerator
  encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert).setProvider("BC"))

  def encryptText(clearText: String): Array[Byte] = {
    val mimeBody = new MimeBodyPart
    mimeBody.setText(clearText)

    val encryptedBodyPart: MimeBodyPart = encrypter.generate(
      mimeBody,
      new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC).setProvider("BC").build()
    )

    println("content-type: " + encryptedBodyPart.getContentType)
    println("content-disposition: " + encryptedBodyPart.getDisposition)
    println("encoding: " + encryptedBodyPart.getEncoding)

    val out = new ByteArrayOutputStream()
    encryptedBodyPart.writeTo(out)

    val encryptedMessage = out.toByteArray
    encryptedMessage
  }

  def encrypt(email: Email): Email = {
    val encryptedMessageBytes = encryptText(email.bodyText.getOrElse(""))
    println(s"encryptedMessageBytes length: ${encryptedMessageBytes.length}")

    val encryptedMessage = Base64.getEncoder.encodeToString(encryptedMessageBytes)
    println(s"encryptedMessage $encryptedMessage")

    val encryptedMail = Email(
      subject = email.subject,
      from = email.from,
      to = email.to,
      bodyText = Some(encryptedMessage),
      headers = List(
        ("Content-Type", "application/pkcs7-mime; smime-type=enveloped-data;"),
        ("Content-Disposition", "attachment; filename=smime.p7m"),
        ("Content-Transfer-Encoding", "base64")
      ),
      attachments = List()
    )
    encryptedMail
  }
}
