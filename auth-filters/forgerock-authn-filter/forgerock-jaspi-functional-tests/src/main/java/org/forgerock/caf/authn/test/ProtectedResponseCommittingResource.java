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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.caf.authn.test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A sub-type of the {@code ProtectedResource} which performs the same operations but in addition also sets the
 * response status to 201 and causes the response to be committed.
 *
 * @since 1.5.0
 */
public class ProtectedResponseCommittingResource extends ProtectedResource {

    private static final long serialVersionUID = 1L;

    /**
     * Calls {@code super}, then sets the response status to 201, and causes the response to be committed.
     *
     * @param req {@inheritDoc}
     * @param resp {@inheritDoc}
     * @throws ServletException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
        resp.setStatus(201);
        resp.flushBuffer();
    }
}
