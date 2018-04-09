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
 */

package org.forgerock.script.source;

import org.forgerock.script.ScriptEntry;
import org.forgerock.script.ScriptName;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;

/**
 * A script whose source is not in a file, or resource, but embedded in
 * configuration string, database entry, or other "in-line" entity.
 *
 * @author Laszlo Hordos
 */
public class EmbeddedScriptSource implements ScriptSource {

    private final ScriptEntry.Visibility visibility;
    private final String source;
    private final ScriptName scriptName;

    public EmbeddedScriptSource(String source, ScriptName scriptName) {
        this(ScriptEntry.Visibility.DEFAULT, source, scriptName);
    }

    public EmbeddedScriptSource(ScriptEntry.Visibility visibility, String source,
            ScriptName scriptName) {
        this.visibility = visibility;
        this.source = source;
        this.scriptName = new ScriptName(
                scriptName.getName(), scriptName.getType(), scriptName.getRevision(), scriptName.getRequestBinding());
    }

    public String guessType() {
        return scriptName.getType();
    }

    public URL getSource() {
        // TODO Cache the source and get that URL. The cache should be centralised.
        return null;
    }

    public URI getSourceURI() {
        // TODO Cache the source and get that URL. The cache should be centralised.
        return null;
    }

    public Reader getReader() throws IOException {
        return new StringReader(source);
    }

    public ScriptName getName() {
        return scriptName;
    }

    public ScriptEntry.Visibility getVisibility() {
        return visibility;
    }

    public ScriptName[] getDependencies() {
        return new ScriptName[0];
    }

    public SourceContainer getParentContainer() {
        return null;
    }
}
