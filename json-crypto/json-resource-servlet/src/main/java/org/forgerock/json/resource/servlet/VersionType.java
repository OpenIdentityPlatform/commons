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
 * Copyright 2014 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

/**
 * Represents potential version types that may be included within the version header.
 */
public enum VersionType {

    CREST_API("api"), RESOURCE("resource");

    private final String type;

    private VersionType(final String type) {
        this.type = type;
    }

    /**
     * Given the version type string, returns the corresponding type instance.
     *
     * @param typeString
     *         The type string
     *
     * @return The corresponding type instance or null if no match.
     */
    public static VersionType getType(final String typeString) {
        for (VersionType versionType : values()) {
            if (versionType.type.equals(typeString)) {
                return versionType;
            }
        }

        return null;
    }

}
