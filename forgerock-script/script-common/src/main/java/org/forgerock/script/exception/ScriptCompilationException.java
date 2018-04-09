/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.script.exception;

import javax.script.ScriptException;


/**
 * An exception that is thrown to indicate that the script compilation failed.
 */
public class ScriptCompilationException extends ScriptException {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public ScriptCompilationException(String message, Exception cause) {
        super(message);
        initCause(cause);
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public ScriptCompilationException(String message, Exception cause, String fileName, int lineNumber, int columnNumber) {
        super(message, fileName, lineNumber, columnNumber);
        initCause(cause);
    }
}
