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

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.json.resource.descriptor.Api.unmodifiableCopyOf;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.http.ResourcePath;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Version;

@SuppressWarnings("javadoc")
public final class ApiDescriptor {
    public static final class Builder {
        private LocalizableMessage description;
        private final Map<Urn, ResourceDescriptor> resources = new LinkedHashMap<>();
        private final Set<RelationDescriptor> relations = new LinkedHashSet<>();
        private final Set<Profile> profiles = new LinkedHashSet<>();
        private final Urn urn;

        private Builder(final Urn urn) {
            this.urn = urn;
        }

        public Builder addResource(final ResourceDescriptor resource) {
            resources.put(resource.getUrn(), new ResourceDescriptor(resource));
            return this;
        }

        public Builder addProfile(final String urn, final JsonValue content) {
            return addProfile(Urn.valueOf(urn), content);
        }

        public Builder addProfile(final Urn urn, final JsonValue content) {
            profiles.add(new Profile(urn, content));
            return this;
        }

        public ResourceDescriptor.Builder addResource(final Urn urn) {
            return ResourceDescriptor.builder(urn, this);
        }

        public ResourceDescriptor.Builder addResource(final String urn) {
            return addResource(Urn.valueOf(urn));
        }

        public RelationDescriptor.Builder<Builder> addRelation(final String path,
                final String resourceUrn) {
            return addRelation(ResourcePath.valueOf(path), Urn.valueOf(resourceUrn));
        }

        public RelationDescriptor.Builder<Builder> addRelation(final String path,
                final Urn resourceUrn) {
            return addRelation(ResourcePath.valueOf(path), resourceUrn);
        }

        public RelationDescriptor.Builder<Builder> addRelation(final ResourcePath path,
                final Urn resourceUrn) {
            return RelationDescriptor.builder(path, resourceUrn,
                    new RelationCapableBuilder<Builder>() {
                        @Override
                        public Builder addRelationFromBuilder(final RelationDescriptor relation) {
                            relations.add(relation);
                            return ApiDescriptor.Builder.this;
                        }
                    });
        }

        public Builder addRelation(final RelationDescriptor relation) {
            relations.add(new RelationDescriptor(relation));
            return this;
        }

        void addResourceFromBuilder(final ResourceDescriptor resource) {
            resources.put(resource.getUrn(), resource);
        }

        public Builder setDescription(final String description) {
            return setDescription(LocalizableMessage.raw(description));
        }

        public Builder setDescription(final LocalizableMessage description) {
            this.description = description;
            return this;
        }

        public ApiDescriptor build() {
            final List<LocalizableMessage> warnings = new LinkedList<>();
            final ApiDescriptor descriptor = build(warnings);
            if (warnings.isEmpty()) {
                return descriptor;
            }
            throw new LocalizedIllegalArgumentException(warnings.get(0));
        }

