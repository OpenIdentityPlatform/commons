/*! @license 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.forgerock.commons.ui.functionaltests.webdriver.WebDriverFactory;
import org.openqa.selenium.*;
import org.springframework.stereotype.Component;

@Component
public class DialogsHelper {

	private WebDriver driver = WebDriverFactory.getWebDriver();
	
	@Inject
	private SeleniumHelper selenium;
	
	public void closeDialog() {
		closeDialogByButtonClick();
	}
	
	public void closeDialogByButtonClick() {
		WebElement element = driver.findElement(By.cssSelector(".dialogActions [name='close']"));
		element.click();
	}
	
	public void closeDialogByClickOnCloseCross() {
		WebElement element = driver.findElement(By.cssSelector(".dialogCloseCross > a > img"));
		element.click();
	}
	
	public void assertActionButtonDisabled(final String buttonName) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Button " + buttonName + " was expecte to be inactive";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("inactive");
			}
		}.checkAssertion();
	}
	
	public void assertActionButtonEnabled(final String buttonName) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Button " + buttonName + " was expecte to be active";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElement(By.cssSelector(".dialogActions input[name='" + buttonName + "']")).getAttribute("class").contains("active");
			}
		}.checkAssertion();
	}
	
}
