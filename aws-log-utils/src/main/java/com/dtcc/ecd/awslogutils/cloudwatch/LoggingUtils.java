package com.dtcc.ecd.awslogutils.cloudwatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;

import com.dtcc.ecd.awslogutils.exception.LogException;

public class LoggingUtils {
	
	
	private static final int CONNECTION_TIMEOUT = 3 * 1000;
	
	static public String getUTCDate()
	{		
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		return dateFormatGmt.format(new Date());
	}
	
	static public String getTimeStamp()
	{		
		SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatGmt.format(new Date()) + " (GMT)";
	}
	
	static public String getUUID()
	{
		UUID uuid = UUID.randomUUID();		
		return uuid.toString().replace("-", "");
	}
	
	static public String getHostName()
	{
		String hostName;
		
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostName = "instance-" + LoggingUtils.getUUID();
		}
		
		return hostName;
	}
	
	static public String getEC2InstanceName()
		throws LogException
	{
		
		// determine the name of the instance we're currently running in
		String EC2Id = "";
		String inputLine;
		
		BufferedReader in = null;
		URL EC2MetaData = null;
		
		try {
			EC2MetaData = new URL("http://169.254.169.254/latest/meta-data/instance-id");			
		} catch (MalformedURLException e) {			
			throw new LogException(e.getMessage());
		}
		
		URLConnection EC2MD;
		
		try {
			EC2MD = EC2MetaData.openConnection();
			EC2MD.setConnectTimeout(CONNECTION_TIMEOUT);
			
			in = new BufferedReader(new InputStreamReader(EC2MD.getInputStream()));
			
			while ((inputLine = in.readLine()) != null)
			{	
				EC2Id = inputLine;
				
				// prevent things from looking strange
				if (EC2Id.length() >= 100)
					break;
			}		
			
			Validator.validateCloudWatchComponentString(EC2Id);			
			
			
		} catch (IOException e) {
			throw new LogException(e.getMessage());			
		}
		finally{
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					throw new LogException(e.getMessage());
				}
		}
		
		return EC2Id;
	}
	
	
	public static void cleanLog4jTheadContext()
	{
		Map<String, String> tcMap = ThreadContext.getImmutableContext();
		
		for (String s : tcMap.keySet())
			if (!s.startsWith(LogConstants.LOG4J_THREADCONTEXT_PREFIX))
				ThreadContext.remove(s);
	}
}
