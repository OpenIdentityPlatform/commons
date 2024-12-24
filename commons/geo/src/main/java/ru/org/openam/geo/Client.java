package ru.org.openam.geo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.geoip2.DatabaseReader;

public class Client {
	final static Logger logger = LoggerFactory.getLogger(Client.class);

	static DatabaseReader city;
	static DatabaseReader asn;
	static {
		try {
			city=new DatabaseReader.Builder(Client.class.getResource("/GeoLite2-City.mmdb").openStream()).fileMode(FileMode.MEMORY).build();
			asn=new DatabaseReader.Builder(Client.class.getResource("/GeoLite2-ASN.mmdb").openStream()).fileMode(FileMode.MEMORY).build();
		} catch (Throwable e) {
			logger.error("error initializing client: {}", e.getMessage());
			throw new RuntimeException("init", e);
		}
	}

	public static Info get(HttpServletRequest request){
		if (request==null)
			throw new IllegalArgumentException("request==null");
		return get(request.getRemoteAddr());
	}
	
	public static List<Info> get(HttpServletRequest request,String header){
		if (request==null)
			throw new IllegalArgumentException("request==null");
		return getList(request.getHeader(header));
	}
	
	static final Cache<String, Info> cache = CacheBuilder.newBuilder()
			.maximumSize(64000)
			.expireAfterAccess(60, TimeUnit.SECONDS)
			.build();
	
	public static Info get(final String ipAddress) {
		Info res=null;
		if (StringUtils.isNotBlank(ipAddress)) {
			try {
				res=cache.get(ipAddress, new Callable<Info>() {
					@Override
					public Info call() throws Exception {
						Info res=new Info(ipAddress);
						return res;
					}
				});
			}catch (ExecutionException e) {}
		}
		if (logger.isDebugEnabled())
			logger.debug("{}", res);
		return res;
	}
	
	public static List<Info> getList(String ipAddress){
		if (StringUtils.isBlank(ipAddress))
			return null;
		final String[] in=ipAddress.split(",");
		final ArrayList<Info> res=new ArrayList<Info>(in.length);
		for (String ip : in) {
			final Info info=get(ip);
			if (info!=null)
				res.add(info);
		}
		return res;
	}
}
