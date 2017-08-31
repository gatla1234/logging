package com.dtcc.ecd.awslogutils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.dtcc.ecd.awslogutils.cloudwatch.CloudWatchConnector;
import com.dtcc.ecd.awslogutils.exception.LogException;
import com.dtcc.ecd.awslogutils.exception.ResiliencyException;

public class CloudWatchClientUtils {
	
	public static CloudWatchConnector getDefaultEC2Connector()
			throws LogException, ResiliencyException {
		
			return new DefaultConnector(
					InstanceProfileCredentialsProvider.getInstance(), null);
		}
	
	
	public static CloudWatchConnector getDefaultEC2Connector(ClientConfiguration clientConfig)
			throws LogException, ResiliencyException {
			
			if (clientConfig == null)
				throw new LogException("clientConfig must be supplied");

			return new DefaultConnector(
					InstanceProfileCredentialsProvider.getInstance(),
					clientConfig);
		}

}
