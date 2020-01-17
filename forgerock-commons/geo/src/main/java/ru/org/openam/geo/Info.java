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

}