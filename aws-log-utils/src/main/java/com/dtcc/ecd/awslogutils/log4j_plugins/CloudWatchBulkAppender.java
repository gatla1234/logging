package com.dtcc.ecd.awslogutils.log4j_plugins;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;
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

import com.amazonaws.services.logs.model.InputLogEvent;
import com.dtcc.ecd.awslogutils.BuiltinLoggers;
import com.dtcc.ecd.awslogutils.LoggerFactory;
import com.dtcc.ecd.awslogutils.cloudwatch.CloudWatchConnector;
import com.dtcc.ecd.awslogutils.cloudwatch.LogConstants;
import com.dtcc.ecd.awslogutils.cloudwatch.LoggingUtils;
import com.dtcc.ecd.awslogutils.cloudwatch.Stream;
import com.dtcc.ecd.awslogutils.cloudwatch.StreamManager;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Plugin(name = "CloudWatchBulkAppender", category = "Core", elementType = "appender", printObject = true)
public class CloudWatchBulkAppender extends AbstractAppender {

	private ConcurrentLinkedQueue<QueuedLogEvent> eventQueue;

	private StreamManager streamManager;
	private CloudWatchConnector cloudWatchClient;

	private String streamExtension;
	private String streamUUID;
	private String SYSID;
	private String applicationName;

	private CloudWatchEventPublisher publisherThread;

	ExecutorService pubThreadPool;

	private boolean initialized;

	public void customInit(CloudWatchConnector cloudWatchClient, String streamUUID, String SYSID,
			String applicationName) throws LogException {
		validate(streamUUID, "Stream UUID");

		validate(SYSID, "SYSID");
		validate(applicationName, "Application Name");

		this.cloudWatchClient = cloudWatchClient;

		this.streamUUID = streamUUID;
		this.SYSID = SYSID;
		this.applicationName = applicationName;

		streamManager.removeStreams();

		publisherThread = new CloudWatchEventPublisher();

		initialized = true;
	}

	protected CloudWatchBulkAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			final boolean ignoreExceptions, String streamExtension) {
		super(name, filter, layout, ignoreExceptions);

		this.streamExtension = streamExtension;
		eventQueue = new ConcurrentLinkedQueue<QueuedLogEvent>();
		streamManager = new StreamManager();

		initialized = false;
	}

	@PluginFactory
	public static CloudWatchBulkAppender createAppender(@PluginAttribute("name") String name,
			@PluginElement("Layout") Layout<? extends Serializable> layout,
			@PluginElement("Filter") final Filter filter, @PluginAttribute("streamExtension") String streamExtension) {
		if (name == null) {
			LOGGER.error("No name provided for CloudWatchAppender");
			return null;
		}
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		return new CloudWatchBulkAppender(name, filter, layout, true, streamExtension);
	}

	@Override
	public void append(LogEvent event) {
		if (!initialized)
			throw new AppenderLoggingException("CloudWatchAppender is not initialzed");

		byte[] rawMessage = getLayout().toByteArray(event);
		String formattedMessage = new String(rawMessage);

		if (!eventQueue.offer(new QueuedLogEvent(event.getTimeMillis(), formattedMessage)))
			throw new AppenderLoggingException("Could not add message to eventqueue because it's full");

		// ensure that only one instance of this thread is running
		if (publisherThread.reserve()) {
			pubThreadPool = Executors.newSingleThreadExecutor();
			pubThreadPool.execute(publisherThread);
			pubThreadPool.shutdown();
		}

	}

	private String getStreamNameSuffix() {
		return String.format("%s.%s.%s.%s", SYSID, applicationName, streamUUID, streamExtension).toLowerCase();
	}

	private String getGroupName() {
		return new String(LogConstants.CLOUDWATCH_APP_GROUP_PREFIX + SYSID + "/" + applicationName).toUpperCase();
	}

	private void validate(String str, String name) throws LogException {
		if (str == null || str.isEmpty())
			throw new LogException("Missing: " + name + " from appender configuration");
	}

	//
	// Nested Class responsible for the actual publishing of events
	//
	public class CloudWatchEventPublisher implements Runnable {

		// these limitations are imposed by AWS
		final int MAX_BATCH_SIZE_BYTES = 1048576;
		final int MAX_EVENTS_PER_BATCH = 10000;

		boolean running;

		// allow only one instance of this thread to run.
		Semaphore instanceLimiter = new Semaphore(1);

		public boolean reserve() {
			return instanceLimiter.tryAcquire();
		}

		public void release() {
			instanceLimiter.release();
		}

		@Override
		public void run() {
			
			Logger consoleLogger = null;
						
			try {
				consoleLogger = LoggerFactory.getEC2Logger(BuiltinLoggers.CONSOLE_LOGGER);
			} catch (LogException e1) {
				
			}

			consoleLogger.debug("processing next batch");

			try{
				List<InputLogEvent> nextBatch = getNextBatch();
	
				while (!nextBatch.isEmpty()) {
					Stream s = null;
					try {
						Collections.sort(nextBatch, new Comparator<InputLogEvent>() {
							@Override
							public int compare(InputLogEvent o1, InputLogEvent o2) {
								return o1.getTimestamp().compareTo(o2.getTimestamp());
							}
						});
	
						s = streamManager.getStream(cloudWatchClient, getGroupName(),
								LoggingUtils.getUTCDate() + "." + getStreamNameSuffix());
						s.publishEventBatch(cloudWatchClient, nextBatch);
	
						consoleLogger.debug("Published: " + nextBatch.size() + " events to cloudwatch");
	
					} catch (LogException | ResiliencyException e) {
						// TODO: do something better
						consoleLogger.debug("Cannot get stream: " + e);
					}
	
					nextBatch = getNextBatch();
				}
			}
			finally
			{
				// guarantee the release, so this thread may be called again
				release();
			}
		}

		//
		// returns the largest possible back of events given the events that are
		// currently
		// in the queue.
		//
		public List<InputLogEvent> getNextBatch() {
			// try and generate the largest possible batches
			int sleepTimeSec = 1;
			int sleepCycles = 5;

			List<InputLogEvent> nextBatch = new ArrayList<InputLogEvent>();

			int batchSize = 0;
			int batchCount = 0;

			QueuedLogEvent queuedEvent = null;

			// do this until my batch grows to the max size or until I run out
			// of time
			while (true) {
				// peek at the event first to see if it fits into the current
				// batch
				queuedEvent = eventQueue.peek();

				// consider the batch completed if nothing shows up in the queue
				// for some time
				if (queuedEvent == null) {
					if (sleepCycles-- < 0)
						return nextBatch;
					else {
						try {
							Thread.sleep(sleepTimeSec * 1000);
						} catch (InterruptedException e) {
							return nextBatch;
						}

						continue;
					}
				}

				String logText = queuedEvent.getFormattedMessage();

				batchSize += logText.getBytes(StandardCharsets.UTF_8).length + 26;

				// check if the event will fit in the batch size
				if (batchSize >= MAX_BATCH_SIZE_BYTES || batchCount++ >= MAX_EVENTS_PER_BATCH)
					return nextBatch;
				else
					eventQueue.poll();

				InputLogEvent cwEvent = new InputLogEvent().withMessage(logText)
						.withTimestamp(queuedEvent.getEventTimeStamp());

				nextBatch.add(cwEvent);
			}
		}
	}
}
