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
 * Copyright 2012-2016 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.http.protocol.Responses.newInternalServerError;
import static org.forgerock.http.routing.Version.version;
import static org.forgerock.json.resource.ActionRequest.ACTION_ID_CREATE;
import static org.forgerock.util.Utils.closeSilently;
import static org.forgerock.util.promise.Promises.newResultPromise;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import org.forgerock.http.header.AcceptApiVersionHeader;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.header.MalformedHeaderException;
import org.forgerock.http.io.PipeBufferedStream;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Version;
import org.forgerock.http.util.Json;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP utility methods and constants.
 */
public final class HttpUtils {
    static final String CACHE_CONTROL = "no-cache";
    static final String CHARACTER_ENCODING = "UTF-8";
    static final Pattern CONTENT_TYPE_REGEX = Pattern.compile(
            "^application/json([ ]*;[ ]*charset=utf-8)?$", Pattern.CASE_INSENSITIVE);
    static final String CRLF = "\r\n";
    static final String ETAG_ANY = "*";

    static final String MIME_TYPE_APPLICATION_JSON = "application/json";
    static final String MIME_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
    static final String MIME_TYPE_TEXT_PLAIN = "text/plain";

    static final String HEADER_CACHE_CONTROL = "Cache-Control";
    static final String HEADER_ETAG = "ETag";
    static final String HEADER_IF_MATCH = "If-Match";
    static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    static final String HEADER_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    static final String HEADER_LOCATION = "Location";
    static final String HEADER_X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    /** the HTTP header for {@literal Content-Disposition}. */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    static final Collection<String> RESTRICTED_HEADER_NAMES = Arrays.asList(
            ContentTypeHeader.NAME,
            AcceptApiVersionHeader.NAME,
            HEADER_IF_MODIFIED_SINCE,
            HEADER_IF_UNMODIFIED_SINCE,
            HEADER_IF_MATCH,
            HEADER_IF_NONE_MATCH,
            HEADER_CACHE_CONTROL,
            HEADER_ETAG,
            HEADER_LOCATION,
            HEADER_X_HTTP_METHOD_OVERRIDE,
            CONTENT_DISPOSITION
    );

    static final String METHOD_DELETE = "DELETE";
    static final String METHOD_GET = "GET";
    static final String METHOD_HEAD = "HEAD";
    static final String METHOD_OPTIONS = "OPTIONS";
    static final String METHOD_PATCH = "PATCH";
    static final String METHOD_POST = "POST";
    static final String METHOD_PUT = "PUT";
    static final String METHOD_TRACE = "TRACE";

    /** the HTTP request parameter for an action. */
    public static final String PARAM_ACTION = param(ActionRequest.FIELD_ACTION);
    /** the HTTP request parameter to specify which fields to return. */
    public static final String PARAM_FIELDS = param(Request.FIELD_FIELDS);
    /** the HTTP request parameter to request a certain mimetype for a filed. */
    public static final String PARAM_MIME_TYPE = param("mimeType");
    /** the HTTP request parameter to request a certain page size. */
    public static final String PARAM_PAGE_SIZE = param(QueryRequest.FIELD_PAGE_SIZE);
    /** the HTTP request parameter to specify a paged results cookie. */
    public static final String PARAM_PAGED_RESULTS_COOKIE =
            param(QueryRequest.FIELD_PAGED_RESULTS_COOKIE);
    /** the HTTP request parameter to specify a paged results offset. */
    public static final String PARAM_PAGED_RESULTS_OFFSET =
            param(QueryRequest.FIELD_PAGED_RESULTS_OFFSET);
    /** the HTTP request parameter to request pretty printing. */
    public static final String PARAM_PRETTY_PRINT = "_prettyPrint";
    /** the HTTP request parameter to specify a query expression. */
    public static final String PARAM_QUERY_EXPRESSION = param(QueryRequest.FIELD_QUERY_EXPRESSION);
    /** the HTTP request parameter to specify a query filter. */
    public static final String PARAM_QUERY_FILTER = param(QueryRequest.FIELD_QUERY_FILTER);
    /** the HTTP request parameter to specify a query id. */
    public static final String PARAM_QUERY_ID = param(QueryRequest.FIELD_QUERY_ID);
    /** the HTTP request parameter to specify the sort keys. */
    public static final String PARAM_SORT_KEYS = param(QueryRequest.FIELD_SORT_KEYS);
    /** The policy used for counting total paged results. */
    public static final String PARAM_TOTAL_PAGED_RESULTS_POLICY = param(QueryRequest.FIELD_TOTAL_PAGED_RESULTS_POLICY);
    /** Request the CREST API Descriptor. */
    public static final String PARAM_CREST_API = param("crestapi");

