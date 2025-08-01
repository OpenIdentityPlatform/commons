/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions copyright 2024 3A Systems LLC.
 */

package org.forgerock.selfservice.example;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newActionResponse;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Example email service.
 *
 * @since 0.1.0
 */
final class ExampleEmailService implements SingletonResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleEmailService.class);

    private final String host;
    private final String port;
    private final String username;
    private final String password;

    ExampleEmailService(JsonValue config) {
        host = config.get("host").required().asString();
        port = config.get("port").required().asString();
        username = System.getProperty("mailserver.username");
        password = System.getProperty("mailserver.password");
        Reject.ifNull(username, password);
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, ActionRequest request) {
        if (request.getAction().equals("send")) {
            try {
                JsonValue response = sendEmail(request.getContent());
                return newActionResponse(response).asPromise();
            } catch (ResourceException rE) {
                return rE.asPromise();
            }
        }

        return new NotSupportedException("Unknown action " + request.getAction()).asPromise();
    }

    private JsonValue sendEmail(JsonValue document) throws ResourceException {
        String to = document.get("to").asString();

        if (isEmpty(to)) {
            throw new BadRequestException("Field to is not specified");
        }

        String from = document.get("from").asString();

        if (isEmpty(from)) {
            throw new BadRequestException("Field from is not specified");
        }

        String subject = document.get("subject").asString();

        if (isEmpty(subject)) {
            throw new BadRequestException("Field subject is not specified");
        }

        String messageBody = document.get("body").asString();

        if (isEmpty(messageBody)) {
            throw new BadRequestException("Field message is not specified");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }

        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(messageBody, "text/html; charset=UTF-8");

            Transport.send(message);
            LOGGER.debug("Email sent to " + to);

        } catch (MessagingException mE) {
            throw new InternalServerErrorException(mE);
        }

        return json(object(field("status", "okay")));
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, ReadRequest request) {
        return new NotSupportedException().asPromise();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, UpdateRequest request) {
        return new NotSupportedException().asPromise();
    }

}
