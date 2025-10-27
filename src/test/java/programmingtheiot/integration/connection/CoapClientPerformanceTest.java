/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.connection;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.connection.CoapClientConnector;

/**
 * This test case class contains performance benchmarking tests for
 * CoapClientConnector using CON and NON message types.
 * Tests measure the time to POST 10,000 messages at each type.
 * 
 * NOTE: The CoAP server must be running before executing these tests.
 */
public class CoapClientPerformanceTest
{
	// static
	
	public static final int DEFAULT_TIMEOUT = 5;
	
	private static final Logger _Logger =
		Logger.getLogger(CoapClientPerformanceTest.class.getName());
	
	public static final int MAX_TEST_RUNS = 10000;
	
	// member var's
	
	private CoapClientConnector coapClient = null;
	
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		this.coapClient = new CoapClientConnector();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}
	
	// test methods
	
	/**
	 * Test POST performance with CON (Confirmable) messages.
	 */
	@Test
	public void testPostRequestCon()
	{
		System.out.println("Testing POST - CON");
		
		execTestPost(MAX_TEST_RUNS, true);
	}
	
	/**
	 * Test POST performance with NON (Non-Confirmable) messages.
	 */
	@Test
	public void testPostRequestNon()
	{
		System.out.println("Testing POST - NON");
		
		execTestPost(MAX_TEST_RUNS, false);
	}
	
	// private methods
	
	/**
	 * Execute POST performance test.
	 * 
	 * @param maxTestRuns Number of messages to send
	 * @param enableCON True for CON (confirmable), False for NON (non-confirmable)
	 */
	private void execTestPost(int maxTestRuns, boolean enableCON)
	{
		SensorData sd = new SensorData();
		String payload = DataUtil.getInstance().sensorDataToJson(sd);
				
		long startMillis = System.currentTimeMillis();
		
		for (int seqNo = 0; seqNo < maxTestRuns; seqNo++) {
			this.coapClient.sendPostRequest(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, ConfigConst.TEMP_SENSOR_NAME, enableCON, payload, DEFAULT_TIMEOUT);
		}
		
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - startMillis;
				
		_Logger.info("POST message - useCON = " + enableCON + " [" + maxTestRuns + "]: " + elapsedMillis + " ms");
	}
	
}