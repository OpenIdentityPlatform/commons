package org.forgerock.commons.ui.functionaltests.helpers;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.JsonNode;
import org.forgerock.commons.ui.functionaltests.helpers.SeleniumHelper.ElementType;
import org.forgerock.commons.ui.functionaltests.utils.JsonUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.stereotype.Component;

@Component
public class FormsHelper {
	
    @Inject
	private WebDriver driver;

    @Inject
	private SeleniumHelper selenium;
    
    @Inject
	private WebDriverWait wait;
    
    @Inject
    private JsonUtils jsonUtils;
    
	/**
	 * Sets the field value and fire event.
	 * @param el id of root element
	 * @param name field name
	 */
	public void setField(String el, String name, String value) {
		WebElement element = selenium.getElement(el, name, ElementType.NAME);
		String tagName = element.getTagName();
		if (tagName.equals("input")) {
			if(element.getAttribute("type").equals("checkbox")) {
				if(value.equals("true")) {
					 ((JavascriptExecutor) driver).executeScript("$('#"+ el +" input[name="+ name +"]').attr('checked', true);");
				} else {
					((JavascriptExecutor) driver).executeScript("$('#"+ el +" input[name="+ name +"]').attr('checked', false);");
				}
			} else {
				try{
					element.clear();
				} catch (Exception e) {
					// TODO: handle exception
				}
				
				element.sendKeys(value);
			}
		} else if (tagName.equals("select")) {
			Select selectBox = new Select(element);
			selectBox.selectByValue(value);
		} else {
			throw new IllegalStateException("No implementation for type " + tagName);
		}
		
		
		String event = element.getAttribute("data-validator-event");
		if(event != null) {
			selenium.fireEvent("#"+ el +" input[name="+ name +"]", event);
		} else {
			selenium.fireEvent("#"+ el +" input[name="+ name +"]", "change");
		}
	}
	
	public String getFieldValue(String el, String name) {
		WebElement element = null;
		if (name != null) {
			element = selenium.getElement(el, name, ElementType.NAME);
		} else {
			element = selenium.getElement(el, el, ElementType.ID);
		}
		String tagName = element.getTagName();
		if (tagName.equals("input")) {
			if(element.getAttribute("type").equals("checkbox")) {
				String checked = element.getAttribute("checked");
				
				if(checked != null && checked.equals("true")) {
					return "true";
				}
				
				return "false";
			}
			
			return element.getAttribute("value");
		} else if (tagName.equals("select")) {
			Select selectBox = new Select(element);
			return selectBox.getFirstSelectedOption().getAttribute("value");
		} else if (tagName.equals("div")) {
			return element.getText();
		} else {
			throw new IllegalStateException("No implementation for type " + tagName);
		}
	}
	
	public void submit(final String el, final String name) {
		wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.findElement(By.name(name)).getAttribute("class").contains("orange");
            }
        });
		
		selenium.getElement(el, name, ElementType.NAME).click();
	}
	
	/**
	 * @param el id of root element
	 * @param json object to fill the form
	 */
	public void fillForm(String el, JsonNode json) {
		String jsonStr = jsonUtils.jsonToString(json);
		
		try {
			((JavascriptExecutor) driver).executeScript("js2form(document.getElementById('"+ el +"'), "+ jsonStr +");");
		} catch (WebDriverException e) {
			System.err.println(e.getMessage());
		}
		
		if ( !readForm(el).equals(json)) {
			fillForm(el, json);
		}
	}
	
	/**
	 * @param el id of root element
	 */
	public JsonNode readForm(String el) {
		String json = null;
		try {
			json = (String) ((JavascriptExecutor) driver).executeScript("return JSON.stringify(form2js('"+ el +"', '.', false));");
		} catch (WebDriverException e) {
			// somethimes it does not work for the first time
			json = (String) ((JavascriptExecutor) driver).executeScript("return JSON.stringify(form2js('"+ el +"', '.', false));");

		}
		
		//form2js includes also buttons...
		//it's a quick fix for that
		json = json.replaceAll(",\"[a-zA-Z]+utton\":\"\"", "");		
		
		return jsonUtils.readJsonFromString(json);
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
	
	public void assertFormValidationError(final String el) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation for form returned 'valid'. Expected: 'invalid'";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElements(By.cssSelector("#"+ el +" [data-validation-status=error]")).size() > 0;
			}
		}.checkAssertion();
	}
	
	public void assertFormValidationPasses(final String el) {	
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation for form returned 'invalid'. Expected: 'valid'";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElements(By.cssSelector("#"+ el +" [data-validation-status=error]")).size() == 0;
			}
		}.checkAssertion();
	}
	
	public void assertValidationPasses(final String el, final String name) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation for " + name + " returned 'invalid'. Expected: 'valid'";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElements(By.cssSelector("#"+ el +" [name="+ name +"][data-validation-status=error]")).size() == 0;
			}
		}.checkAssertion();
		//TODO checking for tick
	}
	
	public void assertValidationError(final String el, final String name) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation for " + name + " returned 'valid'. Expected: 'invalid'";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElements(By.cssSelector("#"+ el +" [name="+ name +"][data-validation-status=error]")).size() != 0;
			}
		}.checkAssertion();
	}

	public String getSelectDisplayValue(String el, String name) {
		WebElement element = selenium.getElement(el, name, ElementType.NAME);
		Select selectBox = new Select(element);
		if (selectBox.getAllSelectedOptions().size() == 0 ) {
			return null;
		}
		return selectBox.getFirstSelectedOption().getText();
	}

	public void assertFormFieldHasValue(final String element, final String fieldName, final String expectedValue) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation of form value returned different value "+ getFieldValue(element, fieldName);
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return getFieldValue(element, fieldName).equals(expectedValue);
			}
		}.checkAssertion();
	}

	public void assertValidationDisabled(final String el, final String fieldName) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Validation for " + fieldName + " returned 'enabled'. Expected: 'disabled'";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				return driver.findElements(By.cssSelector("#"+ el +" [name="+ fieldName +"][data-validation-status=disabled]")).size() != 0;
			}
		}.checkAssertion();
	}

	public String getValueForContentFlow(String el) {
		WebElement element = selenium.getElement(el, el, ElementType.ID);
		return element.findElement(By.className("active")).findElement(By.className("portray")).getAttribute("data-site-image");
	}

	public void changeValueForContentFlow(final String el, final String valueAsText) {
		selenium.new AssertionWithTimeout() {
			@Override
			protected String getAssertionFailedMessage() {
				return "Failed to set content flow value";
			}
			@Override
			protected boolean assertionCondition(WebDriver driver) {
				WebElement element = selenium.getElement(el, el, ElementType.ID);
				return element.findElement(By.cssSelector("[data-site-image='"+valueAsText+"']")).isDisplayed();
			}
		}.checkAssertion();
		
		WebElement element = selenium.getElement(el, el, ElementType.ID);
		element.findElement(By.cssSelector("[data-site-image='"+valueAsText+"']")).click();
	}
	
}
