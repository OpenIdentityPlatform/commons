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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2010–2011 ApexIdentity Inc. All rights reserved.
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

/**
 * Provides an API for the traversal and manipulation of JSON object model structures in Java.
 * <p>
 * Unlike typical Java JSON libraries, org.forgerock.json is not involved in the serial
 * representation of JSON; rather, it focuses exclusively on processing Java structures
 * composed of {@code Map}, {@code List}, {@code String}, {@code Number} and {@code Boolean}
 * objects.
 * <p>
 * Methods and utilities for computing differences and patching JSON structures are also included.
 */
package org.forgerock.json;
