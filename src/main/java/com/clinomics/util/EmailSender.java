package com.clinomics.util;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.clinomics.entity.lims.Sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Component
public class EmailSender {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendMailToFail(List<Sample> failSamples) {
        try {
            MimeMessage msg = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setSubject("Chip 분석 실패 메일");
            helper.setTo(new String[] {"dlee@clinomics.co.kr"});


            Context context =  new Context();
            context.setVariable("samples", failSamples);

            String html = templateEngine.process("mail/template", context);
            helper.setText(html, true);
            
            javaMailSender.send(msg);

        } catch (MessagingException e) {
            
            e.printStackTrace();
        }
        
    }
}