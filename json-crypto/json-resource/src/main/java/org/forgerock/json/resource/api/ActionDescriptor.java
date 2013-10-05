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

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.json.resource.api.ApiDescriptor.defaultToEmptyMessageIfNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;

@SuppressWarnings("javadoc")
public final class ActionDescriptor {
    public static final class ActionBuilder<T> {
        private final String name;
        private final String normalizedName;
        private LocalizableMessage description;
        private final Set<String> parameters = new LinkedHashSet<String>();
        private final ActionCapableBuilder<T> parentBuilder;

        ActionBuilder(final String name, final ActionCapableBuilder<T> parentBuilder) {
            this.name = name;
            this.normalizedName = name.toLowerCase(Locale.ENGLISH);
            this.parentBuilder = parentBuilder;
        }

        public ActionBuilder<T> setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public ActionBuilder<T> setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public ActionBuilder<T> addParameter(final String parameter) {
            parameters.add(parameter);
            return this;
        }

        public T build() {
            final ActionDescriptor action =
                    new ActionDescriptor(name, normalizedName, description,
                            unmodifiableSet(new LinkedHashSet<String>(parameters)));
            return parentBuilder.addActionFromBuilder(action);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ActionBuilder) {
                return normalizedName.equals(((ActionBuilder<?>) obj).normalizedName);
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
            return name;
        }
    }

    private final String name;
    private final LocalizableMessage description;
    private final Set<String> parameters;
    private final String normalizedName;

    static <T> ActionBuilder<T> builder(final String name,
            final ActionCapableBuilder<T> parentBuilder) {
        return new ActionBuilder<T>(name, parentBuilder);
    }

    private ActionDescriptor(final String name, final String normalizedName,
            final LocalizableMessage description, final Set<String> parameters) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = defaultToEmptyMessageIfNull(description);
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public LocalizableMessage getDescription() {
        return description;
    }

    public Set<String> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ActionDescriptor) {
            return normalizedName.equals(((ActionDescriptor) obj).normalizedName);
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
        return name;
    }
}
