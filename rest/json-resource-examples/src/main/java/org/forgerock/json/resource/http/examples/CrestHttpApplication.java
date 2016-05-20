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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http.examples;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.HeaderParameter;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.Placement;
import org.asciidoctor.SafeMode;
import org.forgerock.api.enums.CountPolicy;
import org.forgerock.api.enums.CreateMode;
import org.forgerock.api.enums.PagingMode;
import org.forgerock.api.enums.ParameterSource;
import org.forgerock.api.enums.PatchOperation;
import org.forgerock.api.enums.QueryType;
import org.forgerock.api.markup.ApiDocGenerator;
import org.forgerock.api.models.Action;
import org.forgerock.api.models.ApiDescription;
import org.forgerock.api.models.ApiError;
import org.forgerock.api.models.Definitions;
import org.forgerock.api.models.Delete;
import org.forgerock.api.models.Errors;
import org.forgerock.api.models.Parameter;
import org.forgerock.api.models.Patch;
import org.forgerock.api.models.Paths;
import org.forgerock.api.models.Read;
import org.forgerock.api.models.Reference;
import org.forgerock.api.models.Resource;
import org.forgerock.api.models.Schema;
import org.forgerock.api.models.Services;
import org.forgerock.api.models.SubResources;
import org.forgerock.api.models.Update;
import org.forgerock.api.transform.OpenApiHelper;
import org.forgerock.api.transform.OpenApiTransformer;
import org.forgerock.http.Handler;
import org.forgerock.http.HttpApplication;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.MutableUri;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Router;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.util.Uris;
import org.forgerock.json.resource.descriptor.examples.handler.UserCollectionHandler;
import org.forgerock.services.context.Context;
import org.forgerock.util.Factory;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

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
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Http Application implementation to demonstrate integration with the Commons HTTP Framework.
 */
public class CrestHttpApplication implements HttpApplication {

    private static final String SWAGGER_JSON_ROUTE = "/docs/api/users.json";

    private static final ContentTypeHeader HTML_CONTENT_TYPE_HEADER =
            ContentTypeHeader.valueOf("text/html; charset=UTF-8");

