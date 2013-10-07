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

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.resource.ResourceName;

@SuppressWarnings("javadoc")
public final class RelationDescriptor {
    public enum Multiplicity {
        ONE_TO_ONE, ONE_TO_MANY;
    }

    public static final class RelationBuilder<T> {
        private final ResourceName name;
        private final Urn resourceUrn;
        private LocalizableMessage description;
        private Multiplicity multiplicity = ONE_TO_MANY;
        private final RelationCapableBuilder<T> parentBuilder;

        private RelationBuilder(final ResourceName name, final Urn resourceUrn,
                final RelationCapableBuilder<T> parentBuilder) {
            this.name = name;
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
                    new RelationDescriptor(name, description, multiplicity, resourceUrn);
            return parentBuilder.addRelationFromBuilder(relation);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof RelationBuilder) {
                return name.equals(((RelationBuilder<?>) obj).name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name.toString();
        }
    }

    static <T> RelationBuilder<T> builder(final ResourceName name, final Urn resourceUrn,
            final RelationCapableBuilder<T> parentBuilder) {
        return new RelationBuilder<T>(name, resourceUrn, parentBuilder);
    }

    private final ResourceName name;
    private final Urn resourceUrn;
    private ResourceDescriptor resource;
    private final LocalizableMessage description;
    private final Multiplicity multiplicity;

    private RelationDescriptor(final ResourceName name, final LocalizableMessage description,
            final Multiplicity multiplicity, final Urn resourceUrn) {
        this.name = name;
        this.description = description; // Delegate to resource if null.
        this.multiplicity = multiplicity;
        this.resourceUrn = resourceUrn;
    }

    RelationDescriptor(final RelationDescriptor relation) {
        this.name = relation.name;
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

    public String getResourceName() {
        return name.toString();
    }

    public ResourceName getResourceNameObject() {
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
            return name.equals(((RelationDescriptor) obj).name);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public ResourceDescriptor getResource() {
        return resource;
    }
}
