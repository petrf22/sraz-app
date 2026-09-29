package cz.petrf.sraz.service;

import cz.petrf.sraz.exception.EmailException;
import cz.petrf.sraz.exception.InvalidEmailDomainException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Odesílání HTML e-mailů přes SMTP (Spring Mail) – v produkci STARTTLS na portu 587
 * (application-prod.properties), ve vývoji Mailpit na localhost:1025.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final DisposableEmailService disposableEmailService;

  @Retryable(MailException.class)
  public void sendHtmlEmail(String fromEmail, String toEmail, String mailSubject, String htmlContent) {
    if (disposableEmailService.isDisposable(toEmail)) {
      log.error("isDisposable :: toEmail: {} == true", toEmail);
      throw new InvalidEmailDomainException("Doména pro e-mail %s není povolená.".formatted(toEmail));
    }

    MimeMessage message = mailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(mailSubject);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info("sendHtmlEmail :: odesláno na {}: {}", toEmail, mailSubject);
    } catch (MessagingException e) {
      log.error("sendHtmlEmail :: chyba při sestavení e-mailu pro {}", toEmail, e);
      throw new EmailException("Chyba při odeslání e-mailu");
    }
  }
}
