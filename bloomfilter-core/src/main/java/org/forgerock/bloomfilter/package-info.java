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

/**
 * Implementations of thread-safe, scalable and rolling Bloom Filters. These are Set-like data structures that can
 * scale to very large numbers of entries while only using a small amount of memory (a few bits) per element. The
 * trade-off is that the set membership operation may report false positives (i.e., it may claim that an item is a
 * member of the set when it isn't). The probability of false positives can be tuned by increasing the amount of
 * memory used.
 */
package org.forgerock.bloomfilter;