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

package org.forgerock.json.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.forgerock.http.ServerContext;
import org.forgerock.json.resource.annotations.Create;
import org.forgerock.json.resource.annotations.Patch;
import org.forgerock.json.resource.annotations.Query;
import org.forgerock.json.resource.annotations.Update;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * Represents an annotated (or conventionally named) CREST method on an object request handler.
 * <p>
 * Methods for finding the appropriate annotated methods can be used by the {@link RequestHandler}
 * implementations for annotated classes. The returned instances can then be used to invoke the
 * found method.
 * <p>
 * If no appropriately annotated method is found, an attempt to invoke that method will result in
 * it being handled with a {@link NotSupportedException}.
 */
final class AnnotatedMethod {
    private final Object requestHandler;
    private final Method method;
    private final int idParameter;
    private final int contextParameter;
    private final int requestParameter;
    private final int queryHandlerParameter;
    private final int numberOfParameters;
    private final String operation;

    AnnotatedMethod(String operation, Object requestHandler, Method method, int idParameter, int contextParameter,
            int requestParameter, int queryHandlerParameter, int numberOfParameters) {
        this.operation = operation;
        this.requestHandler = requestHandler;
        this.method = method;
        this.idParameter = idParameter;
        this.contextParameter = contextParameter;
        this.requestParameter = requestParameter;
        this.queryHandlerParameter = queryHandlerParameter;
        this.numberOfParameters = numberOfParameters;
    }

    <T> Promise<T, ? extends ResourceException> invoke(ServerContext context, Request request, String id) {
        return invoke(context, request, null, id);
    }

    <T> Promise<T, ? extends ResourceException> invoke(ServerContext context, Request request,
            QueryResultHandler queryHandler, String id) {
        if (method == null) {
            return Promises.newFailedPromise(new NotSupportedException(operation + " not supported"));
        }
        Object[] args = new Object[numberOfParameters];
        if (idParameter > -1) {
            args[idParameter] = id;
        }
        if (requestParameter > -1) {
            args[requestParameter] = request;
        }
        if (contextParameter > -1) {
            args[contextParameter] = context;
        }
        if (queryHandlerParameter > -1) {
            args[queryHandlerParameter] = queryHandler;
        }
        try {
            return (Promise<T, ? extends ResourceException>) method.invoke(requestHandler, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access the annotated method: " + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Exception from invocation expected to be handled by promise", e);
        }
    }

    static AnnotatedMethod findMethod(Object requestHandler, Class<? extends Annotation> annotation, boolean needsId) {
        for (Method method : requestHandler.getClass().getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                AnnotatedMethod checked = checkMethod(annotation, requestHandler, method, needsId);
                if (checked != null) {
                    return checked;
                }
            }
        }
        for (Method method : requestHandler.getClass().getMethods()) {
            if (method.getName().equals(annotation.getSimpleName().toLowerCase())) {
                AnnotatedMethod checked = checkMethod(annotation, requestHandler, method, needsId);
                if (checked != null) {
                    return checked;
                }
            }
        }
        return new AnnotatedMethod(annotation.getSimpleName(), null, null, -1, -1, -1, -1, -1);
    }

    static AnnotatedMethod checkMethod(Class<?> annotation, Object requestHandler, Method method, boolean needsId) {
        if (Promise.class.equals(method.getReturnType())) {
            int idParam = -1;
            int contextParam = -1;
            int requestParam = -1;
            int queryHandlerParam = -1;
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                Class<?> type = method.getParameterTypes()[i];
                if (String.class.equals(type)) {
                    idParam = i;
                } else if (ServerContext.class.equals(type)) {
                    contextParam = i;
                } else if (Request.class.isAssignableFrom(type)) {
                    requestParam = i;
                } else if (type.isAssignableFrom(QueryResultHandler.class)) {
                    queryHandlerParam = i;
                }
            }
            if (Arrays.asList(Create.class, Update.class, Patch.class, Query.class).contains(annotation) &&
                    requestParam == -1) {
                return null;
            }
            if (queryHandlerParam == -1 && Query.class.equals(annotation) ||
                    queryHandlerParam != -1 && !Query.class.equals(annotation)) {
                return null;
            }
            if (!needsId || idParam > -1) {
                return new AnnotatedMethod(annotation.getSimpleName(), requestHandler, method, idParam, contextParam,
                        requestParam, queryHandlerParam, method.getParameterTypes().length);
            }
        }
        return null;
    }
}
