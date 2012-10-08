package org.forgerock.commons.ui.functionaltests.commons;

import org.forgerock.commons.ui.functionaltests.AbstractTest;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.AssertNoErrors;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RoutingTest extends AbstractTest {
	
	@Test
	@AssertNoErrors
	public void shouldGet404Page() {
		router.routeTo("#incorrect_url");
		
		WebElement element = selenium.getElement("content", "h1", ElementType.TAG);
		Assert.assertEquals(element.getText(), "Page not found");
	}
	
	@Test
	@AssertNoErrors
	public void shouldBackToLoginPageAfter404() {
		router.routeTo("#incorrect_url");
		router.goBack();
		
		WebElement element = selenium.getElement("content", "login", ElementType.NAME);
		Assert.assertNotNull(element);
	}
	
	@Test
	@AssertNoErrors
	public void shouldRedirectToLoginPage() {		
		router.routeTo("#profile/");
		router.assertUrl("#login/");
		
		WebElement element = selenium.getElement("content", "login", ElementType.NAME);
		Assert.assertNotNull(element);
	}
	
	@Test
	@AssertNoErrors
	public void shouldRedirectToDesiredPageAfterLogin() {
		userHelper.createDefaultUser();		
		router.routeTo("#profile/");
		
		WebElement element = selenium.getElement("content", "login", ElementType.NAME);
		Assert.assertNotNull(element);
		
		forms.setField("content", "login", "test@test.test");
		forms.setField("content", "password", "tesT#1#Test");
		forms.submit("content", "loginButton");
		
		router.assertUrl("#profile/");
	}
	
	@Test
	@AssertNoErrors
	public void shouldHistoryWork() {
		userHelper.createDefaultUser();	
		userHelper.loginAsDefaultUser();
		
		router.goToProfile(true);
		router.routeTo("#");
		router.routeTo("#profile/");
		router.routeTo("#profile/old_password/");
		
		router.assertUrl("#profile/old_password/");
		router.goBack();
		router.assertUrl("#profile/");
		router.goBack();
		router.assertUrl("#");
		
		router.goForward();
		router.assertUrl("#profile/");
		router.goForward();
		router.assertUrl("#profile/old_password/");
	}
	
}
