package ru.org.openam.geo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip.LookupService;

public class Client {
	final static Logger logger = LoggerFactory.getLogger(Client.class);

	static LookupService city;
	static LookupService asn;
	static Ehcache cache=null;
	static {
		try {
			File imageFile = File.createTempFile("GeoIPCity"+UUID.randomUUID().toString(),null);
		    IOUtils.copy(Client.class.getResource("/GeoIPCityFree.dat").openStream(),FileUtils.openOutputStream(imageFile));
			city = new LookupService(imageFile, LookupService.GEOIP_MEMORY_CACHE);
			imageFile.deleteOnExit();
			
			imageFile = File.createTempFile("GeoIPASNum"+UUID.randomUUID().toString(),null);
		    IOUtils.copy(Client.class.getResource("/GeoIPASNum.dat").openStream(),FileUtils.openOutputStream(imageFile));
			asn = new LookupService(imageFile, LookupService.GEOIP_MEMORY_CACHE);
			imageFile.deleteOnExit();
			
			CacheManager cacheManager=CacheManager.getCacheManager(null);
			if (cacheManager==null)
				cacheManager=CacheManager.getInstance();
			cache=cacheManager.getEhcache(Client.class.getName());
			if (cache==null)
				synchronized (Client.class.getName()) {
					cache=cacheManager.getEhcache(Client.class.getName());
					if (cache==null){
			    		try{
			    			CacheConfiguration conf=new CacheConfiguration(Client.class.getName(), 20000);
			    			conf.setMaxElementsOnDisk(0);
			    			conf.setTimeToIdleSeconds(600);
			    			conf.setTimeToLiveSeconds(3600);
			    			conf.setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy.LFU);
			    			conf.setStatistics(false);
			    			cacheManager.addCache(new Cache(conf));
			    		}catch(net.sf.ehcache.ObjectExistsException e){}
			    		cache=cacheManager.getEhcache(Client.class.getName());
			    		logger.warn("not found ({})",cache);
			    	}else if (cache.getKeys().size()>0)
			    		logger.warn("re-found ({}) with {} values {}",new Object[]{cache,cache.getKeys().size()});
			    	else
			    		logger.info("found ({})",cache);
				}
			new Thread("clean-"+cache.getName()){
				@Override
				public void run() {
					super.run();
					try{
						while(true){
							sleep(cache.getCacheConfiguration().getTimeToLiveSeconds()*3*1000);
							cache.evictExpiredElements();
						}
					}catch(InterruptedException e){}
				}
			}.start();
		} catch (Throwable e) {
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
	
	public static Info get(String ipAddress) {
		Info res=null;
		if (!StringUtils.isBlank(ipAddress)){
			Element el=cache.get(ipAddress);
			if (el==null||el.isExpired())
				res = new Info(ipAddress);
			else{
				res=(Info)el.getObjectValue();
				if (logger.isDebugEnabled())
					logger.debug("from cache {}",el);
			}
			if ((el==null||el.isExpired())&&res!=null)
				cache.put(new Element(ipAddress, res));
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
