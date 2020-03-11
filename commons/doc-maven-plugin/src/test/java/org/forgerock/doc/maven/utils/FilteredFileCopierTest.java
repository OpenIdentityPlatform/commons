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
 * Copyright 2014 ForgeRock AS
 */

package org.forgerock.doc.maven.utils;

import static org.assertj.core.api.Assertions.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("javadoc")
public class FilteredFileCopierTest {

    @Rule
    public TemporaryFolder sourceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder destinationFolder = new TemporaryFolder();

    @Test
    public void shouldCopyOnlyOthers() throws IOException {
        File doCopy = sourceFolder.newFile("do.cpy");
        File doNotCopy = sourceFolder.newFile("do.not");

        FilteredFileCopier.copyOthers("not", sourceFolder.getRoot(), destinationFolder.getRoot());

        File copied = new File(destinationFolder.getRoot(), "do.cpy");
        File notCopied = new File(destinationFolder.getRoot(), "do.not");

        assertThat(copied).exists();
        assertThat(notCopied).doesNotExist();

        sourceFolder.delete();
        destinationFolder.delete();
    }
}
