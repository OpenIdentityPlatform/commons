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

import com.forgerock.api.annotations.RequestHandler;

import com.forgerock.api.ApiValidationException;
import com.forgerock.api.enums.CountPolicy;
import com.forgerock.api.enums.CreateMode;
import com.forgerock.api.enums.PagingMode;
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
                operationDescription = @com.forgerock.api.annotations.Operation)
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
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "You're forbidden from creating these resources"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400
                                                , description = "You can't create these resources using too much jam")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the created")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true)
        public void create() {

        }
    }

    @Test
    public void testReadAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(ReadAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getRead()).isNotNull();
        Read read = resource.getRead();
        assertThat(read.getDescription()).isEqualTo("A read resource operation.");
        assertThat(read.getErrors()).hasSize(2);
        assertThat(read.getParameters()).hasSize(1);
        assertThat(read.getSupportedLocales()).hasSize(2);
        assertThat(read.getSupportedContexts()).hasSize(1);
        assertThat(read.getStability()).isEqualTo(Stability.STABLE);
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class ReadAnnotatedHandler {
        @com.forgerock.api.annotations.Read(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A read resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Read forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed read request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the read")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.STABLE
                ))
        public void read() {

        }
    }

    @Test
    public void testUpdateAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(UpdateAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getUpdate()).isNotNull();
        Update update = resource.getUpdate();
        assertThat(update.isMvccSupported()).isTrue();
        assertThat(update.getDescription()).isEqualTo("An update resource operation.");
        assertThat(update.getErrors()).hasSize(2);
        assertThat(update.getParameters()).hasSize(3);
        assertThat(update.getSupportedLocales()).hasSize(2);
        assertThat(update.getSupportedContexts()).hasSize(1);
        assertThat(update.getStability()).isEqualTo(Stability.EVOLVING);
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class UpdateAnnotatedHandler {
        @com.forgerock.api.annotations.Update(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "An update resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Update forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed update request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the updated"),
                                @com.forgerock.api.annotations.Parameter
                                        (name = "name", type = "string", description = "Name for the updated"),
                                @com.forgerock.api.annotations.Parameter
                                        (name = "date", type = "date", description = "Date for the updated")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true)
        public void update() {

        }
    }

    @Test
    public void testDeleteAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(DeleteAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getDelete()).isNotNull();
        Delete delete = resource.getDelete();
        assertThat(delete.isMvccSupported()).isTrue();
        assertThat(delete.getDescription()).isEqualTo("A delete resource operation.");
        assertThat(delete.getErrors()).hasSize(2);
        assertThat(delete.getParameters()).hasSize(1);
        assertThat(delete.getSupportedLocales()).hasSize(2);
        assertThat(delete.getSupportedContexts()).hasSize(1);
        assertThat(delete.getStability()).isEqualTo(Stability.EVOLVING);
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class DeleteAnnotatedHandler {
        @com.forgerock.api.annotations.Delete(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A delete resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Delete forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed delete request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the deleted")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true)
        public void delete() {

        }
    }


    @Test
    public void testPatchAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(PatchAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getPatch()).isNotNull();
        Patch patch = resource.getPatch();
        assertThat(patch.isMvccSupported()).isTrue();
        assertThat(patch.getDescription()).isEqualTo("A patch resource operation.");
        assertThat(patch.getErrors()).hasSize(2);
        assertThat(patch.getParameters()).hasSize(1);
        assertThat(patch.getSupportedLocales()).hasSize(2);
        assertThat(patch.getSupportedContexts()).hasSize(1);
        assertThat(patch.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(patch.getOperations()).hasSize(2);
        assertThat(patch.getOperations()[0]).isEqualTo(PatchOperations.INCREMENT);
        assertThat(patch.getOperations()[1]).isEqualTo(PatchOperations.TRANSFORM);
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class PatchAnnotatedHandler {
        @com.forgerock.api.annotations.Patch(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A patch resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Patch forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed patch request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the patched")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                mvccSupported = true,
                operations = {PatchOperations.INCREMENT, PatchOperations.TRANSFORM})
        public void patch() {

        }
    }


    @Test
    public void testActionAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(ActionAnnotatedHandler.class, false);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).isNotNull();
        assertThat(resource.getActions()).hasSize(2);
        Action action1 = resource.getActions()[0];
        assertThat(action1.getDescription()).isEqualTo("An action resource operation.");
        assertThat(action1.getErrors()).hasSize(2);
        assertThat(action1.getParameters()).hasSize(1);
        assertThat(action1.getSupportedLocales()).hasSize(2);
        assertThat(action1.getSupportedContexts()).hasSize(1);
        assertThat(action1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action1.getName()).isEqualTo("action1");
        assertThat(action1.getRequest()).isNotNull();
        assertThat(action1.getResponse()).isNotNull();

        Action action2 = resource.getActions()[1];
        assertThat(action2.getDescription()).isEqualTo("An action resource operation.");
        assertThat(action2.getErrors()).hasSize(2);
        assertThat(action2.getParameters()).hasSize(1);
        assertThat(action2.getSupportedLocales()).hasSize(2);
        assertThat(action2.getSupportedContexts()).hasSize(1);
        assertThat(action2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action2.getName()).isEqualTo("action2");
        assertThat(action2.getRequest()).isNotNull();
        assertThat(action2.getResponse()).isNotNull();
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class ActionAnnotatedHandler {
        @com.forgerock.api.annotations.Action(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "An action resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Action forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action1",
                request = @com.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @com.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action1() {

        }

        @com.forgerock.api.annotations.Action(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "An action resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Action forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action2",
                request = @com.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @com.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action2() {

        }
    }


    @Test
    public void testQueryAnnotatedHandler() throws Exception {
        final Resource resource = Resource.fromAnnotatedType(QueryAnnotatedHandler.class, false);
        assertThat(resource.getQueries()).isNotNull();
        assertThat(resource.getQueries()).hasSize(2);
        Query query1 = resource.getQueries()[0];
        assertThat(query1.getDescription()).isEqualTo("A query resource operation.");
        assertThat(query1.getErrors()).hasSize(2);
        assertThat(query1.getParameters()).hasSize(1);
        assertThat(query1.getSupportedLocales()).hasSize(2);
        assertThat(query1.getSupportedContexts()).hasSize(1);
        assertThat(query1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query1.getType()).isEqualTo(QueryType.ID);
        assertThat(query1.getQueryId()).isEqualTo("query1");
        assertThat(query1.getCountPolicy()[0]).isEqualTo(CountPolicy.ESTIMATE);
        assertThat(query1.getPagingMode()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query1.getPagingMode()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query1.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query1.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query1.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query1.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query1.getSupportedSortKeys()[2]).isEqualTo("key3");

        Query query2 = resource.getQueries()[1];
        assertThat(query2.getDescription()).isEqualTo("A query resource operation.");
        assertThat(query2.getErrors()).hasSize(2);
        assertThat(query2.getParameters()).hasSize(1);
        assertThat(query2.getSupportedLocales()).hasSize(2);
        assertThat(query2.getSupportedContexts()).hasSize(1);
        assertThat(query2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query2.getType()).isEqualTo(QueryType.ID);
        assertThat(query2.getQueryId()).isEqualTo("query2");
        assertThat(query2.getCountPolicy()[0]).isEqualTo(CountPolicy.ESTIMATE);
        assertThat(query2.getPagingMode()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query2.getPagingMode()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query2.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query2.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query2.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query2.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query2.getSupportedSortKeys()[2]).isEqualTo("key3");
    }


    @RequestHandler(resourceSchema = @com.forgerock.api.annotations.Schema(fromType = Response.class))
    private static final class QueryAnnotatedHandler {
        @com.forgerock.api.annotations.Query(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A query resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Query forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the queried")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                type = QueryType.ID,
                id = "query1",
                countPolicies = {CountPolicy.ESTIMATE},
                pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                queryableFields = {"field1", "field2"},
                sortKeys = {"key1", "key2", "key3"})
        public void query1() {

        }
        @com.forgerock.api.annotations.Query(
                operationDescription = @com.forgerock.api.annotations.Operation(
                        contexts = SecurityContext.class,
                        description = "A query resource operation.",
                        errors = {
                                @com.forgerock.api.annotations.Error
                                        (code = 403, description = "Query forbidden"),
                                @com.forgerock.api.annotations.Error
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @com.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the queried")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                type = QueryType.ID,
                id = "query2",
                countPolicies = {CountPolicy.ESTIMATE},
                pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                queryableFields = {"field1", "field2"},
                sortKeys = {"key1", "key2", "key3"})
        public void query2() {

        }
    }


    private static final class Request {
        public String id;
        public Integer field;
    }

    private static final class Response {
        public String id;
        public Integer field;
    }

}
