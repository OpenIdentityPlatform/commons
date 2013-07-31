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
 * Copyright 2012-2013 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;

/**
 * HTTP utility methods and constants.
 */
final class HttpUtils {

    static final String CACHE_CONTROL = "no-cache";
    static final String CHARACTER_ENCODING = "UTF-8";
    static final String CONTENT_TYPE = "application/json";
    static final Pattern CONTENT_TYPE_REGEX = Pattern.compile(
            "^application/json([ ]*;[ ]*charset=utf-8)?$", Pattern.CASE_INSENSITIVE);
    static final String CRLF = "\r\n";
    static final String ETAG_ANY = "*";

    static final String HEADER_CACHE_CONTROL = "Cache-Control";
    static final String HEADER_ETAG = "ETag";
    static final String HEADER_IF_MATCH = "If-Match";
    static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    static final String HEADER_LOCATION = "Location";
    static final String HEADER_X_HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override";

    static final String METHOD_DELETE = "DELETE";
    static final String METHOD_GET = "GET";
    static final String METHOD_HEAD = "HEAD";
    static final String METHOD_OPTIONS = "OPTIONS";
    static final String METHOD_PATCH = "PATCH";
    static final String METHOD_POST = "POST";
    static final String METHOD_PUT = "PUT";
    static final String METHOD_TRACE = "TRACE";

