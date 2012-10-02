package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import junit.framework.Assert;

import org.forgerock.commons.ui.functionaltests.constants.Constants;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

@Component
public class RouterHelper {
	
	@Inject
	private WebDriver driver;
	
	@Inject
	private Constants constants;
	
	@Inject
	private SeleniumHelper selenium;
	
	@Inject
	private WebDriverWait webDriverWait;
	
	public void routeTo(String url) {	
		driver.navigate().to(constants.getBasePage() + url);		
	}
	
	public void goToProfile() {
		this.routeTo("/#profile/");		
		selenium.waitForElement("content", "saveButton", ElementType.NAME);
	}
	
	public void goToRegistration() {
		this.routeTo("/#register/");
		selenium.waitForElement("content", "email", ElementType.NAME);
	}
	
	public void assertUrl(final String url) {
		webDriverWait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return driver.getCurrentUrl().equals(constants.getBasePage() + url);
            }
        });
		
		Assert.assertEquals(constants.getBasePage() + url, driver.getCurrentUrl());
	}
	
}
