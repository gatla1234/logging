package com.dtcc.ecd.awslogutils.exception;

public class LogException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4819644985033253098L;

	public LogException()
	{
		super();
	}
	
	public LogException(String msg)
	{
		super(msg);
	}
	
	public LogException(Throwable t)
	{
		super(t);
	}
}
