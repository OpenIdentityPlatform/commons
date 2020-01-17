/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/

package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.install.AbstractInstallMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.digest.Md5Digester;
import org.codehaus.plexus.digest.Sha1Digester;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for all goals in this plugin.
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 * @ThreadSafe
 */
public abstract class AbstractExternalDependencyMojo extends
        AbstractInstallMojo
{

    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
     * artifactId, version, type, classifier, location, destFile, markerFile and
     * overwrite.) See "Usage" and "Javadoc" for details.
     * 
     * @parameter
     * @required
     */
    protected ArrayList<ArtifactItem> artifactItems;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${project.build.directory}"
     */
    protected String stagingDirectory;

    /**
     * Forces a download, maven install, maven deploy
     * 
     * @parameter default-value="false"
     */
    protected Boolean force = false;

    
    /**
     * If this property is set to true, then the 
     * downloaded file's checksum will not be 
     * verified using the Sonatype artifact query
     * by checksum validation routine.
     * 
     * @parameter default-value="false"
     */
    protected Boolean skipChecksumVerification = false;
    
    /**
     * Create Maven Artifact object from ArtifactItem configuration descriptor
     * 
     * @param item
     * @return Artifact
     */
    protected Artifact createArtifact(ArtifactItem item)
    {
        Artifact artifact = null;

        // create Maven artifact with a classifier
        artifact = artifactFactory.createArtifactWithClassifier(item
                .getGroupId(), item.getArtifactId(), item.getVersion(), item
                .getPackaging(), item.getClassifier());

        return artifact;
    }

    /**
     * Generates a (temporary) POM file from the plugin configuration. It's the
     * responsibility of the caller to delete the generated file when no longer
     * needed.
     * 
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException
     *             If the POM file could not be generated.
     */
    protected File generatePomFile(ArtifactItem artifact)
            throws MojoExecutionException
    {
        Model model = generateModel(artifact);

        Writer writer = null;
        try
        {
            File pomFile = File.createTempFile(artifact.getGroupId() + "."
                    + artifact.getArtifactId(), ".pom");

            writer = WriterFactory.newXmlWriter(pomFile);
            new MavenXpp3Writer().write(writer, model);

            return pomFile;
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(
                    "Error writing temporary POM file: " + e.getMessage(), e);
        }
        finally
        {
            IOUtil.close(writer);
        }
    }

    /**
     * Generates a minimal model from the user-supplied artifact information.
     * 
     * @return The generated model, never <code>null</code>.
     */
    protected Model generateModel(ArtifactItem artifact)
    {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging(artifact.getPackaging());

        return model;
    }

    /**
     * Resolves the file path and returns a file object instance for an artifact
     * item
     * 
     * @return File object for artifact item
     */
    protected File getFullyQualifiedArtifactFilePath(ArtifactItem artifactItem)
    {
        String artifactStagingDirectory = artifactItem.getStagingDirectory();
        if (artifactStagingDirectory == null
                || artifactStagingDirectory.isEmpty())
        {
            artifactStagingDirectory = stagingDirectory;
        }
        return new File(artifactStagingDirectory + File.separator
                + artifactItem.getLocalFile());
    }

    /**
     * Verifies a checksum for the specified file.
     * 
     * @param targetFile
     *            The path to the file from which the checksum is verified, must
     *            not be <code>null</code>.
     * @param digester
     *            The checksum algorithm to use, must not be <code>null</code>.
     * @throws MojoExecutionException
     *             If the checksum could not be installed.
     */
    protected boolean verifyChecksum(File targetFile, Digester digester,
            String checksum) throws MojoExecutionException
    {
        getLog().debug(
                "Calculating " + digester.getAlgorithm() + " checksum for "
                        + targetFile);
        try
        {
            String calculatedChecksum = digester.calc(targetFile);
            getLog().debug("Generated checksum : " + calculatedChecksum);
            getLog().debug("Expected checksum  : " + checksum);
            return (calculatedChecksum.equals(checksum));
        }
        catch (DigesterException e)
        {
            throw new MojoExecutionException("Failed to calculate "
                    + digester.getAlgorithm() + " checksum for " + targetFile,
                    e);
        }
    }

    /**
     * Validate artifact configured checksum against specified file.
     * 
     * @param artifactItem
     *            to validate checksum against
     * @param targetFile
     *            to validate checksum against
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void verifyArtifactItemChecksum(ArtifactItem artifactItem,
            File targetFile) throws MojoExecutionException,
            MojoFailureException, IOException
    {
        // if a checksum was specified, we must verify the checksum against the
        // downloaded file
        if (artifactItem.hasChecksum())
        {
            getLog().info(
                    "verifying checksum on downloaded file: CKSM="
                            + artifactItem.getChecksum());

            // perform MD5 checksum verification
            getLog().info(
                    "testing for MD5 checksum on artifact: "
                            + artifactItem.toString());
            if (!verifyChecksum(targetFile, new Md5Digester(), artifactItem
                    .getChecksum()))
            {
                getLog().info(
                        "verification failed on MD5 checksum for file: "
                                + targetFile.getCanonicalPath());
                getLog().info(
                        "testing for SHA1 checksum on artifact: "
                                + artifactItem.toString());

                // did not pass MD5 checksum verification, now test SHA1
                // checksum
                if (!verifyChecksum(targetFile, new Sha1Digester(),
                        artifactItem.getChecksum()))
                {
                    // checksum verification failed, throw error
                    throw new MojoFailureException(
                            "Both MD5 and SHA1 checksum verification failed for: "
                                    + "\r\n   groupId    : "
                                    + artifactItem.getGroupId()
                                    + "\r\n   artifactId : "
                                    + artifactItem.getArtifactId()
                                    + "\r\n   version    : "
                                    + artifactItem.getVersion()
                                    + "\r\n   checksum   : "
                                    + artifactItem.getChecksum()
                                    + "\r\n   file       : "
                                    + targetFile.getCanonicalPath());
                }
                else
                {
                    getLog().info(
                            "verification passed on SHA1 checksum for artifact: "
                                    + artifactItem.toString());
                }
            }
            else
            {
                getLog().info(
                        "verification passed on MD5 checksum for artifact: "
                                + artifactItem.toString());
            }
        }
    }
    

    /**
     * Validate artifact configured extracted file checksum against specified file.
     * 
     * @param artifactItem
     *            to validate checksum against
     * @param targetFile
     *            to validate checksum against
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void verifyArtifactItemExtractFileChecksum(ArtifactItem artifactItem,
            File targetFile) throws MojoExecutionException,
            MojoFailureException, IOException
    {
        // if a checksum was specified, we must verify the checksum against the
        // extracted file
        if (artifactItem.hasExtractFileChecksum())
        {
            getLog().info(
                    "verifying checksum on extracted file: CKSM="
                            + artifactItem.getChecksum());

            // perform MD5 checksum verification
            getLog().info(
                    "testing for MD5 checksum on artifact: "
                            + artifactItem.toString());
            if (!verifyChecksum(targetFile, new Md5Digester(), artifactItem
                    .getExtractFileChecksum()))
            {
                getLog().info(
                        "verification failed on MD5 checksum for extracted file: "
                                + targetFile.getCanonicalPath());
                getLog().info(
                        "testing for SHA1 checksum on artifact: "
                                + artifactItem.toString());

                // did not pass MD5 checksum verification, now test SHA1
                // checksum
                if (!verifyChecksum(targetFile, new Sha1Digester(),
                        artifactItem.getExtractFileChecksum()))
                {
                    // checksum verification failed, throw error
                    throw new MojoFailureException(
                            "Both MD5 and SHA1 checksum verification failed for: "
                                    + "\r\n   groupId        : "
                                    + artifactItem.getGroupId()
                                    + "\r\n   artifactId     : "
                                    + artifactItem.getArtifactId()
                                    + "\r\n   version        : "
                                    + artifactItem.getVersion()
                                    + "\r\n   checksum       : "
                                    + artifactItem.getChecksum()
                                    + "\r\n   extracted file : "
                                    + targetFile.getCanonicalPath());
                }
                else
                {
                    getLog().info(
                            "verification passed on SHA1 checksum for artifact: "
                                    + artifactItem.toString());
                }
            }
            else
            {
                getLog().info(
                        "verification passed on MD5 checksum for artifact: "
                                + artifactItem.toString());
            }
        }
    }

    
    /**
     * Validate downloaded file artifact checksum does not match another
     * artifact's checksum that already exists in a public Maven 
     * repository.  Using the Sonatype REST API to perform a checksum
     * lookup.
     * 
     * @since 0.2
     * 
     * @param artifactItem
     *            to validate checksum against
     * @param targetFile
     *            to validate checksum against
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void verifyArtifactItemChecksumBySonatypeLookup(ArtifactItem artifactItem,
        File targetFile) throws MojoExecutionException,
        MojoFailureException, IOException
    {
        // skip this artifact checksum verification?
        if (skipChecksumVerification == true ||
            artifactItem.getSkipChecksumVerification() == true)
        {
            return;
        }

        boolean artifactMismatch = false;
        StringBuilder detectedArtifacts = new StringBuilder();
        
        try
        {
            // calculate SHA1 checksum
            String sha1Checksum = sha1Digester.calc(targetFile);
            getLog().debug("performing Sonatype lookup on artifact SHA1 checksum: " + sha1Checksum);
        
            // perform REST query against Sonatype checksum lookup API
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document document = builder.parse("http://repository.sonatype.org/service/local/data_index?sha1="+sha1Checksum);
            NodeList artifactList = document.getElementsByTagName("artifact");

            // were any results returned?
            if(artifactList != null && artifactList.getLength() > 0)
            {
                int nodeCount = artifactList.getLength(); 
                getLog().info(nodeCount + " existing artifacts found in Sonatype checksum lookup.  verifying artifact GAV.");
                
                // iterate over all the query returned artifact definitions and 
                // attempt to determine if any of the returned artifact GAV do
                // no match the GAV of the attempted install artifact.
                for(int index = 0; index < nodeCount; index++)
                {
                    Node artifactNode = artifactList.item(index);
                    if(artifactNode.hasChildNodes())
                    {
                        NodeList children = artifactNode.getChildNodes();
                        for(int loop = 0; loop < children.getLength(); loop++)
                        {
                            Node artifactProperty = children.item(loop);
                            
                            // append returned artifact property names to an output message string
                            if(!artifactProperty.getNodeName().equalsIgnoreCase("#text"))
                            {
                                detectedArtifacts.append("\n       " + artifactProperty.getNodeName() + " : " + artifactProperty.getTextContent());
                            }
                            
                            // attempt to validate the returned artifact's GroupId against the target install artifact
                            if(artifactProperty.getNodeName().equalsIgnoreCase("groupId"))
                            {
                                if(!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getGroupId()))
                                {
                                    getLog().error("artifact found in Sonatype lookup does not match: "+
                                        artifactProperty.getNodeName() + ":" + 
                                        artifactProperty.getTextContent() + " != " + 
                                        artifactItem.getGroupId());
                                    artifactMismatch = true;
                                }
                            }
                            
                            // attempt to validate the returned artifact's ArtifactId against the target install artifact
                            else if(artifactProperty.getNodeName().equalsIgnoreCase("artifactId"))
                            {
                                if(!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getArtifactId()))
                                {
                                    getLog().error("artifact found in Sonatype lookup does not match: "+
                                        artifactProperty.getNodeName() + ":" + 
                                        artifactProperty.getTextContent() + " != " + 
                                        artifactItem.getArtifactId());
                                    artifactMismatch = true;
                                }
                            }
                            
                            // attempt to validate the returned artifact's Version against the target install artifact
                            else if(artifactProperty.getNodeName().equalsIgnoreCase("version"))
                            {
                                if(!artifactProperty.getTextContent().equalsIgnoreCase(artifactItem.getVersion()))
                                {
                                    getLog().error("artifact found in Sonatype lookup does not match: "+
                                        artifactProperty.getNodeName() + ":" + 
                                        artifactProperty.getTextContent() + " != " + 
                                        artifactItem.getVersion());
                                    artifactMismatch = true;
                                }
                            }                                        
                        }
                    }

                    detectedArtifacts.append("\n");
                }
            }
            else
            {
                getLog().debug("no existing artifacts found in Sonatype checksum lookup.  continue with artifact installation.");
            }
        }
        catch(Exception ex)
        {
            getLog().error(ex);
        }
        
        // was a mismatch detected?
        if(artifactMismatch == true)
        {
            // checksum verification failed, throw error
            throw new MojoFailureException(
                    "Sonatype artifact checksum verification failed on artifact defined in POM: \n"
                    + "\n       groupId    : "
                    + artifactItem.getGroupId()
                    + "\n       artifactId : "
                    + artifactItem.getArtifactId()
                    + "\n       version    : "
                    + artifactItem.getVersion()
                    + "\n       checksum   : "
                    + artifactItem.getChecksum()
                    + "\n       file       : "
                    + targetFile.getCanonicalPath()
                    + "\n\n The following artifact(s) were detected using the same checksum:\n"
                    + detectedArtifacts.toString()
                    + "\n\n Please verify that the GAV defined on the target artifact is correct.\n"
                    );
        }
    }
}
