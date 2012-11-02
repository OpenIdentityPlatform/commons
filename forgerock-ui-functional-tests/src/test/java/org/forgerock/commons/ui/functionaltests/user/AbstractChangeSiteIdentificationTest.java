package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.AbstractTest;

public class AbstractChangeSiteIdentificationTest extends AbstractTest {

	protected abstract class ChangeSiteIdentificationValidationTest {

		protected abstract void checkChangeSiteIdentificationViewBehavior();

		public final void run() {
			userHelper.createDefaultUser();
			userHelper.loginAsDefaultUser();
			assertNoErrorsAspect.assertNoErrors();
			
			router.routeTo("#profile/site_identification/", true);
			router.assertUrl("#profile/site_identification/");
			
			//forms.assertValidationDisabled("siteImageChange", "passPhrase");
			forms.assertFormFieldHasValue("siteImageChange", "passPhrase", "zzzz");
			
			String siteImage = forms.getValueForContentFlow("siteImageFlow");
			Assert.assertEquals("mail.png", siteImage);
			
			//dialogsHelper.assertActionButtonDisabled("Save");

			checkChangeSiteIdentificationViewBehavior();
		};
	}
	
	protected void assertFieldHasValue(String fieldName, String expectedValue) {
		assertFieldHasValue("siteImageChange", fieldName, expectedValue);
	}

}
