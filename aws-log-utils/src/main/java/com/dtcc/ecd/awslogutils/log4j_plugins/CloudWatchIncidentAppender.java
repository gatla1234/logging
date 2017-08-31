package com.dtcc.ecd.awslogutils.log4j_plugins;

import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.dtcc.ecd.awslogutils.cloudwatch.CloudWatchConnector;
import com.dtcc.ecd.awslogutils.cloudwatch.LogConstants;
import com.dtcc.ecd.awslogutils.cloudwatch.LoggingUtils;
import com.dtcc.ecd.awslogutils.cloudwatch.Stream;
import com.dtcc.ecd.awslogutils.cloudwatch.StreamManager;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

@Plugin(name = "CloudWatchIncidentAppender", category = "Core", elementType = "appender", printObject = true)
public class CloudWatchIncidentAppender extends AbstractAppender {
		
	private StreamManager streamManager;
	
	private CloudWatchConnector cloudWatchClient;
	private String SYSID;
	private String applicationName;
	
	private boolean initialized;
	
	
	public void customInit(CloudWatchConnector cloudWatchClient, String SYSID, String applicationName)
		throws LogException
	{
		this.SYSID = SYSID;
		this.applicationName = applicationName;
		this.cloudWatchClient = cloudWatchClient;
		
		streamManager.removeStreams();
		
		initialized = true;
	}
	
	
	protected CloudWatchIncidentAppender(String name, Filter filter,
			Layout<? extends Serializable> layout,
			final boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions);
		
		streamManager = new StreamManager();
		
		initialized = false;
	}

	@PluginFactory
	public static CloudWatchIncidentAppender createAppender(
			@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter,
			@PluginAttribute("otherAttribute") String otherAttribute) {
		if (name == null) {
			LOGGER.error("No name provided for CloudWatchAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}
		
		return new CloudWatchIncidentAppender(name, filter, layout, true);
	}

	@Override
	public void append(LogEvent event) 
	{	
		//TODO: handle this better
		if (!initialized)
			throw new AppenderLoggingException("CloudWatchIncidentAppender is not initialzed");
		
		try {
			
			byte[] rawMessage = getLayout().toByteArray(event);			
			String formattedMessage = new String(rawMessage);
						
			Stream s = streamManager.getStream(cloudWatchClient, getGroupName(), getStreamName(event));
			s.publishEvent(cloudWatchClient, formattedMessage);
			
		} catch (LogException | ResiliencyException e) {			
			throw new AppenderLoggingException(e.getCause());
		}		

	}
	
	private String getGroupName()
		throws LogException
	{	
		return LogConstants.SHARED_INCIDENT_LOG_GROUP;		
	}
	
	private String getStreamName(LogEvent event)
		throws LogException
	{		
		String date = LoggingUtils.getUTCDate();
		
		return String.format("%s.%s.%s.%s.incident", date, SYSID, applicationName, LoggingUtils.getUUID()).toLowerCase();
	}
	
}
