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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.http.routing.Version;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Class that represents versioned {@link Resource}s on an API descriptor path.
 */
@JsonDeserialize(builder = VersionedPath.Builder.class)
public final class VersionedPath {

    /**
     * Version {@code 0.0} represents null/empty, for when resource versions are not required by an API (e.g., OpenIDM).
     */
    public static final Version UNVERSIONED = Version.version(0);

    private final Map<Version, Resource> paths;

    private VersionedPath(Builder builder) {
        this.paths = builder.paths;

        if (paths.isEmpty()) {
            throw new ApiValidationException("Must have at least one versioned resource");
        }
        if (paths.size() > 1) {
            for (final Version version : paths.keySet()) {
                if (UNVERSIONED.equals(version)) {
                    throw new ApiValidationException("Version 0.0 (unversioned) must be the only version when used");
                }
            }
        }
    }

    /**
     * Gets a {@code Map} of versions to {@link Resource}s. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of versions to {@link Resource}s.
     */
    @JsonValue
    protected Map<Version, Resource> getPaths() {
        return paths;
    }

    /**
     * Gets the {@link Resource} for a given version.
     *
     * @param version Resource version
     * @return {@link Resource} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Resource get(Version version) {
        return paths.get(version);
    }

    /**
     * Returns all resource-versions on this path.
     *
     * @return All resource-versions.
     */
    @JsonIgnore
    public Set<Version> getVersions() {
        return paths.keySet();
    }

    /**
     * Create a new Builder for VersionedPath.
     *
     * @return Builder
     */
    public static Builder versionedPath() {
        return new Builder();
    }

    /**
     * Allows for mutation of paths when merging {@code Paths} instances.
     * @param v The version.
     * @param resource The resource.
     */
    void addVersion(Version v, Resource resource) {
        if (paths.containsKey(v)) {
            throw new IllegalArgumentException("Trying to redefine version: " + v);
        }
        paths.put(v, Reject.checkNotNull(resource));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VersionedPath that = (VersionedPath) o;
        return Objects.equals(paths, that.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths);
    }

    /**
     * Builder to help construct the VersionedPath.
     */
    public static final class Builder {

        private final Map<Version, Resource> paths = new HashMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a resource-version.
         *
         * @param version Resource-version
         * @param resource {@link Resource}
         * @return Builder
         */
        public Builder put(Version version, Resource resource) {
            if (paths.containsKey(version)) {
                throw new IllegalStateException("version not unique");
            }
            paths.put(Reject.checkNotNull(version), Reject.checkNotNull(resource));
            return this;
        }

        /**
         * Adds a resource-version.
         *
         * @param version Resource-version as string
         * @param resource {@link Resource}
         * @return Builder
         */
        @JsonAnySetter
        public Builder put(String version, Resource resource) {
            return put(Version.version(version), resource);
        }

        /**
         * Builds the VersionedPath instance.
         *
         * @return VersionedPath instance
         */
        public VersionedPath build() {
            return new VersionedPath(this);
        }
    }

}