    private static final ContentTypeHeader JSON_CONTENT_TYPE_HEADER =
            ContentTypeHeader.valueOf("application/json; charset=UTF-8");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public Handler start() throws HttpApplicationException {
        // TODO bootstrap ApiDescription
        final ApiDescription apiDescription = createUserAndDeviceExampleApiDescription();
        final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        Router router = new Router();
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/users"), MemoryBackendHandler.getHandler());
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/groups"), MemoryBackendHandler.getHandler());
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/api/users"),
                UserCollectionHandler.getUsersRouter());
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/api/admins"),
                UserCollectionHandler.getAdminsRouter());

        // convert ApiDescription to HTML documentation
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, "/docs/html"),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        final String asciiDocMarkup = ApiDocGenerator.execute("Users and Devices API", apiDescription,
                                null, null);

                        final String html;
                        synchronized (asciidoctor) {
                            html = asciidoctor.render(asciiDocMarkup,
                                    OptionsBuilder.options()
                                            .attributes(AttributesBuilder.attributes()
                                                    .tableOfContents(Placement.LEFT)
                                                    .sectNumLevels(5)
                                                    .attribute("toclevels", 5)
                                                    .get())
                                            .safe(SafeMode.SAFE)
                                            .headerFooter(true)
                                            .get());
                        }

                        final Response response = new Response(Status.OK);
                        response.getHeaders().add(HTML_CONTENT_TYPE_HEADER);
                        response.setEntity(html);
                        return Response.newResponsePromise(response);
                    }
                });

        // convert ApiDescription to Swagger JSON
        router.addRoute(requestUriMatcher(RoutingMode.STARTS_WITH, SWAGGER_JSON_ROUTE),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        final String host = request.getUri().getHost() + ':' + request.getUri().getPort();

                        final Swagger swagger = OpenApiTransformer.execute("Users and Devices API", host, "/api",
                                request.getUri().getScheme().contains("https"), apiDescription, null);

                        OpenApiHelper.visitAllOperations(
                                new OpenApiHelper.OperationVisitor() {
                                    @Override
                                    public void visit(final Operation operation) {
                                        // add header "Accept-API-Version: resource=XXX, protocol=1.0"
                                        final String resourceVersion =
                                                (String) operation.getVendorExtensions().get("x-resourceVersion");
                                        if (resourceVersion != null) {
                                            final HeaderParameter header = new HeaderParameter();
                                            header.setName("Accept-API-Version");
                                            header.setEnum(
                                                    Arrays.asList("resource=" + resourceVersion + ", protocol=1.0"));
                                            header.setType("string");
                                            header.required(true);
                                            operation.addParameter(header);
                                        }
                                    }
                                }, swagger);

                        final String swaggerJson;
                        try {
                            swaggerJson = OBJECT_MAPPER.writeValueAsString(swagger);
                        } catch (IOException e) {
                            return newResultPromise(new Response(Status.INTERNAL_SERVER_ERROR).setCause(e));
                        }

                        final Response response = new Response(Status.OK);
                        response.getHeaders().add(JSON_CONTENT_TYPE_HEADER);
                        response.setEntity(swaggerJson);
                        return Response.newResponsePromise(response);
                    }
                });

        // redirect to Swagger UI page, given a URL parameter to point to the Swagger JSON endpoint
        router.addRoute(requestUriMatcher(RoutingMode.EQUALS, "/docs/api"),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        final MutableUri uri = request.getUri();
                        final String baseUrl = uri.getScheme() + "://" + uri.getHost() + ':' + uri.getPort();
                        final String url = baseUrl + "/openapi/index.html?url="
                                + Uris.urlEncodeFragment(SWAGGER_JSON_ROUTE)
                                + "&title=" + Uris.urlEncodeFragment("Users and Devices API");

                        final Response response = new Response(Status.FOUND);
                        response.getHeaders().add("Location", url);
                        return Response.newResponsePromise(response);
                    }
                });

        // simple page providing links to HTML docs and Swagger UI
        router.addRoute(requestUriMatcher(RoutingMode.EQUALS, "/"),
                new Handler() {
                    @Override
                    public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                        final String html = "<!DOCTYPE html><html><head><title></title></head><body>"
                                + "<p><a href=\"/docs/api\">Users and Devices API explorer</a></p>"
                                + "<p><a href=\"/docs/html\">Users and Devices API documentation</a></p>"
                                + "</body></html>";

                        final Response response = new Response(Status.OK);
                        response.getHeaders().add(HTML_CONTENT_TYPE_HEADER);
                        response.setEntity(html);
                        return Response.newResponsePromise(response);
                    }
                });
        return router;
    }

    @Override
    public Factory<Buffer> getBufferFactory() {
        return null;
    }

    @Override
    public void stop() {
        // empty
    }

    private static ApiDescription createUserAndDeviceExampleApiDescription() {
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
                .title("User Service")
                .description("User management service")
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
                        .pagingMode(PagingMode.COOKIE, PagingMode.OFFSET)
                        .countPolicy(CountPolicy.NONE)
                        .errors(errorList)
                        .build())
                .items(items()
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
                        .build())
                .subresources(SubResources.subresources()
                        .put("/{userId}/devices", resource()
                                .reference(reference().value("#/services/devices:1.0").build())
                                .build())
                        .build())
                .build();

        final Resource deviceResource1 = resource()
                .title("User-Device Service")
                .description("User-device management service")
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
                        .pagingMode(PagingMode.COOKIE, PagingMode.OFFSET)
                        .countPolicy(CountPolicy.NONE)
                        .errors(errorList)
                        .build())
                .items(items()
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
//                )
//                .put("/admins", versionedPath()
//                        .put(version(1), resource().reference(
//                                reference().value("#/services/users:1.0").build()
//                                ).build()
//                        ).build()
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
