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
package org.forgerock.json.resource.descriptor;

import java.util.Locale;

import org.forgerock.i18n.LocalizableMessage;

@SuppressWarnings("javadoc")
public final class ActionParameter {
    private final String name;
    private final String normalizedName;
    private final LocalizableMessage description;

    ActionParameter(final String name, final LocalizableMessage description) {
        this.name = name;
        this.normalizedName = name.toLowerCase(Locale.ENGLISH);
        this.description = Api.defaultToEmptyMessageIfNull(description);
    }

    public String getName() {
        return name;
    }

    public LocalizableMessage getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ActionParameter) {
            return normalizedName.equals(((ActionParameter) obj).normalizedName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return normalizedName.hashCode();
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
