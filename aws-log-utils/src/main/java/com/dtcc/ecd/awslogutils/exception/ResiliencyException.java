package com.dtcc.ecd.awslogutils.exception;

public class ResiliencyException extends Exception {
	
	private static final long serialVersionUID = -2789816196122204785L;

	public ResiliencyException()
	{
		super();
	}
	
	public ResiliencyException(String msg)
	{
		super(msg);
	}
	
	public ResiliencyException(Throwable t)
	{
		super(t);
	}

}
