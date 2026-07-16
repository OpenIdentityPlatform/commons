/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * Portions Copyrighted 2026 3A Systems, LLC
 **/

package com.savage7.maven.plugin.dependency;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Remove any downloaded external dependency files from the staging directory.
 *
 * @goal clean-external
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see <a href="http://code.google.com/p/maven-external-dependency-plugin/">http://code.google.com/p/maven-external-dependency-plugin/</a>
 * @version 0.1
 */
public class CleanExternalDependencyMojo extends AbstractExternalDependencyMojo
{
    /**
     * Creates a new instance.
     */
    public CleanExternalDependencyMojo()
    {
    }

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            getLog().info("starting to clean external dependency staged files");

            // loop over and process all configured artifact items
            for (ArtifactItem artifactItem : artifactItems)
            {
                //
                // REMOVE ANY DOWNLOADED FILES FROM THE STAGING DIRECTORY
                //
                File downloadFile = getFullyQualifiedArtifactFilePath(artifactItem);

                if (downloadFile.exists())
                {
                    getLog().info(
                            "deleting stated external dependency file: "
                                    + downloadFile.getCanonicalPath());
                    downloadFile.delete();
                }
            }
            getLog().info("finished cleaning external dependency staged files");
        }
        catch (Exception e)
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
