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

package org.forgerock.caf.authentication.api;

import org.forgerock.http.Context;

/**
 * <p>The authentication framework uses this {@code Context} to pass messages and message
 * processing state to authentication contexts for processing by authentication modules.</p>
 *
 * <p>This class encapsulates a request and response message objects for a message exchange. This
 * class may also be used to associate additional context in the form of key/value pairs, with the
 * encapsulated messages.</p>
 *
 * @see javax.security.auth.message.MessageInfo
 *
 * @since 2.0.0
 */
public interface MessageContext extends Context, MessageContextInfo {

    //Methods like setAuthContextState(...) and getAuthContextState(...) will end up here
    //  which do not want to be exposed to auth modules
}
