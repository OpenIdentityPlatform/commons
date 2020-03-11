/*
* Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.  Oracle designates this
* particular file as subject to the "Classpath" exception as provided
* by Oracle in the LICENSE file that accompanied this code.
*
* This code is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
* version 2 for more details (a copy is included in the LICENSE file that
* accompanied this code).
*
* You should have received a copy of the GNU General Public License version
* 2 along with this work; if not, write to the Free Software Foundation,
* Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
*
* Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
* or visit www.oracle.com if you need additional information or have any
* questions.
*/

/*
 * Portions Copyrighted 2012-2014 ForgeRock AS
 */

package org.forgerock.script.javascript;

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
public class RhinoScriptEngineFactory implements ScriptEngineFactory {

    public static final String VERSION = "1.7 release 4";

    public static final String LANGUAGE_NAME = "ECMAScript";

    private static List<String> names;
    private static List<String> mimeTypes;
    private static List<String> extensions;

    private RhinoScriptEngine engine = null;

    static {
        names = new ArrayList(6);
        names.add("js");
        names.add("rhino");
        names.add("JavaScript");
        names.add("javascript");
        names.add("ECMAScript");
        names.add("ecmascript");
        names = Collections.unmodifiableList(names);

        mimeTypes = new ArrayList(4);
        mimeTypes.add("application/javascript");
        mimeTypes.add("application/ecmascript");
        mimeTypes.add("text/javascript");
        mimeTypes.add("text/ecmascript");
        mimeTypes = Collections.unmodifiableList(mimeTypes);

        extensions = new ArrayList(1);
        extensions.add("js");
        extensions = Collections.unmodifiableList(extensions);
    }

    public String getEngineName() {
        return "javascript";
    }

    public String getEngineVersion() {
        return VERSION;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public List<String> getNames() {
        return names;
    }

    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    public String getLanguageVersion() {
        return "1.8";
    }

    public ScriptEngine getScriptEngine(
            final Map<String, Object> configuration,
            final Collection<SourceContainer> sourceContainers,
            final ClassLoader registryLevelClassLoader) {
        if (null == engine) {
            synchronized (this) {
                if (null == engine) {
                    engine = new RhinoScriptEngine(configuration, this, sourceContainers, registryLevelClassLoader);
                }
            }
        }
        return engine;
    }
}
