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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2011-2015 ForgeRock AS.
 */

package org.forgerock.json.resource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.http.routing.Version;
import org.forgerock.json.JsonValue;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * An exception that is thrown during the processing of a JSON resource request.
 * Contains an integer exception code and short reason phrase. A longer
 * description of the exception is provided in the exception's detail message.
 * <p>
 * Positive 3-digit integer exception codes are explicitly reserved for
 * exceptions that correspond with HTTP status codes. For the sake of
 * interoperability with HTTP, if an exception corresponds with an HTTP error
 * status, use the matching HTTP status code.
 */
public class ResourceException extends IOException implements Response {

    /**
     * The name of the JSON field used for the detail.
     *
     * @see #getDetail()
     * @see #toJsonValue()
     */
    public static final String FIELD_DETAIL = "detail";

    /**
     * The name of the JSON field used for the message.
     *
     * @see #getMessage()
     * @see #toJsonValue()
     */
    public static final String FIELD_MESSAGE = "message";

    /**
     * The name of the JSON field used for the reason.
     *
     * @see #getReason()
     * @see #toJsonValue()
     */
    public static final String FIELD_REASON = "reason";

    /**
     * The name of the JSON field used for the code.
     *
     * @see #getCode()
     * @see #toJsonValue()
     */
    public static final String FIELD_CODE = "code";

    /**
     * The name of the JSON field used for the cause message.
     *
     * @see #getCause()
     * @see #toJsonValue()
     */
    public static final String FIELD_CAUSE = "cause";

    /**
     * Indicates that the request could not be understood by the resource due to
     * malformed syntax. Equivalent to HTTP status: 400 Bad Request.
     */
    public static final int BAD_REQUEST = 400;

    /**
     * Indicates the request could not be completed due to a conflict with the
     * current state of the resource. Equivalent to HTTP status: 409 Conflict.
     */
    public static final int CONFLICT = 409;

    /**
     * Indicates that the resource understood the request, but is refusing to
     * fulfill it. Equivalent to HTTP status: 403 Forbidden.
     */
    public static final int FORBIDDEN = 403;

    /**
     * Indicates that a resource encountered an unexpected condition which
     * prevented it from fulfilling the request. Equivalent to HTTP status: 500
     * Internal Server Error.
     */
    public static final int INTERNAL_ERROR = 500;

    /**
     * Indicates that the resource could not be found. Equivalent to HTTP
     * status: 404 Not Found.
     */
    public static final int NOT_FOUND = 404;

    /**
     * Indicates that the resource does not implement/support the feature to
     * fulfill the request HTTP status: 501 Not Implemented.
     */
    public static final int NOT_SUPPORTED = 501;

    /**
     * Indicates that the resource is temporarily unable to handle the request.
     * Equivalent to HTTP status: 503 Service Unavailable.
     */
    public static final int UNAVAILABLE = 503;

    /**
     * Indicates that the resource's current version does not match the version
     * provided. Equivalent to HTTP status: 412 Precondition Failed.
     */
    public static final int VERSION_MISMATCH = 412;

    /**
     * Indicates that the resource requires a version, but no version was
     * supplied in the request. Equivalent to
     * draft-nottingham-http-new-status-03 HTTP status: 428 Precondition
     * Required.
     */
    public static final int VERSION_REQUIRED = 428;

    /** Serializable class a version number. */
    private static final long serialVersionUID = 1L;

    /** flag to indicate whether to include the cause. */
    private boolean includeCause = false;

    /**
     * Returns an exception with the specified HTTP error code, but no detail
     * message or cause, and a default reason phrase. Useful for translating
     * HTTP status codes to the relevant Java exception type. The type of the
     * returned exception will be a sub-type of {@code ResourceException}.
     *
     * <p>If the type of the expected exception is known in advance, prefer to
     * directly instantiate the exception type as usual:
     *
     * <pre>
     *     {@code
     *     throw new InternalServerErrorException("Server failed");
     *     }
     * </pre>
     *
     * @param code
     *            The HTTP error code.
     * @return A resource exception having the provided HTTP error code.
     */
    public static ResourceException newResourceException(final int code) {
        return newResourceException(code, null);
    }

