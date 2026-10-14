/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.cm.junit;

import static org.assertj.core.api.Assertions.fail;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectAwaitCalm;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.await.AwaitCalm;
import org.osgi.test.common.await.AwaitCalmTimeoutException;

/**
 * The helpers these tests used from DefaultTestBundleControl and
 * OSGiTestCase, unchanged: registered and fetched services are remembered by
 * their service object, so they can be released by it.
 */
public abstract class CMTestSupport {
	@InjectBundleContext
	protected BundleContext								context;

	private final Map<Object,ServiceRegistration< ? >>	serviceRegistry	= new HashMap<>();
	private final Map<Object,ServiceReference< ? >>		fetchedServices	= new HashMap<>();
	private volatile String								webServer;

	public String getWebServer() {
		String w = webServer;
		if (w == null) {
			URL base = context.getBundle().getEntry("/");
			webServer = w = (base == null) ? "/" : base.toExternalForm();
		}
		return w;
	}

	public long getLongProperty(String key, long defaultValue) {
		String propValue = AccessController.doPrivileged(
				(PrivilegedAction<String>) () -> context.getProperty(key));
		if (propValue == null) {
			return defaultValue;
		}
		try {
			return Long.parseLong(propValue);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	static final long	SETTLE_QUIET_MILLIS	= 300;

	@InjectAwaitCalm
	protected AwaitCalm	awaitCalm;

	/**
	 * Waits for the framework to settle instead of sleeping a fixed time:
	 * returns once no bundle or service event has arrived for
	 * SETTLE_QUIET_MILLIS, and at the latest after maxMillis, as the former
	 * sleep did.
	 */
	public void settle(long maxMillis) throws InterruptedException {
		Duration quiet = Duration.ofMillis(Math.min(SETTLE_QUIET_MILLIS, Math.max(1, maxMillis / 2)));
		try {
			awaitCalm.waitForQuiet(quiet, Duration.ofMillis(maxMillis));
		} catch (AwaitCalmTimeoutException e) {
			// still busy after maxMillis: go on, as the fixed sleep did
		}
	}

	public void registerService(String clazz, Object service,
			Dictionary<String, ? > properties) throws Exception {
		ServiceRegistration< ? > sr = context.registerService(clazz, service,
				properties);
		synchronized (serviceRegistry) {
			serviceRegistry.put(service, sr);
		}
	}

	public void unregisterService(Object service) {
		ServiceRegistration< ? > sr;
		synchronized (serviceRegistry) {
			sr = serviceRegistry.remove(service);
		}
		if (sr == null) {
			fail("trying to unregister a service which is not currently registered");
		}
		sr.unregister();
	}

	public void unregisterAllServices() {
		ServiceRegistration< ? >[] srs;
		synchronized (serviceRegistry) {
			srs = serviceRegistry.values()
					.toArray(new ServiceRegistration[serviceRegistry.size()]);
			serviceRegistry.clear();
		}
		for (int l = srs.length, i = 0; i < l; i++) {
			srs[i].unregister();
		}
	}

	public <S> S getService(Class<S> clazz) {
		try {
			return getService(clazz, null);
		} catch (InvalidSyntaxException e) {
			/* null can not give an exception of this type */
		}
		return null;
	}

	public <S> S getService(Class<S> clazz, String filter)
			throws InvalidSyntaxException {
		@SuppressWarnings("unchecked")
		ServiceReference<S>[] refs = (ServiceReference<S>[]) context
				.getServiceReferences(clazz.getName(), filter);
		if (refs == null) {
			fail("Can't get service reference for " + clazz.getName() + filter);
		}
		ServiceReference<S> chosenRef = refs[0];
		/* highest ranking, then lowest service id: the ServiceReference order */
		for (int i = 1; i < refs.length; i++) {
			if (refs[i].compareTo(chosenRef) > 0) {
				chosenRef = refs[i];
			}
		}
		S service = context.getService(chosenRef);
		if (service == null) {
			fail("Can't get service for " + clazz.getName() + filter);
		}
		synchronized (fetchedServices) {
			fetchedServices.put(service, chosenRef);
		}
		return service;
	}

	public void ungetService(Object service) {
		ServiceReference< ? > ref;
		synchronized (fetchedServices) {
			ref = fetchedServices.remove(service);
		}
		if (ref == null) {
			fail("trying to unget a service which is not currently bound");
		}
		context.ungetService(ref);
	}

	public static void failException(String message,
			Class< ? > expectedExceptionClass) {
		fail(message + " expected:[" + expectedExceptionClass.getName()
				+ "] and got nothing");
	}

	public static void assertException(String message,
			Class< ? extends Throwable> want, Throwable got) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		if (!want.isInstance(got)) {
			fail(formatted + "expected:[" + want.getName() + "] but was:["
					+ got.getClass().getName() + "]", got);
		}
	}

	/**
	 * Asserts that two Dictionaries of properties are equal; array values
	 * are compared with Arrays.equals, lists and dictionaries element-wise.
	 */
	public static void assertEqualProperties(String message,
			Dictionary< ? , ? > expected, Dictionary< ? , ? > actual) {
		if (expected == actual) {
			return;
		}
		if ((expected == null) || (actual == null)
				|| !dictionaryEquals(expected, actual)) {
			fail((message == null ? "" : message + " ") + "expected:<"
					+ expected + "> but was:<" + actual + ">");
		}
	}

	private static boolean objectEquals(Object expected, Object actual) {
		if (expected == actual) {
			return true;
		}
		if ((expected == null) || (actual == null)) {
			return false;
		}
		if (expected.equals(actual)) {
			return true;
		}
		if (expected instanceof List && actual instanceof List) {
			List< ? > e = (List< ? >) expected;
			List< ? > a = (List< ? >) actual;
			if (e.size() != a.size())
				return false;
			for (int i = 0; i < e.size(); i++) {
				if (!objectEquals(e.get(i), a.get(i)))
					return false;
			}
			return true;
		}
		if (expected instanceof Dictionary && actual instanceof Dictionary) {
			return dictionaryEquals((Dictionary< ? , ? >) expected,
					(Dictionary< ? , ? >) actual);
		}
		Class< ? > clazz = expected.getClass();
		if (clazz.isArray() && actual.getClass().equals(clazz)) {
			if (clazz.getComponentType().isPrimitive()) {
				/* Arrays.equals of the primitive type */
				return Objects.deepEquals(expected, actual);
			}
			return Arrays.equals((Object[]) expected, (Object[]) actual);
		}
		return false;
	}

	private static boolean dictionaryEquals(Dictionary< ? , ? > expected,
			Dictionary< ? , ? > actual) {
		if (expected.size() != actual.size())
			return false;
		for (Enumeration< ? > e = expected.keys(); e.hasMoreElements();) {
			Object key = e.nextElement();
			if (!objectEquals(expected.get(key), actual.get(key)))
				return false;
		}
		return true;
	}
}
