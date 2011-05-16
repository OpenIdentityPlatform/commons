/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal/CDDLv1_0.txt or
 * http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *      Copyright 2011 ForgeRock AS
 */
package org.forgerock.i18n.maven;



import java.io.File;



/**
 * Goal which generates message source files from a one or more property files.
 * 
 * @goal generate-test-messages
 * @phase generate-test-sources
 * @threadSafe
 */
public final class GenerateTestMessagesMojo extends
    AbstractGenerateMessagesMojo
{

  /**
   * The target directory in which the source files should be generated.
   * 
   * @parameter 
   *            default-value="${project.build.directory}/generated-test-sources/messages"
   * @required
   */
  private File targetDirectory;

  /**
   * The resource directory containing the message files.
   * 
   * @parameter default-value="${basedir}/src/test/resources"
   * @required
   */
  private File resourceDirectory;



  /**
   * {@inheritDoc}
   */
  @Override
  void addNewSourceDirectory(final File targetDirectory)
  {
    project.addTestCompileSourceRoot(targetDirectory
        .getAbsolutePath());
  }



  /**
   * {@inheritDoc}
   */
  @Override
  File getResourceDirectory()
  {
    return resourceDirectory;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  File getTargetDirectory()
  {
    return targetDirectory;
  }

}
