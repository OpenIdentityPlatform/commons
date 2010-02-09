package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;



import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.install.AbstractInstallMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

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
    
    protected File getFullyQualifiedArtifactFilePath(ArtifactItem artifactItem)
    {
        String artifactStagingDirectory = artifactItem.getStagingDirectory();
        if(artifactStagingDirectory == null || artifactStagingDirectory.isEmpty())
        {
        	artifactStagingDirectory = stagingDirectory;
        }
    	return new File(artifactStagingDirectory + File.separator + artifactItem.getLocalFile());    	
    }  

}
