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

package org.forgerock.http.routing;

import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.WarningHeader;

/**
 * Implementations of this interface will be responsible for maintaining the
 * behaviour of API Version routing.
 *
 * <p>API Version routing can issue a {@link WarningHeader} if no
 * {@literal Accept-API-Version} header is set on the request. In addition a
 * default behaviour can be set to determine how the route will compare
 * matching API Version routes.</p>
 *
 * @see WarningHeader
 * @see AcceptApiVersionHeader
 * @see DefaultVersionBehaviour
 */
public interface ResourceApiVersionBehaviourManager {

    /**
     * Sets if warning headers should be set on the response if no
     * {@literal Accept-API-Version} header is present on the request.
     *
     * @param warningEnabled {@code true} if warning headers should be set.
     */
    void setWarningEnabled(boolean warningEnabled);

    /**
     * Returns {@code true} if warning headers should be set on the response if
     * no {@literal Accept-API-Version} header is present on the request.
     *
     * @return {@code true} if warning headers should be set.
     */
    boolean isWarningEnabled();

    /**
     * Sets the default routing behaviour to use when the request does not
     * contain the {@literal Accept-API-Version} header.
     *
     * @param behaviour The default routing behaviour when no
     * {@literal Accept-API-Version} header is present on the request.
     */
    void setDefaultVersionBehaviour(DefaultVersionBehaviour behaviour);

    /**
     * Gets the default routing behaviour to use when the request does not
     * contain the {@literal Accept-API-Version} header.
     *
     * @return The default routing behaviour when no
     * {@literal Accept-API-Version} header is present on the request.
     */
    DefaultVersionBehaviour getDefaultVersionBehaviour();
}