    /**
     * Returns an exception with the specified HTTP error code and detail
     * message, but no cause, and a default reason phrase. Useful for
     * translating HTTP status codes to the relevant Java exception type. The
     * type of the returned exception will be a sub-type of
     * {@code ResourceException}.
     *
     * <p>If the type of the expected exception is known in advance, prefer to
     * directly instantiate the exception type as usual:
     *
     * <pre>
     *     {@code
     *     throw new InternalServerErrorException("Server failed");
     *     }
     * </pre>
     *
     * @param code
     *            The HTTP error code.
     * @param message
     *            The detail message.
     * @return A resource exception having the provided HTTP error code.
     */
    public static ResourceException newResourceException(final int code, final String message) {
        return newResourceException(code, message, null);
    }

    /**
     * Returns an exception with the specified HTTP error code, detail message,
     * and cause, and a default reason phrase. Useful for translating HTTP
     * status codes to the relevant Java exception type. The type of the
     * returned exception will be a sub-type of {@code ResourceException}.
     *
     * <p>If the type of the expected exception is known in advance, prefer to
     * directly instantiate the exception type as usual:
     *
     * <pre>
     *     {@code
     *     throw new InternalServerErrorException("Server failed");
     *     }
     * </pre>
     *
     * @param code
     *            The HTTP error code.
     * @param message
     *            The detail message.
     * @param cause
     *            The exception which caused this exception to be thrown.
     * @return A resource exception having the provided HTTP error code.
     */
    public static ResourceException newResourceException(final int code,
                                                         final String message,
                                                         final Throwable cause) {
        final ResourceException ex;
        switch (code) {
        case BAD_REQUEST:
            ex = new BadRequestException(message, cause);
            break;
        case FORBIDDEN:
            ex = new ForbiddenException(message, cause);
            break; // Authorization exceptions
        case NOT_FOUND:
            ex = new NotFoundException(message, cause);
            break;
        case CONFLICT:
            ex = new ConflictException(message, cause);
            break;
        case VERSION_MISMATCH:
            ex = new PreconditionFailedException(message, cause);
            break;
        case VERSION_REQUIRED:
            ex = new PreconditionRequiredException(message, cause);
            break; // draft-nottingham-http-new-status-03
        case INTERNAL_ERROR:
            ex = new InternalServerErrorException(message, cause);
            break;
        case NOT_SUPPORTED:
            ex = new NotSupportedException(message, cause);
            break; // Not Implemented
        case UNAVAILABLE:
            ex = new ServiceUnavailableException(message, cause);
            break;

        // Temporary failures without specific exception classes
        case 408: // Request Time-out
        case 504: // Gateway Time-out
            ex = new RetryableException(code, message, cause);
            break;

        // Permanent Failures without specific exception classes
        case 401: // Unauthorized - Missing or bad authentication
        case 402: // Payment Required
        case 405: // Method Not Allowed
        case 406: // Not Acceptable
        case 407: // Proxy Authentication Required
        case 410: // Gone
        case 411: // Length Required
        case 413: // Request Entity Too Large
        case 414: // Request-URI Too Large
        case 415: // Unsupported Media Type
        case 416: // Requested range not satisfiable
        case 417: // Expectation Failed
        case 502: // Bad Gateway
        case 505: // HTTP Version not supported
            ex = new PermanentException(code, message, cause);
            break;
        default:
            ex = new UncategorizedException(code, message, cause);
        }
        return ex;
    }

    /**
     * Returns an exception with the specified HTTP error code, but no detail
     * message or cause, and a default reason phrase. Useful for translating
     * HTTP status codes to the relevant Java exception type. The type of the
     * returned exception will be a sub-type of {@code ResourceException}.
     *
     * @param code
     *            The HTTP error code.
     * @return A resource exception having the provided HTTP error code.
     * @deprecated in favor of {@link #newResourceException(int)}
     */
    @Deprecated
    public static ResourceException getException(final int code) {
        return newResourceException(code, null);
    }

    /**
     * Returns an exception with the specified HTTP error code and detail
     * message, but no cause, and a default reason phrase. Useful for
     * translating HTTP status codes to the relevant Java exception type. The
     * type of the returned exception will be a sub-type of
     * {@code ResourceException}.
     *
     * @param code
     *            The HTTP error code.
     * @param message
     *            The detail message.
     * @return A resource exception having the provided HTTP error code.
     * @deprecated in favor of {@link #newResourceException(int, String)}
     */
    @Deprecated
    public static ResourceException getException(final int code, final String message) {
        return newResourceException(code, message, null);
    }

