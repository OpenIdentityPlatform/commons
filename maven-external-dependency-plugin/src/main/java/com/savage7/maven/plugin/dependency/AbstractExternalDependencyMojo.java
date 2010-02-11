package com.savage7.maven.plugin.dependency;

/*
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
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

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

/**
 * Base class for all goals in this plugin.
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see  http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 */
public abstract class AbstractExternalDependencyMojo extends AbstractInstallMojo 
{
	
	/**
	 * Used to look up Artifacts in the remote repository.
	 * 
	 * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
	 * @required
	 * @readonly
	 */
	protected ArtifactFactory artifactFactory;
	
	
   /**
    * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
    * artifactId, version, type, classifier, location, destFile, markerFile and overwrite.)
    * See "Usage" and "Javadoc" for details.
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
     * Create Maven Artifact object from ArtifactItem configuration descriptor
     *  
     * @param item
     * @return Artifact
     */
    protected Artifact createArtifact(ArtifactItem item)
    {
        Artifact artifact = null; 
    
       // create Maven artifact with a classifier 
        artifact = artifactFactory.createArtifactWithClassifier(
                            item.getGroupId(),
                            item.getArtifactId(),
                            item.getVersion(),
                            item.getPackaging(), 
                            item.getClassifier());

        return artifact;        
    }      
    
    
    /**
     * Generates a (temporary) POM file from the plugin configuration. It's the responsibility of the caller to delete
     * the generated file when no longer needed.
     *
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException If the POM file could not be generated.
     */
    protected File generatePomFile(ArtifactItem artifact) throws MojoExecutionException
    {
        Model model = generateModel(artifact);

        Writer writer = null;
        try
        {
            File pomFile = File.createTempFile( artifact.getGroupId() + "." + artifact.getArtifactId(), ".pom" );

            writer = WriterFactory.newXmlWriter( pomFile );
            new MavenXpp3Writer().write( writer, model );

            return pomFile;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing temporary POM file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
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
        model.setModelVersion( "4.0.0" );
        model.setGroupId( artifact.getGroupId() );
        model.setArtifactId( artifact.getArtifactId() );
        model.setVersion( artifact.getVersion() );
        model.setPackaging( artifact.getPackaging() );

        return model;
    }
    
    /**
     * Resolves the file path and returns a file object instance for an artifact item
     *
     * @return File object for artifact item
     */
    protected File getFullyQualifiedArtifactFilePath(ArtifactItem artifactItem)
    {
        String artifactStagingDirectory = artifactItem.getStagingDirectory();
        if(artifactStagingDirectory == null || artifactStagingDirectory.isEmpty())
        {
        	artifactStagingDirectory = stagingDirectory;
        }
    	return new File(artifactStagingDirectory + File.separator + artifactItem.getLocalFile());    	
    }  

    
    /**
     * Verifies a checksum for the specified file.
     *
     * @param targetFile The path to the file from which the checksum is verified, must not be <code>null</code>.
     * @param digester The checksum algorithm to use, must not be <code>null</code>.
     * @throws MojoExecutionException If the checksum could not be installed.
     */
    protected boolean verifyChecksum( File targetFile, Digester digester, String checksum ) throws MojoExecutionException
    {
        getLog().debug( "Calculating " + digester.getAlgorithm() + " checksum for " + targetFile );
        try
        {
        	String calculatedChecksum = digester.calc( targetFile );
        	getLog().debug( "Generated checksum : " + calculatedChecksum);
        	getLog().debug( "Expected checksum  : " + checksum);
        	return (calculatedChecksum.equals(checksum));
        }
        catch ( DigesterException e )
        {
            throw new MojoExecutionException( "Failed to calculate " + digester.getAlgorithm() + " checksum for "
                + targetFile, e );
        }
    }
    
    /**
     * Validate artifact configured checksum against specified file. 
     * 
     * @param artifactItem to validate checksum against
     * @param targetFile to validate checksum against
     * @throws MojoExecutionException
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void verifyArtifactItemChecksum(ArtifactItem artifactItem, File targetFile) 
               throws MojoExecutionException, MojoFailureException, IOException
    {    
	    // if a checksum was specified, we must verify the checksum against the downloaded file 
	    if(artifactItem.hasChecksum())
	    {
	    	getLog().info("verifying checksum on downloaded file: CKSM=" + artifactItem.getChecksum());
	    	
	    	// perform MD5 checksum verification
	    	getLog().info("testing for MD5 checksum on artifact: " + artifactItem.toString());
	    	if(!verifyChecksum(targetFile, new Md5Digester(), artifactItem.getChecksum()))
	    	{
	    		getLog().info("verification failed on MD5 checksum for file: " + targetFile.getCanonicalPath());
	    		getLog().info("testing for SHA1 checksum on artifact: " + artifactItem.toString());
	
	    		// did not pass MD5 checksum verification, now test SHA1 checksum
	    		if(!verifyChecksum(targetFile, new Sha1Digester(), artifactItem.getChecksum()))
	    		{
	    			// checksum verification failed, throw error
	    			throw new MojoFailureException("Both MD5 and SHA1 checksum verification failed for: " + 
	    					                       "\r\n   groupId    : " + artifactItem.getGroupId() +
	    					                       "\r\n   artifactId : " + artifactItem.getArtifactId() +
	    					                       "\r\n   version    : " + artifactItem.getVersion() +
	    					                       "\r\n   checksum   : " + artifactItem.getChecksum() +
	    					                       "\r\n   file       : " + targetFile.getCanonicalPath());
	    		}                        		
	        	else
	        	{
	        		getLog().info("verification passed on SHA1 checksum for artifact: " + artifactItem.toString());
	        	}
	    	}
	    	else
	    	{
	    		getLog().info("verification passed on MD5 checksum for artifact: " + artifactItem.toString());
	    	}
	    }
    }
}
