package vn.mentify.be.modules.auth.mail;

public interface EmailSender {

    void send(EmailMessage message);
}
