package org.forgerock.commons.ui.functionaltests.helpers;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import org.testng.Assert;

@Component
public class FormsHelper {
	
    @Inject
	private WebDriver driver;

    @Inject
	private SeleniumHelper selenium;
    
    @Inject
    private JsonUtils jsonUtils;
    
	/**
	 * Sets the field value and fire event.
	 * @param el id of root element
	 * @param name field name
	 */
	public void setField(String el, String name, String value) {
		WebElement element = selenium.getElement(el, name, ElementType.NAME);
		element.sendKeys(value);
		
		String event = element.getAttribute("data-validator-event");
		if(event != null) {
			selenium.fireEvent("#"+ el +" input[name="+ name +"]", event);
		} else {
			selenium.fireEvent("#"+ el +" input[name="+ name +"]", "change");
		}
	}
	
	public void getFieldValue(String el, String name) {
		//TODO
	}
	
	public void submit(String el, String name) {
		selenium.getElement(el, name, ElementType.NAME).click();
	}
	
	/**
	 * @param el id of root element
	 * @param json object to fill the form
	 */
	public void fillForm(String el, JsonNode json) {
		String jsonStr = jsonUtils.jsonToString(json);
		
		((JavascriptExecutor) driver).executeScript("js2form(document.getElementById('"+ el +"'), "+ jsonStr +");");
	}
	
	public JsonNode readForm(String el) {
		//this method shoud execute js script, which will use form2js to
		//read the form
		return null;
	}
	
	/**
	 * Triggers all validators in form.
	 * @param el id of root element
	 */
	public void validateForm(String el) {
		List<WebElement> fields = driver.findElements(By.cssSelector("#"+ el +" [data-validator]"));

		for(WebElement field : fields) {
			String event = field.getAttribute("data-validator-event");
			String name = field.getAttribute("name");
			String tag = field.getTagName();
			
			if(event != null) {				
				selenium.fireEvent("#"+ el +" "+ tag +"[name="+ name +"]", event);
			} else {
				selenium.fireEvent("#"+ el +" "+ tag +"[name="+ name +"]", "change");
			}
		}		
	}
	
	public void assertFormValidationPasses(final String el) {
		//TODO move webDriverWait to @Inject
		
		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.findElements(By.cssSelector("#"+ el +" [data-validation-status=error]")).size() == 0;
            }
        });
		
		Assert.assertEquals(driver.findElements(By.cssSelector("#"+ el +" [data-validation-status=error]")).size(), 0);
	}
	
	public void assertValidationPasses(String el, String name) {
		//TODO
	}
	
	public void assertValidationError(String el, String name) {
		//TODO
	}
	
}
