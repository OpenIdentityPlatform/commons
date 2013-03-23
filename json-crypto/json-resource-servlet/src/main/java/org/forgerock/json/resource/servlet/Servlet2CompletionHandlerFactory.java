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

import static org.forgerock.json.resource.servlet.HttpUtils.fail;

import java.util.concurrent.CountDownLatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A factory which creates blocking completion handlers suitable for use in
 * Servlet 2.x containers or when asynchronous dispatch is not supported.
 */
final class Servlet2CompletionHandlerFactory extends CompletionHandlerFactory {

    /**
     * Completion handler implementation. Package private because it is used as
     * the fall-back implementation in Servlet 3 when async is not supported.
     */
    final static class Servlet2Impl implements CompletionHandler {
        private final HttpServletRequest httpRequest;
        private final HttpServletResponse httpResponse;
        private final CountDownLatch latch = new CountDownLatch(1);

        Servlet2Impl(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
            this.httpRequest = httpRequest;
            this.httpResponse = httpResponse;
        }

        @Override
        public void addCompletionListener(final Runnable runnable) {
            throw new IllegalStateException();
        }

        @Override
        public void awaitIfNeeded() throws Exception {
            latch.await();
        }

        @Override
        public boolean isAsynchronous() {
            return false;
        }

        @Override
        public void onComplete() {
            latch.countDown();
        }

        @Override
        public void onError(final Throwable t) {
            fail(httpRequest, httpResponse, t);
            latch.countDown();
        }

    }

    Servlet2CompletionHandlerFactory() {
        // Nothing to do.
    }

    @Override
    public CompletionHandler createCompletionHandler(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) {
        return new Servlet2Impl(httpRequest, httpResponse);
    }

}
