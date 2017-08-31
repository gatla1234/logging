package com.dtcc.ecd.awslogutils.cloudwatch;

public class LogConstants {
	
	
	// Name of the share incident log group
	public final static String SHARED_INCIDENT_LOG_GROUP 		= "SharedApplicationIncidentLogs"; 
		

	// Cloudwatch constants
	public static final String CLOUDWATCH_APP_GROUP_PREFIX 		= "/DTCC/AD_SOFTWARE/";
	
	
	// log4j constants
	public static final String LOG4J_INTERNAL_CONFIG_XML 				= "dtcc-cloudnative-logger-log4j.xml";
	public static final String LOG4J_CLOUDWATCH_APPENDER_NAME 			= "SimpleLayoutCloudWatchAppender";
	public static final String LOG4J_BULK_CLOUDWATCH_APPENDER_NAME 		= "SimpleLayoutCloudWatchBuklAppender";
	public static final String LOG4J_CLOUDWATCH_SPLUNK_APPENDER_NAME    = "SplunkLayoutCloudWatchAppender";
	public static final String LOG4J_CLOUDWATCH_INCIDENT_APPENDER_NAME 	= "CloudWatchIncidentAppender";	
	
	
	// log4j ThreadContext elements
	public static final String LOG4J_THREADCONTEXT_SYSID			=  "__LOG4J_THREADCONTEXT_SYSID__";
	public static final String LOG4J_THREADCONTEXT_APPLICATION		=  "__LOG4J_THREADCONTEXT_APPLICATION__";
	public static final String LOG4J_THREADCONTEXT_INCIDENT_ORIGIN	=  "__LOG4J_THREADCONTEXT_INCIDENT_ORIGIN__";
	
	public static final String LOG4J_THREADCONTEXT_PREFIX		=  "__LOG4J_THREADCONTEXT";
	
	public static String getAppIncidentLogStreamName(String sysID, String applicationName)
	{
		return sysID + "-" + applicationName + "-" + LoggingUtils.getUUID();
	}
}
