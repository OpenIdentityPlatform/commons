package com.savage7.maven.plugin.dependency;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;


/**
 * Deploy external dependencies to distribution  
 * management defined repository
 *
 * @goal deploy-external
 * 
 * @author <a href="mailto:robert@savage7.com">Robert Savage</a>
 * @see  http://code.google.com/p/maven-external-dependency-plugin/
 * @version 0.1
 * @category Maven Plugin
 */
public class DeployExternalDependencyMojo extends AbstractExternalDependencyMojo
{
   /**
    * @parameter expression="${localRepository}"
    * @required
    * @readonly
    */
    protected ArtifactRepository localRepository;	
	
    /**
     * Used to look up Artifacts in the remote repository.
     * 
     * @parameter expression=
     *  "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;
    
    
    /**
     * @component
     */
    private ArtifactDeployer artifactDeployer;
    
    
    /**
     * Flag whether Maven is currently in online/offline mode.
     * 
     * @parameter default-value="${settings.offline}"
     * @readonly
     */
    private boolean offline;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException  
    {
        // update base configuration parameters
        // (not sure why this is needed, but doesn't see to work otherwise?)
        super.localRepository = this.localRepository;

        getLog().debug("DEPLOYING EXTERNAL DEPENDENCIES - <START>");

        // loop over and process all configured artifacts 
        for(ArtifactItem artifactItem : artifactItems)
        {
            getLog().info("RESOLVING ARTIFACT FOR DEPLOY: " + artifactItem.toString());

            //
            // CREATE MAVEN ARTIFACT
            //
            Artifact artifact = createArtifact(artifactItem);

            // determine if the artifact is already installed in the local Maven repository
            File installedArtifactFile = getLocalRepoFile(artifact);
            
            // only proceed with this artifact if it is not already 
            // installed or it is configured to be forced.
            if(installedArtifactFile.exists()) 
            {
                try 
                {
					artifactResolver.resolve(artifact, null, localRepository);
					
                    //
                    // DEPLOY TO DISTRIBUTION MAVEN REPOSITORY
                    //
                    if( artifactItem.getDeploy() )
                    {
                        failIfOffline();

                        ArtifactRepository repo = getDeploymentRepository();

                        String protocol = repo.getProtocol();      
                        
                        if ( protocol.equalsIgnoreCase( "scp" ) )
                        {
                            File sshFile = new File( System.getProperty( "user.home" ), ".ssh" );

                            if ( !sshFile.exists() )
                            {
                                sshFile.mkdirs();
                            }
                        }        
                        
                        
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
                                    getLog().debug( "DEPLOYING GENERATED POM: " + generatedPomFile.getCanonicalPath());
                                    artifact.addMetadata( pomMetadata );
                                }
                            }
                        	
                        }
                        
                    	// deploy now
                        artifactDeployer.deploy( artifact.getFile(), artifact, repo, localRepository );
                        
                        //TODO: Deploy Checksums? 
                    }										
				} 
                catch(ArtifactResolutionException e) 
                {
                	throw new MojoExecutionException("Error occurred while attempting to resolve artifact.",e);
				} 
                catch (ArtifactNotFoundException e) 
                {
                	throw new MojoExecutionException("Unable to find external dependency in local repository.",e);
				} 
                catch (ArtifactDeploymentException e) 
                {
                	throw new MojoExecutionException("Deployment of external dependency failed.",e);
				} 
                catch (IOException e) 
                {
					throw new MojoExecutionException("File I/O Exception encountered while attempting to deply external dependencies.",e);
				}            	
            }
            else
            {
        		// throw error because we were unable to find the installed external dependency 
        		try 
        		{
					throw new MojoFailureException("Unable to find external dependency '" + 
							                        artifactItem.getArtifactId() + 
							                        "'; file not found in local repository: " + 
							                        installedArtifactFile.getCanonicalPath());
				} 
        		catch (IOException e) 
				{
        			throw new MojoExecutionException("Unable to resolve dependency path in locale repository.",e);
				}
            }
        }

        getLog().debug("DEPLOYING EXTERNAL DEPENDENCIES - <END>'");
    }
    
    
    
    
	private ArtifactRepository getDeploymentRepository() throws MojoExecutionException, MojoFailureException
	{
	    ArtifactRepository repo = null;
	    
	    if ( repo == null )
	    {
	        repo = project.getDistributionManagementArtifactRepository();
	    }
	
	    if ( repo == null )
	    {
	        String msg = "Deployment failed: repository element was not specified in the POM inside"
	            + " distributionManagement element";
	
	        throw new MojoExecutionException( msg );
	    }
	
	    return repo;
	}    
	
    private void failIfOffline() throws MojoFailureException
	{
	    if ( offline )
	    {
	        throw new MojoFailureException( "Cannot deploy artifacts when Maven is in offline mode" );
	    }
	}   	
}
