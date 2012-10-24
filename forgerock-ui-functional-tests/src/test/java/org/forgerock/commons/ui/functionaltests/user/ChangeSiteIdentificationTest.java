package org.forgerock.commons.ui.functionaltests.user;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.testng.annotations.Test;

@Test(singleThreaded=true)
public class ChangeSiteIdentificationTest extends AbstractChangeSiteIdentificationTest {
	
	@Test
	@AssertNoErrors
	public void testDefaultScenario() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testNotValidOnEmptyPassPhase() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
				forms.setField("siteImageChange", "passPhrase", "");
				assertFieldHasValue("passPhrase", "");
				
				forms.assertValidationError("siteImageChange", "passPhrase");
				forms.assertFormValidationError("siteImageChange");
				
				dialogsHelper.assertActionButtonDisabled("Save");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testNotValidOnLessThen4PassPhase() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
				forms.setField("siteImageChange", "passPhrase", "aaa");
				assertFieldHasValue("passPhrase", "aaa");
				
				forms.assertValidationError("siteImageChange", "passPhrase");
				forms.assertFormValidationError("siteImageChange");
				
				dialogsHelper.assertActionButtonDisabled("Save");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testValidPassPhase() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
				forms.setField("siteImageChange", "passPhrase", "Aa!@fae^$24");
				assertFieldHasValue("passPhrase", "Aa!@fae^$24");
				
				forms.assertValidationPasses("siteImageChange", "passPhrase");
				forms.assertFormValidationPasses("siteImageChange");
				
				dialogsHelper.assertActionButtonEnabled("Save");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangeSiteImageValidation() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
				
				forms.changeValueForContentFlow("siteImageFlow", "user.png");
				String siteImage = forms.getValueForContentFlow("siteImageFlow");
				
				Assert.assertEquals("user.png", siteImage);
				dialogsHelper.assertActionButtonEnabled("Save");
				
				forms.changeValueForContentFlow("siteImageFlow", "mail.png");
				siteImage = forms.getValueForContentFlow("siteImageFlow");
				
				Assert.assertEquals("mail.png", siteImage);
				dialogsHelper.assertActionButtonDisabled("Save");
			}
		}.run();
	}
	
	@Test
	@AssertNoErrors
	public void testChangeSiteImageAndSitePhase() {
		new ChangeSiteIdentificationValidationTest() {
			@Override
			protected void checkChangeSiteIdentificationViewBehavior() {
				
				forms.changeValueForContentFlow("siteImageFlow", "user.png");
				String siteImage = forms.getValueForContentFlow("siteImageFlow");
				Assert.assertEquals("user.png", siteImage);
				
				forms.setField("siteImageChange", "passPhrase", "Aa!@fae^$24");
				assertFieldHasValue("passPhrase", "Aa!@fae^$24");
				forms.assertValidationPasses("siteImageChange", "passPhrase");
				forms.assertFormValidationPasses("siteImageChange");
				
				dialogsHelper.assertActionButtonEnabled("Save");
				
				forms.submit("dialogs", "Save");
				
				
				//check changes
				
				userHelper.logout();
				userHelper.loginAsDefaultUser();
				assertNoErrorsAspect.assertNoErrors();
				
				router.routeTo("#profile/site_identification/", true);
				router.assertUrl("#profile/site_identification/");
				
				forms.assertValidationDisabled("siteImageChange", "passPhrase");
				forms.assertFormFieldHasValue("siteImageChange", "passPhrase", "Aa!@fae^$24");
				
				siteImage = forms.getValueForContentFlow("siteImageFlow");
				Assert.assertEquals("user.png", siteImage);
				
				dialogsHelper.assertActionButtonDisabled("Save");
			}
		}.run();
	}
	
}
