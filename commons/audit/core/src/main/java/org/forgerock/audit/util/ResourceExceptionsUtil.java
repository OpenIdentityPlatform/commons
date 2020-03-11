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

package org.forgerock.audit.util;

import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;

/**
 * Utility class to use on ResourceExceptions.
 */
public final class ResourceExceptionsUtil {

    private ResourceExceptionsUtil() { }
    /**
     * Adapts a {@code Throwable} to a {@code ResourceException}. If the
     * {@code Throwable} is an JSON {@code JsonValueException} then an
     * appropriate {@code ResourceException} is returned, otherwise an
     * {@code InternalServerErrorException} is returned.
     *
     * @param t
     *            The {@code Throwable} to be converted.
     * @return The equivalent resource exception.
     */
    public static ResourceException adapt(final Throwable t) {
        int resourceResultCode;
        try {
            throw t;
        } catch (final ResourceException e) {
            return e;
        } catch (final JsonValueException e) {
            resourceResultCode = ResourceException.BAD_REQUEST;
        } catch (final Throwable tmp) {
            resourceResultCode = ResourceException.INTERNAL_ERROR;
        }
        return ResourceException.getException(resourceResultCode, t.getMessage(), t);
    }

    /**
     * Creates a NotSupportedException.
     * @param request the crest request
     * @return a NotSupportedException
     */
    public static ResourceException notSupported(final Request request) {
        return new NotSupportedException(request.getRequestType().name() + " operations are not supported");
    }

    /**
     * Creates a NotSupportedException.
     * @param request the crest request
     * @return a NotSupportedException
     */
    public static ResourceException notSupportedOnCollection(final Request request) {
        return new NotSupportedException(request.getRequestType().name() + " operations are not supported");
    }

    /**
     * Creates a NotSupportedException.
     * @param request the crest request
     * @return a NotSupportedException
     */
    public static ResourceException notSupportedOnInstance(final Request request) {
        return new NotSupportedException(request.getRequestType().name() + " operations are not supported");
    }
}
