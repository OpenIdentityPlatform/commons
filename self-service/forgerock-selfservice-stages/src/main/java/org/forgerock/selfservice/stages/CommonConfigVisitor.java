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

package org.forgerock.selfservice.stages;

import org.forgerock.selfservice.stages.captcha.CaptchaConfigVisitor;
import org.forgerock.selfservice.stages.email.EmailConfigVisitor;
import org.forgerock.selfservice.stages.kba.KbaConfigVisitor;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfigVisitor;
import org.forgerock.selfservice.stages.reset.ResetConfigVisitor;
import org.forgerock.selfservice.stages.user.UserDetailsConfigVisitor;

/**
 * Represents all stage config visitors defined within commons.
 *
 * @since 0.3.0
 */
public interface CommonConfigVisitor extends CaptchaConfigVisitor, EmailConfigVisitor, KbaConfigVisitor,
        UserRegistrationConfigVisitor, ResetConfigVisitor, UserDetailsConfigVisitor {

}
