package org.forgerock.commons.ui.functionaltests.helpers;

import javax.inject.Inject;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class SeleniumHelper {
	
	public static enum ElementType {NAME, ID, CLASS, LINK_TEXT, XPATH, TAG }
	
	@Inject
	private WebDriver driver;
	
	/**
	 * Returns an element. El is an id of root element.
	 */
	public WebElement getElement(String el, String value, ElementType type) {
		WebElement root = driver.findElement(By.id(el));
		
		if(type == ElementType.NAME) {
			return root.findElement(By.name(value));
		} else if(type == ElementType.ID) {
			return root.findElement(By.id(value));
		} else if(type == ElementType.CLASS) {
			return root.findElement(By.className(value));
		} else if(type == ElementType.LINK_TEXT) {
			return root.findElement(By.linkText(value));
		} else if(type == ElementType.XPATH) {
			return root.findElement(By.xpath(value));
		} else if(type == ElementType.TAG) {
			return root.findElement(By.tagName(value));
		}
		
		throw new RuntimeException("Incorrect element type");
	}
	
	public void waitForElement(String el, String value, ElementType type) {
		getElement(el, value, type);
	}
	
	/**
	 * @param selector jQuery selector
	 * @param event event name
	 */
	public void fireEvent(String selector, String event) {	
		((JavascriptExecutor) driver).executeScript("$('"+ selector +"').trigger('"+ event +"')");
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
	
}
