/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2019 Open Identity Platform Community.
 */


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openidentityplatform.commons.cassandra.EmbeddedServer;


public class EmbeddedServerTest {

	@BeforeClass
	public static void init() {
		System.setProperty("datastax-java-driver.basic.request.timeout", "10 seconds"); //slow ddl
		System.setProperty("datastax-java-driver.advanced.auth-provider.class","PlainTextAuthProvider");
		System.setProperty("datastax-java-driver.advanced.auth-provider.username","cassandra");
		System.setProperty("datastax-java-driver.advanced.auth-provider.password","cassandra");
	}
	
	@AfterClass
	public static void destory() {
	}
	
	@Test
	public void start_test() {
		
		try (EmbeddedServer cassandra=new EmbeddedServer()){
			cassandra.run();
		}
	}
}
