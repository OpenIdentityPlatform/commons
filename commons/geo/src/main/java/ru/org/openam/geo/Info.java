/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2020-2024 3A Systems LLC.
 */

package ru.org.openam.geo;

import java.net.InetAddress;
import java.text.MessageFormat;

import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;

public class Info {
	public CityResponse l;
	public AsnResponse o;
	public InetAddress ip;
	String ipString;

	Throwable e;
	
	public Info(String ipString){
		try{
			this.ip=InetAddress.getByName(ipString.trim());
			l=Client.city.city(ip);
			o=Client.asn.asn(ip);
		}catch(Throwable e){
			this.e=e;
			this.ipString=ipString;
		}
	}
	
	public Boolean isLAN(){
		return ip!=null && (ip.isSiteLocalAddress()||ip.isLoopbackAddress());
	}
	
	@Override
	public String toString() {
		if (ip!=null&&ip.isSiteLocalAddress())
			return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,"LAN");
		else if (ip!=null&&ip.isLoopbackAddress())
			return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,"LOCALHOST");
		else if (l!=null)
			return MessageFormat.format("{0} ({1}:{2}/{3})",
					(ip!=null)?ip.getHostAddress():ipString,
					l.getCountry().getName(),//countryName,
					l.getCity().getName(),//city,
					o==null?null:o.getAutonomousSystemOrganization()
					);
		else if (e!=null)
			return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,e.getMessage());
		return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,"UNKNOWN");
	}
	public Throwable getThrowable() {
		return e;
	}
}