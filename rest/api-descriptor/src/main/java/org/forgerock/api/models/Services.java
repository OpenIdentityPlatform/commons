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
import static org.forgerock.util.Reject.*;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class that represents API descriptor's Service {@link Resource} definitions.
 */
@JsonDeserialize(builder = Services.Builder.class)
public final class Services {

    private final Map<String, Resource> services;

    private Services(Builder builder) {
        this.services = builder.services;
    }

    /**
     * Gets a {@code Map} of service-names to {@link Resource}s.
     * This method is currently only used for JSON serialization.
     *
     * @return {@code Map} of service-names to {@link Resource}s.
     */
    @JsonValue
    protected Map<String, Resource> getServices() {
        return services;
    }

    /**
     * Gets the {@link Resource} for a given service-name.
     *
     * @param name Service name
     * @return {@link Schema} or {@code null} if does-not-exist.
     */
    @JsonIgnore
    public Resource get(String name) {
        return services.get(name);
    }

    /**
     * Returns all {@link Services} names.
     *
     * @return All {@link Services} names.
     */
    @JsonIgnore
    public Set<String> getNames() {
        return services.keySet();
    }

    /**
     * Create a new Builder for Services.
     *
     * @return Builder
     */

    public static Builder services() {
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
        Services services1 = (Services) o;
        return Objects.equals(services, services1.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(services);
    }

    /**
     * Builder to help construct the Services.
     */
    public static final class Builder {

        private final Map<String, Resource> services = new TreeMap<>();

        /**
         * Private default constructor.
         */
        private Builder() {
        }

        /**
         * Adds a {@link Resource}.
         *
         * @param name Service name
         * @param resource {@link Resource}
         * @return Builder
         */
        @JsonAnySetter
        public Builder put(String name, Resource resource) {
            if (isEmpty(name) || containsWhitespace(name)) {
                throw new IllegalArgumentException("Resource name required and may not contain whitespace");
            }
            if (services.containsKey(name) && !services.get(name).equals(resource)) {
                throw new IllegalStateException("The give Resource name already exists but the Resource objects"
                        + " are not equal");
            }

            services.put(name, checkNotNull(resource));
            return this;
        }

        /**
         * Builds the Definitions instance.
         *
         * @return Definitions instance
         */
        public Services build() {
            return new Services(this);
        }
    }


}
