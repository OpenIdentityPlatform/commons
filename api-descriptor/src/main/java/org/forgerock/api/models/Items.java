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

import static org.forgerock.api.util.ValidationUtil.isEmpty;
import static org.forgerock.util.Reject.rejectStateIfTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.forgerock.api.ApiValidationException;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents the Items type in API descriptor.
 */
public final class Items {

    private static final Logger LOGGER = LoggerFactory.getLogger(Items.class);

    private final String title;
    private final String description;
    private final Create create;
    private final Read read;
    private final Update update;
    private final Delete delete;
    private final Patch patch;
    private final Action[] actions;
    //private final SubResources subresources;
    private final Parameter[] parameters;

    private Items(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.create = builder.create;
        this.read = builder.read;
        this.update = builder.update;
        this.delete = builder.delete;
        this.patch = builder.patch;
        //this.subresources = builder.subresources;
        this.actions = builder.actions.toArray(new Action[builder.actions.size()]);

        final List<Parameter> parameters = builder.parameters;
        this.parameters = parameters.toArray(new Parameter[parameters.size()]);

        if (create == null && read == null && update == null && delete == null && patch == null && isEmpty(actions)) {
            throw new ApiValidationException("At least one operation required");
        }
    }

    /**
     * Getter of title.
     *
     * @return Title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter of description.
     *
     * @return Description
     */
    public String getDescription() {
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

//    /**
//     * Getter of sub-resources.
//     *
//     * @return Sub-resources
//     */
//    public SubResources getSubresources() {
//        return subresources;
//    }

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
        Items items = (Items) o;
        return Objects.equals(title, items.title)
                && Objects.equals(description, items.description)
                && Objects.equals(create, items.create)
                && Objects.equals(read, items.read)
                && Objects.equals(update, items.update)
                && Objects.equals(delete, items.delete)
                && Objects.equals(patch, items.patch)
                && Arrays.equals(actions, items.actions)
//                && Arrays.equals(subresources, items.subresources)
                && Arrays.equals(parameters, items.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, create, read, update, delete, patch, actions, parameters);
//      return Objects.hash(title, description, create, read, update, delete, patch, actions, parameters, subresources);
    }

    /**
     * Builds a {@link Resource} from this {@code Items} instance.
     *
     * @param mvccSupported {@code true} when MVCC is supported and {@code false} otherwise
     * @param resourceSchema Resource-{@link Schema} or {@code null}
     * @return New {@link Resource}
     */
    @JsonIgnore
    public Resource asResource(final boolean mvccSupported, final Schema resourceSchema) {
        return Resource.resource()
                .mvccSupported(mvccSupported)
                .resourceSchema(resourceSchema)
                .title(getTitle())
                .description(getDescription())
                .create(getCreate())
                .read(getRead())
                .update(getUpdate())
                .delete(getDelete())
                .patch(getPatch())
                .actions(Arrays.asList(getActions()))
                .parameters(Arrays.asList(getParameters()))
                .build();
    }

    /**
     * Create a new Builder for Resoruce.
     *
     * @return Builder
     */
    public static Builder items() {
        return new Builder();
    }

    /**
     * Build an {@code Items} from an annotated request handler.
     *
     * @param type The annotated type.
     * @param descriptor The root descriptor to add definitions to.
     * @return The built {@code Items} object.
     */
    public static Items fromAnnotatedType(final Class<?> type, final ApiDescription descriptor) {
        final Builder builder = items();
        final RequestHandler requestHandler = type.getAnnotation(RequestHandler.class);
        if (requestHandler == null) {
            LOGGER.warn("Asked for Items for annotated type, but type does not have required RequestHandler"
                    + " annotation. Returning null for " + type);
            return null;
        }

        for (final Method m : type.getDeclaredMethods()) {
            org.forgerock.api.annotations.Action action = m.getAnnotation(org.forgerock.api.annotations.Action.class);
            if (action != null) {
                builder.actions.add(Action.fromAnnotation(action, m, descriptor, type));
            }
            Actions actions = m.getAnnotation(Actions.class);
            if (actions != null) {
                for (org.forgerock.api.annotations.Action a : actions.value()) {
                    builder.actions.add(Action.fromAnnotation(a, null, descriptor, type));
                }
            }
            org.forgerock.api.annotations.Create create = m.getAnnotation(org.forgerock.api.annotations.Create.class);
            if (create != null) {
                builder.create = Create.fromAnnotation(create, true, descriptor, type);
            }
            org.forgerock.api.annotations.Read read = m.getAnnotation(org.forgerock.api.annotations.Read.class);
            if (read != null) {
                builder.read = Read.fromAnnotation(read, descriptor, type);
            }
            org.forgerock.api.annotations.Update update =
                    m.getAnnotation(org.forgerock.api.annotations.Update.class);
            if (update != null) {
                builder.update = Update.fromAnnotation(update, descriptor, type);
            }
            org.forgerock.api.annotations.Delete delete =
                    m.getAnnotation(org.forgerock.api.annotations.Delete.class);
            if (delete != null) {
                builder.delete = Delete.fromAnnotation(delete, descriptor, type);
            }
            org.forgerock.api.annotations.Patch patch = m.getAnnotation(org.forgerock.api.annotations.Patch.class);
            if (patch != null) {
                builder.patch = Patch.fromAnnotation(patch, descriptor, type);
            }
        }

        for (org.forgerock.api.annotations.Parameter parameter : requestHandler.parameters()) {
            builder.parameter(Parameter.fromAnnotation(parameter));
        }

        final Items items = builder
                .title(requestHandler.title())
                .description(requestHandler.description())
                //.subresources(subResources)
                .build();

        return items;
    }

    /**
     * Builder to help construct the {@code Items}.
     */
    public final static class Builder {
        private String title;
        private String description;
        private Create create;
        private Read read;
        private Update update;
        private Delete delete;
        private Patch patch;
        //private SubResources subresources;
        private final Set<Action> actions;
        private final List<Parameter> parameters;
        private boolean built = false;

        /**
         * Private default constructor.
         */
        protected Builder() {
            actions = new TreeSet<>();
            parameters = new ArrayList<>();
        }

        /**
         * Set the title.
         *
         * @param title Title of the endpoint
         * @return Builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the description.
         *
         * @param description A description of the endpoint
         * @return Builder
         */
        public Builder description(String description) {
            checkState();
            this.description = description;
            return this;
        }

        /**
         * Set create.
         *
         * @param create The create operation description, if supported
         * @return Builder
         */
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

//        /**
//         * Sets the sub-resources for this resource.
//         *
//         * @param subresources The sub-reosurces definition.
//         * @return Builder
//         */
//        public Builder subresources(SubResources subresources) {
//            checkState();
//            this.subresources = subresources;
//            return this;
//        }

        /**
         * Set multiple supported parameters.
         *
         * @param parameters Extra parameters supported by the resource
         * @return Builder
         */
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
        public Items build() {
            checkState();
            this.built = true;
            if (create == null && read == null && update == null && delete == null && patch == null
                    && actions.isEmpty()) {
                return null;
            }

            return new Items(this);
        }

        private void checkState() {
            rejectStateIfTrue(built, "Already built Items");
        }

    }
}
