/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.script.registry;

import java.util.ArrayList;
import java.util.List;

/**
 * A NAME does ...
 *
 * @author Laszlo Hordos
 */
public final class ThreadClassLoaderManager {

    private static ThreadLocal<ThreadClassLoaderManager> instance =
            new ThreadLocal<ThreadClassLoaderManager>() {
                public ThreadClassLoaderManager initialValue() {
                    return new ThreadClassLoaderManager();
                }
            };

    private final List<ClassLoader> loaderStack = new ArrayList<ClassLoader>();

    private ThreadClassLoaderManager() {

    }

    /**
     * Returns the thread-local instance of the manager.
     *
     * @return
     */
    public static ThreadClassLoaderManager getInstance() {
        return instance.get();
    }

    /**
     * Sets the given loader as the thread-local classloader.
     *
     * @param loader
     *            The class loader. May be null.
     */
    public void pushClassLoader(ClassLoader loader) {
        loaderStack.add(getCurrentClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
    }

    /**
     * Restores the previous loader as the thread-local classloader.
     */
    public void popClassLoader() {
        if (loaderStack.size() == 0) {
            throw new IllegalStateException("Stack size is 0");
        }
        ClassLoader previous = loaderStack.remove(loaderStack.size() - 1);
        Thread.currentThread().setContextClassLoader(previous);
    }

    /**
     * Returns the current thread-local class loader.
     *
     * @return the current thread-local class loader
     */
    public ClassLoader getCurrentClassLoader() {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        return result;
    }
}
