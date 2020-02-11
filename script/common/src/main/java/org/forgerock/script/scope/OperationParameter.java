/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.scope;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;

/**
 * OperationParameter holds the Context associated with the request that invoked a script.
 */
public class OperationParameter implements Parameter {

    protected final Context context;

    public OperationParameter(final Context context) {
        this.context = context;
    }

    /**
     * Returns the internal context.
     * 
     * @return The internal context..
     * @throws org.forgerock.json.resource.NotFoundException
     *             If no such connection exists.
     * @throws ResourceException
     *             If the connection could not be obtained for some other reason
     *             (e.g. due to a configuration or initialization problem).
     */
    public Context getContext(Context savedContext) throws ResourceException {

        if (null != savedContext) {
            return savedContext;
        } else if (null != context) {
            return context;
        }
        throw new InternalServerErrorException("Failed to get Context.");
    }

}
