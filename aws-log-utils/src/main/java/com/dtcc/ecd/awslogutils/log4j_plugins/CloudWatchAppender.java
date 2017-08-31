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

@Plugin(name = "CloudWatchAppender", category = "Core", elementType = "appender", printObject = true)
public class CloudWatchAppender extends AbstractAppender {
		
	private StreamManager streamManager;
	
	private CloudWatchConnector cloudWatchClient;
	private String streamUUID;
	private String SYSID;
	private String applicationName;
	private String instanceIdenitifier;
	private String streamExtension;
	
	private boolean initialized;

	
	public void customInit(CloudWatchConnector cloudWatchClient,
						   String streamUUID, 
						   String SYSID, 
						   String applicationName, 
						   String instanceIdenitifier)
		throws LogException
	{
		validate(streamUUID, "Stream UUID");
		
		validate(SYSID, "SYSID");
		validate(applicationName, "Application Name");
		validate(instanceIdenitifier, "Instance Identifier");
	
		this.cloudWatchClient = cloudWatchClient;
		this.streamUUID = streamUUID;
		this.SYSID = SYSID;
		this.applicationName = applicationName;
		this.instanceIdenitifier = instanceIdenitifier;
		
		streamManager.removeStreams();
		
		initialized = true;
	}
	

	protected CloudWatchAppender(String name, Filter filter,
			Layout<? extends Serializable> layout,
			final boolean ignoreExceptions, String streamExtension) {
		super(name, filter, layout, ignoreExceptions);
		
		streamManager = new StreamManager();
		
		this.streamExtension = streamExtension;
		
		initialized = false;
	}

	@PluginFactory
	public static CloudWatchAppender createAppender(
			@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter,
			@PluginAttribute("streamExtension") String streamExtension) {
		if (name == null) {
			LOGGER.error("No name provided for CloudWatchAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}		
				
		return new CloudWatchAppender(name, filter, layout, true, streamExtension);
	}

	@Override
	public void append(LogEvent event) 
	{	
		
		if (!initialized)
			throw new AppenderLoggingException("CloudWatchAppender is not initialzed");		
				
		try {
			byte[] rawMessage = getLayout().toByteArray(event);		
			String formattedMessage = new String(rawMessage);

			Stream s = streamManager.getStream(cloudWatchClient, getGroupName(), getStreamName());
			s.publishEvent(cloudWatchClient, formattedMessage);
			
		} catch (LogException | ResiliencyException e) {
			throw new AppenderLoggingException(e.getMessage());
		}
	}
	
	private String getGroupName()		
	{	
		return new String(LogConstants.CLOUDWATCH_APP_GROUP_PREFIX + SYSID + "/" + applicationName).toUpperCase();
	}
	
	private String getStreamName()
		throws LogException
	{				
		String date 		= LoggingUtils.getUTCDate();
		String threadID		= Thread.currentThread().getName();
		
		return String.format("%s.%s.%s.%s.%s.%s", date, applicationName, instanceIdenitifier, threadID, streamUUID, streamExtension).toLowerCase();
	}
	

	private void validate(String str, String name)
		throws LogException
	{
		if (str == null || str.isEmpty())
			throw new LogException("Missing: " + name + " from appender configuration");
	}
	
}