    /**
     * Returns an exception with the specified HTTP error code, detail message,
     * and cause, and a default reason phrase. Useful for translating HTTP
     * status codes to the relevant Java exception type. The type of the
     * returned exception will be a sub-type of {@code ResourceException}.
     *
     * @param code
     *            The HTTP error code.
     * @param message
     *            The detail message.
     * @param cause
     *            The exception which caused this exception to be thrown.
     * @return A resource exception having the provided HTTP error code.
     * @deprecated in favor of {@link #newResourceException(int, String, Throwable)}
     */
    @Deprecated
    public static ResourceException getException(final int code, final String message,
            final Throwable cause) {
        return newResourceException(code, message, cause);
    }

    /**
     * Returns the reason phrase for an HTTP error status code, per RFC 2616 and
     * draft-nottingham-http-new-status-03. If no match is found, then a generic
     * reason {@code "Resource Exception"} is returned.
     */
    private static String reason(final int code) {
        String result = "Resource Exception"; // default
        switch (code) {
        case BAD_REQUEST:
            result = "Bad Request";
            break;
        case 401:
            result = "Unauthorized";
            break; // Missing or bad authentication (despite the name)
        case 402:
            result = "Payment Required";
            break;
        case FORBIDDEN:
            result = "Forbidden";
            break; // Authorization exceptions
        case NOT_FOUND:
            result = "Not Found";
            break;
        case 405:
            result = "Method Not Allowed";
            break;
        case 406:
            result = "Not Acceptable";
            break;
        case 407:
            result = "Proxy Authentication Required";
            break;
        case 408:
            result = "Request Time-out";
            break;
        case CONFLICT:
            result = "Conflict";
            break;
        case 410:
            result = "Gone";
            break;
        case 411:
            result = "Length Required";
            break;
        case VERSION_MISMATCH:
            result = "Precondition Failed";
            break;
        case 413:
            result = "Request Entity Too Large";
            break;
        case 414:
            result = "Request-URI Too Large";
            break;
        case 415:
            result = "Unsupported Media Type";
            break;
        case 416:
            result = "Requested range not satisfiable";
            break;
        case 417:
            result = "Expectation Failed";
            break;
        case VERSION_REQUIRED:
            result = "Precondition Required";
            break; // draft-nottingham-http-new-status-03
        case INTERNAL_ERROR:
            result = "Internal Server Error";
            break;
        case NOT_SUPPORTED:
            result = "Not Implemented";
            break;
        case 502:
            result = "Bad Gateway";
            break;
        case UNAVAILABLE:
            result = "Service Unavailable";
            break;
        case 504:
            result = "Gateway Time-out";
            break;
        case 505:
            result = "HTTP Version not supported";
            break;
        }
        return result;
    }

