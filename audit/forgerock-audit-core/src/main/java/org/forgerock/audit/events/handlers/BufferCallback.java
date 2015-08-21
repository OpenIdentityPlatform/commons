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
package org.forgerock.audit.events.handlers;

import java.util.List;

/**
 * A callback for the Buffer.
 *
 * @param <T>
 *            The type of the elements in the buffer.
 */
public interface BufferCallback<T> {

    /**
     * This method is called when the buffer is flushed, providing the elements in the buffer that have been flushed.
     * <p>
     * The implementation of this method must be thread-safe because it can be called by multiple threads.
     *
     * @param elements
     *          The list of elements flushed from the buffer.
     */
    void bufferFlush(List<T> elements);

}
