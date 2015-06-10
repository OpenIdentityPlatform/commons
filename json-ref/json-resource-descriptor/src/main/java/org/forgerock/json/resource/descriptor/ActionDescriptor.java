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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.descriptor;

import static org.forgerock.json.resource.descriptor.Api.unmodifiableCopyOf;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.fluent.JsonValue;

@SuppressWarnings("javadoc")
public final class ActionDescriptor {
    public static final class Builder<T> {
        private final String name;
        private final String normalizedName;
        private LocalizableMessage description;
        private final Set<ActionParameter> parameters = new LinkedHashSet<>();
        private final ActionCapableBuilder<T> parentBuilder;
        private final Set<Profile> profiles = new LinkedHashSet<>();

        Builder(final String name, final ActionCapableBuilder<T> parentBuilder) {
            this.name = name;
            this.normalizedName = name.toLowerCase(Locale.ENGLISH);
            this.parentBuilder = parentBuilder;
        }

        public Builder<T> addProfile(final String urn, final JsonValue content) {
            return addProfile(Urn.valueOf(urn), content);
        }

        public Builder<T> addProfile(final Urn urn, final JsonValue content) {
            profiles.add(new Profile(urn, content));
            return this;
        }

        public Builder<T> setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public Builder<T> setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public Builder<T> addParameter(final String name) {
            return addParameter(name, (LocalizableMessage) null);
        }

        public Builder<T> addParameter(final String name, final String description) {
            return addParameter(name, LocalizableMessage.raw(description));
        }

        public Builder<T> addParameter(final String name, final LocalizableMessage description) {
            parameters.add(new ActionParameter(name, description));
            return this;
        }

        public T build() {
            final ActionDescriptor action =
                    new ActionDescriptor(name, normalizedName, description,
                            unmodifiableCopyOf(parameters), unmodifiableCopyOf(profiles));
            return parentBuilder.addActionFromBuilder(action);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Builder) {
                return normalizedName.equals(((Builder<?>) obj).normalizedName);
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
    private final Set<ActionParameter> parameters;
    private final String normalizedName;
    private final Set<Profile> profiles;

    static <T> Builder<T> builder(final String name, final ActionCapableBuilder<T> parentBuilder) {
        return new Builder<>(name, parentBuilder);
    }

    private ActionDescriptor(final String name, final String normalizedName,
            final LocalizableMessage description, final Set<ActionParameter> parameters,
            final Set<Profile> profiles) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = Api.defaultToEmptyMessageIfNull(description);
        this.parameters = parameters;
        this.profiles = profiles;
    }

    public String getName() {
        return name;
    }

    public LocalizableMessage getDescription() {
        return description;
    }

    public Set<ActionParameter> getParameters() {
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

    public Set<Profile> getProfiles() {
        return profiles;
    }
}
