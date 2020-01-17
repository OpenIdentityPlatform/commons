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

package org.forgerock.api;

import static java.util.Arrays.asList;
import static org.forgerock.api.models.ApiDescription.apiDescription;
import static org.forgerock.api.models.Create.create;
import static org.forgerock.api.models.Definitions.definitions;
import static org.forgerock.api.models.Items.items;
import static org.forgerock.api.models.Paths.paths;
import static org.forgerock.api.models.Query.query;
import static org.forgerock.api.models.Reference.reference;
import static org.forgerock.api.models.Resource.resource;
import static org.forgerock.api.models.Schema.schema;
import static org.forgerock.api.models.Services.services;
import static org.forgerock.api.models.VersionedPath.versionedPath;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValue.field;

import java.util.Arrays;
import java.util.List;

import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.enums.Stability;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Create;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Errors;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Query;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Services;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.models.VersionedPath;

/**
 * Common utilities for API Description tests.
 */
public final class ApiTestUtil {

    private ApiTestUtil() {
        // empty
    }

    /**
     * Builds a {@link ApiDescription} that incorporates all features/attributes.
     *
     * @param versioned {@code true} to use {@link VersionedPath} and {@code false} otherwise
     * @return {@link ApiDescription} instance
     */
    public static ApiDescription createApiDescription(final boolean versioned) {
        final Schema schema = schema()
                .schema(json(object(field("type", "object"))))
                .build();

        final Schema schemaFromRef = schema()
                .reference(
                        reference().value("frapi:test#/definitions/mySchema").build())
                .build();

        // propertyOrder fields allow for testing explicit property-order
        final Schema errorDetailSchema = schema()
                .schema(json(object(
                        field("type", "object"),
                        field("properties", object(
                                field("subCode", object(
                                        field("type", "integer")
                                )),
                                field("reason", object(
                                        field("type", "string"),
                                        field("propertyOrder", 100)
                                )),
                                field("message", object(
                                        field("type", "string"),
                                        field("propertyOrder", 10)
                                ))
                        )))))
                .build();

        final String[] supportedLocales = new String[]{"en"};
        final ApiError notFoundApiError = ApiError.apiError()
                .code(404)
                .description("Custom not-found apiError.")
                .schema(errorDetailSchema)
                .build();
        final ApiError anotherNotFoundApiError = ApiError.apiError()
                .code(404)
                .description("Another custom not-found apiError, for testing apiError-code overloading.")
                .schema(errorDetailSchema)
                .build();
        final Parameter parameter1 = Parameter.parameter()
                .name("param1")
                .description("Description for param 1")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .defaultValue("default")
                .required(true)
                .build();
        final Parameter parameter2 = Parameter.parameter()
                .name("param2")
                .description("Description for param 2")
                .type("string")
                .source(ParameterSource.ADDITIONAL)
                .enumValues("enum1", "enum2")
                .enumTitles("first enum", "second enum")
                .build();

        final Create createIdFromServer = create()
                .description("Default description for create (ID from server).")
                .mode(CreateMode.ID_FROM_SERVER)
                .build();

        final Create createIdFromClient = create()
                .description("Default description for create (ID from client).")
                .mode(CreateMode.ID_FROM_CLIENT)
                .singleton(true)
                .build();

        final Read read = Read.read()
                .description("Default description for read.")
                .supportedLocales(supportedLocales)
                .stability(Stability.STABLE)
                .error(notFoundApiError)
                .error(anotherNotFoundApiError)
                .parameter(parameter1)
                .parameter(parameter2)
                .build();

        final Update update = Update.update()
                .description("Default description for update.")
                .build();

        final Delete delete = Delete.delete()
                .description("Default description for delete.")
                .build();

        final Patch patch = Patch.patch()
                .description("Default description for patch.")
                .operations(PatchOperation.ADD, PatchOperation.COPY)
                .build();

        final Query expressionQuery = query()
                .type(QueryType.EXPRESSION)
                .description("Default description for expression query.")
                .pagingModes(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicies(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .supportedSortKeys("key1", "key2")
                .build();

        final Query filterQuery = query()
                .type(QueryType.FILTER)
                .description("Default description for filter query.")
                .pagingModes(PagingMode.COOKIE, PagingMode.OFFSET)
                .countPolicies(CountPolicy.EXACT, CountPolicy.ESTIMATE)
                .queryableFields("field1", "field2")
                .supportedSortKeys("key1", "key2")
                .build();

        final Query id1Query = query()
                .type(QueryType.ID)
                .queryId("id1")
                .description("Default description for id1 query.")
                .build();

        final Query id2Query = query()
                .type(QueryType.ID)
                .queryId("id2")
                .description("Default description for id2 query.")
                .build();

        final Action action1 = Action.action()
                .name("action1")
                .description("Default description for action1.")
                .request(schema)
                .response(schemaFromRef)
                .build();
        final Action action2 = Action.action()
                .name("action2")
                .description("Default description for action2.")
                .response(schema)
                .build();

        final Resource resourceV1 = resource()
                .title(new LocalizableString("Resource title"))
                .description(new LocalizableString("Default description for resourceV1."))
                .resourceSchema(schema)
                .create(createIdFromServer)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .queries(asList(expressionQuery, filterQuery, id1Query, id2Query))
                .mvccSupported(true)
                .build();
        final Resource resourceV2 = resource()
                .title(new LocalizableString("Resource title"))
                .description(new LocalizableString("Default description for resourceV2."))
                .resourceSchema(schema)
                .create(createIdFromClient)
                .read(read)
                .update(update)
                .delete(delete)
                .patch(patch)
                .action(action1)
                .action(action2)
                .queries(asList(expressionQuery, filterQuery, id1Query, id2Query))
                .mvccSupported(true)
                .build();

        final Definitions definitions = definitions()
                .put("mySchema", schema)
                .build();

        final Errors errors = Errors.errors()
                .put("notFound", ApiError.apiError().code(404).description("Not Found").build())
                .put("internalServerError", ApiError.apiError().code(500)
                        .description("Internal Server ApiError").build())
                .build();

        final VersionedPath versionedPath;
        if (versioned) {
            versionedPath = versionedPath()
                    .put(version(1), resourceV1)
                    .put(version(2), resourceV2)
                    .build();
        } else {
            versionedPath = versionedPath()
                    .put(VersionedPath.UNVERSIONED, resourceV1)
                    .build();
        }

        final Paths paths = paths()
                .put("/testPath", versionedPath)
                .build();
        return apiDescription()
                .id("frapi:test")
                .version("1.0")
                .description(new LocalizableString("Default API description."))
                .definitions(definitions)
                .paths(paths)
                .errors(errors)
                .build();

    }

    public static ApiDescription createUserAndDeviceExampleApiDescription() {
        final Errors errors = Errors.errors()
                .put("badRequest", ApiError.apiError()
                        .code(400)
                        .description("Bad request")
                        .build())
                .put("unauthorized", ApiError.apiError()
                        .code(401)
                        .description("Unauthorized - Missing or bad authentication")
                        .build())
                .build();
        final List<ApiError> errorList = Arrays.asList(
                ApiError.apiError().reference(Reference.reference().value("#/errors/badRequest").build()).build(),
                ApiError.apiError().reference(Reference.reference().value("#/errors/unauthorized").build()).build());

        final Schema userSchema = schema()
                .schema(json(object(
                        field("type", "object"),
                        field("title", "User"),
                        field("description", "User description"),
                        field("required", array("uid", "name", "password")),
                        field("properties", object(
                                field("_id", object(
                                        field("type", "string"),
                                        field("title", "Unique identifier"),
                                        field("writePolicy", "WRITE_ON_CREATE"),
                                        field("errorOnWritePolicyFailure", true)
                                )),
                                field("_rev", object(
                                        field("type", "string"),
                                        field("title", "Revision identifier"),
                                        field("readOnly", true)
                                )),
                                field("uid", object(
                                        field("type", "string"),
                                        field("title", "User ID"),
                                        field("description", "User unique identifier")
                                )),
                                field("name", object(
                                        field("type", "string"),
                                        field("title", "User name"),
                                        field("description", "Name for this user")
                                )),
                                field("devices", object(
                                        field("type", "array"),
                                        field("title", "Devices"),
                                        field("description", "Devices belonging to this user"),
                                        field("items", object(
                                                field("$ref", "#/definitions/device"))),
                                        field("readOnly", false),
                                        field("uniqueItems", true)
                                ))
                        ))
                )))
                .build();

        final Schema deviceSchema = schema()
                .schema(json(object(
                        field("type", "object"),
                        field("title", "Device"),
                        field("description", "Device description"),
                        field("required", array("did", "name", "type")),
                        field("properties", object(
                                field("_id", object(
                                        field("type", "string"),
                                        field("title", "Unique identifier"),
                                        field("writePolicy", "WRITE_ON_CREATE"),
                                        field("errorOnWritePolicyFailure", true)
                                )),
                                field("_rev", object(
                                        field("type", "string"),
                                        field("title", "Revision identifier"),
                                        field("readOnly", true)
                                )),
                                field("did", object(
                                        field("type", "string"),
                                        field("title", "Device ID")
                                )),
                                field("name", object(
                                        field("type", "string"),
                                        field("title", "Device name")
                                )),
                                field("type", object(
                                        field("type", "string"),
                                        field("title", "Device type")
                                )),
                                field("stolen", object(
                                        field("type", "boolean"),
                                        field("title", "Stolen flag"),
                                        field("description", "Set to `true` if the device has been stolen")
                                )),
                                field("rollOutDate", object(
                                        field("type", "string"),
                                        field("format", "date"),
                                        field("title", "Roll-out date"),
                                        field("description", "Device roll-out date")
                                ))
                        ))
                )))
                .build();

        final Definitions definitions = definitions()
                .put("user", userSchema)
                .put("device", deviceSchema)
                .build();

        final Resource userResource1 = resource()
                .title(new LocalizableString("User Service"))
                .description(new LocalizableString("User management service"))
                .mvccSupported(true)
                .resourceSchema(schema()
                        .reference(reference().value("#/definitions/user").build())
                        .build())
                .create(create().mode(CreateMode.ID_FROM_SERVER)
                        .parameter(Parameter.parameter()
                                .name("_action")
                                .type("string")
                                .required(true)
                                .source(ParameterSource.ADDITIONAL)
                                .enumValues("create")
                                .build())
                        .errors(errorList)
                        .build())
                .query(query()
                        .type(QueryType.FILTER)
                        .description("Search for users, matching a filter.")
                        .queryableFields("uid", "name", "password")
                        .pagingModes(PagingMode.COOKIE, PagingMode.OFFSET)
                        .countPolicies(CountPolicy.NONE)
                        .errors(errorList)
                        .build())
                .items(items()
                        .pathParameter(Parameter.parameter()
                                .name("userId")
                                .type("string")
                                .source(ParameterSource.PATH)
                                .required(true)
                                .description("User ID")
                                .build())
                        .create(create()
                                .mode(CreateMode.ID_FROM_CLIENT)
                                .errors(errorList)
                                .build())
                        .read(Read.read()
                                .errors(errorList)
                                .build())
                        .update(Update.update()
                                .errors(errorList)
                                .build())
                        .delete(Delete.delete()
                                .errors(errorList)
                                .build())
                        .patch(Patch.patch()
                                .operations(PatchOperation.ADD)
                                .errors(errorList)
                                .build())
                        .action(Action.action()
                                .name("resetPassword")
                                .error(ApiError.apiError()
                                        .code(501)
                                        .description(
                                                "Action `resetPassword` reached. As it is an example service it has "
                                                        + "not been implemented.")
                                        .build())
                                .errors(errorList)
                                .build())
                        .subresources(SubResources.subresources()
                                .put("/devices", resource()
                                        .reference(reference().value("#/services/devices:1.0").build())
                                        .build())
                                .build())
                        .build())
                .build();

        final Resource deviceResource1 = resource()
                .title(new LocalizableString("User-Device Service"))
                .description(new LocalizableString("User-device management service"))
                .mvccSupported(true)
                .resourceSchema(schema()
                        .reference(reference().value("#/definitions/device").build())
                        .build())
                .create(create().mode(CreateMode.ID_FROM_SERVER)
                        .parameter(Parameter.parameter()
                                .name("_action")
                                .type("string")
                                .required(true)
                                .source(ParameterSource.ADDITIONAL)
                                .enumValues("create")
                                .build())
                        .errors(errorList)
                        .build())
                .query(query()
                        .type(QueryType.FILTER)
                        .description("Search for users, matching a filter.")
                        .queryableFields("did", "name", "type")
                        .pagingModes(PagingMode.COOKIE, PagingMode.OFFSET)
                        .countPolicies(CountPolicy.NONE)
                        .errors(errorList)
                        .build())
                .items(items()
                        .pathParameter(Parameter.parameter()
                                .name("deviceId")
                                .type("string")
                                .source(ParameterSource.PATH)
                                .required(true)
                                .description("Device ID")
                                .build())
                        .create(create()
                                .mode(CreateMode.ID_FROM_CLIENT)
                                .errors(errorList)
                                .build())
                        .read(Read.read()
                                .errors(errorList)
                                .build())
                        .update(Update.update()
                                .errors(errorList)
                                .build())
                        .delete(Delete.delete()
                                .errors(errorList)
                                .build())
                        .patch(Patch.patch()
                                .operations(PatchOperation.ADD)
                                .errors(errorList)
                                .build())
                        .action(Action.action()
                                .name("markAsStolen")
                                .error(ApiError.apiError()
                                        .code(501)
                                        .description(
                                                "Action `markAsStolen` reached. As it is an example service it has "
                                                        + "not been implemented.")
                                        .build())
                                .errors(errorList)
                                .build())
                        .build())
                .build();

        final Services services = services()
                .put("users:1.0", userResource1)
                .put("devices:1.0", deviceResource1)
                .build();

        final Paths paths = paths()
                .put("/users", versionedPath()
                                .put(version(1), resource().reference(
                                        reference().value("#/services/users:1.0").build()
                                        ).build()
                                ).build()
                )
                .put("/admins", versionedPath()
                        .put(version(1), resource().reference(
                                reference().value("#/services/users:1.0").build()
                                ).build()
                        ).build()
                ).build();

        return apiDescription()
                .id("example:sub-resources")
                .version("1.0")
                .description("Users can have devices, but the devices are their own resources.")
                .definitions(definitions)
                .services(services)
                .paths(paths)
                .errors(errors)
                .build();
    }

}
