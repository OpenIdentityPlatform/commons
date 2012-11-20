package org.forgerock.commons.ui.functionaltests.user;

import org.forgerock.commons.ui.functionaltests.AbstractTest;

public class AbstractForgottenPasswordTest extends AbstractTest {

	protected abstract class ForgottenPasswordPhase1ValidationTest {

		protected abstract void checkForgottenPasswordViewBehavior();

		public final void run() {
			userHelper.createDefaultUser();

			router.routeTo("#profile/forgotten_password/");
			router.assertUrl("#profile/forgotten_password/");
			
			forms.assertFormFieldHasValue("dialogs", "resetUsername", "");
			
			checkForgottenPasswordViewBehavior();
		};

	}
	
	protected abstract class ForgottenPasswordPhase2ValidationTest {

		protected abstract void checkForgottenPasswordViewBehavior();

		public final void run() {
			userHelper.createDefaultUser();

			router.routeTo("#profile/forgotten_password/");
			router.assertUrl("#profile/forgotten_password/");
			
			forms.assertFormFieldHasValue("dialogs", "resetUsername", "");
			
			forms.setField("dialogs", "resetUsername", "test@test.test");
			forms.assertValidationPasses("dialogs", "resetUsername");
			
			forms.assertFormFieldHasValue("fgtnSecurityQuestion", null, "What was your first pet's name?");
			forms.assertFormFieldHasValue("dialogs", "fgtnSecurityAnswer", "");
			
			/*forms.assertValidationError("dialogs", "fgtnSecurityAnswer");
			forms.assertValidationError("dialogs", "password");
			forms.assertValidationError("dialogs", "passwordConfirm");
			
			forms.assertFormValidationError("dialogs");*/
			
			checkForgottenPasswordViewBehavior();
		};

	}
}
