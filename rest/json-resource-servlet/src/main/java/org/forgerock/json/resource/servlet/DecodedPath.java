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
 * Copyright 2012 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

/**
 * A URL which has been decoded in order to determine the component name,
 * resource ID if present, and whether or not the container is a collection.
 */
final class DecodedPath {

    private final String component;
    private final boolean isCollection;
    private final String resourceId;

    DecodedPath(final String component, final String resourceId, final boolean isCollection) {
        this.component = component;
        this.resourceId = resourceId;
        this.isCollection = isCollection;
    }

    String getComponent() {
        return component;
    }

    String getResourceId() {
        return resourceId;
    }

    boolean isCollection() {
        return isCollection;
    }

}
