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
 * Copyright 2012-2014 ForgeRock AS.
 */
package org.forgerock.json.resource.servlet;

import static org.forgerock.util.Utils.closeSilently;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PreconditionFailedException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.encode.Base64url;

/**
 * HTTP utility methods and constants.
 */
public final class HttpUtils {
    static final String CACHE_CONTROL = "no-cache";
    static final String CHARACTER_ENCODING = "UTF-8";
    static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
    static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";
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

    /** the HTTP request parameter for an action. */
    public static final String PARAM_ACTION = param(ActionRequest.FIELD_ACTION);
    /** the HTTP request parameter to request debugging. */
    public static final String PARAM_DEBUG = "_debug";
    /** the HTTP request parameter to specify which fields to return. */
    public static final String PARAM_FIELDS = param(Request.FIELD_FIELDS);
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

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String FILENAME = "filename";
    private static final String MIME_TYPE = "mimetype";
    private static final String CONTENT = "content";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String NAME = "name";
    private static final Pattern MULTIPART_FIELD_REGEX = Pattern.compile("^cid:(.*)#(" + FILENAME
            + "|" + MIME_TYPE + "|" + CONTENT + ")$", Pattern.CASE_INSENSITIVE);
    private static final int PART_NAME = 1;
    private static final int PART_DATA_TYPE = 2;
    private static final String REFERENCE_TAG = "$ref";

    private static final int BUFFER_SIZE = 1024;
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
                closeSilently(writer, resp.getOutputStream());
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
    static JsonValue getJsonActionContent(final HttpServletRequest req) throws ResourceException {
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
        resp.setContentType(APPLICATION_JSON_CONTENT_TYPE);
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

    private static BodyPart getJsonRequestPart(final MimeMultipart mimeMultiparts)
            throws BadRequestException, ResourceException {
        try {
            for (int i = 0; i < mimeMultiparts.getCount(); i++) {
                BodyPart part = mimeMultiparts.getBodyPart(i);
                ContentType contentType = new ContentType(part.getContentType());
                if (contentType.match(APPLICATION_JSON_CONTENT_TYPE)) {
                    return part;
                }
            }
            throw new BadRequestException(
                    "The request could not be processed because the multipart request "
                            + "does not include Content-Type: " + APPLICATION_JSON_CONTENT_TYPE);
        } catch (final MessagingException e) {
            throw new BadRequestException(
                    "The request could not be processed because the request cant be parsed", e);
        } catch (final IOException e) {
            throw adapt(e);
        }

    }

    private static String getRequestPartData(final MimeMultipart mimeMultiparts,
            final String partName, final String partDataType) throws BadRequestException,
            ResourceException, IOException, MessagingException {
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
            Object content) throws BadRequestException, ResourceException {
        try {
            JsonValue root = new JsonValue(content);

            ArrayDeque<JsonValue> stack = new ArrayDeque<JsonValue>();
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
            ContentType contentType = new ContentType(unknownContentType);
            return contentType.match(MULTIPART_FORM_CONTENT_TYPE);
        } catch (final ParseException e) {
            throw new BadRequestException("The request content type can't be parsed.", e);
        }
    }

    private static Object parseJsonBody(final HttpServletRequest req, final boolean allowEmpty)
            throws BadRequestException, ResourceException {
        JsonParser parser = null;
        try {
            boolean isMultiPartRequest = isMultiPartRequest(req.getContentType());
            MimeMultipart mimeMultiparts = null;
            if (isMultiPartRequest) {
                mimeMultiparts = new MimeMultipart(new HttpServletRequestDataSource(req));
                BodyPart jsonPart = getJsonRequestPart(mimeMultiparts);
                parser = JSON_MAPPER.getJsonFactory().createJsonParser(jsonPart.getInputStream());
            } else {
                parser = JSON_MAPPER.getJsonFactory().createJsonParser(req.getInputStream());
            }
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
        } catch (final JsonParseException e) {
            throw new BadRequestException(
                    "The request could not be processed because the provided "
                            + "content is not valid JSON", e)
                .setDetail(new JsonValue(e.getMessage()));
        } catch (final EOFException e) {
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
        } finally {
            closeSilently(parser);
        }
    }

    private static String param(final String field) {
        return "_" + field;
    }

    private HttpUtils() {
        // Prevent instantiation.
    }

    private static class HttpServletRequestDataSource implements DataSource {
        private HttpServletRequest request;

        HttpServletRequestDataSource(HttpServletRequest request) throws IOException {
            this.request = request;
        }

        public InputStream getInputStream() throws IOException {
            return request.getInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        public String getContentType() {
            return request.getContentType();
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
}
