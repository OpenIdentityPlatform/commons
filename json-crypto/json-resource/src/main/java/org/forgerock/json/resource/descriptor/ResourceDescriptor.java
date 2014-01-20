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

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.json.resource.descriptor.Api.unmodifiableCopyOf;

import java.util.LinkedHashSet;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceName;

@SuppressWarnings("javadoc")
public final class ResourceDescriptor {
    public static final class Builder {
        private LocalizableMessage description;
        private final Set<RelationDescriptor> relations = new LinkedHashSet<RelationDescriptor>();
        private Urn parentResourceUrn;
        private Schema schema;
        private final Set<ActionDescriptor> actions = new LinkedHashSet<ActionDescriptor>();
        private final ApiDescriptor.Builder parentBuilder;
        private final Set<Profile> profiles = new LinkedHashSet<Profile>();
        private final Urn urn;

        private Builder(final Urn urn, final ApiDescriptor.Builder parentBuilder) {
            this.urn = urn;
            this.parentBuilder = parentBuilder;
        }

        public RelationDescriptor.Builder<Builder> addRelation(final String name,
                final String resourceUrn) {
            return addRelation(ResourceName.valueOf(name), Urn.valueOf(resourceUrn));
        }

        public RelationDescriptor.Builder<Builder> addRelation(final String name,
                final Urn resourceUrn) {
            return addRelation(ResourceName.valueOf(name), resourceUrn);
        }

        public Builder addProfile(final String urn, final JsonValue content) {
            return addProfile(Urn.valueOf(urn), content);
        }

        public Builder addProfile(final Urn urn, final JsonValue content) {
            profiles.add(new Profile(urn, content));
            return this;
        }

        public RelationDescriptor.Builder<Builder> addRelation(final ResourceName name,
                final Urn resourceUrn) {
            return RelationDescriptor.builder(name, resourceUrn,
                    new RelationCapableBuilder<Builder>() {
                        @Override
                        public Builder addRelationFromBuilder(final RelationDescriptor relation) {
                            relations.add(relation);
                            return ResourceDescriptor.Builder.this;
                        }
                    });
        }

        public Builder addRelation(final RelationDescriptor relation) {
            relations.add(new RelationDescriptor(relation));
            return this;
        }

        public ActionDescriptor.Builder<Builder> addAction(final String name) {
            return ActionDescriptor.builder(name, new ActionCapableBuilder<Builder>() {
                @Override
                public Builder addActionFromBuilder(final ActionDescriptor action) {
                    actions.add(action);
                    return Builder.this;
                }
            });
        }

        public Builder addRelation(final ActionDescriptor action) {
            actions.add(action);
            return this;
        }

        public Builder setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public Builder setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public Builder setParent(final Urn parentResourceUrn) {
            this.parentResourceUrn = parentResourceUrn;
            return this;
        }

        public Builder setParent(final String parentResourceUrn) {
            return setParent(Urn.valueOf(parentResourceUrn));
        }

        public Builder setSchema(final Schema schema) {
            this.schema = schema;
            return this;
        }

        public ApiDescriptor.Builder build() {
            final ResourceDescriptor resource =
                    new ResourceDescriptor(urn, description, parentResourceUrn, schema,
                            unmodifiableCopyOf(actions), unmodifiableCopyOf(relations),
                            unmodifiableCopyOf(profiles));
            parentBuilder.addResourceFromBuilder(resource);
            return parentBuilder;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof Builder) {
                return urn.equals(((Builder) obj).urn);
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

    static Builder builder(final Urn urn, final ApiDescriptor.Builder parentBuilder) {
        return new Builder(urn, parentBuilder);
    }

    private final LocalizableMessage description;
    private final Schema schema;
    private ResourceDescriptor parent;
    private final Urn parentUrn;
    private final Set<ResourceDescriptor> mutableChildren = new LinkedHashSet<ResourceDescriptor>();
    private final Set<ResourceDescriptor> children = unmodifiableSet(mutableChildren);
    private final Set<ActionDescriptor> actions;
    private final Set<RelationDescriptor> relations;
    private final Set<Profile> profiles;
    private final Urn urn;

    private ResourceDescriptor(final Urn urn, final LocalizableMessage description,
            final Urn parentUrn, final Schema schema, final Set<ActionDescriptor> actions,
            final Set<RelationDescriptor> relations, final Set<Profile> profiles) {
        this.urn = urn;
        this.description = Api.defaultToEmptyMessageIfNull(description);
        this.parentUrn = parentUrn;
        this.schema = schema;
        this.actions = actions;
        this.relations = relations;
        this.profiles = profiles;
    }

    ResourceDescriptor(final ResourceDescriptor resource) {
        this.urn = resource.urn;
        this.description = resource.description;
        this.parentUrn = resource.parentUrn;
        this.schema = resource.schema;
        this.actions = resource.actions;
        this.profiles = resource.profiles;

        // Need to copy the relations in order to make them unresolved.
        final Set<RelationDescriptor> unresolvedRelations =
                new LinkedHashSet<RelationDescriptor>(resource.relations.size());
        for (final RelationDescriptor relation : resource.relations) {
            unresolvedRelations.add(new RelationDescriptor(relation));
        }
        this.relations = unmodifiableSet(unresolvedRelations);
    }

    public String getName() {
        return urn.getName();
    }

    public LocalizableMessage getDescription() {
        return description;
    }

    public Version getVersion() {
        return urn.getVersion();
    }

    public Set<RelationDescriptor> getRelations() {
        return relations;
    }

    public Set<ResourceDescriptor> getChildren() {
        return children;
    }

    public ResourceDescriptor getParent() {
        return parent;
    }

    public Set<ActionDescriptor> getActions() {
        return actions;
    }

    public Schema getSchema() {
        return schema;
    }

    public Urn getParentUrn() {
        return parentUrn;
    }

    void setParent(final ResourceDescriptor parent) {
        this.parent = parent;
        parent.mutableChildren.add(this);
    }

    public Set<Profile> getProfiles() {
        return profiles;
    }

    public Urn getUrn() {
        return urn;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ResourceDescriptor) {
            return urn.equals(((ResourceDescriptor) obj).urn);
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
