/*******************************************************************************
 * Copyright (c) 2014 Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andy Clement - initial API and implementation
 *******************************************************************************/
package org.aspectj.systemtest.ajc184;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsAspectJ184 {

	public static Test suite() {
		TestSuite suite = new TestSuite("AspectJ 1.8.4 tests");
		// $JUnit-BEGIN$
		suite.addTest(Ajc184Tests.suite());
		// $JUnit-END$
		return suite;
	}
}
