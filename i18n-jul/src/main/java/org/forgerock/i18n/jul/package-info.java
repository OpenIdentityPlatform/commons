/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal/CDDLv1_0.txt or
 * http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *      Copyright 2011 ForgeRock AS
 */

/**
 * This package provides a localization mechanism for
 * {@code java.util.logging}. Using the ForgeRock I18N framework for logging
 * ensures that message type safety is enforced at compile time.
 * <p>
 * Example usage:
 * <pre>
 *
 * import static com.example.AppMessages.EXAMPLE_MESSAGE;
 *
 * ...
 *
 * // EXAMPLE_MESSAGE has parameters String and Integer
 * LocalizedLogger logger = LocalizedLogger.getLocalizedLogger("mylogger");
 * logger.warning(EXAMPLE_MESSAGE, "a string", 123);
 * </pre>
 */
package org.forgerock.i18n.jul;



