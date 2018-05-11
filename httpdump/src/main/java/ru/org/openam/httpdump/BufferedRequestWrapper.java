package ru.org.openam.httpdump;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BufferedRequestWrapper extends HttpServletRequestWrapper {
	final static Logger logger = LoggerFactory.getLogger(BufferedRequestWrapper.class.getName());
	byte[] body=null;

    public BufferedRequestWrapper(HttpServletRequest httpServletRequest) {
        super(httpServletRequest);
        setAttribute(BufferedRequestWrapper.class.getName(), this);
    }

    private void readBody(){
    	if (body==null 
    			&& !StringUtils.containsIgnoreCase(getContentType(), "application/x-www-form-urlencoded")
    			&& !StringUtils.containsIgnoreCase(getContentType(), "multipart/form-data")
    		)
	    	try{
	 	        InputStream is = super.getInputStream();
	 	        body = IOUtils.toByteArray(is);
	 	        is.close();
	         }catch(IOException e){
	         	logger.warn("{}: {}",e.getMessage(),Dump.toString(getRequest()));
	         }
    }
    
    @Override
    public ServletInputStream getInputStream() throws IOException {
    	readBody();
    	if (body==null)
    		return super.getInputStream();
    	return new ServletInputStreamImpl(new ByteArrayInputStream(body));
    }

    @Override
    public BufferedReader getReader() throws IOException {
    	readBody();
    	if (body==null)
    		return super.getReader();
        String enc = getCharacterEncoding();
        if(enc == null) enc = "UTF-8";
        return new BufferedReader(new InputStreamReader(getInputStream(), enc));
    }

    private class ServletInputStreamImpl extends ServletInputStream {
        private InputStream is;

        public ServletInputStreamImpl(InputStream is) {
            this.is = is;
        }

        public int read() throws IOException {
            return is.read();
        }

        public boolean markSupported() {
            return false;
        }

        public synchronized void mark(int i) {
            throw new RuntimeException(new IOException("mark/reset not supported"));
        }

        public synchronized void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }
    }
    
    public String getRequestBody() {
    	readBody();
    	if (body==null)
    		return MessageFormat.format("{0}:{1}", getContentType(),getContentLength());
		try{
	    	BufferedReader reader = getReader();
			String line = null;
			StringBuilder inputBuffer = new StringBuilder();
			do {
				line = reader.readLine();
				if (null != line) 
					inputBuffer.append(line.trim());
			} while (line != null);
			reader.close();
			return inputBuffer.toString().trim();
		}catch (IOException e) {
			return null;
		}
	}

}

