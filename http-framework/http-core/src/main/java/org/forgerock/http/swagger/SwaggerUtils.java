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

package org.forgerock.http.swagger;

import static org.forgerock.http.protocol.Entity.APPLICATION_JSON_CHARSET_UTF_8;
import static org.forgerock.http.protocol.Responses.newInternalServerError;

import org.forgerock.http.header.AcceptLanguageHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.util.Json;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.swagger.models.Swagger;

/**
 * Swagger utility.
 */
public final class SwaggerUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new Json.LocalizableStringModule());

    /**
     * Request parameter for the OpenAPI API Descriptor.
     */
    public static final String API_PARAMETER = "_api";

    /**
     * Clone a {@code Swagger} instance.
     * @param descriptor The instance to clone.
     * @return The newly cloned instance.
     */
    public static Swagger clone(Swagger descriptor) {
        Swagger swagger = new SwaggerExtended()
                .basePath(descriptor.getBasePath())
                .consumes(descriptor.getConsumes())
                .info(descriptor.getInfo())
                .produces(descriptor.getProduces())
                .responses(descriptor.getResponses())
                .schemes(descriptor.getSchemes())
                .tags(descriptor.getTags())
                .vendorExtensions(descriptor.getVendorExtensions());
        swagger.setDefinitions(descriptor.getDefinitions());
        swagger.setPaths(descriptor.getPaths());
        swagger.setSecurity(descriptor.getSecurity());
        swagger.setParameters(descriptor.getParameters());
        swagger.setSecurityDefinitions(descriptor.getSecurityDefinitions());
        return swagger;
    }

    /**
     * Send the request for the API Descriptor to the handler, and package as a response.
     * @param handler The handler.
     * @param request The request.
     * @param context The contenxt.
     * @return The response.
     */
    public static Response request(Describable<Swagger, Request> handler, Request request,
            Context context) {
        try {
            Swagger result = handler.handleApiRequest(context, request);
            if (result != null) {
                ObjectWriter writer = OBJECT_MAPPER.writer();
                if (request.getHeaders().containsKey(AcceptLanguageHeader.NAME)) {
                    writer = writer.withAttribute(Json.PREFERRED_LOCALES_ATTRIBUTE,
                            request.getHeaders().get(AcceptLanguageHeader.class).getLocales());
                }
                Response response = new Response()
                        .setStatus(Status.OK)
                        .setEntity(writer.writeValueAsBytes(result));
                response.getHeaders().put(ContentTypeHeader.NAME, APPLICATION_JSON_CHARSET_UTF_8);
                return response;
            } else {
                return new Response(Status.NOT_IMPLEMENTED);
            }
        } catch (RuntimeException | JsonProcessingException | MalformedHeaderException e) {
            return newInternalServerError(e);
        }
    }

    /**
     * Check to see if the request is for the OpenAPI API descriptor.
     * @param request The request.
     * @return {@code true} if the request contains the {@code _api} query parameter.
     */
    public static boolean isApiRequest(Request request) {
        return request.getForm().containsKey(SwaggerUtils.API_PARAMETER);
    }

    private SwaggerUtils() {
        // utility class
    }
}
