/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.javascript;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public class SLF4JErrorReporter implements ErrorReporter {

    private static final Logger logger = LoggerFactory.getLogger(SLF4JErrorReporter.class);
    private final ErrorReporter chainedReporter;

    public static ErrorReporter chain(ErrorReporter chainedReporter) {
        return new SLF4JErrorReporter(chainedReporter);
    }

    public SLF4JErrorReporter(ErrorReporter chainedReporter) {
        this.chainedReporter = chainedReporter;
    }

    public void warning(String message, String sourceName, int line, String lineSource,
            int lineOffset) {
        logger.warn("", new Object[] { message, sourceName, line, lineSource, lineOffset });
        if (chainedReporter != null) {
            chainedReporter.warning(message, sourceName, line, lineSource, lineOffset);
        }
    }

    public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
        logger.error("", new Object[] { message, sourceName, line, lineSource, lineOffset });
        if (chainedReporter != null) {
            chainedReporter.error(message, sourceName, line, lineSource, lineOffset);
        } else {
            throw runtimeError(message, sourceName, line, lineSource, lineOffset);
        }
    }

    public EvaluatorException runtimeError(String message, String sourceName, int line,
            String lineSource, int lineOffset) {
        if (chainedReporter != null) {
            return chainedReporter.runtimeError(message, sourceName, line, sourceName, lineOffset);
        } else {
            return new EvaluatorException(message, sourceName, line, sourceName, lineOffset);
        }
    }
}
