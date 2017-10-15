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

import static org.forgerock.api.models.Reference.reference;
import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static org.forgerock.util.Reject.rejectStateIfTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.forgerock.api.ApiValidationException;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Queries;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.LocalizableString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Class that represents the Resource type in API descriptor.
 * <p>
 *     {@code Resource}s may be either a reference to another {@code Resource} that will be defined elsewhere in the
 *     API Descriptor, or a described resource. If a {@link Reference} is provided, then none of the other fields may
 *     be used, and if any of the other fields are used, a reference may not be provided.
 * </p>
 */
@JsonDeserialize(builder = Resource.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Resource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Resource.class);
    private static final String SERVICES_REFERENCE = "#/services/%s";

    @JsonProperty("$ref")
    private final Reference reference;
    private final Schema resourceSchema;
    private final LocalizableString title;
    private final LocalizableString description;
    private final Create create;
    private final Read read;
    private final Update update;
    private final Delete delete;
    private final Patch patch;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Action[] actions;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Query[] queries;
    private final SubResources subresources;
    private final Items items;
    private final Boolean mvccSupported;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Parameter[] parameters;

    private Resource(Builder builder) {
        this.reference = builder.reference;
        this.resourceSchema = builder.resourceSchema;
        this.title = builder.title;
        this.description = builder.description;
        this.create = builder.create;
        this.read = builder.read;
        this.update = builder.update;
        this.delete = builder.delete;
        this.patch = builder.patch;
        this.subresources = builder.subresources;
        this.actions = builder.actions.toArray(new Action[builder.actions.size()]);
        this.queries = builder.queries.toArray(new Query[builder.queries.size()]);
        this.items = builder.items;
        this.mvccSupported = builder.mvccSupported;
        this.parameters = builder.parameters.toArray(new Parameter[builder.parameters.size()]);

        if ((create != null || read != null || update != null || delete != null || patch != null
                || !isEmpty(actions) || !isEmpty(queries)) && reference != null) {
            throw new ApiValidationException("Cannot have a reference as well as operations");
        }
        if (mvccSupported == null && reference == null) {
            throw new ApiValidationException("mvccSupported required for non-reference Resources");
        }
    }

    /**
     * Getter of resource schema.
     *
     * @return Resource schema
     */
    public Schema getResourceSchema() {
        return resourceSchema;
    }

    /**
     * Getter of title.
     *
     * @return Title
     */
    public LocalizableString getTitle() {
        return title;
    }

    /**
     * Getter of description.
     *
     * @return Description
     */
    public LocalizableString getDescription() {
        return description;
    }

    /**
     * Getter of Create.
     *
     * @return Create
     */
    public Create getCreate() {
        return create;
    }

    /**
     * Getter of Read.
     *
     * @return Read
     */
    public Read getRead() {
        return read;
    }

    /**
     * Getter of Update.
     *
     * @return Update
     */
    public Update getUpdate() {
        return update;
    }

    /**
     * Getter of Delete.
     *
     * @return Delete
     */
    public Delete getDelete() {
        return delete;
    }

    /**
     * Getter of Patch.
     *
     * @return Patch
     */
    public Patch getPatch() {
        return patch;
    }

    /**
     * Getter of actions.
     *
     * @return Actions
     */
    public Action[] getActions() {
        return actions;
    }

    /**
     * Getter of queries.
     *
     * @return Queries
     */
    public Query[] getQueries() {
        return queries;
    }

    /**
     * Getter of sub-resources.
     *
     * @return Sub-resources
     */
    public SubResources getSubresources() {
        return subresources;
    }

    /**
     * Gets the reference.
     * @return The reference.
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Getter of items.
     *
     * @return Items
     */
    public Items getItems() {
        return items;
    }

    /**
     * Informs if MVCC is supported.
     *
     * @return {@code true} if MVCC is supported and {@code false} otherwise
     */
    public Boolean isMvccSupported() {
        return mvccSupported;
    }

    /**
     * Getter of the parameters array.
     *
     * @return Parameters
     */
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(reference, resource.reference)
                && Objects.equals(resourceSchema, resource.resourceSchema)
                && Objects.equals(title, resource.title)
                && Objects.equals(description, resource.description)
                && Objects.equals(create, resource.create)
                && Objects.equals(read, resource.read)
                && Objects.equals(update, resource.update)
                && Objects.equals(delete, resource.delete)
                && Objects.equals(patch, resource.patch)
                && Arrays.equals(actions, resource.actions)
                && Arrays.equals(queries, resource.queries)
                && Objects.equals(subresources, resource.subresources)
                && Objects.equals(items, resource.items)
                && Objects.equals(mvccSupported, resource.mvccSupported)
                && Arrays.equals(parameters, resource.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, resourceSchema, title, description, create, read, update, delete, patch, actions,
                queries, subresources, items, mvccSupported, parameters);
    }

    /**
     * Create a new Builder for Resoruce.
     *
     * @return Builder
     */
    public static Builder resource() {
        return new Builder();
    }

    /**
     * Build a {@code Resource} from an annotated request handler.
     * @param type The annotated type.
     * @param variant The annotated type variant.
     * @param descriptor The root descriptor to add definitions to.
     * @return The built {@code Resource} object.
     */
    public static Resource fromAnnotatedType(Class<?> type, AnnotatedTypeVariant variant, ApiDescription descriptor) {
        return fromAnnotatedType(type, variant, null, null, descriptor);
    }

    /**
     * Build a {@code Resource} from an annotated request handler.
     * @param type The annotated type.
     * @param variant The annotated type variant.
     * @param subResources The sub resources object to be included, if any sub-resources exist, or null.
     * @param descriptor The root descriptor to add definitions to.
     * @param extraParameters Extra parameters not from the resource annotation.
     * @return The built {@code Resource} object.
     */
    public static Resource fromAnnotatedType(Class<?> type, AnnotatedTypeVariant variant, SubResources subResources,
            ApiDescription descriptor, Parameter... extraParameters) {
        return fromAnnotatedType(type, variant, subResources, null, descriptor, extraParameters);
    }

    /**
     * Build a {@code Resource} from an annotated request handler.
     * @param type The annotated type.
     * @param variant The annotated type variant.
     * @param items The items definition for a collection variant, or null.
     * @param descriptor The root descriptor to add definitions to.
     * @param extraParameters Extra parameters not from the resource annotation.
     * @return The built {@code Resource} object.
     */
    public static Resource fromAnnotatedType(Class<?> type, AnnotatedTypeVariant variant,
            Items items, ApiDescription descriptor, Parameter... extraParameters) {
        return fromAnnotatedType(type, variant, null, items, descriptor, extraParameters);
    }

    private static Resource fromAnnotatedType(Class<?> type, AnnotatedTypeVariant variant,
            SubResources subResources, Items items, ApiDescription descriptor, Parameter... extraParameters) {
        Builder builder = resource();
        Handler handler = findHandlerAnnotation(variant, type);
        if (handler == null) {
            return null;
        }
        boolean foundCrudpq = false;
        for (Method m : type.getMethods()) {
            boolean instanceMethod = Arrays.asList(m.getParameterTypes()).indexOf(String.class) > -1;
            org.forgerock.api.annotations.Action action = m.getAnnotation(org.forgerock.api.annotations.Action.class);
            if (action != null && instanceMethod == variant.actionRequiresId) {
                builder.actions.add(Action.fromAnnotation(action, m, descriptor, type));
            }
            Actions actions = m.getAnnotation(Actions.class);
            if (actions != null && instanceMethod == variant.actionRequiresId) {
                for (org.forgerock.api.annotations.Action a : actions.value()) {
                    builder.actions.add(Action.fromAnnotation(a, null, descriptor, type));
                }
            }
            org.forgerock.api.annotations.Create create = m.getAnnotation(org.forgerock.api.annotations.Create.class);
            if (create != null) {
                builder.create = Create.fromAnnotation(create, variant.instanceCreate, descriptor, type);
                foundCrudpq = true;
            }
            if (variant.rudpOperations) {
                org.forgerock.api.annotations.Read read = m.getAnnotation(org.forgerock.api.annotations.Read.class);
                if (read != null) {
                    builder.read = Read.fromAnnotation(read, descriptor, type);
                    foundCrudpq = true;
                }
                org.forgerock.api.annotations.Update update =
                        m.getAnnotation(org.forgerock.api.annotations.Update.class);
                if (update != null) {
                    builder.update = Update.fromAnnotation(update, descriptor, type);
                    foundCrudpq = true;
                }
                org.forgerock.api.annotations.Delete delete =
                        m.getAnnotation(org.forgerock.api.annotations.Delete.class);
                if (delete != null) {
                    builder.delete = Delete.fromAnnotation(delete, descriptor, type);
                    foundCrudpq = true;
                }
                org.forgerock.api.annotations.Patch patch = m.getAnnotation(org.forgerock.api.annotations.Patch.class);
                if (patch != null) {
                    builder.patch = Patch.fromAnnotation(patch, descriptor, type);
                    foundCrudpq = true;
                }
            }
            if (variant.queryOperations) {
                org.forgerock.api.annotations.Query query = m.getAnnotation(org.forgerock.api.annotations.Query.class);
                if (query != null) {
                    builder.queries.add(Query.fromAnnotation(query, m, descriptor, type));
                    foundCrudpq = true;
                }
                Queries queries = m.getAnnotation(Queries.class);
                if (queries != null) {
                    for (org.forgerock.api.annotations.Query q : queries.value()) {
                        builder.queries.add(Query.fromAnnotation(q, null, descriptor, type));
                        foundCrudpq = true;
                    }
                }
            }
        }
        Schema resourceSchema = Schema.fromAnnotation(handler.resourceSchema(), descriptor, type);
        if (foundCrudpq && resourceSchema == null) {
            throw new IllegalArgumentException("CRUDPQ operation(s) defined, but no resource schema declared");
        }

        for (org.forgerock.api.annotations.Parameter parameter : handler.parameters()) {
            builder.parameter(Parameter.fromAnnotation(type, parameter));
        }
        for (Parameter param : extraParameters) {
            builder.parameter(param);
        }

        Resource resource = builder.resourceSchema(resourceSchema)
                .mvccSupported(handler.mvccSupported())
                .title(new LocalizableString(handler.title(), type))
                .description(new LocalizableString(handler.description(), type))
                .subresources(subResources)
                .items(items)
                .build();

        if (!handler.id().isEmpty()) {
            descriptor.addService(handler.id(), resource);
            Reference reference = reference().value(String.format(SERVICES_REFERENCE, handler.id())).build();
            resource = resource().reference(reference).build();
        }
        return resource;
    }

    private static Handler findHandlerAnnotation(AnnotatedTypeVariant variant, Class<?> type) {
        switch (variant) {
        case SINGLETON_RESOURCE:
            if (type.getAnnotation(SingletonProvider.class) != null) {
                return type.getAnnotation(SingletonProvider.class).value();
            }
            break;
        case REQUEST_HANDLER:
            if (type.getAnnotation(RequestHandler.class) != null) {
                return type.getAnnotation(RequestHandler.class).value();
            }
            break;
        default:
            if (type.getAnnotation(CollectionProvider.class) != null) {
                return type.getAnnotation(CollectionProvider.class).details();
            }
        }
        LOGGER.info("Asked for Resource for annotated type, but type does not have required RequestHandler"
                + " annotation. No api descriptor will be available for " + type);
        return null;
    }

    /**
     * The variant of the annotated type. Allows the annotation processing to make assumptions about what type of
     * operations are expected from this context of the type.
     */
    public enum AnnotatedTypeVariant {
        /** A singleton resource handler. (expect RUDPA operations). */
        SINGLETON_RESOURCE(true, true, false, false),
        /** A collection resource handler, collection endpoint (expect CAQ opererations). */
        COLLECTION_RESOURCE_COLLECTION(false, false, false, true),
        /** A collection resource handler, instance endpoint (expect CRUDPA operations). */
        COLLECTION_RESOURCE_INSTANCE(true, true, true, false),
        /** A plain request handler (expects all operations). */
        REQUEST_HANDLER(false, true, false, true);

        private final boolean instanceCreate;
        private final boolean rudpOperations;
        private final boolean actionRequiresId;
        private final boolean queryOperations;

        AnnotatedTypeVariant(boolean instanceCreate, boolean rudpOperations, boolean actionRequiresId,
                boolean queryOperations) {
            this.instanceCreate = instanceCreate;
            this.rudpOperations = rudpOperations;
            this.actionRequiresId = actionRequiresId;
            this.queryOperations = queryOperations;
        }
    }

    /**
     * Builder to help construct the Resource.
     */
    public final static class Builder {
        private Schema resourceSchema;
        private LocalizableString title;
        private LocalizableString description;
        private Create create;
        private Read read;
        private Update update;
        private Delete delete;
        private Patch patch;
        private SubResources subresources;
        private final Set<Action> actions;
        private final Set<Query> queries;
        private Items items;
        private Boolean mvccSupported;
        private Reference reference;
        private final List<Parameter> parameters;
        private boolean built = false;

        /**
         * Private default constructor.
         */
        protected Builder() {
            actions = new TreeSet<>();
            queries = new TreeSet<>();
            parameters = new ArrayList<>();
        }

        /**
         * Set a reference.
         * @param reference The reference.
         * @return This builder.
         */
        @JsonProperty("$ref")
        public Builder reference(Reference reference) {
            checkState();
            this.reference = reference;
            return this;
        }

        /**
         * Set the resource schema.
         *
         * @param resourceSchema The schema of the resource for this path.
         * Required when any of create, read, update, delete, patch are supported
         * @return Builder
         */
        @JsonProperty("resourceSchema")
        public Builder resourceSchema(Schema resourceSchema) {
            checkState();
            this.resourceSchema = resourceSchema;
            return this;
        }

        /**
         * Set the title.
         *
         * @param title Title of the endpoint
         * @return Builder
         */
        public Builder title(LocalizableString title) {
            this.title = title;
            return this;
        }

        /**
         * Set the title.
         *
         * @param title Title of the endpoint
         * @return Builder
         */
        @JsonProperty("title")
        public Builder title(String title) {
            return title(new LocalizableString(title));
        }

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        public Builder description(LocalizableString description) {
            checkState();
            this.description = description;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        @JsonProperty("description")
        public Builder description(String description) {
            checkState();
            return description(new LocalizableString(description));
        }

        /**
         * Set create.
         *
         * @param create The create operation description, if supported
         * @return Builder
         */
        @JsonProperty("create")
        public Builder create(Create create) {
            checkState();
            this.create = create;
            return this;
        }

        /**
         * Set Read.
         *
         * @param read The read operation description, if supported
         * @return Builder
         */
        @JsonProperty("read")
        public Builder read(Read read) {
            checkState();
            this.read = read;
            return this;
        }

        /**
         * Set Update.
         *
         * @param update The update operation description, if supported
         * @return Builder
         */
        @JsonProperty("update")
        public Builder update(Update update) {
            checkState();
            this.update = update;
            return this;
        }

        /**
         * Set Delete.
         *
         * @param delete The delete operation description, if supported
         * @return Builder
         */
        @JsonProperty("delete")
        public Builder delete(Delete delete) {
            checkState();
            this.delete = delete;
            return this;
        }

        /**
         * Set Patch.
         *
         * @param patch The patch operation description, if supported
         * @return Builder
         */
        @JsonProperty("patch")
        public Builder patch(Patch patch) {
            checkState();
            this.patch = patch;
            return this;
        }

        /**
         * Set Actions.
         *
         * @param actions The list of action operation descriptions, if supported
         * @return Builder
         */
        @JsonProperty("actions")
        public Builder actions(List<Action> actions) {
            checkState();
            this.actions.addAll(actions);
            return this;
        }

        /**
         * Adds one Action to the list of Actions.
         *
         * @param action Action operation description to be added to the list
         * @return Builder
         */
        public Builder action(Action action) {
            checkState();
            this.actions.add(action);
            return this;
        }

        /**
         * Set Queries.
         *
         * @param queries The list or query operation descriptions, if supported
         * @return Builder
         */
        @JsonProperty("queries")
        public Builder queries(List<Query> queries) {
            checkState();
            this.queries.addAll(queries);
            return this;
        }

        /**
         * Adds one Query to the list of queries.
         *
         * @param query Query operation description to be added to the list
         * @return Builder
         */
        public Builder query(Query query) {
            checkState();
            this.queries.add(query);
            return this;
        }

        /**
         * Sets the sub-resources for this resource.
         *
         * @param subresources The sub-reosurces definition.
         * @return Builder
         */
        @JsonProperty("subresources")
        public Builder subresources(SubResources subresources) {
            checkState();
            this.subresources = subresources;
            return this;
        }

        /**
         * Allocates the operations given in the parameter by their type.
         *
         * @param operations One or more Operations
         * @return Builder
         */
        @JsonProperty("operations")
        public Builder operations(Operation... operations) {
            checkState();
            Reject.ifNull(operations);
            for (Operation operation : operations) {
                operation.allocateToResource(this);
            }
            return this;
        }

        /**
         * Setter for MVCC-supported flag.
         *
         * @param mvccSupported Whether this resource supports MVCC
         * @return Builder
         */
        @JsonProperty("mvccSupported")
        public Builder mvccSupported(Boolean mvccSupported) {
            checkState();
            this.mvccSupported = mvccSupported;
            return this;
        }

        /**
         * Adds items-resource.
         *
         * @param items The definition of the collection items
         * @return Builder
         */
        @JsonProperty("items")
        public Builder items(Items items) {
            checkState();
            this.items = items;
            return this;
        }

        /**
         * Set multiple supported parameters.
         *
         * @param parameters Extra parameters supported by the resource
         * @return Builder
         */
        @JsonProperty("parameters")
        public Builder parameters(List<Parameter> parameters) {
            checkState();
            this.parameters.addAll(parameters);
            return this;
        }

        /**
         * Sets a single supported parameter.
         *
         * @param parameter Extra parameter supported by the resource
         * @return Builder
         */
        public Builder parameter(Parameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        /**
         * Construct a new instance of Resource.
         *
         * @return Resource instance
         */
        public Resource build() {
            checkState();
            this.built = true;
            if (create == null && read == null && update == null && delete == null && patch == null
                    && actions.isEmpty() && queries.isEmpty() && reference == null && items == null
                    && subresources == null) {
                return null;
            }

            return new Resource(this);
        }

        private void checkState() {
            rejectStateIfTrue(built, "Already built Resource");
        }

    }
}
