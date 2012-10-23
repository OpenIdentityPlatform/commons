package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Component
public class SeleniumHelper {

	public static enum ElementType {
		NAME, ID, CLASS, LINK_TEXT, XPATH, TAG, CSS
	}

	@Inject
	private WebDriver driver;
	
	@Inject
    private WebDriverWait webDriverWait;

	/**
	 * Returns an element. El is an id of root element.
	 */
	public WebElement getElement(String el, String value, ElementType type) {
		WebElement root = driver.findElement(By.id(el));

		if (type == ElementType.NAME) {
			return root.findElement(By.name(value));
		} else if (type == ElementType.ID) {
			return root.findElement(By.id(value));
		} else if (type == ElementType.CLASS) {
			return root.findElement(By.className(value));
		} else if (type == ElementType.LINK_TEXT) {
			return root.findElement(By.linkText(value));
		} else if (type == ElementType.XPATH) {
			return root.findElement(By.xpath(value));
		} else if (type == ElementType.TAG) {
			return root.findElement(By.tagName(value));
		} else if (type == ElementType.CSS) {
			return root.findElement(By.cssSelector(value));
		}

		throw new RuntimeException("Incorrect element type");
	}

	public void waitForElement(final String el, final String value, final ElementType type) {
		new AssertionWithTimeout() {
			
			@Override
			protected String getAssertionFailedMessage() {
				return "Element " + value + " has not been found on page";
			}
			
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return getElement(el, value, type) != null;
			}
		};
	}

	/**
	 * @param selector
	 *            jQuery selector
	 * @param event
	 *            event name
	 */
	public void fireEvent(String selector, String event) {
		((JavascriptExecutor) driver).executeScript("$('" + selector
				+ "').trigger('" + event + "')");
	}

	public void removeCookies() {
		driver.manage().deleteAllCookies();
	}

	public void setSessionCookie(String id) {
		Cookie cookie = new Cookie("JSESSIONID", id);
		driver.manage().addCookie(cookie);
	}

	public WebDriver getDriver() {
		return driver;
	}

	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}

	public void dragAndDrop(WebElement draggedElement,
			WebElement containerToDrop) {
		new Actions(driver).dragAndDrop(draggedElement, containerToDrop)
				.build().perform();
	}

	public abstract class AssertionWithTimeout {

		public final void checkAssertion() {
			try {
				webDriverWait.until(new ExpectedCondition<Boolean>() {
					public Boolean apply(WebDriver d) {
						return timeoutCondition();
					}
				});
			} catch (TimeoutException e) {
				Assert.fail(getTimeoutExceptionMessage());
			}
			Assert.assertTrue(assertionCondition(driver), getAssertionFailedMessage());
		}

		protected boolean timeoutCondition() {
			return assertionCondition(driver);
		}

		protected String getTimeoutExceptionMessage() {
			return getAssertionFailedMessage();
		}

		protected abstract boolean assertionCondition(WebDriver driver);

		protected abstract String getAssertionFailedMessage();
	}

}
