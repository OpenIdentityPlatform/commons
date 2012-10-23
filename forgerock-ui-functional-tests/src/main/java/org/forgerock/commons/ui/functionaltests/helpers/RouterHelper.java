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
	
	public void routeTo(String url, boolean withRefresh) {
		if (withRefresh) {
			driver.navigate().refresh();
		}
		
		driver.navigate().to(constants.getBasePage() + url);		
	}
	
	public void goBack() {
		driver.navigate().back();
	}
	
	public void goForward() {
		driver.navigate().forward();
	}
	
	public void goToProfile(boolean refreshBeforeNavigate) {
		if (refreshBeforeNavigate) {
			driver.navigate().refresh();
		}
		this.routeTo("#profile/");		
		this.assertUrl("#profile/");
		selenium.waitForElement("content", "saveButton", ElementType.NAME);
	}
	
	public void goToRegistration() {
		this.routeTo("#register/");
		this.assertUrl("#register/");
		selenium.waitForElement("content", "terms", ElementType.NAME);
		selenium.waitForElement("content", "securityQuestion", ElementType.NAME);
	}
	
	public void assertUrl(final String url) {
		webDriverWait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return driver.getCurrentUrl().equals(constants.getBasePage() + url);
            }
        });
		
		Assert.assertEquals(constants.getBasePage() + url, driver.getCurrentUrl());
	}

	public void goToAddMoreApps(boolean refreshBeforeNavigate) {
		if (refreshBeforeNavigate) {
			driver.navigate().refresh();
		}
		this.routeTo("#applications/addmore/");
		this.assertUrl("#applications/addmore/");
		selenium.waitForElement("content", "itemize", ElementType.CLASS);
	}
}
