/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 ForgeRock AS. All rights reserved.
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
 * Portions copyright 2026 3A Systems LLC.
 */

package org.forgerock.script.groovy;

import groovy.lang.GroovySystem;
import org.forgerock.script.engine.ScriptEngine;
import org.forgerock.script.engine.ScriptEngineFactory;
import org.forgerock.script.source.SourceContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public class GroovyScriptEngineFactory implements ScriptEngineFactory {

    private static List<String> names;
    private static List<String> mimeTypes;
    private static List<String> extensions;

    public static final String LANGUAGE_NAME = "Groovy";

    private volatile GroovyScriptEngineImpl engine = null;

    static {
        names = new ArrayList<String>(2);
        names.add("groovy");
        names.add("Groovy");
        names = Collections.unmodifiableList(names);

        mimeTypes = new ArrayList<String>(1);
        mimeTypes.add("application/x-groovy");
        mimeTypes = Collections.unmodifiableList(mimeTypes);

        extensions = new ArrayList<String>(1);
        extensions.add("groovy");
        extensions = Collections.unmodifiableList(extensions);
    }

    @Override
    public String getEngineName() {
        return "Groovy Scripting Engine";
    }

    @Override
    public String getEngineVersion() {
        return "2.0";
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return GroovySystem.getVersion();
    }

    @Override
    public ScriptEngine getScriptEngine(Map<String, Object> configuration, Collection<SourceContainer> sourceContainers,
            ClassLoader registryLevelClassLoader) {
        if (null == engine) {
            synchronized (this) {
                if (null == engine) {
                    // TODO use registry-level class loader
                    engine = new GroovyScriptEngineImpl(configuration, this);
                }
            }
        }
        return engine;
    }
}
