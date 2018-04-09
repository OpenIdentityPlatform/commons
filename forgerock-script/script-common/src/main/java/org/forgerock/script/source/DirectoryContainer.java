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
import org.forgerock.script.engine.ScriptEngineFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 * A DirectoryContainer loads the scrips from a directory.
 *
 * @author Laszlo Hordos
 */
public class DirectoryContainer implements SourceContainer, ScriptEngineFactoryAware {

    private final ScriptEntry.Visibility visibility = ScriptEntry.Visibility.DEFAULT;
    private final ScriptName unitName;
    private final URL context;
    private final URI contextURI;
    private final boolean subdirectories = true;

    Iterable<ScriptEngineFactory> factories = null;

    /*
     * "location" : { "directory" : "script/public", "subdirectories" : "true",
     * "visibility" : "public", "type" : "auto-detect" }
     */

    public DirectoryContainer(String name, URL context) throws URISyntaxException {
        this.unitName = new ScriptName(name, SourceUnit.AUTO_DETECT);
        this.context = context;
        this.contextURI = context.toURI();
    }

    public ScriptSource findScriptSource(ScriptName name) {
        URL sourceURL = getSourceFile(name.getName(), null);
        if (null == sourceURL) {
            List<String> extensions = getExtensions(name);
            if (null != extensions) {
                for (String extension : extensions) {
                    sourceURL = getSourceFile(name.getName(), extension);
                }
            }
        }
        if (null != sourceURL) {
            try {
                return new URLScriptSource(getVisibility(), sourceURL, name, this);
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

    public ScriptEntry.Visibility getVisibility() {
        return visibility;
    }

    private URL getSourceFile(final String name, final String extension) {
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
            public URL run() {
                try {
                    String filename =
                            extension == null ? name : name.replace('.', File.separatorChar) + "."
                                    + extension;
                    URL ret = getResource(filename);
                    if (isFile(ret) && getFileForUrl(ret, filename) != null) {
                        return ret;
                    }
                } catch (Throwable t) {
                    /* ignore */
                }
                return null;
            }
        });
    }

    private List<String> getExtensions(ScriptName name) {
        // TODO Use a Util class to find the engine!!
        if (null != factories) {
            for (ScriptEngineFactory factory : factories) {
                if (factory.getNames().contains(name.getType())) {
                    return factory.getExtensions();
                }
            }
        }
        return null;
    }

    private URL getResource(String name) {
        try {
            return new URL(context, name);
        } catch (MalformedURLException e) {
            /* ignore */
            return null;
        }
    }

    private boolean isFile(URL ret) {
        return ret != null && ret.getProtocol().equals("file");
    }

    private File getFileForUrl(URL ret, String filename) {
        String fileWithoutPackage = filename;
        if (fileWithoutPackage.indexOf('/') != -1) {
            int index = fileWithoutPackage.lastIndexOf('/');
            fileWithoutPackage = fileWithoutPackage.substring(index + 1);
        }
        return fileReallyExists(ret, fileWithoutPackage);
    }

    private File fileReallyExists(URL ret, String fileWithoutPackage) {
        File path = new File(decodeFileName(ret.getFile())).getParentFile();
        if (path.exists() && path.isDirectory()) {
            File file = new File(path, fileWithoutPackage);
            if (file.exists()) {
                // file.exists() might be case insensitive. Let's do
                // case sensitive match for the filename
                File parent = file.getParentFile();
                for (String child : parent.list()) {
                    if (child.equals(fileWithoutPackage)) {
                        return file;
                    }
                }
            }
        }
        // file does not exist!
        return null;
    }

    /**
     * This method will take a file name and try to "decode" any URL encoded
     * characters. For example if the file name contains any spaces this method
     * call will take the resulting %20 encoded values and convert them to
     * spaces.
     */
    private String decodeFileName(String fileName) {
        String decodedFile = fileName;
        try {
            decodedFile = URLDecoder.decode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /* ignore for now */
        }
        return decodedFile;
    }

    public void setScriptEngineFactory(final Iterable<ScriptEngineFactory> factory) {
        this.factories = factory;
    }

    private static URL[] createRoots(String[] urls) throws MalformedURLException {
        if (urls == null) {
            return null;
        }
        URL[] roots = new URL[urls.length];
        for (int i = 0; i < roots.length; i++) {
            if (urls[i].indexOf("://") != -1) {
                roots[i] = new URL(urls[i]);
            } else {
                roots[i] = new File(urls[i]).toURI().toURL();
            }
        }
        return roots;
    }

    public URL getSource() {
        return context;
    }

    public URI getSourceURI() {
        return contextURI;
    }

    public SourceContainer getParentContainer() {
        return null;
    }
}
