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

import static org.forgerock.api.models.Definitions.definitions;
import static org.forgerock.api.models.Errors.errors;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Services.services;
import static org.forgerock.api.util.ValidationUtil.isEmpty;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.util.i18n.LocalizableString;

/** Class that represents the ApiDescription type in API descriptor. */
@JsonDeserialize(builder = ApiDescription.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiDescription {

    private final String id;
    private final String version;
    private final LocalizableString description;
    private final Definitions definitions;
    private final Services services;
    private final Errors errors;
    private final Paths paths;

    private ApiDescription(Builder builder) {
        this.id = builder.id;
        this.version = builder.version;
        this.description = builder.description;
        this.definitions = builder.definitions == null ? definitions().build() : builder.definitions;
        this.services = builder.services == null ? services().build() : builder.services;
        this.errors = builder.errors == null ? errors().build() : builder.errors;
        this.paths = builder.paths == null ? paths().build() : builder.paths;

        if (isEmpty(id) || isEmpty(version)) {
            throw new ApiValidationException("id and version required");
        }
    }

    /**
     * Getter of id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Getter of version.
     *
     * @return The version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets description of API Descriptor.
     *
     * @return Description of API Descriptor
     */
    public LocalizableString getDescription() {
        return description;
    }

    /**
     * Getter of definitions.
     *
     * @return Definitions map
     */
    public Definitions getDefinitions() {
        return definitions.getDefinitions().isEmpty() ? null : definitions;
    }

    /**
     * This allows the models package to mutate the schema defined here. This is used when processing
     * annotations on resources that may reference schemas using an id, so those schemas need to be defined here
     * rather than in-line in the resource descriptions.
     *
     * @param id The definition id.
     * @param schema The definition.
     */
    void addDefinition(String id, Schema schema) {
        if (schema.getReference() != null) {
            throw new IllegalArgumentException("Cannot define a schema using a reference");
        }
        Schema defined = definitions.get(id);
        if (defined != null && !defined.equals(schema)) {
            throw new IllegalArgumentException("Trying to redefine already defined schema, " + id);
        }
        definitions.getDefinitions().put(id, schema);
    }

    /**
     * Getter of services.
     *
     * @return Services map
     */
    public Services getServices() {
        return services.getServices().isEmpty() ? null : services;
    }

    /**
     * This allows the models package to mutate the resources defined here. This is used when processing
     * annotations on resources that may reference resources using an id, so those services need to be defined here
     * rather than in-line in the resource descriptions.
     *
     * @param id The resource id.
     * @param resource The resource definition.
     * @see Resource#fromAnnotatedType(Class, Resource.AnnotatedTypeVariant, ApiDescription)
     */
    void addService(String id, Resource resource) {
        if (resource.getReference() != null) {
            throw new IllegalArgumentException("Cannot define a resource using a reference");
        }
        Resource defined = services.get(id);
        if (defined != null && !defined.equals(resource)) {
            throw new IllegalArgumentException("Trying to redefine already defined resource, " + id);
        }
        services.getServices().put(id, resource);
    }

    /**
     * Getter of errors.
     *
     * @return Errors map
     */
    public Errors getErrors() {
        return errors.getErrors().isEmpty() ? null : errors;
    }

    /**
     * This allows the models package to mutate the errors defined here. This is used when processing annotations on
     * resources that may reference errors using an id, so those errors need to be defined here rather than in-line in
     * the resource descriptions.
     *
     * @param id The error id.
     * @param apiError The error definition.
     * @see ApiError#fromAnnotation(org.forgerock.api.annotations.ApiError, ApiDescription, Class)
     */
    void addError(String id, ApiError apiError) {
        if (apiError.getReference() != null) {
            throw new IllegalArgumentException("Cannot define an apiError using a reference");
        }
        ApiError defined = errors.get(id);
        if (defined != null && !defined.equals(apiError)) {
            throw new IllegalArgumentException("Trying to redefine already defined apiError, " + id);
        }
        errors.getErrors().put(id, apiError);
    }

    /**
     * Getter of paths.
     *
     * @return Paths
     */
    // Jackson queries PathsModule.PathsSerializer.isEmpty() to know whether a Paths object is empty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Paths getPaths() {
        return paths;
    }

    /**
     * Create a new Builder for ApiDescription.
     *
     * @return Builder
     */
    public static Builder apiDescription() {
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
        ApiDescription that = (ApiDescription) o;
        return Objects.equals(id, that.id)
                && Objects.equals(version, that.version)
                && Objects.equals(description, that.description)
                && Objects.equals(definitions, that.definitions)
                && Objects.equals(services, that.services)
                && Objects.equals(errors, that.errors)
                && Objects.equals(paths, that.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, description, definitions, services, errors, paths);
    }

    /**
     * Builder for the ApiDescription.
     */
    public static final class Builder {

        private String id;
        private LocalizableString description;
        private Definitions definitions;
        private Errors errors;
        private Services services;
        private Paths paths;
        private String version;

        /**
         * Private default constructor with the mandatory fields.
         */
        private Builder() {
        }

        /**
         * Set the id.
         *
         * @param id ApiDescription id
         * @return Builder
         */
        @JsonProperty("id")
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description Description of API Description
         * @return Builder
         */
        @JsonProperty("description")
        public Builder description(String description) {
            this.description = new LocalizableString(description);
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description Description of API Description
         * @return Builder
         */
        public Builder description(LocalizableString description) {
            this.description = description;
            return this;
        }

        /**
         * Set the definitions.
         *
         * @param definitions Definitions for this API Description
         * @return Builder
         */
        @JsonProperty("definitions")
        public Builder definitions(Definitions definitions) {
            this.definitions = definitions;
            return this;
        }


        /**
         * Set the services.
         *
         * @param services Services for this API Description
         * @return Builder
         */
        @JsonProperty("services")
        public Builder services(Services services) {
            this.services = services;
            return this;
        }

        /**
         * Set the errors.
         *
         * @param errors Errors for this API Description
         * @return Builder
         */
        @JsonProperty("errors")
        public Builder errors(Errors errors) {
            this.errors = errors;
            return this;
        }

        /**
         * Set the paths.
         *
         * @param paths Paths
         * @return Builder
         */
        @JsonProperty("paths")
        public Builder paths(Paths paths) {
            this.paths = paths;
            return this;
        }

        /**
         * Set the version of the API.
         *
         * @param version The version.
         * @return This builder.
         */
        @JsonProperty("version")
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Builds the ApiDescription instance.
         *
         * @return ApiDescription instance
         */
        public ApiDescription build() {
            return new ApiDescription(this);
        }
    }
}

