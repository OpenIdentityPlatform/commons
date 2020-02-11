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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.api.models.Resource.AnnotatedTypeVariant.COLLECTION_RESOURCE_COLLECTION;
import static org.forgerock.api.models.Resource.AnnotatedTypeVariant.SINGLETON_RESOURCE;
import static org.forgerock.api.models.Resource.fromAnnotatedType;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.io.IOException;
import java.io.InputStream;

import org.forgerock.api.annotations.Actions;
import org.forgerock.api.annotations.CollectionProvider;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Queries;
import org.forgerock.api.annotations.SingletonProvider;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.jackson.JacksonUtils;
import org.forgerock.http.util.Json;
import org.forgerock.util.i18n.LocalizableString;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourceTest {

    public static final String I18NJSONSCHEMA_JSON = "i18njsonschema.json";
    public static final String TRANSLATED_JSON_SCHEMA_DESC_TITLE =
            ".*(Json schema description).*(Json schema title).*";
    private final LocalizableString title = new LocalizableString("My Title");
    private final LocalizableString description = new LocalizableString("My Description");
    public static final String TRANSLATED_DISCRIPTION_TWICE_REGEX =
            ".*(If you see this it has been translated).*(If you see this it has been translated).*";
    private final LocalizableString i18nDescription = new LocalizableString("i18n:api-dictionary#description_test",
            ResourceTest.class.getClassLoader());
    private Schema schema;
    private Schema i18nSchema;
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
    public void beforeClass() throws IOException {
        schema = Schema.schema()
                .schema(json(object()))
                .build();
        i18nSchema = getI18nSchema();
        create = Create.create()
                .mode(CreateMode.ID_FROM_SERVER)
                .build();
        read = Read.read()
                .build();
        update = Update.update()
                .build();
        delete = Delete.delete()
                .build();
        patch = Patch.patch()
                .operations(PatchOperation.ADD, PatchOperation.COPY)
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
                .title(title)
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
                .mvccSupported(true)
                .build();

        assertTestBuilder(resource);
    }

    /**
     * Test the {@Resource} builder with builder-methods that take arrays as arguments.
     */
    @Test
    public void testBuilderWithArrayMethods() {
        final Resource resource = Resource.resource()
                .title(title)
                .description(description)
                .resourceSchema(schema)
                .create(create)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .actions(asList(action1, action2))
                .queries(asList(query1, query2))
                .mvccSupported(true)
                .build();

        assertTestBuilder(resource);
    }

    @Test
    public void testBuilderWithOperationsArray() {
        final Resource resource = Resource.resource()
                .title(title)
                .description(description)
                .resourceSchema(schema)
                .operations(create, read, update, delete, patch, action1, action2, query1, query2)
                .mvccSupported(true)
                .build();

        assertTestBuilder(resource);
    }

    private void assertTestBuilder(final Resource resource) {
        assertThat(resource.getTitle()).isEqualTo(title);
        assertThat(resource.getDescription()).isEqualTo(description);
        assertThat(resource.getResourceSchema()).isEqualTo(schema);
        assertThat(resource.getCreate()).isEqualTo(create);
        assertThat(resource.getRead()).isEqualTo(read);
        assertThat(resource.getUpdate()).isEqualTo(update);
        assertThat(resource.getDelete()).isEqualTo(delete);
        assertThat(resource.getPatch()).isEqualTo(patch);
        assertThat(resource.getActions()).contains(action1, action2);
        assertThat(resource.getQueries()).contains(query1, query2);
        assertThat(resource.isMvccSupported()).isTrue();
    }

    @Test
    public void testEmptyResource() {
        assertThat(Resource.resource().build()).isNull();
    }

    @Test
    public void testSimpleAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(SimpleAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).hasSize(1);
        Action action = resource.getActions()[0];
        assertThat(action.getName()).isEqualTo("myAction");
        assertThat(action.getApiErrors()).isNull();
        assertThat(action.getParameters()).isNull();
        assertThat(descriptor.getErrors()).isNull();
        assertThat(descriptor.getDefinitions()).isNull();
        assertThat(resource.isMvccSupported()).isTrue();
    }

    @SingletonProvider(@Handler(mvccSupported = true))
    private static final class SimpleAnnotatedHandler {
        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation)
        public void myAction() {

        }
    }

    @Test
    public void testReferencedSchema() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(ReferencedSchemaHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getRead()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getResourceSchema().getReference().getValue()).isEqualTo("#/definitions/frapi:response");
        assertThat(descriptor.getDefinitions().getDefinitions()).hasSize(1).containsKeys("frapi:response");
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = IdentifiedResponse.class),
            mvccSupported = true))
    private static final class ReferencedSchemaHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation."
                ))
        public void read() {

        }
    }

    @Test
    public void testReferencedError() throws Exception {
        ApiDescription descriptor = createApiDescription();
        Resource resource = Resource.fromAnnotatedType(ReferencedErrorHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getRead()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        assertThat(resource.getRead().getApiErrors()).hasSize(1);
        assertThat(resource.getRead().getApiErrors()[0].getReference().getValue()).isEqualTo("#/errors/frapi:myerror");
        assertThat(descriptor.getErrors().getErrors()).hasSize(1).containsKeys("frapi:myerror");
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = IdentifiedResponse.class),
            mvccSupported = true))
    private static final class ReferencedErrorHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation.",
                        errors = @org.forgerock.api.annotations.ApiError(id = "frapi:myerror", code = 500,
                                description = "Our bad.")
                ))
        public void read() {

        }
    }

    @Test
    public void testCreateAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(CreateAnnotatedHandler.class, COLLECTION_RESOURCE_COLLECTION,
                descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getCreate()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        Create create = resource.getCreate();
        assertThat(create.getDescription()).isEqualTo(new LocalizableString("A create resource operation."));
        assertThat(create.getApiErrors()).hasSize(2);
        assertThat(create.getParameters()).hasSize(1);
        assertThat(create.getSupportedLocales()).hasSize(2);
        assertThat(create.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(create.getMode()).isEqualTo(CreateMode.ID_FROM_SERVER);
    }

    @CollectionProvider(details = @Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class CreateAnnotatedHandler {
        @org.forgerock.api.annotations.Create(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A create resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.ApiError
                                        (code = 403, description = "You're forbidden from creating these resources"),
                                @org.forgerock.api.annotations.ApiError
                                        (code = 400
                                                , description = "You can't create these resources using too much jam")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the created")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING))
        public void create() {

        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testResourceSchemaRequiredForCrudpq() throws Exception {
        ApiDescription descriptor = createApiDescription();
        fromAnnotatedType(ResourceSchemaRequiredAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
    }

    @SingletonProvider(@Handler(mvccSupported = true))
    private static final class ResourceSchemaRequiredAnnotatedHandler {
        @org.forgerock.api.annotations.Create(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A create resource operation."))
        public void create() {

        }
    }

    @Test
    public void testReadAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(ReadAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getRead()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        Read read = resource.getRead();
        assertThat(read.getDescription()).isEqualTo(new LocalizableString("A read resource operation."));
        assertThat(read.getApiErrors()).isNull();
        assertThat(read.getParameters()).isNull();
        assertThat(read.getSupportedLocales()).hasSize(0);
        assertThat(read.getStability()).isEqualTo(Stability.STABLE);
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class ReadAnnotatedHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation."
                ))
        public void read() {

        }
    }

    @Test
    public void testUpdateAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(UpdateAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getUpdate()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        Update update = resource.getUpdate();
        assertThat(update.getDescription()).isEqualTo(new LocalizableString("An update resource operation."));
        assertThat(update.getApiErrors()).isNull();
        assertThat(update.getParameters()).isNull();
        assertThat(update.getSupportedLocales()).hasSize(0);
        assertThat(update.getStability()).isEqualTo(Stability.STABLE);
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class UpdateAnnotatedHandler {
        @org.forgerock.api.annotations.Update(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "An update resource operation."))
        public void update() {

        }
    }

    @Test
    public void testDeleteAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(DeleteAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getDelete()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        Delete delete = resource.getDelete();
        assertThat(delete.getDescription()).isEqualTo(new LocalizableString("A delete resource operation."));
        assertThat(delete.getApiErrors()).isNull();
        assertThat(delete.getParameters()).isNull();
        assertThat(delete.getSupportedLocales()).hasSize(0);
        assertThat(delete.getStability()).isEqualTo(Stability.STABLE);
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class DeleteAnnotatedHandler {
        @org.forgerock.api.annotations.Delete(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A delete resource operation."))
        public void delete() {

        }
    }

    @Test
    public void testPatchAnnotatedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(PatchAnnotatedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNotNull();
        assertThat(resource.getPatch()).isNotNull();
        assertThat(resource.isMvccSupported()).isTrue();
        Patch patch = resource.getPatch();
        assertThat(patch.getDescription()).isEqualTo(new LocalizableString("A patch resource operation."));
        assertThat(patch.getApiErrors()).isNull();
        assertThat(patch.getParameters()).isNull();
        assertThat(patch.getSupportedLocales()).hasSize(0);
        assertThat(patch.getStability()).isEqualTo(Stability.STABLE);
        assertThat(patch.getOperations()).hasSize(2);
        assertThat(patch.getOperations()).contains(PatchOperation.INCREMENT, PatchOperation.TRANSFORM);
    }

    @SingletonProvider(@Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class PatchAnnotatedHandler {
        @org.forgerock.api.annotations.Patch(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A patch resource operation."
                ),
                operations = {PatchOperation.INCREMENT, PatchOperation.TRANSFORM})
        public void patch() {

        }
    }

    @DataProvider
    public Object[][] actionAnnotations() {
        return new Object[][]{{ActionAnnotatedHandler.class}, {ActionsAnnotatedHandler.class}};
    }

    @Test(dataProvider = "actionAnnotations")
    public void testActionAnnotatedHandler(Class<?> type) throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(type, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getResourceSchema()).isNull();
        assertThat(resource.getActions()).isNotNull();
        assertThat(resource.getActions()).hasSize(2);
        Action action1 = resource.getActions()[0];
        assertThat(action1.getDescription()).isEqualTo(new LocalizableString("An action resource operation."));
        assertThat(action1.getApiErrors()).hasSize(2);
        assertThat(action1.getParameters()).hasSize(1);
        assertThat(action1.getSupportedLocales()).hasSize(2);
        assertThat(action1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action1.getName()).isEqualTo("action1");
        assertThat(action1.getRequest()).isNotNull();
        assertThat(action1.getResponse()).isNotNull();

        Action action2 = resource.getActions()[1];
        assertThat(action2.getDescription()).isEqualTo(new LocalizableString("An action resource operation."));
        assertThat(action2.getApiErrors()).hasSize(2);
        assertThat(action2.getParameters()).hasSize(1);
        assertThat(action2.getSupportedLocales()).hasSize(2);
        assertThat(action2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(action2.getName()).isEqualTo("action2");
        assertThat(action2.getRequest()).isNotNull();
        assertThat(action2.getResponse()).isNotNull();
    }

    ApiDescription createApiDescription() {
        return ApiDescription.apiDescription().id("frapi:test").version("1.0").build();
    }

    @SingletonProvider(@Handler(mvccSupported = true))
    private static final class ActionAnnotatedHandler {
        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "An action resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.ApiError
                                        (code = 403, description = "Action forbidden"),
                                @org.forgerock.api.annotations.ApiError
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action1",
                request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @org.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action1() {

        }

        @org.forgerock.api.annotations.Action(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "An action resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.ApiError
                                        (code = 403, description = "Action forbidden"),
                                @org.forgerock.api.annotations.ApiError
                                        (code = 400, description = "Malformed action request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the action")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                name = "action2",
                request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                response = @org.forgerock.api.annotations.Schema(fromType = Response.class))
        public void action2() {

        }
    }

    @SingletonProvider(@Handler(mvccSupported = true))
    private static final class ActionsAnnotatedHandler {
        @Actions({
                @org.forgerock.api.annotations.Action(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                description = "An action resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 403, description = "Action forbidden"),
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 400, description = "Malformed action request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the action")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        name = "action1",
                        request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                        response = @org.forgerock.api.annotations.Schema(fromType = Response.class)),
                @org.forgerock.api.annotations.Action(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                description = "An action resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 403, description = "Action forbidden"),
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 400, description = "Malformed action request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the action")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        name = "action2",
                        request = @org.forgerock.api.annotations.Schema(fromType = Request.class),
                        response = @org.forgerock.api.annotations.Schema(fromType = Response.class))})
        public void actions() {

        }
    }

    @DataProvider
    public Object[][] queryAnnotations() {
        return new Object[][]{{QueryAnnotatedHandler.class}, {QueriesAnnotatedHandler.class}};
    }

    @Test(dataProvider = "queryAnnotations")
    public void testQueryAnnotatedHandler(Class<?> type) throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(type, COLLECTION_RESOURCE_COLLECTION, descriptor);
        assertThat(resource.getQueries()).isNotNull();
        assertThat(resource.getQueries()).hasSize(2);
        Query query1 = resource.getQueries()[0];
        assertThat(query1.getDescription()).isEqualTo(new LocalizableString("A query resource operation."));
        assertThat(query1.getApiErrors()).hasSize(2);
        assertThat(query1.getParameters()).hasSize(1);
        assertThat(query1.getSupportedLocales()).hasSize(2);
        assertThat(query1.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query1.getType()).isEqualTo(QueryType.ID);
        assertThat(query1.getQueryId()).isEqualTo("query1");
        assertThat(query1.getCountPolicies()[0]).isEqualTo(CountPolicy.ESTIMATE);
        assertThat(query1.getPagingModes()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query1.getPagingModes()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query1.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query1.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query1.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query1.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query1.getSupportedSortKeys()[2]).isEqualTo("key3");

        Query query2 = resource.getQueries()[1];
        assertThat(query2.getDescription()).isEqualTo(new LocalizableString("A query resource operation."));
        assertThat(query2.getApiErrors()).hasSize(2);
        assertThat(query2.getParameters()).hasSize(1);
        assertThat(query2.getSupportedLocales()).hasSize(2);
        assertThat(query2.getStability()).isEqualTo(Stability.EVOLVING);
        assertThat(query2.getType()).isEqualTo(QueryType.ID);
        assertThat(query2.getQueryId()).isEqualTo("query2");
        assertThat(query2.getCountPolicies()[0]).isEqualTo(CountPolicy.NONE);
        assertThat(query2.getPagingModes()[0]).isEqualTo(PagingMode.COOKIE);
        assertThat(query2.getPagingModes()[1]).isEqualTo(PagingMode.OFFSET);
        assertThat(query2.getQueryableFields()[0]).isEqualTo("field1");
        assertThat(query2.getQueryableFields()[1]).isEqualTo("field2");
        assertThat(query2.getSupportedSortKeys()[0]).isEqualTo("key1");
        assertThat(query2.getSupportedSortKeys()[1]).isEqualTo("key2");
        assertThat(query2.getSupportedSortKeys()[2]).isEqualTo("key3");
    }

    @CollectionProvider(details = @Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class QueryAnnotatedHandler {
        @org.forgerock.api.annotations.Query(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A query resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.ApiError
                                        (code = 403, description = "Query forbidden"),
                                @org.forgerock.api.annotations.ApiError
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
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
        @org.forgerock.api.annotations.Query(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A query resource operation.",
                        errors = {
                                @org.forgerock.api.annotations.ApiError
                                        (code = 403, description = "Query forbidden"),
                                @org.forgerock.api.annotations.ApiError
                                        (code = 400, description = "Malformed query request")
                        },
                        parameters = {
                                @org.forgerock.api.annotations.Parameter
                                        (name = "id", type = "string", description = "Identifier for the queried")
                        },
                        locales = {"en-GB", "en-US"},
                        stability = Stability.EVOLVING
                ),
                type = QueryType.ID,
                countPolicies = {CountPolicy.NONE},
                pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                queryableFields = {"field1", "field2"},
                sortKeys = {"key1", "key2", "key3"})
        public void query2() {

        }
    }

    @CollectionProvider(details = @Handler(
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class QueriesAnnotatedHandler {
        @Queries({
                @org.forgerock.api.annotations.Query(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                description = "A query resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 403, description = "Query forbidden"),
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 400, description = "Malformed query request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the queried")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        type = QueryType.ID,
                        id = "query1",
                        countPolicies = {CountPolicy.ESTIMATE},
                        pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                        queryableFields = {"field1", "field2"},
                        sortKeys = {"key1", "key2", "key3"}),
                @org.forgerock.api.annotations.Query(
                        operationDescription = @org.forgerock.api.annotations.Operation(
                                description = "A query resource operation.",
                                errors = {
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 403, description = "Query forbidden"),
                                        @org.forgerock.api.annotations.ApiError
                                                (code = 400, description = "Malformed query request")
                                },
                                parameters = {
                                        @org.forgerock.api.annotations.Parameter
                                                (name = "id", type = "string",
                                                        description = "Identifier for the queried")
                                },
                                locales = {"en-GB", "en-US"},
                                stability = Stability.EVOLVING
                        ),
                        type = QueryType.ID,
                        id = "query2",
                        countPolicies = {CountPolicy.NONE},
                        pagingModes = {PagingMode.COOKIE, PagingMode.OFFSET},
                        queryableFields = {"field1", "field2"},
                        sortKeys = {"key1", "key2", "key3"})})
        public void queryies() {

        }
    }

    @Test
    public void testResourceCustomSerializer() throws JsonProcessingException {

        final Read readLocal = Read.read()
                .description(i18nDescription)
                .error(ApiError.apiError().code(12).description(i18nDescription).build())
                .build();


        Resource resource = Resource.resource()
                .description(description)
                .resourceSchema(i18nSchema)
                .operations(create, readLocal, update, delete, patch, action1, action2, query1, query2)
                .mvccSupported(false)
                .build();

        ObjectMapper mapper = new ObjectMapper().registerModule(new Json.LocalizableStringModule());

        String serialized = mapper.writeValueAsString(resource);

        assertThat(serialized.matches(TRANSLATED_DISCRIPTION_TWICE_REGEX));
        assertThat(serialized.matches(TRANSLATED_JSON_SCHEMA_DESC_TITLE));

    }

    @Test
    public void testReferencedHandler() throws Exception {
        ApiDescription descriptor = createApiDescription();
        final Resource resource = fromAnnotatedType(ReferencedHandler.class, SINGLETON_RESOURCE, descriptor);
        assertThat(resource.getReference()).isNotNull();
        assertThat(resource.getReference().getValue()).isEqualTo("#/services/referenced");
        final Resource referenced = descriptor.getServices().get("referenced");
        assertThat(referenced).isNotNull();
        assertThat(referenced.getResourceSchema()).isNotNull();
        assertThat(referenced.getRead()).isNotNull();
    }

    @SingletonProvider(@Handler(id = "referenced",
            resourceSchema = @org.forgerock.api.annotations.Schema(fromType = Response.class),
            mvccSupported = true))
    private static final class ReferencedHandler {
        @org.forgerock.api.annotations.Read(
                operationDescription = @org.forgerock.api.annotations.Operation(
                        description = "A read resource operation."
                ))
        public void read() {

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

    @org.forgerock.api.annotations.Schema(id = "frapi:response")
    private static final class IdentifiedResponse {
        public String id;
        public Integer field;
    }

    private Schema getI18nSchema() throws IOException {
        InputStream is = this.getClass().getResourceAsStream(I18NJSONSCHEMA_JSON);
        return Schema.schema().schema(json(JacksonUtils.OBJECT_MAPPER.readValue(is, Object.class))).build();
    }
}
