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
package org.forgerock.json.resource;

/**
 * Constants used for resource versioning.
 */
public final class VersionConstants {

    /**
     * This class should not be instantiated.
     */
    private VersionConstants() {
    }

    /** The key that is used to determine the acceptable API versions. */
    public static final String ACCEPT_API_VERSION = "Accept-API-Version";

    /** The key that is used to indicate the chosen API versions. */
    public static final String CONTENT_API_VERSION = "Content-API-Version";

    /** The key for indicating the protocol version. */
    public static final String PROTOCOL = "protocol";

    /** The key for indicating the resource version. */
    public static final String RESOURCE = "resource";

}