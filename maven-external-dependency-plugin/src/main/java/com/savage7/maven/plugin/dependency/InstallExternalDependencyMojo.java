package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.util.FileUtils;


/**
 * Install external dependencies to local repository  
 *
 * @goal install-external
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see  http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 */
public class InstallExternalDependencyMojo extends AbstractExternalDependencyMojo
{
   /**
    * @parameter expression="${localRepository}"
    * @required
    * @readonly
    */
    protected ArtifactRepository localRepository;	
	
   /**
    * @component
    */
    protected ArtifactInstaller installer;
    
    /**
     * Digester for MD5.
     *
     * @component default-value="sha1"
     */
    protected Digester md5Digester;

    /**
     * Digester for SHA-1.
     *
     * @component default-value="sha1"
     */
    protected Digester sha1Digester;    

    /**
     * Flag whether to create checksums (MD5, SHA-1) or not.
     *
     * @parameter expression="${createChecksum}" default-value="true"
     */
    protected boolean createChecksum = true;  
    

   
    
    public void execute() throws MojoExecutionException, MojoFailureException  
    {
        try
        {
            // update base configuration parameters
            // (not sure why this is needed, but doesn't see to work otherwise?)
            super.localRepository = this.localRepository;
            super.createChecksum = this.createChecksum;
            super.md5Digester = this.md5Digester;
            super.sha1Digester = this.sha1Digester;
            
            Boolean cachedCreateChecksums = this.createChecksum; 

            getLog().debug("INSTALL EXTERNAL DEPENDENCIES - <START>");

            // loop over and process all configured artifacts 
            for(ArtifactItem artifactItem : artifactItems)
            {
                getLog().info("RESOLVING ARTIFACT FOR INSTALL: " + artifactItem.toString());
                
                //
                // CREATE MAVEN ARTIFACT
                //
                Artifact artifact = createArtifact(artifactItem);

                // determine if the artifact is already installed in the local Maven repository
                Boolean artifactAlreadyInstalled = getLocalRepoFile(artifact).exists();
                
                // only proceed with this artifact if it is not already 
                // installed or it is configured to be forced.
                if(!artifactAlreadyInstalled || 
                    artifactItem.getForce() ||
                    force)
                {
                	if(artifactItem.getForce())
                    {
                        getLog().debug("FORCING ARTIFACT INSTALL: " + artifactItem.toString());
                    }
                    
                	// ensure the artifact file is located in the staging directory
                	File stagedArtifactFile = getFullyQualifiedArtifactFilePath(artifactItem);
                	if(stagedArtifactFile.exists())
                	{                	
                        //
                        // INSTALL MAVEN ARTIFACT TO LOCAL REPOSITORY
                        //
                        if(artifact != null &&
                           artifactItem.getInstall())
                        {
                            // create Maven artifact POM file
                            File generatedPomFile = null;

                            // don't generate a POM file for POM artifacts
                            if ( !"pom".equals( artifactItem.getPackaging() ) )
                            {
                                // if a POM file was provided for the artifact item, then
                            	// use that POM file instead of generating a new one
                            	if ( artifactItem.getPomFile() != null )
                                {
                                    ArtifactMetadata pomMetadata = new ProjectArtifactMetadata( artifact, artifactItem.getPomFile() );
                                    artifact.addMetadata( pomMetadata );
                                }
                                else
                                {
                                    // dynamically create a new POM file for this artifact
                                	generatedPomFile = generatePomFile(artifactItem);
                                    ArtifactMetadata pomMetadata = new ProjectArtifactMetadata( artifact, generatedPomFile );

                                    if ( artifactItem.getGeneratePom() == true )
                                    {
                                        getLog().debug( "INSTALLING GENERATED POM: " + generatedPomFile.getCanonicalPath());
                                        artifact.addMetadata( pomMetadata );
                                    }
                                }
                            }                        	
                        	
                            getLog().info("INSTALLING ARTIFACT TO M2 LOCAL REPO: " + localRepository.getId() );

                            // install artifact to local repository
                            installer.install(stagedArtifactFile,artifact,localRepository);
                            
                            // install checksum files to local repository
                            if(artifactItem.getCreateChecksum() != null)
                            {
                            	super.createChecksum = artifactItem.getCreateChecksum().equalsIgnoreCase("true");
                            }
                            else
                            {
                            	super.createChecksum = cachedCreateChecksums;
                            }
                          	installChecksums( artifact );
                        }
                        else
                        {
                            getLog().debug("CONFIGURED TO NOT INSTALL ARTIFACT: " + artifactItem.toString());
                        }                		
                	}
                	else
                	{
                		// throw error because we were unable to install the external dependency 
                		throw new MojoFailureException("Unable to install external dependency '" + 
                				                        artifactItem.getArtifactId() + 
                				                        "'; file not found in staging path: " + 
                				                        stagedArtifactFile.getCanonicalPath());
                	}
                	
                	
                    //
                    // DOWNLOAD FILE FROM URL
                    //
                    if(artifactItem.getDownloadUrl() != null)
                    {
                    	File downloadFile = getFullyQualifiedArtifactFilePath(artifactItem);

                    	getLog().info("DOWNLOADING ARTIFACT FROM: " + artifactItem.getDownloadUrl());
                        getLog().info("DOWNLOADING ARTIFACT TO: " + downloadFile.getCanonicalPath());
                        
                        // download file from URL 
                        FileUtils.copyURLToFile(new URL(artifactItem.getDownloadUrl()), downloadFile);

                        getLog().debug("ARTIFACT FILE DOWNLOADED SUCCESSFULLY.");
                    }
                }
                else
                {
                    getLog().debug("ARTIFACT ALREADY EXISTS IN LOCAL REPO; NO DOWNLOAD NEEDED " + artifactItem.toString());
                }
            }

            getLog().debug("INSTALL EXTERNAL DEPENDENCIES - <END>'");            
        } 
        catch (Exception e) 
        {
            getLog().error(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }      
}
