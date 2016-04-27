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
 * Copyright 2015-2016 ForgeRock AS.
 */

/**
 * An implementation of the {@code Promise} API in Java. Promises provide a simple
 * API for chaining together asynchronous tasks and result transformation.
 * <p>
 * Example using CREST:
 *
 * <pre>
 * final ConnectionFactory server = getConnectionFactory();
 * final AtomicReference&lt;Connection&gt; connectionHolder = new AtomicReference&lt;Connection&gt;();
 * final Promise&lt;Resource, ResourceException&gt; promise = server.getConnectionAsync()
 *     .thenAsync(new AsyncFunction&lt;Connection, Resource, ResourceException&gt;() {
 *         // Read resource.
 *         public Promise&lt;Resource, ResourceException&gt; apply(final Connection connection)
 *                 throws ResourceException {
 *             connectionHolder.set(connection); // Save connection for later.
 *             return connection.readAsync(ctx(), Requests.newReadRequest(&quot;users/1&quot;));
 *         }
 *     }).thenAsync(new AsyncFunction&lt;Resource, Resource, ResourceException&gt;() {
 *         // Update resource.
 *         public Promise&lt;Resource, ResourceException&gt; apply(final Resource user) throws ResourceException {
 *             return connectionHolder.get().updateAsync(ctx(),
 *                     Requests.newUpdateRequest(&quot;users/1&quot;, userAliceWithIdAndRev(1, 1)));
 *         }
 *     }).then(new SuccessHandler&lt;Resource&gt;() {
 *         // Check updated resource.
 *         public void handleResult(final Resource user) {
 *             // Update successful!
 *         }
 *     }).thenAlways(new Runnable() {
 *         // Close the connection.
 *         public void run() {
 *             final Connection connection = connectionHolder.get();
 *             if (connection != null) {
 *                 connection.close();
 *             }
 *         }
 *     });
 * </pre>
 *
 * @see <a href="http://promises-aplus.github.io/promises-spec/">Promises/A+</a>
 */
package org.forgerock.util.promise;

