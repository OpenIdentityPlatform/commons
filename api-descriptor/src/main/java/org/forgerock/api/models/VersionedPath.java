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

import static org.forgerock.api.util.ValidationUtil.containsWhitespace;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import org.forgerock.api.ApiValidationException;
import org.forgerock.util.Reject;

/**
 * Class that represents versioned {@link Resource}s on an API descriptor path.
 */
public final class VersionedPath implements PathNode {

    private final Map<String, Resource> paths;

    private VersionedPath(Builder builder) {
        this.paths = builder.paths;

        if (paths.isEmpty()) {
            throw new ApiValidationException("Must have at least one versioned resource");
        }
    }

    /**
     * Gets a {@code Map} of versions to {@link Resource}s. This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of versions to {@link Resource}s.
     */
    @JsonValue
    protected Map<String, Resource> getPaths() {
        return paths;
    }

    /**
     * Gets the {@link Resource} for a given version.
     *
     * @param version Resource version
     * @return {@link Resource} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Resource get(String version) {
        return paths.get(version);
    }

    /**
     * Returns all resource-versions on this path.
     *
     * @return All resource-versions.
     */
    @JsonIgnore
    public Set<String> getVersions() {
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
     * Builder to help construct the VersionedPath.
     */
    public static final class Builder {

        private final Map<String, Resource> paths = new HashMap<>();

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
        public Builder put(String version, Resource resource) {
            // TODO can we agree on a regex to validate `version`?
            if (isEmpty(version) || containsWhitespace(version)) {
                throw new IllegalArgumentException("version required and may not contain whitespace");
            }
            if (paths.containsKey(version)) {
                throw new IllegalStateException("version not unique");
            }
            paths.put(version, Reject.checkNotNull(resource));
            return this;
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