    static final String PARAM_ACTION = param(ActionRequest.FIELD_ACTION);
    static final String PARAM_DEBUG = "_debug";
    static final String PARAM_FIELDS = param(Request.FIELD_FIELDS);
    static final String PARAM_PAGE_SIZE = param(QueryRequest.FIELD_PAGE_SIZE);
    static final String PARAM_PAGED_RESULTS_COOKIE = param(QueryRequest.FIELD_PAGED_RESULTS_COOKIE);
    static final String PARAM_PAGED_RESULTS_OFFSET = param(QueryRequest.FIELD_PAGED_RESULTS_OFFSET);
    static final String PARAM_PRETTY_PRINT = "_prettyPrint";
    static final String PARAM_QUERY_EXPRESSION = param(QueryRequest.FIELD_QUERY_EXPRESSION);
    static final String PARAM_QUERY_FILTER = param(QueryRequest.FIELD_QUERY_FILTER);
    static final String PARAM_QUERY_ID = param(QueryRequest.FIELD_QUERY_ID);
    static final String PARAM_SORT_KEYS = param(QueryRequest.FIELD_SORT_KEYS);

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

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
    static boolean asBooleanValue(final String name, final String[] values)
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
    static int asIntValue(final String name, final String[] values) throws ResourceException {
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
    static String asSingleValue(final String name, final String[] values) throws ResourceException {
        if (values == null || values.length == 0) {
            // FIXME: i18n.
            throw new BadRequestException("No values provided for the request parameter \'" + name
                    + "\'");
        } else if (values.length > 1) {
            // FIXME: i18n.
            throw new BadRequestException(
                    "Multiple values provided for the single-valued request parameter \'" + name
                            + "\'");
        }
        return values[0];
    }

    /**
     * Throws a {@code NullPointerException} if the provided object is
     * {@code null}.
     *
     * @param <T>
     *            The parameter type.
     * @param object
     *            The object to test.
     * @return The object if not {@code null}.
     * @throws NullPointerException
     *             If {@code object} is {@code null}.
     */
    static <T> T checkNotNull(final T object) {
        if (object == null) {
            throw new NullPointerException();
        }
        return object;
    }

    /**
     * Closes the provided {@code Closeable}s ignoring null values and any
     * exceptions.
     *
     * @param closeables
     *            The {@code Closeable}s to be closed.
     */
    static void closeQuietly(final Closeable... closeables) {
        for (final Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (final Exception ignored) {
                    // Ignore.
                }
            }
        }
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
    static void fail(final HttpServletRequest req, final HttpServletResponse resp, final Throwable t) {
        if (!resp.isCommitted()) {
            final ResourceException re = adapt(t);
            try {
                resp.reset();
                prepareResponse(resp);
                resp.setStatus(re.getCode());
                final JsonGenerator writer = getJsonGenerator(req, resp);
                writer.writeObject(re.toJsonValue().getObject());
                closeQuietly(writer, resp.getOutputStream());
            } catch (final IOException ignored) {
                // Ignore the error since this was probably the cause.
            }
        }
    }

    static String getIfMatch(final HttpServletRequest req) {
        final String etag = req.getHeader(HEADER_IF_MATCH);
        if (etag != null) {
            if (etag.length() >= 2) {
                // Remove quotes.
                if (etag.charAt(0) == '"') {
                    return etag.substring(1, etag.length() - 1);
                }
            } else if (etag.equals(ETAG_ANY)) {
                // If-Match * is implied anyway.
                return null;
            }
        }
        return etag;
    }

    static String getIfNoneMatch(final HttpServletRequest req) {
        final String etag = req.getHeader(HEADER_IF_NONE_MATCH);
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
    static JsonValue getJsonContentIfPresent(final HttpServletRequest req) throws ResourceException {
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
    static JsonValue getJsonContent(final HttpServletRequest req) throws ResourceException {
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
    static JsonGenerator getJsonGenerator(final HttpServletRequest req,
            final HttpServletResponse resp) throws IOException {
        final JsonGenerator writer =
                JSON_MAPPER.getJsonFactory().createJsonGenerator(resp.getOutputStream());
        writer.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

        // Enable pretty printer if requested.
        final String[] values = getParameter(req, PARAM_PRETTY_PRINT);
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
    static List<PatchOperation> getJsonPatchContent(final HttpServletRequest req)
            throws ResourceException {
        final JsonValue json = new JsonValue(parseJsonBody(req, false));
        if (!json.isList()) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON array");
        }
        final List<PatchOperation> patch = new ArrayList<PatchOperation>(json.size());
        for (final JsonValue operation : json) {
            if (operation.isMap()) {
                try {
                    final String type =
                            operation.get(PatchOperation.FIELD_OPERATION).required().asString();
                    final JsonPointer field =
                            operation.get(PatchOperation.FIELD_FIELD).required().asPointer();
                    final JsonValue value = operation.get(PatchOperation.FIELD_VALUE);
                    patch.add(PatchOperation.operation(type, field, value));
                } catch (final Exception e) {
                    throw new BadRequestException(
                            "The request could not be processed because the provided "
                                    + "content is not a valid JSON patch: " + e.getMessage());
                }
            } else {
                throw new BadRequestException(
                        "The request could not be processed because the provided "
                                + "content is not a JSON array of patch operations");
            }
        }
        return patch;
    }

    /**
     * Returns the effective method name for an HTTP request taking into account
     * the "X-HTTP-Method-Override" header.
     *
     * @param req
     *            The HTTP request.
     * @return The effective method name.
     */
    static String getMethod(final HttpServletRequest req) {
        String method = req.getMethod();
        if (HttpUtils.METHOD_POST.equals(method)
                && req.getHeader(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE) != null) {
            method = req.getHeader(HttpUtils.HEADER_X_HTTP_METHOD_OVERRIDE);
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
    static String[] getParameter(final HttpServletRequest req, final String parameter) {
        // Need to do case-insensitive matching.
        for (final Map.Entry<String, String[]> p : req.getParameterMap().entrySet()) {
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
    static boolean hasParameter(final HttpServletRequest req, final String parameter) {
        return getParameter(req, parameter) != null;
    }

    /**
     * Determines whether debugging was requested in an HTTP request.
     *
     * @param req
     *            The HTTP request.
     * @return {@code true} if debugging was requested.
     * @throws ResourceException
     *             If the debug parameter could not be parsed.
     */
    static boolean isDebugRequested(final HttpServletRequest req) throws ResourceException {
        final String[] values = req.getParameterValues(PARAM_DEBUG);
        return (values != null && asBooleanValue(PARAM_DEBUG, values));
    }

    static void prepareResponse(final HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setHeader(HEADER_CACHE_CONTROL, CACHE_CONTROL);
    }

    static void rejectIfMatch(final HttpServletRequest req) throws ResourceException,
            PreconditionFailedException {
        if (req.getHeader(HEADER_IF_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-Match not supported for " + getMethod(req)
                    + " requests");
        }
    }

    static void rejectIfNoneMatch(final HttpServletRequest req) throws ResourceException,
            PreconditionFailedException {
        if (req.getHeader(HEADER_IF_NONE_MATCH) != null) {
            // FIXME: i18n
            throw new PreconditionFailedException("If-None-Match not supported for "
                    + getMethod(req) + " requests");
        }
    }

    private static JsonValue getJsonContent0(final HttpServletRequest req, final boolean allowEmpty)
            throws ResourceException {
        final Object body = parseJsonBody(req, allowEmpty);
        if (body == null) {
            return new JsonValue(new LinkedHashMap<String, Object>(0));
        } else if (!(body instanceof Map)) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not a JSON object");
        } else {
            return new JsonValue(body);
        }
    }

    private static Object parseJsonBody(final HttpServletRequest req, final boolean allowEmpty)
            throws BadRequestException, ResourceException {
        JsonParser parser = null;
        try {
            parser = JSON_MAPPER.getJsonFactory().createJsonParser(req.getInputStream());
            final Object content = parser.readValueAs(Object.class);

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

            return content;
        } catch (final JsonParseException e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not valid JSON", e).setDetail(new JsonValue(e
                            .getMessage()));
        } catch (final EOFException e) {
            if (allowEmpty) {
                return null;
            } else {
                throw new BadRequestException("The request could not be processed "
                        + "because it did not contain any JSON content");
            }
        } catch (final IOException e) {
            throw adapt(e);
        } finally {
            closeQuietly(parser);
        }
    }

    private static String param(final String field) {
        return "_" + field;
    }

    private HttpUtils() {
        // Prevent instantiation.
    }

}
