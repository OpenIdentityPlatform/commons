package com.savage7.maven.plugin.dependency;

import java.io.File;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


 
/**
 * Remove any downloaded external dependency files  
 * from the staging directory
 *
 * @goal clean-external
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see  http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 */
public class CleanExternalDependencyMojo extends AbstractExternalDependencyMojo
{
   /**
    * @parameter expression="${localRepository}"
    * @required
    * @readonly
    */	
    protected ArtifactRepository localRepository;

    
    public void execute() throws MojoExecutionException, MojoFailureException  
    {
        try
        {
            // update base configuration parameters
            // (not sure why this is needed, but doesn't see to work otherwise?)
            super.localRepository = this.localRepository;

            getLog().debug("CLEANING EXTERNAL DEPENDENCIES - <START>");

            // loop over and process all configured artifacts 
            for(ArtifactItem artifactItem : artifactItems)
            {
                //
                // REMOVE ANY DOWNLOADED FILES FROM THE STAGING DIRECTORY
                //
            	File downloadFile = getFullyQualifiedArtifactFilePath(artifactItem);

                if(downloadFile.exists())
                {
                	getLog().info("DELETING EXTERNAL DEPENDENCY FILE: " + downloadFile.getCanonicalPath());
                	downloadFile.delete();
                }
            }

            getLog().debug("CLEANING EXTERNAL DEPENDENCIES - <END>'");
            
        } 
        catch (Exception e) 
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
    
  
}
