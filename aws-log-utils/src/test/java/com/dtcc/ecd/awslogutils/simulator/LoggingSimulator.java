package com.dtcc.ecd.awslogutils.simulator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import com.dtcc.ecd.awsutils.auth.AWSAuthUtils;
import com.dtcc.ecd.awsutils.log.AWSAuthSimpleLogger;
import com.dtcc.ecd.awslogutils.BuiltinLoggers;
import com.dtcc.ecd.awslogutils.DefaultConnector;
import com.dtcc.ecd.awslogutils.LoggerFactory;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public class LoggingSimulator {

	public static void main(String[] args) throws LogException {

		//
		// These variables define the behavior of the simulator
		//
		int numLoggerTheads = 100;
		int logEventsPerThread = 10000;
		boolean recreateLogGroup = false;

		ExecutorService loggingThreadPool;

		AWSAuthUtils awsAuthUtils = new AWSAuthUtils(new AWSAuthSimpleLogger(true));
		DefaultConnector cwConnector;

		try {
			if (awsAuthUtils.isUseProxy()) {
				cwConnector = new DefaultConnector(awsAuthUtils.getCredentials(),
						awsAuthUtils.getClientConfiguration());
			} else {
				cwConnector = new DefaultConnector(awsAuthUtils.getCredentials(), null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		LoggerFactory.configureLog4j("ECD", "LoggingSimulator", cwConnector, false, false);
		Logger consoleLogger = LoggerFactory.getEC2Logger(BuiltinLoggers.CONSOLE_LOGGER);
		consoleLogger.info("starting...");

		loggingThreadPool = Executors.newFixedThreadPool(numLoggerTheads);

		String testLogGroup = "/DTCC/AD_SOFTWARE/ECD/LOGGINGSIMULATOR".toUpperCase();

		if (recreateLogGroup) {
			consoleLogger.info("Cleaning up log group: " + testLogGroup);
			try {
				cwConnector.deleteLogGroup(testLogGroup);
			} catch (LogException | ResiliencyException e) {
				consoleLogger.info("There was an error deleting the log group, because: " + e);
			}
		}

		for (int i = 0; i < numLoggerTheads; i++) {
			loggingThreadPool.execute(new SimulatorThread(i, logEventsPerThread));
		}

		loggingThreadPool.shutdown();

		consoleLogger.info("Simulator is waiting for threads to complete");

		try {
			loggingThreadPool.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			consoleLogger.error("There was an error while waiting for thread pool to terminate: " + e);
			return;
		}

		consoleLogger.info("All Simulator threads are completed");

	}
}