    /** Protocol Version 1. */
    public static final Version PROTOCOL_VERSION_1 = version(1);
    /** Protocol Version 2 - supports upsert on PUT. */
    public static final Version PROTOCOL_VERSION_2 = version(2);
    /**
     * Protocol Version 2.1 - supports defacto standard for create requests when the ID of the created resource is
     * to be allocated by the server, which are represented as a POST to the collection endpoint without an
     * {@code _action} query parameter.
     */
    public static final Version PROTOCOL_VERSION_2_1 = version(2, 1);
    /** The default version of the named protocol. */
    public static final Version DEFAULT_PROTOCOL_VERSION = PROTOCOL_VERSION_2_1;
    static final String FIELDS_DELIMITER = ",";
    static final String SORT_KEYS_DELIMITER = ",";

    static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .registerModules(new Json.JsonValueModule(), new Json.LocalizableStringModule());

    private static final String FILENAME = "filename";
    private static final String MIME_TYPE = "mimetype";
    private static final String CONTENT = "content";
    private static final String NAME = "name";
    private static final Pattern MULTIPART_FIELD_REGEX = Pattern.compile("^cid:(.*)#(" + FILENAME
            + "|" + MIME_TYPE + "|" + CONTENT + ")$", Pattern.CASE_INSENSITIVE);
    private static final int PART_NAME = 1;
    private static final int PART_DATA_TYPE = 2;
    private static final String REFERENCE_TAG = "$ref";

    private static final int BUFFER_SIZE = 1_024;
    private static final int EOF = -1;

    /**
     * Adapts an {@code Exception} to a {@code ResourceException}.
     *
     * @param t
     *            The exception which caused the request to fail.
     * @return The equivalent resource exception.
     */
    static ResourceException adapt(final Throwable t) {
        if (t instanceof ResourceException) {
            return (ResourceException) t;
        } else {
            return new InternalServerErrorException(t);
        }
    }

    /**
     * Parses a header or request parameter as a boolean value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The boolean value.
     * @throws ResourceException
     *             If the value could not be parsed as a boolean.
     */
    static boolean asBooleanValue(final String name, final List<String> values)
            throws ResourceException {
        final String value = asSingleValue(name, values);
        return Boolean.parseBoolean(value);
    }

