/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock AS. All rights reserved.
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

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;

import javax.script.ScriptException;
import java.util.Map;

import static org.forgerock.json.resource.ResourceException.FIELD_CODE;
import static org.forgerock.json.resource.ResourceException.FIELD_DETAIL;
import static org.forgerock.json.resource.ResourceException.FIELD_MESSAGE;
import static org.forgerock.json.resource.ResourceException.getException;

/**
 * An exception that is thrown to indicate that an executed script encountered
 * an exception.
 *
 * @author Paul C. Bryan
 */
public class ScriptThrownException extends ScriptException {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /** Value that was thrown by the script. */
    private Object value;

    /**
     * Constructs a new exception with the specified value and detail message.
     */
    public ScriptThrownException(String message, Object value) {
        super(message);
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified value and cause.
     */
    public ScriptThrownException(Exception e, Object value) {
        super(e);
        this.value = value;
    }

    public ScriptThrownException(String message, String fileName, int lineNumber, Object value) {
        super(message, fileName, lineNumber);
        this.value = value;
    }

    public ScriptThrownException(String message, String fileName, int lineNumber, int columnNumber,
            Object value) {
        super(message, fileName, lineNumber, columnNumber);
        this.value = value;
    }

    /**
     * Returns the value that was thrown from the script.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Converts the script exception to an appropriate json resource exception.
     *
     * The exception message is set to, in order of precedence 1. Specific
     * message set in the thrown script exception 2. Default exception supplied
     * to this method, or if null 3. value toString of this exception
     *
     * @param defaultMsg
     *            a default message to use if no explicit message is set, or
     *            null if value toString shoudl be used instead
     * @return the appropriate JsonResourceException
     * @throws org.forgerock.json.JsonValueException
     *             when value can not be converted to ResourceException
     */
    public ResourceException toResourceException(int defaultCode, String defaultMsg) {
        if (value instanceof ResourceException) {
            return (ResourceException) value;
        }
        if (value instanceof Map) {
            // Convention on structuring well known exceptions with value that
            // contains
            // code : Integer matching ResourceException codes
            // (required for it to be considered a known exception definition)
            // reason : String - optional exception reason, not set this use the
            // default value
            // message : String - optional exception message, set to
            // value.toString if not present
            // detail : Map<String, Object> - optional structure with exception
            // detail
            // cause : Throwable - optional cause to chain
            JsonValue val = new JsonValue(value);
            Integer openidmCode = val.get(FIELD_CODE).asInteger();
            if (openidmCode != null) {
                String message = val.get(FIELD_MESSAGE).asString();
                if (message == null) {
                    if (defaultMsg != null) {
                        message = defaultMsg;
                    } else {
                        message = String.valueOf(value);
                    }
                }
                JsonValue failureDetail = val.get(FIELD_DETAIL);
                Throwable throwable = (Throwable) val.get("cause").getObject();
                if (throwable == null) {
                    throwable = this;
                }
                return getException(openidmCode.intValue(), message, throwable).setDetail(
                        failureDetail);

            }
        }
        if (defaultMsg != null) {
            return getException(defaultCode, defaultMsg, this);
        } else if (value == null) {
            return getException(defaultCode, null, this);
        } else {
            return getException(defaultCode, String.valueOf(value), this);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + getValue().toString();
    }
}
