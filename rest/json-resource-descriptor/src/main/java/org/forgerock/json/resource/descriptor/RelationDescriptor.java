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

import static org.forgerock.json.resource.descriptor.Api.unmodifiableCopyOf;
import static org.forgerock.json.resource.descriptor.RelationDescriptor.Multiplicity.ONE_TO_MANY;

import java.util.LinkedHashSet;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.http.ResourcePath;

@SuppressWarnings("javadoc")
public final class RelationDescriptor {
    public enum Multiplicity {
        ONE_TO_ONE, ONE_TO_MANY;
    }

    public static final class Builder<T> {
        private final ResourcePath path;
        private final Urn resourceUrn;
        private LocalizableMessage description;
        private Multiplicity multiplicity = ONE_TO_MANY;
        private final RelationCapableBuilder<T> parentBuilder;
        private final Set<Profile> profiles = new LinkedHashSet<Profile>();
        private final Set<ActionDescriptor> actions = new LinkedHashSet<ActionDescriptor>();

        private Builder(final ResourcePath path, final Urn resourceUrn,
                final RelationCapableBuilder<T> parentBuilder) {
            this.path = path;
            this.resourceUrn = resourceUrn;
            this.parentBuilder = parentBuilder;
        }

        public ActionDescriptor.Builder<Builder<T>> addAction(final String name) {
            return ActionDescriptor.builder(name, new ActionCapableBuilder<Builder<T>>() {
                @Override
                public Builder<T> addActionFromBuilder(final ActionDescriptor action) {
                    actions.add(action);
                    return Builder.this;
                }
            });
        }

        public Builder<T> setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public Builder<T> addProfile(final String urn, final JsonValue content) {
            return addProfile(Urn.valueOf(urn), content);
        }

        public Builder<T> addProfile(final Urn urn, final JsonValue content) {
            profiles.add(new Profile(urn, content));
            return this;
        }

        public Builder<T> setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public Builder<T> setMultiplicity(final Multiplicity multiplicity) {
            this.multiplicity = multiplicity;
            return this;
        }

        public T build() {
            final RelationDescriptor relation =
                    new RelationDescriptor(path, description, multiplicity,
                            unmodifiableCopyOf(actions), resourceUrn, unmodifiableCopyOf(profiles));
            return parentBuilder.addRelationFromBuilder(relation);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Builder) {
                return path.equals(((Builder<?>) obj).path);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return path.toString();
        }
    }

    static <T> Builder<T> builder(final ResourcePath path, final Urn resourceUrn,
            final RelationCapableBuilder<T> parentBuilder) {
        return new Builder<T>(path, resourceUrn, parentBuilder);
    }

    private final ResourcePath path;
    private final Urn resourceUrn;
    private ResourceDescriptor resource;
    private final LocalizableMessage description;
    private final Multiplicity multiplicity;
    private final Set<Profile> profiles;
    private final Set<ActionDescriptor> actions;

    private RelationDescriptor(final ResourcePath path, final LocalizableMessage description,
            final Multiplicity multiplicity, final Set<ActionDescriptor> actions,
            final Urn resourceUrn, final Set<Profile> profiles) {
        this.path = path;
        this.description = description; // Delegate to resource if null.
        this.multiplicity = multiplicity;
        this.actions = actions;
        this.resourceUrn = resourceUrn;
        this.profiles = profiles;
    }

    RelationDescriptor(final RelationDescriptor relation) {
        this.path = relation.path;
        this.description = relation.description;
        this.multiplicity = relation.multiplicity;
        this.actions = relation.actions;
        this.resourceUrn = relation.resourceUrn;
        this.profiles = relation.profiles;
    }

    void setResource(final ResourceDescriptor resource) {
        this.resource = resource;
    }

    public Set<ActionDescriptor> getActions() {
        return actions;
    }

    public Urn getResourceUrn() {
        return resourceUrn;
    }

    public String getResourcePath() {
        return path.toString();
    }

    public ResourcePath getResourcePathObject() {
        return path;
    }

    public LocalizableMessage getDescription() {
        return description != null ? description : resource.getDescription();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof RelationDescriptor) {
            return path.equals(((RelationDescriptor) obj).path);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path.toString();
    }

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public ResourceDescriptor getResource() {
        return resource;
    }

    public Set<Profile> getProfiles() {
        return profiles;
    }
}