    /**
     * Parses a header or request parameter as an integer value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The integer value.
     * @throws ResourceException
     *             If the value could not be parsed as a integer.
     */
    static int asIntValue(final String name, final List<String> values) throws ResourceException {
        final String value = asSingleValue(name, values);
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            // FIXME: i18n.
            throw new BadRequestException("The value \'" + value + "\' for parameter '" + name
                    + "' could not be parsed as a valid integer");
        }
    }

    /**
     * Parses a header or request parameter as a single string value.
     *
     * @param name
     *            The name of the header or parameter.
     * @param values
     *            The header or parameter values.
     * @return The single string value.
     * @throws ResourceException
     *             If the value could not be parsed as a single string.
     */
    static String asSingleValue(final String name, final List<String> values) throws ResourceException {
        if (values == null || values.isEmpty()) {
            // FIXME: i18n.
            throw new BadRequestException("No values provided for the request parameter \'" + name
                    + "\'");
        } else if (values.size() > 1) {
            // FIXME: i18n.
            throw new BadRequestException(
                    "Multiple values provided for the single-valued request parameter \'" + name
                            + "\'");
        }
        return values.get(0);
    }

    /**
     * Safely fail an HTTP request using the provided {@code Exception}.
     *
     * @param req
     *            The HTTP request.
     * @param t
     *            The resource exception indicating why the request failed.
     */
    static Promise<Response, NeverThrowsException> fail(org.forgerock.http.protocol.Request req, final Throwable t) {
        return fail0(req, null, t);
    }

    /**
     * Safely fail an HTTP request using the provided {@code Exception}.
     *
     * @param req
     *            The HTTP request.
     * @param resp
     *            The HTTP response.
     * @param t
     *            The resource exception indicating why the request failed.
     */
    static Promise<Response, NeverThrowsException> fail(org.forgerock.http.protocol.Request req,
            org.forgerock.http.protocol.Response resp, final Throwable t) {
        return fail0(req, resp, t);
    }

    private static Promise<Response, NeverThrowsException> fail0(org.forgerock.http.protocol.Request req,
            org.forgerock.http.protocol.Response resp, Throwable t) {
        final ResourceException re = adapt(t);
        try {
            if (resp == null) {
                resp = prepareResponse(req);
            } else {
                resp = prepareResponse(req, resp);
            }
            resp.setStatus(Status.valueOf(re.getCode()));
            final JsonGenerator writer = getJsonGenerator(req, resp);
            Json.makeLocalizingObjectWriter(JSON_MAPPER, req).writeValue(writer, re.toJsonValue().getObject());
            closeSilently(writer);
            return newResultPromise(resp);
        } catch (final IOException ignored) {
            // Ignore the error since this was probably the cause.
            return newResultPromise(newInternalServerError());
        } catch (MalformedHeaderException e) {
            return newResultPromise(new Response(Status.BAD_REQUEST).setEntity("Malformed header"));
        }
    }

    /**
     * Determines which CREST operation (CRUDPAQ) of the incoming request.
     *
     * @param request The request.
     * @return The Operation.
     * @throws ResourceException If the request operation could not be
     * determined or is not supported.
     */
    public static RequestType determineRequestType(org.forgerock.http.protocol.Request request)
            throws ResourceException {
        // Dispatch the request based on method, taking into account
        // method override header.
        final String method = getMethod(request);
        if (METHOD_DELETE.equals(method)) {
            return RequestType.DELETE;
        } else if (METHOD_GET.equals(method)) {
            if (hasParameter(request, PARAM_QUERY_ID)
                    || hasParameter(request, PARAM_QUERY_EXPRESSION)
                    || hasParameter(request, PARAM_QUERY_FILTER)) {
                return RequestType.QUERY;
            } else if (hasParameter(request, PARAM_CREST_API)) {
                return RequestType.API;
            } else {
                return RequestType.READ;
            }
        } else if (METHOD_PATCH.equals(method)) {
            return RequestType.PATCH;
        } else if (METHOD_POST.equals(method)) {
            return determinePostRequestType(request);
        } else if (METHOD_PUT.equals(method)) {
            return determinePutRequestType(request);
        } else {
            // TODO: i18n
            throw new NotSupportedException("Method " + method + " not supported");
        }
    }

    private static RequestType determinePostRequestType(org.forgerock.http.protocol.Request request)
            throws ResourceException {
        List<String> parameter = getParameter(request, PARAM_ACTION);

        boolean defactoCreate = getRequestedProtocolVersion(request).compareTo(PROTOCOL_VERSION_2_1) >= 0
                && (parameter == null || parameter.isEmpty());

        return defactoCreate || asSingleValue(PARAM_ACTION, parameter).equalsIgnoreCase(ACTION_ID_CREATE)
                ? RequestType.CREATE
                : RequestType.ACTION;
    }

    /**
     * Determine whether the PUT request should be interpreted as a CREATE or an UPDATE depending on
     * If-None-Match header, If-Match header, and protocol version.
     *
     * @param request The request.
     * @return true if request is interpreted as a create; false if interpreted as an update
     */
    private static RequestType determinePutRequestType(org.forgerock.http.protocol.Request request)
            throws BadRequestException {

        final Version protocolVersion = getRequestedProtocolVersion(request);
        final String ifNoneMatch = getIfNoneMatch(request);
        final String ifMatch = getIfMatch(request, protocolVersion);

        /* CREST-100
         * For protocol version 1:
         *
         *  - "If-None-Match: x" is present, where 'x' is any non-* value: this is a bad request
         *  - "If-None-Match: *" is present: this is a create which will fail if the object already exists.
         *  - "If-None-Match: *" is not present:
         *          This is an update which will fail if the object does not exist.  There are two ways to
         *          perform the update, using the value of the If-Match header:
         *           - "If-Match: <rev>" : update the object if its revision matches the header value
         *           - "If-Match: * : update the object regardless of the object's revision
         *           - "If-Match:" header is not present : same as "If-Match: *"; update regardless of object revision
         *
         * For protocol version 2 onward:
         *
         * Two methods of create are implied by PUT:
         *
         *  - "If-None-Match: x" is present, where 'x' is any non-* value: this is a bad request
         *  - "If-None-Match: *" is present, this is a create which will fail if the object already exists.
         *  - "If-Match" is present; this is an update only:
         *           - "If-Match: <rev>" : update the object if its revision matches the header value
         *           - "If-Match: * : update the object regardless of the object's revision
         *  - Neither "If-None-Match" nor "If-Match" are present, this is either a create or an update ("upsert"):
         *          Attempt a create; if it fails, attempt an update.  If the update fails, return an error
         *          (the record could have been deleted between the create-failure and the update, for example).
         */

        /* CREST-346 */
        if (ifNoneMatch != null && !ETAG_ANY.equals(ifNoneMatch)) {
            throw new BadRequestException("\"" + ifNoneMatch + "\" is not a supported value for If-None-Match on PUT");
        }

        if (ETAG_ANY.equals(ifNoneMatch)) {
            return RequestType.CREATE;
        } else if (ifNoneMatch == null && ifMatch == null && protocolVersion.getMajor() >= 2) {
            return RequestType.CREATE;
        } else {
            return RequestType.UPDATE;
        }
    }

    /**
     * Attempts to parse the version header and return a corresponding resource {@link Version} representation.
     * Further validates that the specified versions are valid. That being not in the future and no earlier
     * that the current major version.
     *
     * @param req
     *         The HTTP servlet request
     *
     * @return A non-null resource  {@link Version} instance
     *
     * @throws BadRequestException
     *         If an invalid version is requested
     */
    static Version getRequestedResourceVersion(org.forgerock.http.protocol.Request req) throws BadRequestException {
        return getAcceptApiVersionHeader(req).getResourceVersion();
    }

    /**
     * Attempts to parse the version header and return a corresponding protocol {@link Version} representation.
     * Further validates that the specified versions are valid. That being not in the future and no earlier
     * that the current major version.
     *
     * @param req
     *         The HTTP servlet request
     *
     * @return A non-null resource  {@link Version} instance
     *
     * @throws BadRequestException
     *         If an invalid version is requested
     */
    static Version getRequestedProtocolVersion(org.forgerock.http.protocol.Request req) throws BadRequestException {
        Version protocolVersion = getAcceptApiVersionHeader(req).getProtocolVersion();
        return protocolVersion != null ? protocolVersion : DEFAULT_PROTOCOL_VERSION;
    }

    /**
     * Validate and return the AcceptApiVersionHeader.
     *
     * @param req
     *         The HTTP servlet request
     *
     * @return A non-null resource  {@link Version} instance
     *
     * @throws BadRequestException
     *         If an invalid version is requested
     */
    private static AcceptApiVersionHeader getAcceptApiVersionHeader(org.forgerock.http.protocol.Request req)
            throws BadRequestException {
        AcceptApiVersionHeader apiVersionHeader;
        try {
            apiVersionHeader = AcceptApiVersionHeader.valueOf(req);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
        validateProtocolVersion(apiVersionHeader.getProtocolVersion());
        return apiVersionHeader;
    }

    /**
     * Validate the Protocol version as not in the future.
     *
     * @param protocolVersion the protocol version from the request
     * @throws BadRequestException if the request marks a protocol version greater than the current version
     */
    private static void validateProtocolVersion(Version protocolVersion) throws BadRequestException {
        if (protocolVersion != null && protocolVersion.getMajor() > DEFAULT_PROTOCOL_VERSION.getMajor()) {
            throw new BadRequestException("Unsupported major version: " + protocolVersion);
        }
        if (protocolVersion != null && protocolVersion.getMinor() > DEFAULT_PROTOCOL_VERSION.getMinor()) {
            throw new BadRequestException("Unsupported minor version: " + protocolVersion);
        }
    }

    static String getIfMatch(org.forgerock.http.protocol.Request req, Version protocolVersion) {
        final String etag = req.getHeaders().getFirst(HEADER_IF_MATCH);
        if (etag != null) {
            if (etag.length() >= 2) {
                // Remove quotes.
                if (etag.charAt(0) == '"') {
                    return etag.substring(1, etag.length() - 1);
                }
            } else if (etag.equals(ETAG_ANY) && protocolVersion.getMajor() < 2) {
                // If-Match * is implied prior to version 2
                return null;
            }
        }
        return etag;
    }

    static String getIfNoneMatch(org.forgerock.http.protocol.Request req) {
        final String etag = req.getHeaders().getFirst(HEADER_IF_NONE_MATCH);
        if (etag != null) {
            if (etag.length() >= 2) {
                // Remove quotes.
                if (etag.charAt(0) == '"') {
                    return etag.substring(1, etag.length() - 1);
                }
            } else if (etag.equals(ETAG_ANY)) {
                // If-None-Match *.
                return ETAG_ANY;
            }
        }
        return etag;
    }

    /**
     * Returns the content of the provided HTTP request decoded as a JSON
     * object. The content is allowed to be empty, in which case an empty JSON
     * object is returned.
     *
     * @param req
     *            The HTTP request.
     * @return The content of the provided HTTP request decoded as a JSON
     *         object.
     * @throws ResourceException
     *             If the content could not be read or if the content was not
     *             valid JSON.
     */
    static JsonValue getJsonContentIfPresent(org.forgerock.http.protocol.Request req) throws ResourceException {
        return getJsonContent0(req, true);
    }

    /**
     * Returns the content of the provided HTTP request decoded as a JSON
     * object. If there is no content then a {@link BadRequestException} will be
     * thrown.
     *
     * @param req
     *            The HTTP request.
     * @return The content of the provided HTTP request decoded as a JSON
     *         object.
     * @throws ResourceException
     *             If the content could not be read or if the content was not
     *             valid JSON.
     */
    static JsonValue getJsonContent(org.forgerock.http.protocol.Request req) throws ResourceException {
        return getJsonContent0(req, false);
    }

    /**
     * Creates a JSON generator which can be used for serializing JSON content
     * in HTTP responses.
     *
     * @param req
     *            The HTTP request.
     * @param resp
     *            The HTTP response.
     * @return A JSON generator which can be used to write out a JSON response.
     * @throws IOException
     *             If an error occurred while obtaining an output stream.
     */
    static JsonGenerator getJsonGenerator(org.forgerock.http.protocol.Request req,
            Response resp) throws IOException {

        PipeBufferedStream pipeStream = new PipeBufferedStream();
        resp.setEntity(pipeStream.getOut());

        final JsonGenerator writer =
                JSON_MAPPER.getFactory().createGenerator(pipeStream.getIn());

        // Need to have the JsonGenerator close the stream so that it is
        // properly released.
        writer.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);

        // Enable pretty printer if requested.
        final List<String> values = getParameter(req, PARAM_PRETTY_PRINT);
        if (values != null) {
            try {
                if (asBooleanValue(PARAM_PRETTY_PRINT, values)) {
                    writer.useDefaultPrettyPrinter();
                }
            } catch (final ResourceException e) {
                // Ignore because we may be trying to obtain a generator in
                // order to output an error.
            }
        }
        return writer;
    }

    /**
     * Returns the content of the provided HTTP request decoded as a JSON patch
     * object.
     *
     * @param req
     *            The HTTP request.
     * @return The content of the provided HTTP request decoded as a JSON patch
     *         object.
     * @throws ResourceException
     *             If the content could not be read or if the content was not a
     *             valid JSON patch.
     */
    static List<PatchOperation> getJsonPatchContent(org.forgerock.http.protocol.Request req)
            throws ResourceException {
        return PatchOperation.valueOfList(new JsonValue(parseJsonBody(req, false)));
    }

    /**
     * Returns the content of the provided HTTP request decoded as a JSON action
     * content.
     *
     * @param req
     *            The HTTP request.
     * @return The content of the provided HTTP request decoded as a JSON action
     *         content.
     * @throws ResourceException
     *             If the content could not be read or if the content was not
     *             valid JSON.
     */
    static JsonValue getJsonActionContent(org.forgerock.http.protocol.Request req) throws ResourceException {
        return new JsonValue(parseJsonBody(req, true));
    }

    /**
     * Returns the effective method name for an HTTP request taking into account
     * the "X-HTTP-Method-Override" header.
     *
     * @param req
     *            The HTTP request.
     * @return The effective method name.
     */
    static String getMethod(org.forgerock.http.protocol.Request req) {
        String method = req.getMethod();
        if (HttpUtils.METHOD_POST.equals(method)
                && req.getHeaders().getFirst(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE) != null) {
            method = req.getHeaders().getFirst(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE);
        }
        return method;
    }

    /**
     * Returns the named parameter from the provided HTTP request using case
     * insensitive matching.
     *
     * @param req
     *            The HTTP request.
     * @param parameter
     *            The parameter to return.
     * @return The parameter values or {@code null} if it wasn't present.
     */
    static List<String> getParameter(org.forgerock.http.protocol.Request req, String parameter) {
        // Need to do case-insensitive matching.
        for (final Map.Entry<String, List<String>> p : req.getForm().entrySet()) {
            if (p.getKey().equalsIgnoreCase(parameter)) {
                return p.getValue();
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if the named parameter is present in the provided
     * HTTP request using case insensitive matching.
     *
     * @param req
     *            The HTTP request.
     * @param parameter
     *            The parameter to return.
     * @return {@code true} if the named parameter is present.
     */
    static boolean hasParameter(org.forgerock.http.protocol.Request req, String parameter) {
        return getParameter(req, parameter) != null;
    }

    static Response prepareResponse(org.forgerock.http.protocol.Request req) throws ResourceException {
        return prepareResponse(req, new Response(Status.OK));
    }

    static Response prepareResponse(org.forgerock.http.protocol.Request req, org.forgerock.http.protocol.Response resp)
            throws ResourceException {
        //get content type from req path
        try {
            resp.setStatus(Status.OK);
            String mimeType = req.getForm().getFirst(PARAM_MIME_TYPE);
            if (METHOD_GET.equalsIgnoreCase(getMethod(req)) && mimeType != null && !mimeType.isEmpty()) {
                ContentType contentType = new ContentType(mimeType);
                resp.getHeaders().put(new ContentTypeHeader(contentType.toString(), CHARACTER_ENCODING, null));
            } else {
                resp.getHeaders().put(new ContentTypeHeader(MIME_TYPE_APPLICATION_JSON, CHARACTER_ENCODING, null));
            }

            resp.getHeaders().put(HEADER_CACHE_CONTROL, CACHE_CONTROL);
            return resp;
        } catch (ParseException e) {
            throw new BadRequestException("The mime type parameter '" + req.getForm().getFirst(PARAM_MIME_TYPE)
                    + "' can't be parsed", e);
        }
    }

    static void rejectIfMatch(org.forgerock.http.protocol.Request req) throws ResourceException {
        if (req.getHeaders().getFirst(HEADER_IF_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-Match not supported for " + getMethod(req) + " requests");
        }
    }

    static void rejectIfNoneMatch(org.forgerock.http.protocol.Request req) throws ResourceException,
            PreconditionFailedException {
        if (req.getHeaders().getFirst(HEADER_IF_NONE_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-None-Match not supported for "
                    + getMethod(req) + " requests");
        }
    }

    private static JsonValue getJsonContent0(org.forgerock.http.protocol.Request req, boolean allowEmpty)
            throws ResourceException {
        final Object body = parseJsonBody(req, allowEmpty);
        if (body == null) {
            return new JsonValue(new LinkedHashMap<>(0));
        } else if (!(body instanceof Map)) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON object");
        } else {
            return new JsonValue(body);
        }
    }

    private static BodyPart getJsonRequestPart(final MimeMultipart mimeMultiparts)
            throws BadRequestException, ResourceException {
        try {
            for (int i = 0; i < mimeMultiparts.getCount(); i++) {
                BodyPart part = mimeMultiparts.getBodyPart(i);
                ContentType contentType = new ContentType(part.getContentType());
                if (contentType.match(MIME_TYPE_APPLICATION_JSON)) {
                    return part;
                }
            }
            throw new BadRequestException(
                    "The request could not be processed because the multipart request "
                    + "does not include Content-Type: " + MIME_TYPE_APPLICATION_JSON);
        } catch (final MessagingException e) {
            throw new BadRequestException(
                    "The request could not be processed because the request cant be parsed", e);
        } catch (final IOException e) {
            throw adapt(e);
        }

    }

    private static String getRequestPartData(final MimeMultipart mimeMultiparts,
            final String partName, final String partDataType) throws IOException, MessagingException {
        if (mimeMultiparts == null) {
            throw new BadRequestException(
                    "The request parameter is null when retrieving part data for part name: "
                            + partName);
        }

        if (partDataType == null || partDataType.isEmpty()) {
            throw new BadRequestException("The request is requesting an unknown part field");
        }
        MimeBodyPart part = null;
        for (int i = 0; i < mimeMultiparts.getCount(); i++) {
            part = (MimeBodyPart) mimeMultiparts.getBodyPart(i);
            ContentDisposition disposition =
                    new ContentDisposition(part.getHeader(CONTENT_DISPOSITION, null));
            if (disposition.getParameter(NAME).equalsIgnoreCase(partName)) {
                break;
            }
        }

        if (part == null) {
            throw new BadRequestException(
                    "The request is missing a referenced part for part name: " + partName);
        }

        if (MIME_TYPE.equalsIgnoreCase(partDataType)) {
            return new ContentType(part.getContentType()).toString();
        } else if (FILENAME.equalsIgnoreCase(partDataType)) {
            return part.getFileName();
        } else if (CONTENT.equalsIgnoreCase(partDataType)) {
            return Base64url.encode(toByteArray(part.getInputStream()));
        } else {
            throw new BadRequestException(
                    "The request could not be processed because the multipart request "
                            + "requests data from the part that isn't supported. Data requested: "
                            + partDataType);
        }
    }

    private static boolean isAReferenceJsonObject(JsonValue node) {
        return node.keys() != null && node.keys().size() == 1
                && REFERENCE_TAG.equalsIgnoreCase(node.keys().iterator().next());
    }

    private static Object swapRequestPartsIntoContent(final MimeMultipart mimeMultiparts,
            Object content) throws ResourceException {
        try {
            JsonValue root = new JsonValue(content);

            ArrayDeque<JsonValue> stack = new ArrayDeque<>();
            stack.push(root);

            while (!stack.isEmpty()) {
                JsonValue node = stack.pop();
                if (isAReferenceJsonObject(node)) {
                    Matcher matcher =
                            MULTIPART_FIELD_REGEX.matcher(node.get(REFERENCE_TAG).asString());
                    if (matcher.matches()) {
                        String partName = matcher.group(PART_NAME);
                        String requestPartData =
                                getRequestPartData(mimeMultiparts, partName, matcher
                                        .group(PART_DATA_TYPE));
                        root.put(node.getPointer(), requestPartData);
                    } else {
                        throw new BadRequestException("Invalid reference tag '" + node.toString()
                                + "'");
                    }
                } else {
                    Iterator<JsonValue> iter = node.iterator();
                    while (iter.hasNext()) {
                        stack.push(iter.next());
                    }
                }
            }
            return root;
        } catch (final IOException e) {
            throw adapt(e);
        } catch (final MessagingException e) {
            throw new BadRequestException(
                    "The request could not be processed because the request is not a valid multipart request");
        }
    }

    static boolean isMultiPartRequest(final String unknownContentType) throws BadRequestException {
        try {
            if (unknownContentType == null) {
                return false;
            }
            ContentType contentType = new ContentType(unknownContentType);
            return contentType.match(MIME_TYPE_MULTIPART_FORM_DATA);
        } catch (final ParseException e) {
            throw new BadRequestException("The request content type can't be parsed.", e);
        }
    }

    private static Object parseJsonBody(org.forgerock.http.protocol.Request req, boolean allowEmpty)
            throws ResourceException {
        try {
            String contentType = req.getHeaders().getFirst(ContentTypeHeader.class);
            if (contentType == null && !allowEmpty) {
                throw new BadRequestException("The request could not be processed because the "
                        + " content-type was not specified and is required");
            }
            boolean isMultiPartRequest = isMultiPartRequest(contentType);
            MimeMultipart mimeMultiparts = null;
            JsonParser jsonParser;
            if (isMultiPartRequest) {
                mimeMultiparts = new MimeMultipart(new HttpServletRequestDataSource(req));
                BodyPart jsonPart = getJsonRequestPart(mimeMultiparts);
                jsonParser = JSON_MAPPER.getFactory().createParser(jsonPart.getInputStream());
            } else {
                jsonParser = JSON_MAPPER.getFactory().createParser(req.getEntity().getRawContentInputStream());
            }
            try (JsonParser parser = jsonParser) {
                Object content = parser.readValueAs(Object.class);

                // Ensure that there is no trailing data following the JSON resource.
                boolean hasTrailingGarbage;
                try {
                    hasTrailingGarbage = parser.nextToken() != null;
                } catch (JsonParseException e) {
                    hasTrailingGarbage = true;
                }
                if (hasTrailingGarbage) {
                    throw new BadRequestException(
                            "The request could not be processed because there is "
                                    + "trailing data after the JSON content");
                }

                if (isMultiPartRequest) {
                    swapRequestPartsIntoContent(mimeMultiparts, content);
                }

                return content;
            }
        } catch (final JsonParseException e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not valid JSON", e)
                .setDetail(new JsonValue(e.getMessage()));
        } catch (final JsonMappingException e) {
            if (allowEmpty) {
                return null;
            } else {
                throw new BadRequestException("The request could not be processed "
                        + "because it did not contain any JSON content", e);
            }
        } catch (final IOException e) {
            throw adapt(e);
        } catch (final MessagingException e) {
            throw new BadRequestException(
                    "The request could not be processed because it can't be parsed", e);
        }
    }

    private static String param(final String field) {
        return "_" + field;
    }

    private HttpUtils() {
        // Prevent instantiation.
    }

    private static class HttpServletRequestDataSource implements DataSource {
        private org.forgerock.http.protocol.Request request;

        HttpServletRequestDataSource(org.forgerock.http.protocol.Request request) {
            this.request = request;
        }

        public InputStream getInputStream() throws IOException {
            return request.getEntity().getRawContentInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        public String getContentType() {
            return request.getHeaders().getFirst(ContentTypeHeader.class);
        }

        public String getName() {
            return "HttpServletRequestDataSource";
        }
    }

    private static byte[] toByteArray(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final byte[] data = new byte[BUFFER_SIZE];
        int size;
        while ((size = inputStream.read(data)) != EOF) {
            byteArrayOutputStream.write(data, 0, size);
        }
        byteArrayOutputStream.flush();
        return byteArrayOutputStream.toByteArray();
    }

    static HttpContextFactory staticContextFactory(final Context parentContext) {
        return new HttpContextFactory() {
            @Override
            public Context createContext(Context parent, org.forgerock.http.protocol.Request request) {
                return parentContext;
            }
        };
    }
}
