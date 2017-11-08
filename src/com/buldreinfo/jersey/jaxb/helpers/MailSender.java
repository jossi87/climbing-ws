package com.buldreinfo.jersey.jaxb.helpers;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailSender {
	public static void sendMail(final String toRecipient, final String subject, final String body) throws AddressException, MessagingException, UnsupportedEncodingException {
		final Properties p = new Properties();
		p.put("mail.smtp.auth", "true");
		p.put("mail.smtp.starttls.enable", "true");
		p.put("mail.smtp.host", "smtp.domeneshop.no");
		p.put("mail.smtp.port", "587");

		Session session = Session.getInstance(p, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("jossiorg1", "fp-forme-700-skyts-Unikt");
			}
		});

		InternetAddress[] to = new InternetAddress[1];
		to[0] = new InternetAddress(toRecipient);

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("no-reply@jossi.org", "no-reply@jossi.org"));
		message.setRecipients(Message.RecipientType.TO, to);
		message.setSubject(subject);

		// Create the message part 
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		// Fill message
		messageBodyPart.setText(body);
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		// Put parts in message
		message.setContent(multipart);

		Transport.send(message);
	}
}