    /**
     * Returns the message which should be returned by {@link #getMessage()}.
     */
    private static String message(final int code, final String message, final Throwable cause) {
        if (message != null) {
            return message;
        } else if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        } else {
            return reason(code);
        }
    }

    /** The numeric code of the exception. */
    private final int code;

    /** The short reason phrase of the exception. */
    private String reason;

    /** Additional detail which can be evaluated by applications. */
    private JsonValue detail = new JsonValue(null);

    /** Resource API Version. */
    private Version resourceApiVersion;

    /**
     * Constructs a new exception with the specified exception code, and
     * {@code null} as its detail message. If the error code corresponds with a
     * known HTTP error status code, then the reason phrase is set to a
     * corresponding reason phrase, otherwise is set to a generic value
     * {@code "Resource Exception"}.
     *
     * @param code
     *            The numeric code of the exception.
     */
    protected ResourceException(final int code) {
        this(code, null, null);
    }

    /**
     * Constructs a new exception with the specified exception code and detail
     * message.
     *
     * @param code
     *            The numeric code of the exception.
     * @param message
     *            The detail message.
     */
    protected ResourceException(final int code, final String message) {
        this(code, message, null);
    }

    /**
     * Constructs a new exception with the specified exception code and detail
     * message.
     *
     * @param code
     *            The numeric code of the exception.
     * @param cause
     *            The exception which caused this exception to be thrown.
     */
    protected ResourceException(final int code, final Throwable cause) {
        this(code, null, cause);
    }

    /**
     * Constructs a new exception with the specified exception code, reason
     * phrase, detail message and cause.
     *
     * @param code
     *            The numeric code of the exception.
     * @param message
     *            The detail message.
     * @param cause
     *            The exception which caused this exception to be thrown.
     */
    protected ResourceException(final int code, final String message, final Throwable cause) {
        super(message(code, message, cause), cause);
        this.code = code;
        this.reason = reason(code);
    }

    /**
     * Returns the numeric code of the exception.
     *
     * @return The numeric code of the exception.
     */
    public final int getCode() {
        return code;
    }

    /**
     * Returns true if the HTTP error code is in the 500 range.
     *
     * @return <code>true</code> if HTTP error code is in the 500 range.
     */
    public boolean isServerError() {
        return code >= 500 && code <= 599;
    }

    /**
     * Returns the additional detail which can be evaluated by applications. By
     * default there is no additional detail (
     * {@code getDetail().isNull() == true}), and it is the responsibility of
     * the resource provider to add it if needed.
     *
     * @return The additional detail which can be evaluated by applications
     *         (never {@code null}).
     */
    public final JsonValue getDetail() {
        return detail;
    }

    /**
     * Returns the short reason phrase of the exception.
     *
     * @return The short reason phrase of the exception.
     */
    public final String getReason() {
        return reason;
    }

    /**
     * Sets the additional detail which can be evaluated by applications. By
     * default there is no additional detail (
     * {@code getDetail().isNull() == true}), and it is the responsibility of
     * the resource provider to add it if needed.
     *
     * @param detail
     *            The additional detail which can be evaluated by applications.
     * @return This resource exception.
     */
    public final ResourceException setDetail(JsonValue detail) {
        this.detail = detail != null ? detail : new JsonValue(null);
        return this;
    }

    /**
     * Sets/overrides the short reason phrase of the exception.
     *
     * @param reason
     *            The short reason phrase of the exception.
     * @return This resource exception.
     */
    public final ResourceException setReason(final String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Returns this ResourceException with the includeCause flag set to true
     * so that toJsonValue() method will include the cause if there is
     * one supplied.
     *
     * @return  the exception where this flag has been set
     */
    public final ResourceException includeCauseInJsonValue() {
        includeCause = true;
        return this;
    }

    /**
     * Returns the exception in a JSON object structure, suitable for inclusion
     * in the entity of an HTTP error response. The JSON representation looks
     * like this:
     *
     * <pre>
     * {
     *     "code"    : 404,
     *     "reason"  : "...",  // optional
     *     "message" : "...",  // required
     *     "detail"  : { ... } // optional
     *     "cause"   : { ... } // optional iff includeCause is set to true
     * }
     * </pre>
     *
     * @return The exception in a JSON object structure, suitable for inclusion
     *         in the entity of an HTTP error response.
     */
    public final JsonValue toJsonValue() {
        final Map<String, Object> result = new LinkedHashMap<>(4);
        result.put(FIELD_CODE, code); // required
        if (reason != null) { // optional
            result.put(FIELD_REASON, reason);
        }
        final String message = getMessage();
        if (message != null) { // should always be present
            result.put(FIELD_MESSAGE, message);
        }
        if (!detail.isNull()) {
            result.put(FIELD_DETAIL, detail.getObject());
        }
        if (includeCause && getCause() != null && getCause().getMessage() != null) {
            final Map<String, Object> cause = new LinkedHashMap<>(2);
            cause.put("message", getCause().getMessage());
            result.put(FIELD_CAUSE, cause);
        }
        return new JsonValue(result);
    }

    @Override
    public void setResourceApiVersion(Version version) {
        this.resourceApiVersion = version;
    }

    @Override
    public Version getResourceApiVersion() {
        return resourceApiVersion;
    }

    /**
     * Return this ResourceException as a Promise.
     *
     * @param <V> the result value type of the promise
     * @return an Exception promise of type ResourceException
     */
    public <V> Promise<V, ResourceException> asPromise() {
        return Promises.newExceptionPromise(this);
    }
}
