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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.caf.authentication.framework;

import static javax.security.auth.message.AuthStatus.*;

import javax.security.auth.message.AuthStatus;

/**
 * Utility class providing utility methods for determining the meaning behind each of the different {@link AuthStatus}
 * values.
 *
 * @since 1.5.0
 */
public final class AuthStatusUtils {

    /**
     * Private utility constructor.
     */
    private AuthStatusUtils() { }

    /**
     * Converts the given {@code AuthStatus} into its {@code String} representation.
     *
     * @param authStatus The {@code AuthStatus}.
     * @return An AuthStatus String.
     */
    public static String asString(AuthStatus authStatus) {
        if (isSuccess(authStatus)) {
            return "SUCCESS";
        } else if (isFailure(authStatus)) {
            return "FAILURE";
        } else if (isSendContinue(authStatus)) {
            return "SEND_CONTINUE";
        } else if (isSendFailure(authStatus)) {
            return "SEND_FAILURE";
        } else if (isSendSuccess(authStatus)) {
            return "SEND_SUCCESS";
        } else {
            return "null";
        }
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SUCCESS}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SUCCESS}.
     */
    public static boolean isSuccess(AuthStatus authStatus) {
        return SUCCESS.equals(authStatus);
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_SUCCESS}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_SUCCESS}.
     */
    public static boolean isSendSuccess(AuthStatus authStatus) {
        return SEND_SUCCESS.equals(authStatus);
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_CONTINUE}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_CONTINUE}.
     */
    public static boolean isSendContinue(AuthStatus authStatus) {
        return SEND_CONTINUE.equals(authStatus);
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_FAILURE}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#SEND_FAILURE}.
     */
    public static boolean isSendFailure(AuthStatus authStatus) {
        return SEND_FAILURE.equals(authStatus);
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#FAILURE}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@link AuthStatus#FAILURE}.
     */
    public static boolean isFailure(AuthStatus authStatus) {
        return FAILURE.equals(authStatus);
    }

    /**
     * Returns {@code true} if the given {@code AuthStatus} value is {@code null}.
     *
     * @param authStatus The {@code AuthStatus} value.
     * @return {@code true} if the given {@code AuthStatus} value is {@code null}.
     */
    public static boolean isNull(AuthStatus authStatus) {
        return authStatus == null;
    }

}
