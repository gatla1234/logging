package com.dtcc.ecd.awslogutils.cloudwatch;

import com.dtcc.ecd.awslogutils.exception.LogException;

public class Validator {
	
	public static void validateSYSID(String SYSID)
			throws LogException
	{
		if (SYSID == null || SYSID.equals(""))
			throw new LogException("SYSID is null or empty");
		
		if (SYSID.length() != 3)
			throw new LogException("SYSID is loo long. Expecting 3 character string");
		
		if (!SYSID.matches("[a-zA-Z0-9]+"))
			throw new LogException("SYSID failed validation. Expecting 'a-zA-Z0-9'");
	}
	
	// Validates a string that is part of either a CloudWatch group or stream name
	public static void validateCloudWatchComponentString(String componentName)
			throws LogException
	{
		if (componentName == null || componentName.equals(""))
			throw new LogException("ApplicationName is null or empty");
		
		if (componentName.length() > 100)
			throw new LogException("ApplicationName is loo long, it exceeds 100 characters");
		
		if (!componentName.matches("[a-zA-Z0-9_-]+"))
			throw new LogException("ApplicationName failed validation. 'a-zA-Z0-9_-'");
	}
}
