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

package org.forgerock.util.generator;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.forgerock.util.annotations.VisibleForTesting;

/**
 * Defines the contract to generate global unique identifiers.
 */
public interface IdGenerator {

    /**
     * Returns a new globally unique identifier.
     * @return a new globally unique identifier.
     */
    String generate();

    /**
     * The default implementation of {@link IdGenerator}.
     */
    IdGenerator DEFAULT = new SequenceUuidIdGenerator(UUID.randomUUID());

    /**
     * Default implementation of the {@link IdGenerator} that will output some ids based on the following pattern :
     * {@code <uuid> + '-' + an incrementing sequence}.
     */
    final class SequenceUuidIdGenerator implements IdGenerator {

        private final String prefix;
        private final AtomicLong sequence = new AtomicLong();

        @VisibleForTesting
        SequenceUuidIdGenerator(UUID uuid) {
            this.prefix = uuid.toString();
        }

        @Override
        public String generate() {
            return prefix + '-' + sequence.getAndIncrement();
        }
    }
}
