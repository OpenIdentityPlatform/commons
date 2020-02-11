/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014
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
import org.osgi.framework.Bundle;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A BundleContainer loads the scripts from an OSGi bundle.
 *
 * @author Laszlo Hordos
 */
public class BundleContainer implements SourceContainer {

    private final ScriptName unitName;
    private final Bundle bundle;
    private final URL source;
    private final URI sourceURI;
    private final ScriptEntry.Visibility visibility = ScriptEntry.Visibility.DEFAULT;

    public BundleContainer(String unitName, Bundle bundle) throws URISyntaxException {
        this.unitName = new ScriptName(unitName, AUTO_DETECT);
        this.bundle = bundle;
        this.source = bundle.getResource("/");
        this.sourceURI = source.toURI();
    }

    public ScriptSource findScriptSource(ScriptName name) {
        URL source = bundle.getResource(name.getName());
        if (source != null) {
            try {
                return new URLScriptSource(getVisibility(), source, name, this);
            } catch (URISyntaxException e) {
                // return null (below) as URLScriptSource was not able to convert
                // source to a URI.  In practice, this should never be triggered
                // as this object's constructor does the same thing and will
                // throw an exception at instantiation
            }
        }
        return null;
    }

    public ScriptName getName() {
        return unitName;
    }

    public URL getSource() {
        return source;
    }

    public URI getSourceURI() {
        return sourceURI;
    }

    public ScriptEntry.Visibility getVisibility() {
        return visibility;
    }

    public SourceContainer getParentContainer() {
        return null;
    }
}
