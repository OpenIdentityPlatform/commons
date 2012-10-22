package org.forgerock.commons.ui.functionaltests.user;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrorsAspect;

public class AbstractEnterOldPasswordTest extends AbstractTest{
	
	@Inject
	private AssertNoErrorsAspect assertNoErrorsAspect;

	protected String formatStringFromForm(String value) {
		return value.substring(1, value.length()-1);
	}
	
	protected abstract class EnterOldPasswordValidationTest {
		
		protected abstract void checkEnterOldPasswordViewBehavior();
		
		public final void run() {
			userHelper.createDefaultUser();
			userHelper.loginAsDefaultUser();
			assertNoErrorsAspect.assertNoErrors();
			
			router.routeTo("#profile/old_password/", true);
			router.assertUrl("#profile/old_password/");
			
			forms.assertValidationError("enterOldPassword", "oldPassword");
			forms.assertFormValidationError("enterOldPassword");
			
			checkEnterOldPasswordViewBehavior();
		};

	}
	
	protected void assertFieldHasValue(String fieldName, String expectedValue) {
		assertFieldHasValue("enterOldPassword", fieldName, expectedValue);
	}
	
}
