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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.jaspi.logging;

import org.forgerock.auth.common.LoggingConfigurator;

import javax.security.auth.message.MessageInfo;

/**
 * Wrapper interface which contains the type for all LoggingConfigurators for the Jaspi Runtime.
 *
 * @since 1.3.0
 */
public interface JaspiLoggingConfigurator extends LoggingConfigurator<MessageInfo> {
}
