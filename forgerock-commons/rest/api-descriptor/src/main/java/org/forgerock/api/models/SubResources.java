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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.api.models;

import static org.forgerock.api.util.ValidationUtil.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.util.PathUtil;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Sub-resources of a resource are declared here.
 */
@JsonDeserialize(builder = SubResources.Builder.class)
public final class SubResources {
    private final Map<String, Resource> subResources;

    private SubResources(Builder builder) {
        this.subResources = builder.subResources;
    }

    /**
     * Gets a {@code Map} of paths to {@link Resource}s.
     *
     * @return The {@code Map}.
     */
    @JsonValue
    public Map<String, Resource> getSubResources() {
        return subResources;
    }

    /**
     * Gets the {@link Resource} for a given sub-resource name.
     *
     * @param name Sub-resource name
     * @return {@link Resource} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Resource get(String name) {
        return subResources.get(name);
    }

    /**
     * Returns all sub-resource names.
     *
     * @return The names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return subResources.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubResources that = (SubResources) o;
        return Objects.equals(subResources, that.subResources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subResources);
    }

    /**
     * Create a new Builder for sub-resources.
     *
     * @return Builder
     */
    public static Builder subresources() {
        return new Builder();
    }

    /**
     * Builder to help construct the SubResources.
     */
    public static final class Builder {

        private final Map<String, Resource> subResources = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a sub-resource.
         *
         * @param path Sub-resource path
         * @param resource {@link Resource}
         * @return Builder
         */
        @JsonAnySetter
        public Builder put(String path, Resource resource) {
            if (path == null || containsWhitespace(path)) {
                throw new IllegalArgumentException("path required and may not contain whitespace");
            }
            if (!path.isEmpty()) {
                // paths must start with a slash (OpenAPI spec) and not end with one
                path = PathUtil.buildPath(path);
            }
            if (subResources.containsKey(path)) {
                throw new IllegalStateException("path not unique");
            }
            subResources.put(path, Reject.checkNotNull(resource));
            return this;
        }

        /**
         * Builds the {@link SubResources} instance.
         *
         * @return {@link SubResources} instance
         */
        public SubResources build() {
            return new SubResources(this);
        }
    }

}
