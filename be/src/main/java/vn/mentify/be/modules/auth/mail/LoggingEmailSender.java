package vn.mentify.be.modules.auth.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Email delivery is disabled; message prepared for recipient domain={} subject={}",
                domainOf(message.to()), message.subject());
    }

    private String domainOf(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(at + 1) : "unknown";
    }
}
