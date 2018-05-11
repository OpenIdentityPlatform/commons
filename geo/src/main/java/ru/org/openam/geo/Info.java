package ru.org.openam.geo;

import java.net.InetAddress;
import java.text.MessageFormat;

import com.maxmind.geoip.Location;

public class Info {
	public Location l;
	public String o;
	public InetAddress ip;
	String ipString;
	Throwable e;
	
	public Info(String ipString){
		try{
			this.ip=InetAddress.getByName(ipString.trim());
			final long i=toLong();
			l=Client.city.getLocation(i);
			o=Client.asn.getOrg(i);
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
			return MessageFormat.format("{11} ({3}:{1}/{10}/[{5}~{6}])",
					l.area_code,
					l.city,
					l.countryCode,
					l.countryName,
					l.dma_code,
					l.latitude,
					l.longitude,
					l.metro_code,
					l.postalCode,
					l.region,
					o,
					(ip!=null)?ip.getHostAddress():ipString
					);
		else if (e!=null)
			return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,e.getMessage());
		return MessageFormat.format("{0} ({1})",(ip!=null)?ip.getHostAddress():ipString,"UNKNOWN");
	}	
	
	private long toLong() {
		try{
			byte [] address=ip.getAddress();
	        long ipnum = 0;
	        for (int i = 0; i < 4; ++i) {
	            long y = address[i];
	            if (y < 0) {
	                y+= 256;
	            }
	            ipnum += y << ((3-i)*8);
	        }
	        return ipnum;
		}catch(Throwable e){
			throw new RuntimeException("toLong",e);
		}
    }
}