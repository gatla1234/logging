package com.dtcc.ecd.awslogutils.log4j_plugins;

import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.*;

import com.dtcc.ecd.awslogutils.cloudwatch.LogConstants;


/*
 * 
 * Alternative ThreadContext converter. This differs from the default one in the following two ways"
 * 1) It will skip all  attributes added by the custom cloudwatch appender
 * 		i.e. the ones that begin with __LOG4J_THREADCONTEXT
 * 2) it will remove the curly brackets around attributes
 * 
 */
@Plugin(name="ThreadContextConverter", category = "Converter")
@ConverterKeys({"XX"})
public class ThreadContextConverter extends LogEventPatternConverter{
    protected ThreadContextConverter(String name, String style) {
        super(name, style);
    }

    public static ThreadContextConverter newInstance(String[] options) {
        return new ThreadContextConverter("XX","XX");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String formatterThreadContext = "";
        
        Map<String, String> tcMap = ThreadContext.getImmutableContext();
        String value = "";
        
        for (String key : tcMap.keySet())
        {
        	if (!key.startsWith(LogConstants.LOG4J_THREADCONTEXT_PREFIX))
        	{
        		value = tcMap.get(key).trim();
        		if (value.contains(" "))
        		{
        			//remove any existing single quotes first
        			value = value.replace("'", " ");
        			value = "'" + value + "'";
        		}
        					        		
        		formatterThreadContext += key + "=" + value + " ";
        	}
        }
        
        toAppendTo.append(formatterThreadContext.trim());
    }
}

