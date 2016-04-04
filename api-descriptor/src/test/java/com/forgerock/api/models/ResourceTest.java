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

package com.forgerock.api.models;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.forgerock.api.ApiValidationException;
import com.forgerock.api.annotations.*;
import com.forgerock.api.annotations.Error;
import com.forgerock.api.annotations.Parameter;
import com.forgerock.api.enums.CreateMode;
import com.forgerock.api.enums.PatchOperations;
import com.forgerock.api.enums.QueryType;
import com.forgerock.api.enums.Stability;

import org.forgerock.services.context.SecurityContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ResourceTest {

    private final String description = "My Description";
    private Schema schema;
    private Create create;
    private Read read;
    private Update update;
    private Delete delete;
    private Patch patch;
    private Action action1;
    private Action action2;
    private Query query1;
    private Query query2;

    @BeforeClass
    public void beforeClass() {
        schema = Schema.schema()
                .schema(json(object()))
                .build();
        create = Create.create()
                .mode(CreateMode.ID_FROM_SERVER)
                .mvccSupported(true)
                .build();
        read = Read.read()
                .build();
        update = Update.update()
                .mvccSupported(true)
                .build();
        delete = Delete.delete()
                .mvccSupported(true)
                .build();
        patch = Patch.patch()
                .mvccSupported(true)
                .operations(PatchOperations.ADD, PatchOperations.COPY)
                .build();
        action1 = Action.action()
                .name("action1")
                .response(schema)
                .build();
        action2 = Action.action()
                .name("action2")
                .response(schema)
                .build();
        query1 = Query.query()
                .type(QueryType.ID)
                .queryId("q1")
                .build();
        query2 = Query.query()
                .type(QueryType.ID)
                .queryId("q2")
                .build();
    }

    /**
     * Test the {@Resource} builder with builder-methods that do <em>not</em> take arrays as arguments.
     */
    @Test
    public void testBuilderWithNonArrayMethods() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .action(action2)
                .query(query1)
                .query(query2)
                .build();

        assertTestBuilder(resource);
    }

    /**
     * Test the {@Resource} builder with builder-methods that take arrays as arguments.
     */
    @Test
    public void testBuilderWithArrayMethods() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .actions(asList(action1, action2))
                .queries(asList(query1, query2))
                .build();

        assertTestBuilder(resource);
    }

    @Test
    public void testBuilderWithOperationsArray() {
        final Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(schema)
                .operations(create, read, update, delete, patch, action1, action2, query1, query2)
                .build();

        assertTestBuilder(resource);
    }

    private void assertTestBuilder(final Resource resource) {
        assertThat(resource.getDescription()).isEqualTo(description);
        assertThat(resource.getResourceSchema()).isEqualTo(schema);
        assertThat(resource.getCreate()).isEqualTo(create);
        assertThat(resource.getRead()).isEqualTo(read);
        assertThat(resource.getUpdate()).isEqualTo(update);
        assertThat(resource.getDelete()).isEqualTo(delete);
        assertThat(resource.getPatch()).isEqualTo(patch);
        assertThat(resource.getActions()).contains(action1, action2);
        assertThat(resource.getQueries()).contains(query1, query2);
    }

    @Test(expectedExceptions = ApiValidationException.class)
    public void testEmptyResource() {
        Resource.resource().build();
    }

    @Test
    public void testSimpleAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(SimpleAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).hasSize(1);
        Action action = resource.getActions()[0];
        assertThat(action.getName()).isEqualTo("myAction");
        assertThat(action.getErrors()).isEmpty();
        assertThat(action.getParameters()).isEmpty();
    }

    @RequestHandler
    private static final class SimpleAnnotatedHandler {
        @com.forgerock.api.annotations.Action(
                operationDescription = @com.forgerock.api.annotations.Operation
        )
        public void myAction() {

        }
    }

    @Test
    public void testCreateAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(CreateAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getCreate()).isNotNull();
        Create create = resource.getCreate();
        assertThat(create.isMvccSupported()).isTrue();
        assertThat(create.getDescription()).isEqualTo("A create resource operation.");
        assertThat(create.getErrors()).hasSize(2);
        assertThat(create.getParameters()).hasSize(1);
        assertThat(create.getSupportedLocales()).hasSize(2);
        assertThat(create.getSupportedContexts()).hasSize(1);
        assertThat(create.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(create.getMode()).isEqualTo(CreateMode.ID_FROM_SERVER);
    }

    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class CreateAnnotatedHandler {
        @com.forgerock.api.annotations.Create(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A create resource operation.",
                        errors = {
                                @Error(code = 403, description = "You're forbidden from creating these resources"),
                                @Error(code = 400, description = "You can't create these resources using too much jam")
                        },
                        parameters = {
                                @Parameter(name = "id", type = "string", description = "Identifier for the created")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true
        )
        public void create() {

        }
    }

    private static final class Response {
        public String id;
        public Integer field;
    }
}
