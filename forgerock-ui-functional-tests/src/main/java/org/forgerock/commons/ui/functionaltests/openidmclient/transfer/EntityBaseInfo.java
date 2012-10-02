package org.forgerock.commons.ui.functionaltests.openidmclient.transfer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityBaseInfo extends BaseResult{

	private String _id;
	private String _rev;
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String get_rev() {
		return _rev;
	}
	public void set_rev(String _rev) {
		this._rev = _rev;
	}
	
	@Override
	public String toString() {
		return "EntityBaseInfo [_id=" + _id + ", _rev=" + _rev + "]";
	}
	
}
