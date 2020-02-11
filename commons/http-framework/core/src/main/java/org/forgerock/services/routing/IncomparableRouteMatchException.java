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

package org.forgerock.services.routing;

/**
 * An exception which is thrown when two incompatible {@link RouteMatch}
 * instances are attempted to be compared.
 */
public class IncomparableRouteMatchException extends Exception {

    private static final long serialVersionUID = 4933718263312991528L;

    /**
     * Constructs a {@code IncomparableRouteMatchException} with the two
     * {@link RouteMatch} instance that caused the exception.
     *
     * @param firstRouteMatch The first {@code RouteMatch} instance.
     * @param secondRouteMatch The second {@code RouteMatch} instance.
     */
    public IncomparableRouteMatchException(RouteMatch firstRouteMatch, RouteMatch secondRouteMatch) {
        super(firstRouteMatch.getClass().toString() + " cannot be compared to "
                + secondRouteMatch.getClass().toString());
    }
}
