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
 */

package org.forgerock.selfservice.stages.email;

import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.StageType;

/**
 * Configuration for the email stage.
 *
 * @since 0.1.0
 */
public class EmailStageConfig implements StageConfig {

    /**
     * Email stage type.
     */
    public static final StageType<EmailStageConfig> TYPE =
            StageType.valueOf("emailValidation", EmailStageConfig.class);

    private String identityServiceUrl;
    private String identityIdField;
    private String identityEmailField;

    private String emailServiceUrl;
    private String emailSubject;
    private String emailMessage;
    private String emailFrom;

    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
    }

    public String getIdentityIdField() {
        return identityIdField;
    }

    public void setIdentityIdField(String identityIdField) {
        this.identityIdField = identityIdField;
    }

    public String getIdentityEmailField() {
        return identityEmailField;
    }

    public void setIdentityEmailField(String identityEmailField) {
        this.identityEmailField = identityEmailField;
    }

    public String getEmailServiceUrl() {
        return emailServiceUrl;
    }

    public void setEmailServiceUrl(String emailServiceUrl) {
        this.emailServiceUrl = emailServiceUrl;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailMessage() {
        return emailMessage;
    }

    public void setEmailMessage(String emailMessage) {
        this.emailMessage = emailMessage;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    @Override
    public StageType<?> getStageType() {
        return TYPE;
    }

}
