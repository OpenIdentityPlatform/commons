package org.forgerock.commons.ui.functionaltests.helpers;

public enum Consts {

	BASE_PAGE("base_page", "http://localhost:28080/"), 
	WEB_BROWSER("web_browser", "FIREFOX");

	private String key;
	private Object value;

	private Consts(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

}
