package org.forgerock.commons.ui.functionaltests.constants;

public enum Consts {

	BASE_PAGE("base_page", "http://localhost:28080/"), 
	WEB_BROWSER("web_browser", "FIREFOX"),
	WAIT_TIME("wait_time", "1"),
	OPENIDM_SERVER("openidm_server", "http://localhost:8080/");

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
