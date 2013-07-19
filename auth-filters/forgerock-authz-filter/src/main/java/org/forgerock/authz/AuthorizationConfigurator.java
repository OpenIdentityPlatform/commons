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

package org.forgerock.authz;

import org.forgerock.auth.common.Configurator;

/**
 * Base interface for all Authorization Configurators, responsible for providing configuration to the AuthZFilter.
 * <p>
 * Provides a method for getting the instance of the AuthorizationFilter which contains the logic to determine
 * whether the request is authorized or not.
 *
 * @author Phill Cunnington
 * @since 1.0.0
 */
public interface AuthorizationConfigurator extends Configurator {

    /**
     * Gets the instance of the Authorization Filter, which contains the logic to determine
     * whether the request is authorized or not.
     *
     * @return The Authorization Filter instance.
     */
    AuthorizationFilter getAuthorizationFilter();
}
