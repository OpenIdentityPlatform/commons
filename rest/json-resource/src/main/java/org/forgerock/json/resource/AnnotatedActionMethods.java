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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.http.ServerContext;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.annotations.Action;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;

/**
 * This class is used to find all methods annotated with {@link Action}, and provides
 * a method for invoking the appropriate action method or handling the failure case
 * for when an action isn't supported - i.e. no matching annotated method exists.
 */
class AnnotatedActionMethods {

    private Map<String, AnnotatedMethod> methods = new HashMap<String, AnnotatedMethod>();

    Promise<JsonValue, ? extends ResourceException> invoke(ServerContext context, ActionRequest request, String id) {
        AnnotatedMethod method = methods.get(request.getAction());
        if (method == null) {
            return Promises.newFailedPromise(new NotSupportedException(request.getAction() + "not supported"));
        }
        return method.invoke(context, request, id);
    }

    static AnnotatedActionMethods findAll(Object requestHandler, boolean needsId) {
        AnnotatedActionMethods methods = new AnnotatedActionMethods();
        for (Method method : requestHandler.getClass().getMethods()) {
            Action action = method.getAnnotation(Action.class);
            if (action != null) {
                AnnotatedMethod checked = AnnotatedMethod.checkMethod(Action.class, requestHandler, method, needsId);
                if (checked != null) {
                    String actionName = action.value();
                    if (actionName == null || actionName.length() == 0) {
                        actionName = method.getName();
                    }
                    methods.methods.put(actionName, checked);
                }
            }
        }
        return methods;
    }

}
