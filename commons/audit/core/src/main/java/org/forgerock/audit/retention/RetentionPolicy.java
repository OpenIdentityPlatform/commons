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
package org.forgerock.audit.retention;

import java.io.File;
import java.util.List;

/**
 * Defines the retention conditions and the files that need to be deleted.
 */
public interface RetentionPolicy {
    /**
     * Returns all files that should be deleted according to the policy.
     *
     * @param fileNamingPolicy The naming policy used generate the log file names.
     * @return Not-null, An array of files that should be deleted according to the policy.
     */
    List<File> deleteFiles(FileNamingPolicy fileNamingPolicy);
}