        public ApiDescriptor build(final Collection<LocalizableMessage> warnings) {
            /*
             * Check the resource descriptors to see if they contain any invalid
             * references. Repeat until only valid resource descriptors remain.
             */
            final Map<Urn, ResourceDescriptor> resolvedResources = new LinkedHashMap<>(resources);
            boolean foundInvalidResource;
            do {
                foundInvalidResource = false;
                final Iterator<Map.Entry<Urn, ResourceDescriptor>> iterator =
                        resolvedResources.entrySet().iterator();
                while (iterator.hasNext()) {
                    boolean resourceIsValid = true;
                    final ResourceDescriptor resource = iterator.next().getValue();

                    // Check parent chain.
                    ResourceDescriptor rd = resource;
                    for (Urn parentUrn = rd.getParentUrn(); parentUrn != null; parentUrn =
                            rd.getParentUrn()) {
                        // Check to see if the resource parent chain is circular.
                        if (parentUrn.equals(resource.getUrn())) {
                            if (warnings != null) {
                                // TODO: i18n.
                                warnings.add(LocalizableMessage.raw(
                                        "The resource '%s' in API '%s' is invalid because "
                                                + "it has a circular parent chain", resource
                                                .getUrn(), urn));
                            }
                            resourceIsValid = false;
                            break;
                        }
                        // Check to see if the resource parent exists.
                        rd = resolvedResources.get(parentUrn);
                        if (rd == null) {
                            if (warnings != null) {
                                // TODO: i18n.
                                warnings.add(LocalizableMessage.raw(
                                        "The resource '%s' in API '%s' is invalid because "
                                                + "it has a non-existant parent '%s'", resource
                                                .getUrn(), urn, parentUrn));
                            }
                            resourceIsValid = false;
                            break;
                        }
                    }

                    // Check the resource's relations.
                    for (final RelationDescriptor relation : resource.getRelations()) {
                        final ResourceDescriptor target =
                                resolvedResources.get(relation.getResourceUrn());
                        if (target == null) {
                            if (warnings != null) {
                                // TODO: i18n.
                                warnings.add(LocalizableMessage
                                        .raw("The relation '%s' for resource '%s' in API '%s' "
                                                + "is invalid because it refers to a non-existant resource '%s'",
                                                relation.getResourcePath(), resource.getUrn(), urn,
                                                relation.getResourceUrn()));
                            }
                            resourceIsValid = false;
                        }
                    }

                    // Remove the resource descriptor if it was found to be invalid.
                    if (!resourceIsValid) {
                        iterator.remove();
                        foundInvalidResource = true;
                    }
                }
            } while (foundInvalidResource);

            // Resolve remaining valid resource descriptors.
            for (final ResourceDescriptor resource : resolvedResources.values()) {
                final Urn parentUrn = resource.getParentUrn();
                if (parentUrn != null) {
                    resource.setParent(resolvedResources.get(parentUrn));
                }
                for (final RelationDescriptor relation : resource.getRelations()) {
                    relation.setResource(resolvedResources.get(relation.getResourceUrn()));
                }
            }

            // Now resolve API relations.
            final Set<RelationDescriptor> resolvedRelations = new LinkedHashSet<>(relations.size());
            for (final RelationDescriptor relation : relations) {
                final ResourceDescriptor resource =
                        resolvedResources.get(relation.getResourceUrn());
                if (resource != null) {
                    relation.setResource(resource);
                    resolvedRelations.add(relation);
                } else if (warnings != null) {
                    // TODO: i18n.
                    warnings.add(LocalizableMessage.raw(
                            "The relation '%s' in API '%s' is invalid because it refers "
                                    + "to a non-existant resource '%s'",
                            relation.getResourcePath(), urn, relation.getResourceUrn()));
                }
            }
            return new ApiDescriptor(urn, description, unmodifiableSet(resolvedRelations),
                    unmodifiableCopyOf(resolvedResources.values()), unmodifiableCopyOf(profiles));
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

    private final LocalizableMessage description;
    private final Set<ResourceDescriptor> resources;
    private final Set<RelationDescriptor> relations;
    private final Set<Profile> profiles;
    private final Urn urn;

    private ApiDescriptor(final Urn urn, final LocalizableMessage description,
            final Set<RelationDescriptor> relations, final Set<ResourceDescriptor> resources,
            final Set<Profile> profiles) {
        this.urn = urn;
        this.description = Api.defaultToEmptyMessageIfNull(description);
        this.relations = relations;
        this.resources = resources;
        this.profiles = profiles;
    }

    public static Builder builder(final String urn) {
        return new Builder(Urn.valueOf(urn));
    }

    public static Builder builder(final Urn urn) {
        return new Builder(urn);
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

    public Set<ResourceDescriptor> getResources() {
        return resources;
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
        } else if (obj instanceof ApiDescriptor) {
            return urn.equals(((ApiDescriptor) obj).urn);
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
