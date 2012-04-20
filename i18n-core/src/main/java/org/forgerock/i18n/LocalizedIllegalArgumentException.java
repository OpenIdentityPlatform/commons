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
 *      Copyright 2009 Sun Microsystems, Inc.
 *      Portions copyright 2011 ForgeRock AS
 */

package org.forgerock.i18n;

/**
 * Thrown to indicate that a method has been passed an illegal or inappropriate
 * argument.
 * <p>
 * A {@code LocalizedIllegalArgumentException} contains a localized error
 * message which may be used to provide the user with detailed diagnosis
 * information. The localized message can be retrieved using the
 * {@link #getMessageObject} method.
 */
public class LocalizedIllegalArgumentException extends IllegalArgumentException
        implements LocalizableException {

    /**
     * Generated serialization ID.
     */
    private static final long serialVersionUID = -8512235024837904757L;

    // The I18N message associated with this exception.
    private final LocalizableMessage message;

    /**
     * Creates a new localized illegal argument exception with the provided
     * message.
     *
     * @param message
     *            The message that explains the problem that occurred.
     */
    public LocalizedIllegalArgumentException(final LocalizableMessage message) {
        super(String.valueOf(message));
        this.message = message;
    }

    /**
     * Creates a new localized illegal argument exception with the provided
     * message and cause.
     *
     * @param message
     *            The message that explains the problem that occurred.
     * @param cause
     *            The cause which may be later retrieved by the
     *            {@link #getCause} method. A {@code null} value is permitted,
     *            and indicates that the cause is nonexistent or unknown.
     */
    public LocalizedIllegalArgumentException(final LocalizableMessage message,
            final Throwable cause) {
        super(String.valueOf(message), cause);
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    public final LocalizableMessage getMessageObject() {
        return this.message;
    }
}
