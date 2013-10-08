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
package org.forgerock.json.resource.api;

import org.forgerock.json.fluent.JsonValue;

@SuppressWarnings("javadoc")
public final class Profile {
    private final JsonValue content;
    private final Urn urn;

    Profile(final Urn urn, final JsonValue content) {
        this.urn = urn;
        this.content = content;
    }

    public JsonValue getContent() {
        return content;
    }

    public Urn getUrn() {
        return urn;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Profile) {
            return urn.equals(((Profile) obj).urn);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn.toString();
    }
}
