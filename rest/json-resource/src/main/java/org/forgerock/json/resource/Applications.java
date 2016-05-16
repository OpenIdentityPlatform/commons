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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.json.resource;

/**
 * A utility class for dealing with {@link CrestApplication} instances.
 */
public final class Applications {

    /**
     * Create a simple {@link CrestApplication} using the provided factory, id and version.
     * @param factory The factory.
     * @param id The id.
     * @param version The version.
     * @return The application.
     */
    public static CrestApplication simpleCrestApplication(final ConnectionFactory factory, final String id,
            final String version) {
        return new CrestApplication() {
            @Override
            public ConnectionFactory getConnectionFactory() {
                return factory;
            }

            @Override
            public String getApiId() {
                return id;
            }

            @Override
            public String getApiVersion() {
                return version;
            }
        };
    }

    private Applications() {
        // utility class
    }
}
