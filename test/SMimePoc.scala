import java.io.{BufferedInputStream, ByteArrayOutputStream}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.util.Base64

import javax.mail.internet.MimeBodyPart
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

object SMimePoc extends App {

  import org.bouncycastle.jce.provider.BouncyCastleProvider
  import java.security.Security

  Security.addProvider(new BouncyCastleProvider)

  val clearText = "This is the clear text"

  val encrypted = encrypt(clearText)

  println(clearText)
  println(encrypted)

  def encrypt(clearText: String): String = {

    // load certificate
    val cf = CertificateFactory.getInstance("X.509")
    val is = getClass.getResourceAsStream("certificates/test.crt")
    val caInput = new BufferedInputStream(is)

    var cert: X509Certificate = null
    try
      cert = cf.generateCertificate(caInput).asInstanceOf[X509Certificate]
    finally caInput.close()

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

    val mimeBody = new MimeBodyPart
    mimeBody.setText(clearText)

    val encryptedMimeBody: MimeBodyPart = encrypter.generate(
      mimeBody,
      new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC).setProvider("BC").build()
    )

    println("content-type: " + encryptedMimeBody.getContentType)
    println("content-disposition: " + encryptedMimeBody.getDisposition)
    println("encoding: " + encryptedMimeBody.getEncoding)

    val encryptedMessage: String = encryptedMimeBody.getContent match {
      case processor: SMIMEStreamingProcessor =>
        val outputStream = new ByteArrayOutputStream
        processor.write(outputStream)

        val data = outputStream.toByteArray
        Base64.getEncoder.encodeToString(data)
      case _ => throw new IllegalArgumentException("unexpected content in encrypted mime body")
    }

    encryptedMessage
  }
}
