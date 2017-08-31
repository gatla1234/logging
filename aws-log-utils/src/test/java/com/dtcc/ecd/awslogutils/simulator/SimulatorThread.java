package com.dtcc.ecd.awslogutils.simulator;

import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.dtcc.ecd.awslogutils.BuiltinLoggers;
import com.dtcc.ecd.awslogutils.LoggerFactory;
import com.dtcc.ecd.awslogutils.exception.LogException;

public class SimulatorThread implements Runnable {
	
	int threadNumber;
	int numMessages;
	Logger logger;
	
	static int message_id = 0;
	
	public SimulatorThread(int threadNumber, int numMessages)
	{
		this.threadNumber = threadNumber;
		this.numMessages = numMessages;
	}
	
	
	public Logger getRandomLogger() throws LogException
	{
		return LoggerFactory.getEC2Logger(BuiltinLoggers.SIMPLE_LOGGER);
		
		/*if (Math.random() < 0.5)		
			return LoggerFactory.getEC2Logger(BuiltinLoggers.SIMPLE_LOGGER);
		else
			return LoggerFactory.getEC2Logger(BuiltinLoggers.SPLUNK_LOGGER);*/
	}

	@Override
	public void run() {		
		
		String[] users = {"Mark H.", "Dave W.", "William B.", "Michael E.", "Santosh K."};
		String[] transaction_ids = {"TID_ABC", "TID_123", "TID_XYZ."};
		String[] bank_name = {"BONY", "CHASE"};
		
		String[] messages = {"Processing event", 
						     "computing lots of calculations", 
						     "Warning: Something happened",
						     "Initializing",
						     "Shutting Down",
						     "Waiting for dependency to complete",
						     "Debug message",
						     "printing value of some really important object",
						     "really important log event",
						     "Something that was not very important just happened",
						     "Critical Error",
						     "Boring event just happened"};
						     
		
		Random r1, r2, r3, r4;
					
		r1 = new Random();
		r2 = new Random();
		r3 = new Random();
		r4 = new Random();
		
		
		String publisherName = "Publisher-" + String.format("%05d", threadNumber);
		
		ThreadContext.put("publisherName", publisherName);
		ThreadContext.put("userName", users[r1.nextInt(users.length)]);
		ThreadContext.put("transactionID", transaction_ids[r2.nextInt(transaction_ids.length)]);
		ThreadContext.put("bankName", bank_name[r3.nextInt(bank_name.length)]);
		
		
		try {
			logger = getRandomLogger();			
		} catch (LogException e1) {
			
			return;
		}
				
		
		
		for (int i=0; i<numMessages; i++)
		{	        
			String message =  publisherName + " -- " + messages[r4.nextInt(messages.length)] +  " - Event: " + i;
			logger.info(message);
		}	
		
		
	}

}
