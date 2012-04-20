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



