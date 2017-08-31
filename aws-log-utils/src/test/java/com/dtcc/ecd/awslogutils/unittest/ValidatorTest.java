package com.dtcc.ecd.awslogutils.unittest;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.dtcc.ecd.awslogutils.cloudwatch.Validator;
import com.dtcc.ecd.awslogutils.exception.LogException;

public class ValidatorTest  {
	
	@Before
	public void setupTestCases()
		throws Exception
	{
		        
	}
	
	
	@Test
	public void test_validate_good_SYSIDs()
	{		
		try
		{
			Validator.validateSYSID("ABC");
			Validator.validateSYSID("xyz");
			Validator.validateSYSID("A1B");
			Validator.validateSYSID("1B1");
			Validator.validateSYSID("123");			
		}
		catch(LogException le)
		{
			fail(le.getMessage());
		}
	}
	
	@Test
	public void test_validate_bad_SYSIDs()
	{	
		
		try{
			Validator.validateSYSID(null);
			fail("Allowed to pass null SYSID");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("");
			fail("Allowed to pass empty SYSID");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("   ");
			fail("Allowed to pass empty SYSID");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("Ab ");
			fail("Allowed to pass too short SYSID");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("AB");
			fail("Allowed to pass too short SYSID");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("A-B");
			fail("Allowed to pass 'A-B'");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("---");
			fail("Allowed to pass '---'");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("!@#$");
			fail("Allowed to pass '!@#$'");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("ABCD2");
			fail("Allowed to pass 'ABCD2'");
		}
		catch (LogException le){}
		
		try{
			Validator.validateSYSID("ABCDE");
			fail("Allowed to pass 'ABCDE'");
		}
		catch (LogException le){}
	}
	
	
	@Test
	public void test_validate_good_CWComponentName()
	{		
		try{
			Validator.validateCloudWatchComponentString("Application");
			Validator.validateCloudWatchComponentString("Application1");
			Validator.validateCloudWatchComponentString("Application-1");
			Validator.validateCloudWatchComponentString("99_Application");
			Validator.validateCloudWatchComponentString("Appl9Ca-T10n0-33940d");			
		}
		catch(LogException le){
			fail(le.getMessage());
		}
	}
	
	@Test
	public void test_validate_bad_CWComponentName()
	{	
		
		try{
			Validator.validateCloudWatchComponentString(null);
			fail("Allowed Null Application name");
		}
		catch (LogException le){}
		
		try{
			Validator.validateCloudWatchComponentString("");
			fail("Allowed empty Application name");
		}
		catch (LogException le){}
		
		try{
			Validator.validateCloudWatchComponentString("  ");
			fail("Allowed white space in Application name");
		}
		catch (LogException le){}
		
		try{
			Validator.validateCloudWatchComponentString("Application1  ");
			fail("Allowed white space in Application name");
		}
		catch (LogException le){}
		
		try{
			Validator.validateCloudWatchComponentString("Application&^%%11");
			fail("Allowed illegal character space in Application name");
		}
		catch (LogException le){}
	}
	
}
