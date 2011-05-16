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



import static org.fest.assertions.Assertions.assertThat;

import org.forgerock.i18n.maven.AbstractGenerateMessagesMojo.MessageFile;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



/**
 * Tests the MessageFile class.
 */
@Test
public class MessageFileTest
{

  /**
   * Data provider for {@link #testMessageFile}.
   * 
   * @return Test data.
   */
  @DataProvider(parallel = true)
  public Object[][] messageFileStrings()
  {
    return new Object[][] {
        { "path/to/test.properties", "TestMessages", "path.to",
            "test", "path.to.test" },
        { "path/to/another_test.properties", "AnotherTestMessages",
            "path.to", "another_test", "path.to.another_test" },
        { "path/to/yet_another_test.properties",
            "YetAnotherTestMessages", "path.to", "yet_another_test",
            "path.to.yet_another_test" }, };
  }



  /**
   * Tests MessageFile class getters.
   * 
   * @param s
   *          The message file name.
   * @param className
   *          The expected class name.
   * @param packageName
   *          The expected package name.
   * @param shortName
   *          The expected short name.
   * @param resourceBundleName
   *          The expected resource bundle name.
   */
  @Test(dataProvider = "messageFileStrings")
  public void testMessageFile(final String s, final String className,
      final String packageName, final String shortName,
      final String resourceBundleName)
  {
    final MessageFile mf = new MessageFile(s);
    assertThat(mf.getName()).isEqualTo(s);
    assertThat(mf.getClassName()).isEqualTo(className);
    assertThat(mf.getPackageName()).isEqualTo(packageName);
    assertThat(mf.getShortName()).isEqualTo(shortName);
    assertThat(mf.getResourceBundleName()).isEqualTo(
        resourceBundleName);
  }

}
