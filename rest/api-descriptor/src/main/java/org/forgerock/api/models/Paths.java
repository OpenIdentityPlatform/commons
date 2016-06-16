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
import org.forgerock.http.routing.Version;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class that represents the Paths type in API descriptor.
 */
@JsonDeserialize(builder = Paths.Builder.class)
public final class Paths {

    private final Map<String, VersionedPath> paths;

    private Paths(Builder builder) {
        this.paths = builder.paths;
    }

    /**
     * Gets a {@code Map} of path-names to Paths. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of path-names to Paths.
     */
    @JsonValue
    protected Map<String, VersionedPath> getPaths() {
        return paths;
    }

    /**
     * Gets the Path for a given Path-name.
     *
     * @param name Path name
     * @return Path or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public VersionedPath get(String name) {
        return paths.get(name);
    }

    /**
     * Returns all Path names.
     *
     * @return All Path names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return paths.keySet();
    }

    /**
     * Create a new Builder for Paths.
     *
     * @return Builder
     */
    public static Builder paths() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Paths paths1 = (Paths) o;
        return Objects.equals(paths, paths1.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths);
    }

    /**
     * Builder to help construct the Paths.
     */
    public static final class Builder {

        private final Map<String, VersionedPath> paths = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a Path.
         *
         * @param path Path string
         * @param versionedPath Versioned path
         * @return Builder
         */
        @JsonAnySetter
        public Builder put(String path, VersionedPath versionedPath) {
            if (path == null || containsWhitespace(path)) {
                throw new IllegalArgumentException("path required and may not contain whitespace");
            }
            if (!path.isEmpty()) {
                // paths must start with a slash (OpenAPI spec) and not end with one
                path = PathUtil.buildPath(path);
            }
            if (paths.containsKey(path)) {
                throw new IllegalStateException("path not unique");
            }
            paths.put(path, Reject.checkNotNull(versionedPath));
            return this;
        }

        /**
         * Merge the path definition into the existing path definitions. If there is already a {@code VersionedPath}
         * at this path, then the versions will be added together.
         *
         * @param path Path string
         * @param versionedPath Versioned path
         * @return Builder.
         */
        public Builder merge(String path, VersionedPath versionedPath) {
            if (path == null || containsWhitespace(path)) {
                throw new IllegalArgumentException("path required and may not contain whitespace");
            }
            if (!path.isEmpty()) {
                // paths must start with a slash (OpenAPI spec) and not end with one
                path = PathUtil.buildPath(path);
            }
            if (!paths.containsKey(path)) {
                put(path, Reject.checkNotNull(versionedPath));
            } else {
                VersionedPath existing = paths.get(path);
                for (Version v : versionedPath.getVersions()) {
                    existing.addVersion(v, versionedPath.get(v));
                }
            }
            return this;
        }

        /**
         * Builds the Paths instance.
         *
         * @return Paths instance
         */
        public Paths build() {
            return new Paths(this);
        }
    }

}