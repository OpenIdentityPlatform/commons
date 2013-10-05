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
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;

@SuppressWarnings("javadoc")
public final class ResourceDescriptor {
    public static final class ResourceBuilder {
        private final Urn urn;
        private LocalizableMessage description;
        private final Set<RelationDescriptor> relations = new LinkedHashSet<RelationDescriptor>();
        private Urn parentResourceUrn;
        private Schema schema;
        private final Set<ActionDescriptor> actions = new LinkedHashSet<ActionDescriptor>();
        private final ApiDescriptor.ApiBuilder parentBuilder;

        private ResourceBuilder(final Urn urn, final ApiDescriptor.ApiBuilder parentBuilder) {
            this.urn = urn;
            this.parentBuilder = parentBuilder;
        }

        public RelationDescriptor.RelationBuilder<ResourceBuilder> addRelation(final String name,
                final String resourceUrn) {
            return addRelation(name, Urn.valueOf(resourceUrn));
        }

        public RelationDescriptor.RelationBuilder<ResourceBuilder> addRelation(final String name,
                final Urn resourceUrn) {
            return RelationDescriptor.builder(name, resourceUrn,
                    new RelationCapableBuilder<ResourceBuilder>() {
                        @Override
                        public ResourceBuilder addRelationFromBuilder(
                                final RelationDescriptor relation) {
                            relations.add(relation);
                            return ResourceDescriptor.ResourceBuilder.this;
                        }
                    });
        }

        public ResourceBuilder addRelation(final RelationDescriptor relation) {
            relations.add(new RelationDescriptor(relation));
            return this;
        }

        public ActionDescriptor.ActionBuilder<ResourceBuilder> addAction(final String name) {
            return ActionDescriptor.builder(name, new ActionCapableBuilder<ResourceBuilder>() {
                @Override
                public ResourceBuilder addActionFromBuilder(final ActionDescriptor action) {
                    actions.add(action);
                    return ResourceDescriptor.ResourceBuilder.this;
                }
            });
        }

        public ResourceBuilder addRelation(final ActionDescriptor action) {
            actions.add(action);
            return this;
        }

        public ResourceBuilder setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public ResourceBuilder setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public ResourceBuilder setParent(final Urn parentResourceUrn) {
            this.parentResourceUrn = parentResourceUrn;
            return this;
        }

        public ResourceBuilder setParent(final String parentResourceUrn) {
            return setParent(Urn.valueOf(parentResourceUrn));
        }

        public ResourceBuilder setSchema(final Schema schema) {
            this.schema = schema;
            return this;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ResourceBuilder) {
                return urn.equals(((ResourceBuilder) obj).urn);
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

        public ApiDescriptor.ApiBuilder build() {
            final ResourceDescriptor resource =
                    new ResourceDescriptor(urn, description, parentResourceUrn, schema,
                            unmodifiableSet(new LinkedHashSet<ActionDescriptor>(actions)),
                            unmodifiableSet(new LinkedHashSet<RelationDescriptor>(relations)));
            parentBuilder.addResourceFromBuilder(resource);
            return parentBuilder;
        }
    }

    static ResourceBuilder builder(final Urn urn, final ApiDescriptor.ApiBuilder parentBuilder) {
        return new ResourceBuilder(urn, parentBuilder);
    }

    private final Urn urn;
    private final LocalizableMessage description;
    private final Schema schema;
    private ResourceDescriptor parent;
    private final Urn parentUrn;
    private final Set<ResourceDescriptor> mutableChildren = new LinkedHashSet<ResourceDescriptor>();
    private final Set<ResourceDescriptor> children = unmodifiableSet(mutableChildren);
    private final Set<ActionDescriptor> actions;
    private final Set<RelationDescriptor> relations;

    private ResourceDescriptor(final Urn urn, final LocalizableMessage description,
            final Urn parentUrn, final Schema schema, final Set<ActionDescriptor> actions,
            final Set<RelationDescriptor> relations) {
        this.urn = urn;
        this.description = defaultToEmptyMessageIfNull(description);
        this.parentUrn = parentUrn;
        this.schema = schema;
        this.actions = actions;
        this.relations = relations;
    }

    ResourceDescriptor(final ResourceDescriptor resource) {
        this.urn = resource.urn;
        this.description = resource.description;
        this.parentUrn = resource.parentUrn;
        this.schema = resource.schema;
        this.actions = unmodifiableSet(new LinkedHashSet<ActionDescriptor>(resource.actions));

        // Need to copy the relations in order to make them unresolved.
        final Set<RelationDescriptor> unresolvedRelations =
                new LinkedHashSet<RelationDescriptor>(resource.relations.size());
        for (final RelationDescriptor relation : resource.relations) {
            unresolvedRelations.add(new RelationDescriptor(relation));
        }
        this.relations = unmodifiableSet(unresolvedRelations);
    }

    public Urn getUrn() {
        return urn;
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

    public Urn getParentUrn() {
        return parentUrn;
    }

    void setParent(final ResourceDescriptor parent) {
        this.parent = parent;
        parent.mutableChildren.add(this);
    }
}
