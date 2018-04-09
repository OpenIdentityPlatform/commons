/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All rights reserved.
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

package org.forgerock.script.engine;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resources;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public abstract class AbstractScriptEngine implements ScriptEngine {

    private static final List<String> IMPORTS = new ArrayList<String>();

    static {
        IMPORTS.add("org.forgerock.http.*");
        IMPORTS.add("org.forgerock.http.context.*");
        IMPORTS.add("org.forgerock.json.resource.*");
        IMPORTS.add("org.forgerock.json.*");
        IMPORTS.add("static " + Resources.class.getName() + ".*");
        IMPORTS.add("static " + Requests.class.getName() + ".*");
    }

    public static List<String> getImports() {
        return IMPORTS;
    }

    /**
     * This is the writer to be used to output from scripts. By default, a
     * <code>PrintWriter</code> based on <code>System.out</code> is used.
     * Accessor methods getWriter, setWriter are used to manage this field.
     *
     * @see java.lang.System#out
     * @see java.io.PrintWriter
     */
    protected Writer writer;

    /**
     * This is the writer to be used to output errors from scripts. By default,
     * a <code>PrintWriter</code> based on <code>System.err</code> is used.
     * Accessor methods getErrorWriter, setErrorWriter are used to manage this
     * field.
     *
     * @see java.lang.System#err
     * @see java.io.PrintWriter
     */
    protected Writer errorWriter;

    /**
     * This is the reader to be used for input from scripts. By default, a
     * <code>InputStreamReader</code> based on <code>System.in</code> is used
     * and default charset is used by this reader. Accessor methods getReader,
     * setReader are used to manage this field.
     *
     * @see java.lang.System#in
     * @see java.io.InputStreamReader
     */
    protected Reader reader;

    protected AbstractScriptEngine() {
        reader = new InputStreamReader(System.in);
        writer = new PrintWriter(System.out, true);
        errorWriter = new PrintWriter(System.err, true);
    }

    /** @see javax.script.ScriptContext#getWriter() */
    public Writer getWriter() {
        return writer;
    }

    /** @see javax.script.ScriptContext#getReader() */
    public Reader getReader() {
        return reader;
    }

    /** @see javax.script.ScriptContext#setReader(java.io.Reader) */
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /** @see javax.script.ScriptContext#setWriter(java.io.Writer) */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /** @see javax.script.ScriptContext#getErrorWriter() */
    public Writer getErrorWriter() {
        return errorWriter;
    }

    /** @see javax.script.ScriptContext#setErrorWriter(java.io.Writer) */
    public void setErrorWriter(Writer writer) {
        this.errorWriter = writer;
    }

    /** {@inheritDoc  */
    // public Map<String, Object> compileBindings(final Context context,
    // final Map<String, Object> request, Map<String, Object>... scopes) {

    //
    // Set<String> safeAttributes = null != request ? request.keySet() :
    // Collections.EMPTY_SET;
    // Map<String, Object> scope = new HashMap<String, Object>();
    // for (Map<String, Object> next : scopes) {
    // if (null == next)
    // continue;
    // for (Map.Entry<String, Object> entry : next.entrySet()) {
    // if (scope.containsKey(entry.getKey()) ||
    // safeAttributes.contains(entry.getKey())) {
    // continue;
    // }
    //
    //
    //
    // scope.put(entry.getKey(), entry.getValue());
    // }
    // }
    //
    // JsonValue requestScope = null;
    // if (null != request) {
    // // Make a deep copy and merge
    // request.putAll(scope.copy().asMap());
    // requestScope = new JsonValue(request);
    // } else {
    // // Make a deep copy
    // requestScope = scope.copy();
    // }
    // requestScope.getTransformers().add(getOperationParameter(context));

    // return new SimpleBindings(new JsonValueMap(requestScope));
    // }

    /** {@inheritDoc  */
    public Object compileObject(final Context context, Object value) {
        // JsonValue temp = new JsonValue(value);
        // temp.getTransformers().add(getOperationParameter(context));
        // temp = temp.copy();
        // return temp.getObject();

        return null;
    }

    /**
     * Gets the {@code null} object representation.
     * <p/>
     * If the {@code null} object has special representation in the script scope
     * this method returns with that object.
     *
     * @return {@code null} or representation of {@code null} object.
     */
    protected Object getNull() {
        return null;
    }

    // protected abstract OperationParameter getOperationParameter(final Context
    // context);
}
