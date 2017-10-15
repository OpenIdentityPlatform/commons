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

import static org.forgerock.api.enums.ParameterSource.*;
import static org.forgerock.api.util.ValidationUtil.*;
import static org.forgerock.util.Reject.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.forgerock.api.ApiValidationException;
import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.util.i18n.LocalizableString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents the Items type in API descriptor.
 */
@JsonDeserialize(builder = Items.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Items {

    private static final Logger LOGGER = LoggerFactory.getLogger(Items.class);

    private final Create create;
    private final Read read;
    private final Update update;
    private final Delete delete;
    private final Patch patch;
    private final Action[] actions;
    private final SubResources subresources;
    private final Parameter pathParameter;

    private Items(Builder builder) {
        this.create = builder.create;
        this.read = builder.read;
        this.update = builder.update;
        this.delete = builder.delete;
        this.patch = builder.patch;
        this.subresources = builder.subresources;
        this.pathParameter = builder.pathParameter;
        this.actions = builder.actions.toArray(new Action[builder.actions.size()]);

        if (create == null && read == null && update == null && delete == null && patch == null && isEmpty(actions)) {
            throw new ApiValidationException("At least one operation required");
        }
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
        return actions.length == 0 ? null : actions;
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
     * Get the path parameter.
     *
     * @return The path parameter.
     */
    public Parameter getPathParameter() {
        return pathParameter;
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
        return Objects.equals(create, items.create)
                && Objects.equals(read, items.read)
                && Objects.equals(update, items.update)
                && Objects.equals(delete, items.delete)
                && Objects.equals(patch, items.patch)
                && Arrays.equals(actions, items.actions)
                && Objects.equals(subresources, items.subresources)
                && Objects.equals(pathParameter, items.pathParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(create, read, update, delete, patch, actions, pathParameter, subresources);
    }

    /**
     * Builds a {@link Resource} from this {@code Items} instance.
     *
     * @param mvccSupported {@code true} when MVCC is supported and {@code false} otherwise
     * @param resourceSchema Resource-{@link Schema} or {@code null}
     * @param title The resource title.
     * @param description The resource description.
     * @return New {@link Resource}
     */
    @JsonIgnore
    public Resource asResource(boolean mvccSupported, Schema resourceSchema, LocalizableString title,
            LocalizableString description) {
        final List<Action> actions =
                getActions() == null ? Collections.<Action>emptyList() : Arrays.asList(getActions());
        return Resource.resource()
                .mvccSupported(mvccSupported)
                .resourceSchema(resourceSchema)
                .title(title)
                .description(description)
                .create(getCreate())
                .read(getRead())
                .update(getUpdate())
                .delete(getDelete())
                .patch(getPatch())
                .actions(actions)
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
     * @param subResources The sub resources.
     * @return The built {@code Items} object.
     */
    public static Items fromAnnotatedType(Class<?> type, ApiDescription descriptor, SubResources subResources) {
        final Builder builder = items();
        final CollectionProvider provider = type.getAnnotation(CollectionProvider.class);
        if (provider == null) {
            LOGGER.info("Asked for Items for annotated type, but type does not have required RequestHandler"
                    + " annotation. No api descriptor will be available for " + type);
            return null;
        }
        builder.pathParameter(Parameter.fromAnnotation(type, provider.pathParam()));

        for (final Method m : type.getMethods()) {
            boolean instanceMethod = Arrays.asList(m.getParameterTypes()).indexOf(String.class) > -1;
            org.forgerock.api.annotations.Action action = m.getAnnotation(org.forgerock.api.annotations.Action.class);
            if (action != null && instanceMethod) {
                builder.actions.add(Action.fromAnnotation(action, m, descriptor, type));
            }
            Actions actions = m.getAnnotation(Actions.class);
            if (actions != null && instanceMethod) {
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

        return builder.subresources(subResources).build();
    }

    /**
     * Builder to help construct the {@code Items}.
     */
    public final static class Builder {
        private Create create;
        private Read read;
        private Update update;
        private Delete delete;
        private Patch patch;
        private SubResources subresources;
        private Parameter pathParameter = Parameter.parameter().name("id").type("string").source(PATH).required(true)
                .build();
        private final Set<Action> actions;
        private boolean built = false;

        /**
         * Private default constructor.
         */
        protected Builder() {
            actions = new TreeSet<>();
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
         * Sets the path parameter for this resource.
         *
         * @param pathParameter The path parameter definition.
         * @return Builder
         */
        @JsonProperty("pathParameter")
        public Builder pathParameter(Parameter pathParameter) {
            checkState();
            this.pathParameter = pathParameter;
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
         * Construct a new instance of Resource.
         *
         * @return Resource instance
         */
        public Items build() {
            checkState();
            this.built = true;
            if (create == null && read == null && update == null && delete == null && patch == null
                    && actions.isEmpty() && subresources == null) {
                return null;
            }

            return new Items(this);
        }

        private void checkState() {
            rejectStateIfTrue(built, "Already built Items");
        }

    }
}
