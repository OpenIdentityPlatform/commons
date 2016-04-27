/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.audit.rotation;

import java.io.File;
import java.io.Writer;

/**
 * This class holds some information while a file is being rotated. It will be passed as arguments to the
 * {@code RotationHooks}.
 */
public class RotationContext {

    private File initialFile;
    private File nextFile;
    private Writer writer;

    public File getInitialFile() {
        return initialFile;
    }

    public void setInitialFile(File initialFile) {
        this.initialFile = initialFile;
    }

    public File getNextFile() {
        return nextFile;
    }

    public void setNextFile(File nextFile) {
        this.nextFile = nextFile;
    }

    public Writer getWriter() {
        return writer;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}
