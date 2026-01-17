package cz.petrf.sraz.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import cz.petrf.sraz.exception.EmailException;
import cz.petrf.sraz.exception.InvalidEmailDomainException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final DisposableEmailService disposableEmailService;
  @Value("${app.mail.resend.api.key:}")
  private String resendApiKey;
  private Resend resend = null;

  @Retryable(value = {MailException.class})
  public void sendHtmlEmail(String fromEmail, String toEmail, String mailSubject, String htmlContent) {
    if (disposableEmailService.isDisposable(toEmail)) {
      log.error("isDisposable :: toEmail: {} == false", toEmail);
      throw new InvalidEmailDomainException("Doména pro e-mail %s není povolená.".formatted(toEmail));
    }

    if (resend!=null) {
      sendOverResend(fromEmail, toEmail, mailSubject, htmlContent);
    } else {
      sendOverJavaMail(fromEmail, toEmail, mailSubject, htmlContent);
    }
  }

  private void sendOverResend(String fromEmail, String toEmail, String mailSubject, String htmlContent) {
    log.info("sendOverResend from {} to {} ", fromEmail, toEmail);

    CreateEmailOptions params = CreateEmailOptions.builder()
        .from(fromEmail)
        .to(toEmail)
        .subject(mailSubject)
        .html(htmlContent)
        .build();

    try {
      CreateEmailResponse data = Objects.requireNonNull(resend).emails().send(params);
      log.info("Magic link byl odeslán na email: {} (email ID: {})", toEmail, data.getId());
    } catch (ResendException e) {
      log.error("Chyba při odeslání emailu s přihlášením k aplikaci na adresu: {}", toEmail, e);
      throw new EmailException("Chyba při odeslání emailu s přihlášením k aplikaci");
    }
  }

  private void sendOverJavaMail(String fromEmail, String toEmail, String mailSubject, String htmlContent) {
    log.info("sendOverJavaMail from {} to {} ", fromEmail, toEmail);

    MimeMessage message = mailSender.createMimeMessage();

    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(mailSubject);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info("Magic link byl odeslán na email: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Chyba při odeslání emailu s přihlášením k aplikaci na adresu: {}", toEmail, e);
      throw new EmailException("Chyba při odeslání emailu s přihlášením k aplikaci");
    }
  }

  @PostConstruct
  public void postConstruct() {
    resend = Optional.ofNullable(StringUtils.trimToNull(resendApiKey))
        .map(Resend::new)
        .orElse(null);
  }
}