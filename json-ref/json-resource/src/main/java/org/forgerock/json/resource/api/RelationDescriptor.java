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

import static org.forgerock.json.resource.api.RelationDescriptor.Multiplicity.ONE_TO_MANY;

import java.util.Locale;

import org.forgerock.i18n.LocalizableMessage;

@SuppressWarnings("javadoc")
public final class RelationDescriptor {
    public enum Multiplicity {
        ONE_TO_ONE, ONE_TO_MANY;
    }

    public static final class RelationBuilder<T> {
        private final String name;
        private final Urn resourceUrn;
        private LocalizableMessage description;
        private Multiplicity multiplicity = ONE_TO_MANY;
        private final String normalizedName;
        private final RelationCapableBuilder<T> parentBuilder;

        private RelationBuilder(final String name, final Urn resourceUrn,
                final RelationCapableBuilder<T> parentBuilder) {
            this.name = name;
            this.normalizedName = name.toLowerCase(Locale.ENGLISH);
            this.resourceUrn = resourceUrn;
            this.parentBuilder = parentBuilder;
        }

        public RelationBuilder<T> setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public RelationBuilder<T> setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public RelationBuilder<T> setMultiplicity(final Multiplicity multiplicity) {
            this.multiplicity = multiplicity;
            return this;
        }

        public T build() {
            final RelationDescriptor relation =
                    new RelationDescriptor(name, normalizedName, description, multiplicity,
                            resourceUrn);
            return parentBuilder.addRelationFromBuilder(relation);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof RelationBuilder) {
                return normalizedName.equals(((RelationBuilder<?>) obj).normalizedName);
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

    static <T> RelationBuilder<T> builder(final String name, final Urn resourceUrn,
            final RelationCapableBuilder<T> parentBuilder) {
        return new RelationBuilder<T>(name, resourceUrn, parentBuilder);
    }

    private final String name;
    private final Urn resourceUrn;
    private ResourceDescriptor resource;
    private final LocalizableMessage description;
    private final Multiplicity multiplicity;
    private final String normalizedName;

    private RelationDescriptor(final String name, final String normalizedName,
            final LocalizableMessage description, final Multiplicity multiplicity,
            final Urn resourceUrn) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description; // Delegate to resource if null.
        this.multiplicity = multiplicity;
        this.resourceUrn = resourceUrn;
    }

    RelationDescriptor(final RelationDescriptor relation) {
        this.name = relation.name;
        this.normalizedName = relation.normalizedName;
        this.description = relation.description;
        this.multiplicity = relation.multiplicity;
        this.resourceUrn = relation.resourceUrn;
    }

    void setResource(final ResourceDescriptor resource) {
        this.resource = resource;
    }

    public Urn getResourceUrn() {
        return resourceUrn;
    }

    public String getName() {
        return name;
    }

    public LocalizableMessage getDescription() {
        return description != null ? description : resource.getDescription();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof RelationDescriptor) {
            return normalizedName.equals(((RelationDescriptor) obj).normalizedName);
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

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public ResourceDescriptor getResource() {
        return resource;
    }
}
