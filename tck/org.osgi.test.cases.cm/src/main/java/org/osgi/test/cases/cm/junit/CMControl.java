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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;
import static org.osgi.test.cases.cm.shared.Util.singletonDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.cm.ReadOnlyConfigurationException;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.osgi.test.cases.cm.common.ConfigurationListenerImpl;
import org.osgi.test.cases.cm.common.SynchronizerImpl;
import org.osgi.test.cases.cm.shared.ModifyPid;
import org.osgi.test.cases.cm.shared.Synchronizer;
import org.osgi.test.cases.cm.shared.Util;


/**
 * @author Ikuo YAMASAKI, NTT Corporation, added many tests.
 * @author Carsten Ziegeler, Adobe, added ConfigAdmin 1.5/1.6 tests
 */
public class CMControl extends CMTestSupport {


	String				testMethodName;

	@BeforeEach
	void testMethodName(TestInfo testInfo) {
		testMethodName = testInfo.getTestMethod().get().getName();
	}
	private ConfigurationAdmin cm;
	private PermissionAdmin permAdmin;
	private long					SIGNAL_WAITING_TIME;
	private List<PermissionInfo>					list;
	private boolean permissionFlag;
	private Bundle setAllPermissionBundle;

	private String									thisLocation;
	private Bundle									thisBundle;

	private Set<String>								existingConfigs;

	private static final String SP = ServicePermission.class.getName();
	private static final String PP = PackagePermission.class.getName();
	private static final String AP = AdminPermission.class.getName();
	private static final String CP = ConfigurationPermission.class.getName();
	private static final Dictionary<String,Object>	propsForSync1;
	static {
		propsForSync1 = new Hashtable<String,Object>();
		propsForSync1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync1");
	}
	private static final Dictionary<String,Object> propsForSync2;
	static {
		propsForSync2 = new Hashtable<String,Object>();
		propsForSync2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync2");
	}

	private static final Dictionary<String,Object> propsForSync1_1;
	static {
		propsForSync1_1 = new Hashtable<String,Object>();
		propsForSync1_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync1-1");
	}

	private static final Dictionary<String,Object> propsForSync1_2;
	static {
		propsForSync1_2 = new Hashtable<String,Object>();
		propsForSync1_2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync1-2");
	}

	private static final Dictionary<String,Object> propsForSync2_1;
	static {
		propsForSync2_1 = new Hashtable<String,Object>();
		propsForSync2_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync2-1");
	}
	private static final Dictionary<String,Object> propsForSync3_1;
	static {
		propsForSync3_1 = new Hashtable<String,Object>();
		propsForSync3_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync3-1");
	}
	private static final Dictionary<String,Object> propsForSync3_2;
	static {
		propsForSync3_2 = new Hashtable<String,Object>();
		propsForSync3_2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync3-2");
	}
	private static final Dictionary<String,Object> propsForSync4_1;
	static {
		propsForSync4_1 = new Hashtable<String,Object>();
		propsForSync4_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync4-1");
	}
	private static final Dictionary<String,Object> propsForSync4_2;
	static {
		propsForSync4_2 = new Hashtable<String,Object>();
		propsForSync4_2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"sync4-2");
	}
	private static final Dictionary<String,Object> propsForSyncF1_1;
	static {
		propsForSyncF1_1 = new Hashtable<String,Object>();
		propsForSyncF1_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncF1-1");
	}

	private static final Dictionary<String,Object> propsForSyncF1_2;
	static {
		propsForSyncF1_2 = new Hashtable<String,Object>();
		propsForSyncF1_2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncF1-2");
	}
	private static final Dictionary<String,Object> propsForSyncF2_1;
	static {
		propsForSyncF2_1 = new Hashtable<String,Object>();
		propsForSyncF2_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncF2-1");
	}
	private static final Dictionary<String,Object> propsForSyncF3_1;
	static {
		propsForSyncF3_1 = new Hashtable<String,Object>();
		propsForSyncF3_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncF3-1");
	}
	private static final Dictionary<String,Object> propsForSyncF3_2;
	static {
		propsForSyncF3_2 = new Hashtable<String,Object>();
		propsForSyncF3_2.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncF3-2");
	}

	private static final Dictionary<String,Object> propsForSyncT5_1;
	static {
		propsForSyncT5_1 = new Hashtable<String,Object>();
		propsForSyncT5_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncT5-1");
	}

	private static final Dictionary<String,Object> propsForSyncT6_1;
	static {
		propsForSyncT6_1 = new Hashtable<String,Object>();
		propsForSyncT6_1.put(
				org.osgi.test.cases.cm.shared.Constants.SERVICEPROP_KEY_SYNCID,
				"syncT6-1");
	}

	private static final String neverlandLocation = "http://neverneverland/";

	List<String>				bLocs				= new ArrayList<>();
	@BeforeEach
	public void setUp() throws Exception {
		SIGNAL_WAITING_TIME = getLongProperty(
				"org.osgi.test.cases.cm.signal_waiting_time", 4000);
		// printoutBundleList();

		for (Bundle b : context.getBundles()) {
			if (b.getState() != Bundle.UNINSTALLED) {
			bLocs.add(b.getLocation());
			}
		}
	    assignCm();

		// populate the created configurations so that
		// listConfigurations can return these configurations
		list = new ArrayList<>(5);

		if (System.getSecurityManager() != null) {
			permAdmin = getService(PermissionAdmin.class);
			setAllPermissionBundle = context.installBundle(
					getWebServer() + "setallpermission.jar");
			thisBundle = context.getBundle();
			thisLocation = thisBundle.getLocation();
		} else {
			permissionFlag = true;
		}
		// existing configurations
        Configuration[] configs = cm.listConfigurations(null);
		existingConfigs = new HashSet<>();
        if (configs != null) {
            for (int i = 0; i < configs.length; i++) {
                Configuration config = configs[i];
                // System.out.println("setUp() -- Register pre-existing config " + config.getPid());
                existingConfigs.add(config.getPid());
            }
        }
	}

	@AfterEach
	public void tearDown() throws Exception {
		resetPermissions();
		cleanCM(existingConfigs);
		if (this.setAllPermissionBundle != null) {
			this.setAllPermissionBundle.uninstall();
			this.setAllPermissionBundle = null;
		}
		if (permAdmin != null)
			ungetService(permAdmin);
		list = null;

        unregisterAllServices();
		ungetService(cm);
		System.out.println("tearDown()");
		// this.printoutBundleList();
		for (Bundle b : context.getBundles()) {
			if (!bLocs.contains(b.getLocation())) {
				b.uninstall();
			}
		}
	}

	private void resetPermissions() throws BundleException {
		if (permAdmin == null)
			return;
		try {
			if (this.setAllPermissionBundle == null)
				this.setAllPermissionBundle = context.installBundle(
						getWebServer() + "setallpermission.jar");
			this.setAllPermissionBundle.start();
			this.setAllPermissionBundle.stop();
		} catch (BundleException e) {
			Exception ise = new IllegalStateException(
					"fail to install or start setallpermission bundle.");
			ise.initCause(e);
			throw e;
		}
		this.printoutPermissions();
	}

	private void printoutPermissions() {
		if (permAdmin == null)
			return;
		String[] locations = this.permAdmin.getLocations();
		if (locations != null)
			for (int i = 0; i < locations.length; i++) {
				System.out.println("locations[" + i + "]=" + locations[i]);
				PermissionInfo[] pInfos = this.permAdmin
						.getPermissions(locations[i]);
				for (int j = 0; j < pInfos.length; j++) {
					System.out.println("\t" + pInfos[j]);
				}
			}
		PermissionInfo[] pInfos = this.permAdmin.getDefaultPermissions();
		if (pInfos == null)
			System.out.println("default permission=null");
		else {
			System.out.println("default permission=");
			for (int j = 0; j < pInfos.length; j++) {
				System.out.println("\t" + pInfos[j]);
			}
		}

	}

	private void setBundlePermission(Bundle b, List<PermissionInfo> list) {
		if (permAdmin == null)
			return;
		PermissionInfo[] pis = new PermissionInfo[list.size()];
		pis = list.toArray(pis);
		permAdmin.setPermissions(b.getLocation(), pis);
		this.printoutPermissions();
	}

	private List<PermissionInfo> getBundlePermission(Bundle b) {
        if (permAdmin == null) return null;
        PermissionInfo[] pis = permAdmin.getPermissions(b.getLocation());
        return Arrays.asList(pis);
    }

	private void add(List<PermissionInfo> permissionsInfos, String clazz,
			String name,
			String actions) {
		permissionsInfos.add(new PermissionInfo(clazz, name, actions));
	}

	/** *** Test methods **** */

	/**
	 * Test that the methods throws IllegalStateException when operating on a
	 * deleted Configuration
	 *
	 * @spec Configuration.delete()
	 * @spec Configuration.getBundleLocation()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.setBundleLocation(String)
	 */
	@Test
	public void testDeletedConfiguration() throws Exception {
		String pid = Util.createPid();
		Configuration conf = null;
		/* Get a brand new configuration and delete it. */
		conf = cm.getConfiguration(pid);
		conf.delete();
		/*
		 * A list of all methodcalls that should be made to the deleted
		 * Configuration object
		 */
		MethodCall[] methods = {
				new MethodCall(Configuration.class, "delete"),
				new MethodCall(Configuration.class, "getBundleLocation"),
				new MethodCall(Configuration.class, "getFactoryPid"),
				new MethodCall(Configuration.class, "getPid"),
				new MethodCall(Configuration.class, "getProperties"),
				new MethodCall(Configuration.class, "getChangeCount"),
				new MethodCall(Configuration.class, "setBundleLocation",
						String.class, "somelocation"),
				new MethodCall(Configuration.class, "update"),
				new MethodCall(Configuration.class, "update", Dictionary.class,
						new Hashtable<>())
		};
		/* Make all the methodcalls in the list */
		for (int i = 0; i < methods.length; i++) {
			try {
				/* Call the method on the Configuration object */
				methods[i].invoke(conf);
				/*
				 * In this case it should always throw an IllegalStateException
				 * so if we end up here, somethings wrong
				 */
				failException(methods[i].getName(), IllegalStateException.class);
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				/* Check that we got the correct exception */
				assertException(methods[i].getName(),
						IllegalStateException.class, e);
			}
		}
	}

	/**
	 * TODO comments
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec ConfigurationAdmin.getConfiguration(String,String)
	 * @spec Configuration.getBundleLocation()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.setBundleLocation(String)
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetConfiguration() throws Exception {
		this.setInappropriatePermission();

		String pid = Util.createPid();
		String thisLocation = getLocation();
		Configuration conf = null;
		/* Get a brand new configuration */
		conf = cm.getConfiguration(pid);

		checkConfiguration(conf, "A new Configuration object", pid,
				thisLocation);

		/* Get location of the configuration */
		/* must fail because of inappropriate Permission. */
		String message = "try to get location without appropriate ConfigurationPermission.";
		try {
            conf.getBundleLocation();
            /*
             * A SecurityException should have been thrown if security is
             * enabled
             */
            if (System.getSecurityManager() != null) failException(message, SecurityException.class);
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            /* Check that we got the correct exception */
            assertException(message, SecurityException.class, e);
            /*
             * A SecurityException should not have been thrown if security is
             * not enabled
             */
            if (System.getSecurityManager() == null) fail("Security is not enabled", e);
		}

		/* Get the configuration again (should be exactly the same) */
		conf = cm.getConfiguration(pid);
		checkConfiguration(conf, "The same Configuration object", pid,
				thisLocation);

		/*
		 * Change the location of the bundle and then get the Configuration
		 * again. The location should not have been touched.
		 */
		/* must fail because of inappropriate Permission. */
		message = "try to set location without appropriate ConfigurationPermission.";
		try {
			conf.setBundleLocation(neverlandLocation);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		this.setAppropriatePermission();
		conf.setBundleLocation(neverlandLocation);

		conf = cm.getConfiguration(pid);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location Neverland").isEqualTo(neverlandLocation);

		this.setInappropriatePermission();
		/* must fail because of inappropriate Permission. */
		message = "try to get configuration whose location is different from the caller bundle without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(pid);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		/* Clean up */
		conf.delete();
	}

	/**
	 * TODO comments
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String,String)
	 * @spec Configuration.getBundleLocation()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.getProperties()
	 *
	 * @throws Exception
	 */
	@Test
	public void testGetConfigurationWithLocation() throws Exception {
		final String pid1 = Util.createPid("1");
		final String pid2 = Util.createPid("2");
		final String pid3 = Util.createPid("3");
		// final String thisLocation = getLocation();
		Configuration conf = null;

		this.printoutPermissions();
		this.setInappropriatePermission();

		/*
		 * Without appropriate ConfigurationPermission, Get a brand new
		 * configuration.
		 */
		String message = "try to get configuration without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(pid1, thisLocation);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			fail("Throwable must not be thrown for Configuration#getBundleLocation()", e);
		}

		/*
		 * Without appropriate ConfigurationPermission, Get an existing
		 * configuration with thisLocation, but specify the location (which
		 * should then be ignored).
		 */
		// TODO Change the explanation
		message = "try to get configuration without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(pid1, neverlandLocation);
			// checkConfiguration(conf, "The same Configuration object",
			// pid1,thisLocation);
			fail("SecurityException must be thrown because cm must check the set location.");
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// fail("Throwable must not be thrown because the configuring bundle has implicit CP for thisLocation", e);
		}

		conf.delete();

		this.setInappropriatePermission();
		/*
		 * Without appropriate ConfigurationPermission, Get a brand new
		 * configuration with a specified location
		 */
		message = "try to get configuration without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(pid2, neverlandLocation);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		this.setAppropriatePermission();

		/* Get a brand new configuration with a specified location */
		conf = cm.getConfiguration(pid2, neverlandLocation);
		checkConfiguration(conf, "A new Configuration object", pid2,
				neverlandLocation);
		conf.delete();

		this.setInappropriatePermission();
		/*
		 * Without appropriate ConfigurationPermission, Get a brand new
		 * configuration with no location
		 */
		message = "try to get configuration without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(pid3, null);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		this.setAppropriatePermission();
		/* Get a brand new configuration with no location */
		conf = cm.getConfiguration(pid3, null);
		checkConfiguration(conf, "A new Configuration object", pid3, null);
		conf.delete();

	}

	/**
	 * TODO comments
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String,String)
	 * @spec Configuration.getBundleLocation()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.getProperties()
	 *
	 * @throws Exception
	 */
	@Test
	public void testConfigurationWithNullLocation() throws Exception {
		final String bundlePid = Util.createPid("bundle1Pid");
		final String thisLocation = getLocation();
		Configuration conf = null;

		this.setInappropriatePermission();
		/*
		 * Without appropriate ConfigurationPermission, Get a brand new
		 * configuration with no location
		 */
		String message = "try to get configuration without appropriate ConfigurationPermission.";
		try {
			conf = cm.getConfiguration(bundlePid, null);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		this.setAppropriatePermission();
		/* Get a brand new configuration with no location */
		conf = cm.getConfiguration(bundlePid, null);
		checkConfiguration(conf, "A new Configuration object", bundlePid, null);

		Dictionary<String,Object> props = new Hashtable<>();
		props.put("StringKey", testMethodName);
		conf.update(props);

		/*
		 * Get existing Configuration with neverland location, which should be
		 * ignored
		 */
		Configuration conf3 = cm.getConfiguration(bundlePid, neverlandLocation);
		assertThat(conf3.getPid()).as("Pid").isEqualTo(bundlePid);
		assertThat(conf3.getFactoryPid()).as("FactoryPid").isNull();
		assertThat(this.getBundleLocationForCompare(conf3)).as("Location").isNull();
		assertThat(conf3
				.getProperties().get("StringKey")).as("The same Confiuration props").isEqualTo(testMethodName);

		System.out.println("###########################"
				+ conf3.getBundleLocation());
		// System.out.println("###########################"+cm.getConfiguration(bundlePid).getBundleLocation());

		this.setInappropriatePermission();

		/* Get location of the configuration with null location */
		/* must fail because of inappropriate Permission. */
		message = "try to get location without appropriate ConfigurationPermission.";
		try {
			conf.getBundleLocation();
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}

		message = "try to get configuration with null location without appropriate ConfigurationPermission.";
		/*
		 * try { Configuration conf4 = cm.getConfiguration(bundlePid);
		 *
		 * A SecurityException should have been thrown if security is enabled
		 *
		 * if (System.getSecurityManager() != null) failException(message,
		 * SecurityException.class); } catch (AssertionError e) { throw e;
		 * } catch (Throwable e) { Check that we got the correct exception
		 * assertException(message, SecurityException.class, e);
		 *
		 * A SecurityException should not have been thrown if security is not
		 * enabled
		 *
		 * if (System.getSecurityManager() == null)
		 * fail("Security is not enabled", e); }
		 */
		Configuration conf4 = cm.getConfiguration(bundlePid);
		/* location MUST be changed to the callers bundle's location. */
		assertThat(this.getBundleLocationForCompare(conf4)).as("Location").isEqualTo(thisLocation);

		/* In order to get Location, appropriate permission is required. */
		this.setAppropriatePermission();
		cm.getConfiguration(bundlePid).setBundleLocation(null);
		Configuration conf5 = cm.getConfiguration(bundlePid);

		/* location MUST be changed to the callers bundle's location. */
		assertThat(this.getBundleLocationForCompare(conf5)).as("Location").isEqualTo(thisLocation);

		// this.setAppropriatePermission();
		// conf3 = cm.getConfiguration(pid3);
		assertThat(this.getBundleLocationForCompare(conf3)).as("Location").isEqualTo(thisLocation);

		this.setInappropriatePermission();
		/* Set location of the configuration to null location */
		/* must fail because of inappropriate Permission. */
		message = "try to set location to null without appropriate ConfigurationPermission.";
		try {
			conf.setBundleLocation(null);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}
		this.setAppropriatePermission();
		conf.setBundleLocation(null);

		this.setInappropriatePermission();
		/* Set location of the configuration with null location to others */
		/* must fail because of inappropriate Permission. */
		message = "try to set location to null without appropriate ConfigurationPermission.";
		try {
			conf.setBundleLocation(thisLocation);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}
		try {
			conf.setBundleLocation(neverlandLocation);
			/*
			 * A SecurityException should have been thrown if security is
			 * enabled
			 */
			if (System.getSecurityManager() != null)
				failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, SecurityException.class, e);
			/*
			 * A SecurityException should not have been thrown if security is
			 * not enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		}
		conf.delete();
	}

	/**
	 * Test the change counter.
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
	public void testChangeCount() throws Exception {
		System.out.println("Testing change count...");
        // create config with pid
		System.out.println("Create test configuration");
        final String pid = "test_ca_counter_" + ConfigurationListenerImpl.LISTENER_PID_SUFFIX;
        final Configuration config = this.cm.getConfiguration(pid);
        final long startCount = config.getChangeCount();

        // register sync and async listener for change updates
		System.out.println("Create and register configuration listeners");
		// list to check whether the sync listener is called
		final List<ConfigurationEvent> events = new ArrayList<>();
		final SynchronousConfigurationListener scl = new SynchronousConfigurationListener() {

			// the sync listener is called during the update method and the
			// change count should of course already be updated.
			public void configurationEvent(final ConfigurationEvent event) {
		        if (event.getPid() != null
				    && event.getPid().endsWith(ConfigurationListenerImpl.LISTENER_PID_SUFFIX)
				    && event.getFactoryPid() == null) {

					assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
					assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);
			        assertThat(config.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + config.getChangeCount()).isTrue();
			        events.add(event);
				}
			}
		};
		ConfigurationListenerImpl cl = null;
		final SynchronizerImpl synchronizer = new SynchronizerImpl();
		this.registerService(SynchronousConfigurationListener.class.getName(), scl, null);
		try {
			cl = createConfigurationListener(synchronizer);
	        // update config with properties
			config.update(singletonDictionary("x", "x"));
	        assertThat(events.size() == 1).as("Sync listener not called.").isTrue();

	        assertThat(config.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + config.getChangeCount()).isTrue();

			System.out.println("Wait until the ConfigurationListener has gotten the update");
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			System.out.println("Checking configuration event");
			assertThat(cl.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// reget configuration and check count
			System.out.println("Checking regetting configuration");
			final Configuration configNew = this.cm.getConfiguration(pid);
			assertThat(configNew.getChangeCount()).as("Configuration change count shouldn't have changed").isEqualTo(config.getChangeCount());
	        assertThat(configNew.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + configNew.getChangeCount()).isTrue();

			System.out.println("Testing change count...finished");
		} finally {
	        // clean up
			if ( cl != null ) {
				removeConfigurationListener(cl);
			}
			this.unregisterService(scl);
	        config.delete();
		}
    }

	/**
	 * Test the change counter for factory configuration
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
	public void testChangeCountFactory() throws Exception {
		System.out.println("Testing change count factory...");
        // create config with pid
		System.out.println("Create test configuration");
        final String factoryPid = "test_ca_counter_factory_" + ConfigurationListenerImpl.LISTENER_PID_SUFFIX;
        final Configuration config = this.cm.createFactoryConfiguration(factoryPid);
        final String pid = config.getPid();
        final long startCount = config.getChangeCount();

        // register sync and async listener for change updates
		System.out.println("Create and register configuration listeners");
		// list to check whether the sync listener is called
		final List<ConfigurationEvent> events = new ArrayList<>();
		final SynchronousConfigurationListener scl = new SynchronousConfigurationListener() {

			// the sync listener is called during the update method and the
			// change count should of course already be updated.
			public void configurationEvent(final ConfigurationEvent event) {
		        if (event.getPid() != null
				    && event.getFactoryPid().endsWith(ConfigurationListenerImpl.LISTENER_PID_SUFFIX)) {

					assertThat(event.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
					assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
					assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);
			        assertThat(config.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + config.getChangeCount()).isTrue();
			        events.add(event);
				}
			}
		};
		ConfigurationListenerImpl cl = null;
		final SynchronizerImpl synchronizer = new SynchronizerImpl();
		this.registerService(SynchronousConfigurationListener.class.getName(), scl, null);
		try {
			cl = createConfigurationListener(synchronizer);
	        // update config with properties
			config.update(singletonDictionary("x", "x"));
	        assertThat(events.size() == 1).as("Sync listener not called.").isTrue();

	        assertThat(config.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + config.getChangeCount()).isTrue();

			System.out.println("Wait until the ConfigurationListener has gotten the update");
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			System.out.println("Checking configuration event");
			assertThat(cl.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
			assertThat(cl.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// reget configuration and check count
			System.out.println("Checking regetting configuration");
            final Configuration[] cfs = cm.listConfigurations( "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                    + factoryPid + ")" );
			final Configuration configNew = cfs[0];
			assertThat(configNew.getChangeCount()).as("Configuration change count shouldn't have changed").isEqualTo(config.getChangeCount());
	        assertThat(configNew.getChangeCount() > startCount).as("Expect second change count to be higher than " + startCount + " : " + configNew.getChangeCount()).isTrue();

			System.out.println("Testing change count...finished");
		} finally {
	        // clean up
			if ( cl != null ) {
				removeConfigurationListener(cl);
			}
			this.unregisterService(scl);
	        config.delete();
		}
    }

    /**
	 * Test sync listener.
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
    public void testSyncListener() throws Exception {
		System.out.println("Testing sync listener...");

		System.out.println("Create and register sync configuration listeners");
		// List of events
		final List<ConfigurationEvent> events = new ArrayList<>();
		// Thread check
		final Thread callerThread = Thread.currentThread();

		final SynchronousConfigurationListener scl = new SynchronousConfigurationListener() {

			// the sync listener is called during the update method and the
			// change count should of course already be updated.
			public void configurationEvent(final ConfigurationEvent event) {
		        if (event.getPid() != null
				    && event.getPid().endsWith(ConfigurationListenerImpl.LISTENER_PID_SUFFIX)
				    && event.getFactoryPid() == null) {
		        	// check thread
		        	if ( Thread.currentThread() != callerThread ) {
		        		fail("Method is not called in sync.");
		        	}
		        	events.add(event);

				}
			}
		};
		this.registerService(SynchronousConfigurationListener.class.getName(), scl, null);
		Configuration config = null;
		try {
	        // create config with pid
			System.out.println("Create test configuration");
	        final String pid = "test_sync_config_" + ConfigurationListenerImpl.LISTENER_PID_SUFFIX;
	        config = this.cm.getConfiguration(pid);
			config.update(singletonDictionary("y", "y"));
			assertThat(events).as("No event received: " + events).hasSize(1);

	        ConfigurationEvent event = events.get(0);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// update config
			config.update(singletonDictionary("x", "x"));
			assertThat(events).as("No event received").hasSize(2);
	        event = events.get(1);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// update location
			config.setBundleLocation("location");
			assertThat(events).as("No event received").hasSize(3);
	        event = events.get(2);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_LOCATION_CHANGED);

	        // delete config
	        config.delete();
	        config = null;
			assertThat(events).as("No event received").hasSize(4);
	        event = events.get(3);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);

		} finally {
			this.unregisterService(scl);
			if ( config != null ) {
				config.delete();
			}
		}

		System.out.println("Testing sync listener...finished");
    }

    /**
	 * Test sync listener for factory config.
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
    public void testSyncListenerFactory() throws Exception {
		System.out.println("Testing sync listener...");

		System.out.println("Create and register sync configuration listeners");
		// List of events
		final List<ConfigurationEvent> events = new ArrayList<>();
		// Thread check
		final Thread callerThread = Thread.currentThread();

		final SynchronousConfigurationListener scl = new SynchronousConfigurationListener() {

			// the sync listener is called during the update method and the
			// change count should of course already be updated.
			public void configurationEvent(final ConfigurationEvent event) {
		        if (event.getPid() != null
				    && event.getFactoryPid().endsWith(ConfigurationListenerImpl.LISTENER_PID_SUFFIX)) {
		        	// check thread
		        	if ( Thread.currentThread() != callerThread ) {
		        		fail("Method is not called in sync.");
		        	}
		        	events.add(event);

				}
			}
		};
		this.registerService(SynchronousConfigurationListener.class.getName(), scl, null);
		Configuration config = null;
		try {
	        // create config with pid
			System.out.println("Create test configuration");
	        final String factoryPid = "test_sync_config_factory_" + ConfigurationListenerImpl.LISTENER_PID_SUFFIX;
	        config = this.cm.createFactoryConfiguration(factoryPid);
	        final String pid = config.getPid();
			config.update(singletonDictionary("y", "y"));
			assertThat(events).as("No event received: " + events).hasSize(1);

	        ConfigurationEvent event = events.get(0);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// update config
			config.update(singletonDictionary("x", "x"));
			assertThat(events).as("No event received").hasSize(2);
	        event = events.get(1);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			// update location
			config.setBundleLocation("location");
			assertThat(events).as("No event received").hasSize(3);
	        event = events.get(2);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_LOCATION_CHANGED);

	        // delete config
	        config.delete();
	        config = null;
			assertThat(events).as("No event received").hasSize(4);
	        event = events.get(3);
			assertThat(event.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(event.getFactoryPid()).as("Config event factory pid match").isEqualTo(factoryPid);
			assertThat(event.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);

		} finally {
			this.unregisterService(scl);
			if ( config != null ) {
				config.delete();
			}
		}

		System.out.println("Testing sync listener...finished");
    }

    /**
	 * Test targeted pids
	 * Create configs for the same pid, each new config is either more "specific" than
	 * the previous one or "invalid"
	 * Check if the new config is either bound or ignored
	 * Then delete configs in reverse order and check if either nothing is happening
	 * ("invalid" configs) or rebound to a previous config happens.
	 *
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
    public void testTargetedPid() throws Exception {
    	System.out.println("Testing targeted pids...");

		final Bundle bundleT5 = context.installBundle(
				getWebServer() + "bundleT5.jar");
		final Bundle bundleT6 = context.installBundle(
				getWebServer() + "bundleT6.jar");
		final String pidBase = Util.createPid("pid_targeted1");
		// create a list of configurations
		// the first char is a "bitset":
		// if bit 1 is set, T5 should get the config
		// if bit 2 is set, T6 should get the config
		// so: 0 : no delivery, 1 : T5, 2: T6, 3: T5 + T6
		final String[] pids = new String[] {
				"3" + pidBase,
				"1" + pidBase + "|" + bundleT5.getSymbolicName(),
				"2" + pidBase + "|" + bundleT6.getSymbolicName(),
				"0" + pidBase + "|Not" + bundleT5.getSymbolicName(),
				"1" + pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString(),
				"2" + pidBase + "|" + bundleT6.getSymbolicName() + "|" + bundleT6.getHeaders().get(Constants.BUNDLE_VERSION).toString(),
				"0" + pidBase + "|" + bundleT5.getSymbolicName() + "|555.555.555.Not",
				"0" + pidBase + "|" + bundleT6.getSymbolicName() + "|555.555.555.Not",
				"1" + pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT5.getLocation(),
				"2" + pidBase + "|" + bundleT6.getSymbolicName() + "|" + bundleT6.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT6.getLocation(),
				"0" + pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT5.getLocation() + "Not",
				"0" + pidBase + "|" + bundleT6.getSymbolicName() + "|" + bundleT6.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT6.getLocation() + "Not"
		};
		final List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final List<Configuration> configs = new ArrayList<>();

		try {
			final SynchronizerImpl sync1_1 = new SynchronizerImpl("T5-1");
			list.add(context.registerService(Synchronizer.class,
					sync1_1, propsForSyncT5_1));
			final SynchronizerImpl sync2_1 = new SynchronizerImpl("T6-1");
			list.add(context.registerService(Synchronizer.class,
					sync2_1, propsForSyncT6_1));

			this.startTargetBundle(bundleT5);
			this.setCPtoBundle("*", ConfigurationPermission.TARGET, bundleT5, false);
			this.startTargetBundle(bundleT6);
			this.setCPtoBundle("*", ConfigurationPermission.TARGET, bundleT6, false);

			System.out.println("Wait for signal.");
			int count1_1 = 0, count2_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();

			// let's create some configurations
			String previousKeyT5 = null, previousKeyT6 = null;
			for(int i=0; i<pids.length; i++) {
				// key contains marker at char 0 + pid
				final String key = pids[i];
				final String pid = key.substring(1);
				System.out.println("Creating config " + pid);
				final Configuration c = this.cm.getConfiguration(pid, "?*");
				configs.add(c);
				final String propPreviousKeyT5 = previousKeyT5;
				final String propPreviousKeyT6 = previousKeyT6;
				Dictionary<String,Object> newprops = new Hashtable<>();
				newprops.put("test", key);
				if (propPreviousKeyT5 != null) {
					newprops.put("previousT5", propPreviousKeyT5);
				}
				if (propPreviousKeyT6 != null) {
					newprops.put("previousT6", propPreviousKeyT6);
				}
				c.update(newprops);

		        // check T5
		        final char deliveryMarker = key.charAt(0);
		        if ( deliveryMarker == '0' || deliveryMarker == '2') {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
					assertThat(sync1_1.getProps().get("test")).as("Pid is wrong").isEqualTo(key);
					previousKeyT5 = key;
		        }
		        // check T6
		        if ( deliveryMarker == '0' || deliveryMarker == '1') {
		        	assertNoCallback(sync2_1, count2_1);
		        } else {
		        	count2_1 = assertCallback(sync2_1, count2_1);
					assertThat(sync2_1.getProps().get("test")).as("Pid is wrong").isEqualTo(key);
					previousKeyT6 = key;
		        }
			}

			// we now delete the configuration in reverse order
			while ( configs.size() > 0 ) {
				final Configuration c = configs.remove(configs.size() - 1);
				final String key = (String) c.getProperties().get("test");
				previousKeyT5 = (String) c.getProperties().get("previousT5");
				previousKeyT6 = (String) c.getProperties().get("previousT6");
				c.delete();

		        // check T5
		        final char deliveryMarker = key.charAt(0);
		        if ( deliveryMarker == '0' || deliveryMarker == '2') {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
		        	if ( configs.size() == 0 ) {
		        		// removed last config, so this is a delete
		        		assertThat(sync1_1.getProps()).isNull();
		        	} else {
		        		// this is an update = downgrade to a previous config
		        		assertThat(sync1_1.getProps()).isNotNull();
		        		final String newPid = (String) sync1_1.getProps().get("test");
		        		assertThat(newPid).as("Pid is wrong").isEqualTo(previousKeyT5);
		        	}
		        }
		        // check T6
		        if ( deliveryMarker == '0' || deliveryMarker == '1') {
		        	assertNoCallback(sync2_1, count2_1);
		        } else {
		        	count2_1 = assertCallback(sync2_1, count2_1);
		        	if ( configs.size() == 0 ) {
		        		// removed last config, so this is a delete
		        		assertThat(sync2_1.getProps()).isNull();
		        	} else {
		        		// this is an update = downgrade to a previous config
		        		assertThat(sync2_1.getProps()).isNotNull();
		        		final String newPid = (String) sync2_1.getProps().get("test");
		        		assertThat(newPid).as("Pid is wrong").isEqualTo(previousKeyT6);
		        	}
		        }
			}


		} finally {
			cleanUpForCallbackTest(bundleT5, bundleT6, null, null, list);
			Iterator<Configuration> i = configs.iterator();
			while ( i.hasNext() ) {
				final Configuration c = i.next();
				c.delete();
			}
		}
    	System.out.println("Testing targeted pids...finished");
    }

    /**
	 * Test targeted factory pids
	 *
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
    public void testTargetedFactoryPid() throws Exception {
    	System.out.println("Testing targeted factory pids...");

		final Bundle bundleT5 = context.installBundle(
				getWebServer() + "bundleT5.jar");
		final String pidBase = Util.createPid("pid_targeted3");
		final String[] pids = new String[] {
				pidBase,
				pidBase + "|" + bundleT5.getSymbolicName(),
				pidBase + "|Not" + bundleT5.getSymbolicName(),
				pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString(),
				pidBase + "|" + bundleT5.getSymbolicName() + "|555.555.555.Not",
				pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT5.getLocation(),
				pidBase + "|" + bundleT5.getSymbolicName() + "|" + bundleT5.getHeaders().get(Constants.BUNDLE_VERSION).toString() + "|" + bundleT5.getLocation() + "Not"
		};
		final List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final List<Configuration> configs = new ArrayList<>();

		try {
			final SynchronizerImpl sync1_1 = new SynchronizerImpl("T5-1");
			list.add(context.registerService(Synchronizer.class,
					sync1_1, propsForSyncT5_1));

			this.startTargetBundle(bundleT5);
			this.setCPtoBundle("*", ConfigurationPermission.TARGET, bundleT5, false);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();

			// let's create some configurations
			for(int i=0; i<pids.length; i++) {
				final String factoryPid = pids[i];
				System.out.println("Creating factory config " + factoryPid);
				final Configuration c = this.cm.createFactoryConfiguration(factoryPid, null);
				configs.add(c);
				Dictionary<String,Object> newprops = new Hashtable<>();
				newprops.put("test", c.getPid());
				newprops.put("factoryPid", factoryPid);
				c.update(newprops);

		        if ( factoryPid.indexOf("Not") != -1 ) {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
					assertThat(sync1_1.getProps().get("test")).as("Pid is wrong").isEqualTo(c.getPid());
		        }
			}
			// now delete them - order doesn't really matter, but we use reverse order anyway
			while ( configs.size() > 0 ) {
				final Configuration c = configs.remove(configs.size() - 1);
				final String pid = (String) c.getProperties().get("test");
				final String factoryPid = (String) c.getProperties().get("factoryPid");
				c.delete();
		        if ( factoryPid.indexOf("Not") != -1 ) {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
					assertThat(sync1_1.getProps().get("test")).as("Pid is wrong").isEqualTo(pid);
					assertThat(sync1_1.getProps().get("_deleted_")).as("Pid is not delete").isEqualTo(Boolean.TRUE);
		        }
			}


		} finally {
			cleanUpForCallbackTest(bundleT5, null, null, null, list);
			Iterator<Configuration> i = configs.iterator();
			while ( i.hasNext() ) {
				final Configuration c = i.next();
				c.delete();
			}
		}
    	System.out.println("Testing targeted factory pids...finished");
    }

    /**
	 * Test targeted pids
	 * Use a pid with a '|' .
	 *
	 * Enterprise 5.0 - ConfigAdmin 1.5
	 */
	@Test
    public void testNegativeTargetedPid() throws Exception {
    	System.out.println("Testing targeted pids...");

		final Bundle bundleT5 = context.installBundle(
				getWebServer() + "bundleT5.jar");
		final String pid1 = Util.createPid("pid");
		final String pid2 = Util.createPid("pid|targeted2");

		final String[] pids = new String[] {pid1, pid2};
		final List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final List<Configuration> configs = new ArrayList<>();

		try {
			final SynchronizerImpl sync1_1 = new SynchronizerImpl("T5-2");
			list.add(context.registerService(Synchronizer.class,
					sync1_1, propsForSyncT5_1));

			this.startTargetBundle(bundleT5);
			this.setCPtoBundle("*", ConfigurationPermission.TARGET, bundleT5, false);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();

			// let's create some configurations
			for(int i=0; i<pids.length; i++) {
				final String pid = pids[i];
				System.out.println("Creating config " + pid);
				final Configuration c = this.cm.getConfiguration(pid, null);
				configs.add(c);
				c.update(singletonDictionary("test", pid));

		        if ( pid.indexOf("|") == -1 ) {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
					assertThat(sync1_1.getProps().get("test")).as("Pid is wrong").isEqualTo(pid);
		        }
			}

			// we now delete the configuration in reverse order
			while ( configs.size() > 0 ) {
				final Configuration c = configs.remove(configs.size() - 1);
				final String pid = (String) c.getProperties().get("test");
				c.delete();
		        if ( pid.indexOf("|") == -1 ) {
		        	assertNoCallback(sync1_1, count1_1);
		        } else {
		        	count1_1 = assertCallback(sync1_1, count1_1);
		        	// remove, so no rebind
	        		assertThat(sync1_1.getProps()).isNull();
		        }
			}


		} finally {
			cleanUpForCallbackTest(bundleT5, null, null, null, list);
			Iterator<Configuration> i = configs.iterator();
			while ( i.hasNext() ) {
				final Configuration c = i.next();
				c.delete();
			}
		}
    	System.out.println("Testing targeted pids...finished");
    }

    /**
	 * Dynamic binding( configuration with null location and ManagedService)
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String,String)
	 * @spec Configuration.getBundleLocation()
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.getProperties()
	 *
	 * @throws Exception
	 */
	@Test
    public void testDynamicBinding() throws Exception {
		Bundle bundle1 = context.installBundle(
				getWebServer() + "targetb1.jar");

		final String bundlePid = Util.createPid("bundlePid1");
		Configuration conf = null;
		ServiceRegistration<Synchronizer> reg = null;
		Dictionary<String,Object> props = null;

		/*
		 * 1. created newly with non-null location and after set to null. After
		 * that, ManagedService is registered.
		 */
		System.out.println("############ 1 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, bundle1.getLocation());

			props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-1");
			conf.update(props);
			conf.setBundleLocation(null);

			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			assertThat(conf.getBundleLocation()).as("Dynamic binding(STARTED)").isEqualTo(bundle1.getLocation());

			bundle1.stop();
			assertThat(bundle1.getLocation()).as("Dynamic binding(STOPPED). Wait for a while.").isEqualTo(conf.getBundleLocation());
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			/*
			 * After the dynamically bound bundle has been uninstalled, the
			 * location must be reset to null.
			 */
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED)").isNull();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			// conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}

		/* 2. created newly with null location.(with properties) */
		System.out.println("############ 2 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, null);
			/* props is set. */
			props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-2");
			conf.update(props);
			reg = null;

			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			assertThat(conf.getBundleLocation()).as("Dynamic binding(STARTED)").isEqualTo(bundle1.getLocation());

			bundle1.stop();
			assertThat(bundle1.getLocation()).as("Dynamic binding(STOPPED).Wait for a while.").isEqualTo(conf.getBundleLocation());
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED)").isNull();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			// conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}
		/* 3. created newly with null location. (no properties) */
		System.out.println("############ 3 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, null);
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			assertThat(conf.getBundleLocation()).as("Dynamic binding(STARTED)").isEqualTo(bundle1.getLocation());

			bundle1.stop();
			assertThat(bundle1.getLocation()).as("Dynamic binding(STOPPED).Wait for a while.").isEqualTo(conf.getBundleLocation());
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED)").isNull();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			// conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}
		/*
		 * 4. created newly with null location and dynamic bound. Then
		 * explicitly set location. --> never dynamically unbound - bound
		 * anymore.
		 */
		System.out.println("############ 4 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, null);
			props = new Hashtable<String,Object>();
			props.put("StringKey", testMethodName + "-4");
			conf.update(props);
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			assertThat(conf.getBundleLocation()).as("Dynamic binding(STARTED)").isEqualTo(bundle1.getLocation());

			conf.setBundleLocation(bundle1.getLocation());
			bundle1.stop();
			assertThat(bundle1.getLocation()).as("No more Dynamic binding(STOPPED). Wait for a while.").isEqualTo(conf.getBundleLocation());
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			assertThat(conf.getBundleLocation()).as("No more Dynamic binding(UNINSTALLED)").isEqualTo(bundle1.getLocation());
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			// conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}
		/*
		 * 5. dynamic binding and cm restart 1 (with properties).
		 */
		/**
		 * (a)install test bundle. (b)configure test bundle. (c)stop
		 * configuration admin service. (d)start configuration admin service
		 *
		 * ==> configuration is still bound to the target bundle
		 */
		System.out.println("############ 5 testDynamicBinding()");
		Bundle cmBundle = null;
		try {
			conf = cm.getConfiguration(bundlePid, null);
			props = new Hashtable<String,Object>();
			props.put("StringKey", testMethodName + "-5");
			conf.update(props);
			reg = null;

			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			assertThat(calledback).as("ManagedService MUST be called back.").isTrue();
			bundle1.stop();

			restartCM();

			settle(SIGNAL_WAITING_TIME * 2);
			Configuration[] confs = cm.listConfigurations(null);
			assertThat(confs).as("confs must NOT be empty:").isNotNull();
			assertThat(confs.length - existingConfigs.size()).as("confs.length must be one").isOne();
			for (int i = 0; i < confs.length; i++) {
                conf = confs[i];
                if (!existingConfigs.contains(conf.getPid())) {
                    System.out.println("confs[" + i + "].getBundleLocation()=" + confs[i].getBundleLocation());
                    assertThat(confs[i].getBundleLocation()).as("Dynamic binding(UNINSTALLED):confs[" + i
                        + "].getBundleLocation() must be the target bundle").isEqualTo(bundle1.getLocation());
                }
            }
			conf = cm.getConfiguration(bundlePid);
			assertThat(conf.getBundleLocation()).as("Restarted CM: Must be still bound to the target bundle.").isEqualTo(bundle1.getLocation());
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}

		/*
		 * 6. dynamic binding and cm restart 2(with properties).
		 */
		/**
		 * (a)install test bundle. (b)configure test bundle. (c)stop
		 * configuration admin service. (d)uninstall test bundle. (e)start
		 * configuration admin service
		 *
		 * ==> configuration is still bound to the uninstalled bundle
		 */
		System.out.println("############ 6 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, null);
			props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-6");
			conf.update(props);
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			cmBundle = stopCmBundle();
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			bundle1.stop();
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			startCmBundle(cmBundle);
			cm = getService(ConfigurationAdmin.class);
			settle(SIGNAL_WAITING_TIME * 2);
			Configuration[] confs = cm.listConfigurations(null);
			assertThat(confs).as("confs must NOT be empty:").isNotNull();
			assertThat(confs.length - existingConfigs.size()).as("confs.length must be one").isOne();
            for (int i = 0; i < confs.length; i++) {
                conf = confs[i];
                if (!existingConfigs.contains(conf.getPid())) {
                    System.out.println("confs[" + i + "].getBundleLocation()=" + confs[i].getBundleLocation());
                    assertThat(confs[i].getBundleLocation()).as("Dynamic binding(UNINSTALLED):confs[" + i + "].getBundleLocation() must be null").isEqualTo(null);
                }
            }
			conf = cm.getConfiguration(bundlePid);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED): Must be Re-bound").isEqualTo(context.getBundle().getLocation());
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}

		/*
		 * 7. dynamic binding and cm restart 2 (with no properties).
		 */
		/**
		 * (a)install test bundle. (b)configure test bundle. (c)stop
		 * configuration admin service. (d)uninstall test bundle. (e)start
		 * configuration admin service
		 *
		 * ==> configuration is still bound to the uninstalled bundle
		 */
		System.out.println("############ 7 testDynamicBinding()");
		try {
			conf = cm.getConfiguration(bundlePid, null);
			// props = new Hashtable();
			// props.put("StringKey", testMethodName+"-7");
			// conf.update(props);
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME);
			cmBundle = stopCmBundle();
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			bundle1.stop();
			settle(SIGNAL_WAITING_TIME);
			bundle1.uninstall();
			System.out.println("Target Bundle is uninstalled. Wait for a while to check unbound.");
			settle(SIGNAL_WAITING_TIME);
			startCmBundle(cmBundle);
			cm = getService(ConfigurationAdmin.class);
			settle(SIGNAL_WAITING_TIME * 2);
			conf = cm.getConfiguration(bundlePid, null);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED): Must be reset to null").isEqualTo(null);
			conf = cm.getConfiguration(bundlePid);
			assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED): Must be Re-bound").isEqualTo(context.getBundle().getLocation());
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
			conf = null;
		}

		/*
		 * [In Progress] 8. After created newly with non-null location and
		 * ManagedService is registered, the location is set to null. What
		 * happens ?
		 */
		System.out.println("############ 8 testDynamicBinding()");
		ServiceRegistration< ? > reg2 = null;
		Bundle bundle2 = null;
		try {
			conf = cm.getConfiguration(bundlePid, getWebServer()
					+ "targetb1.jar");
			props = new Hashtable<String,Object>();
			props.put("StringKey", testMethodName + "-8");
			conf.update(props);

			SynchronizerImpl sync = new SynchronizerImpl("ID1");
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			assertThat(calledback).as("ManagedService MUST be called back in case conf has no properties.").isTrue();
			assertThat(conf.getBundleLocation()).as("Dynamic binding(STARTED)").isEqualTo(bundle1.getLocation());

			conf.setBundleLocation(null);
			System.out.println("Wait for signal.");
			// calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			// assertThat(// calledback).as("ManagedService MUST NOT be called back.").isFalse();
			// assertThat(conf.getBundleLocation()).as("Must be still bound to the target bundle.").isEqualTo(// bundle1.getLocation());

			// TODO: check if the unbound once (updated(null) is called)
			// and bound againg (updated(props) is called ).

			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count++);
			assertThat(calledback).as("ManagedService MUST be called back.").isTrue();
			conf.update(props);
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count++);
			assertThat(calledback).as("ManagedService MUST be called back.").isTrue();
			assertThat(conf.getBundleLocation()).as("Must be bound to the target bundle. again.").isEqualTo(bundle1.getLocation());

			bundle2 = context.installBundle(
					getWebServer() + "targetb2.jar");
			SynchronizerImpl sync2 = new SynchronizerImpl("ID2");
			reg2 = context.registerService(Synchronizer.class,
					sync2, propsForSync2);
			this.startTargetBundle(bundle2);
			System.out.println("Wait for signal.");
			int count2 = 0;
			boolean calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			//assertThat(//		calledback2).as(//		"ManagedService MUST NOT be called back in case conf has no properties.").isFalse();
			assertThat(calledback2).as("ManagedService MUST be called back with null property.").isTrue();

			bundle1.stop();
			assertThat(bundle1.getLocation()).as("Dynamic binding(STOPPED). Wait for a while.").isEqualTo(conf.getBundleLocation());
            calledback = sync2.waitForSignal(SIGNAL_WAITING_TIME, count2 + 1);
			assertThat(calledback).as("ManagedService2 MUST NOT be called back.").isFalse();
			bundle1.uninstall();
			System.out.println("Dynamic binding(UNINSTALLED). Wait for a while.");
			calledback2 = sync2
					.waitForSignal(SIGNAL_WAITING_TIME * 2, ++count2);
            assertThat(calledback2).as("ManagedService2 MUST be called back.").isTrue();

            assertThat(conf.getBundleLocation()).as("Dynamic binding(UNINSTALLED). Wait for a while.").isEqualTo(bundle2.getLocation());
			props.put("StringKey", "stringvalue2");
			conf.update(props);
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService MUST be called back.").isTrue();
			conf.delete();
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService MUST be called back.").isTrue();
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService MUST NOT be called back.").isFalse();

		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
			if (reg2 != null)
				reg2.unregister();
			reg2 = null;
			reg2 = null;
			if (bundle2 != null && bundle2.getState() != Bundle.UNINSTALLED)
				bundle2.uninstall();
			bundle2 = null;
			conf = cm.getConfiguration(bundlePid);
			if (conf != null)
				conf.delete();
		}

        /*
         * 9. Test dynamic bindings from getConfiguration(pid) and
         * createConfiguration(pid) (Member Bug 2551)
         */
        System.out.println("############ 9 testDynamicBinding()");
        String dynamicPid1 = Util.createPid("dynamicPid1");
        String dynamicPid2 = Util.createPid("dynamicPid2");
        String dynamicFactoryPid = Util.createPid("dynamicFactoryPid");
        String dynamicFactoryPidInstance = null;
        try {
			props = new Hashtable<String,Object>();
            props.put("StringKey", "String Value");

            // make sure this bundle has enough permissions
            this.setAppropriatePermission();

            // ensure unbound configuration
            conf = this.cm.getConfiguration(dynamicPid1, null);
            assertThat(conf).as("Configuration must exist for " + dynamicPid1).isNotNull();
            assertThat(conf.getProperties()).as("Configuration must be new for " + dynamicPid1).isNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicPid1 + " must be unbound").isNull();
            conf.update(props);

            SynchronizerImpl sync = new SynchronizerImpl("ID1");
			reg = context.registerService(Synchronizer.class, sync,
					propsForSync1);
            bundle1 = context.installBundle(getWebServer() + "targetb1.jar");
            this.startTargetBundle(bundle1);
            System.out.println("Wait for signal.");

			ServiceReference<ConfigurationAdmin> caref = bundle1
					.getBundleContext()
					.getServiceReference(ConfigurationAdmin.class);
            ConfigurationAdmin ca = bundle1.getBundleContext().getService(caref);

            // ensure configuration 1 is bound to bundle1
            conf = ca.getConfiguration(dynamicPid1);
            assertThat(conf).as("Configuration must exist for " + dynamicPid1).isNotNull();
            assertThat(conf.getProperties()).as("Configuration must not be new for " + dynamicPid1).isNotNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicPid1 + " must be bound to " + bundle1.getLocation()).isEqualTo(bundle1.getLocation());

            // ensure configuration 2 is bound to bundle1
            conf = ca.getConfiguration(dynamicPid2);
            assertThat(conf).as("Configuration must exist for " + dynamicPid2).isNotNull();
            assertThat(conf.getProperties()).as("Configuration must be new for " + dynamicPid2).isNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicPid2 + " must be bound to " + bundle1.getLocation()).isEqualTo(bundle1.getLocation());
            conf.update(props);

            // ensure factory configuration bound to bundle1
            conf = ca.createFactoryConfiguration(dynamicFactoryPid);
            dynamicFactoryPidInstance = conf.getPid();
            assertThat(conf).as("Factory Configuration must exist for " + dynamicFactoryPid).isNotNull();
            assertThat(conf.getProperties()).as("Factory Configuration must be new for " + dynamicFactoryPid).isNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Factory Configuration for " + dynamicFactoryPid + " must be bound to " + bundle1.getLocation()).isEqualTo(bundle1.getLocation());
            conf.update(props);

            SynchronizerImpl sync2 = new SynchronizerImpl("SyncListener");
			reg2 = context.registerService(ConfigurationListener.class,
					new SyncEventListener(sync2),
                null);

            // unsinstall the bundle, make sure configurations are unbound
            bundle1.uninstall();

            // wait for three (CM_LOCATION_CHANGED) events
            boolean threeEvents = sync2.waitForSignal(500, 3);
            assertThat(threeEvents).as("Expecting three CM_LOCATION_CHANGED events after bundle uninstallation").isTrue();

            // ensure configuration 1 is unbound
            conf = this.cm.getConfiguration(dynamicPid1, null);
            assertThat(conf).as("Configuration must exist for " + dynamicPid1).isNotNull();
            assertThat(conf.getProperties()).as("Configuration must not be new for " + dynamicPid1).isNotNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicPid1 + " must be unbound").isNull();

            // ensure configuration 2 is unbound
            conf = this.cm.getConfiguration(dynamicPid2, null);
            assertThat(conf).as("Configuration must exist for " + dynamicPid2).isNotNull();
            assertThat(conf.getProperties()).as("Configuration must not be new for " + dynamicPid2).isNotNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicPid2 + " must be unbound").isNull();

            // ensure factory configuration is unbound
            conf = this.cm.getConfiguration(dynamicFactoryPidInstance, null);
            assertThat(conf).as("Configuration must exist for " + dynamicFactoryPidInstance).isNotNull();
            assertThat(conf.getFactoryPid()).as("Configuration " + dynamicFactoryPidInstance + " must be factory configuration for "
                + dynamicFactoryPid).isEqualTo(dynamicFactoryPid);
            assertThat(conf.getProperties()).as("Configuration must not be new for " + dynamicFactoryPidInstance).isNotNull();
            assertThat(this.getBundleLocationForCompare(conf)).as("Configuration for " + dynamicFactoryPidInstance + " must be unbound").isNull();

        } finally {
            if (reg != null) reg.unregister();
            reg = null;
            if (reg2 != null) reg2.unregister();
            reg2 = null;
            if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED) bundle1.uninstall();
            bundle1 = null;
            conf = cm.getConfiguration(dynamicPid1);
            if (conf != null) conf.delete();
            conf = cm.getConfiguration(dynamicPid2);
            if (conf != null) conf.delete();
            if (dynamicFactoryPidInstance != null) {
                conf = cm.getConfiguration(dynamicFactoryPidInstance);
                if (conf != null) conf.delete();
            }
        }
	}

	private void startTargetBundle(Bundle bundle) throws BundleException {
		if (this.permissionFlag)
			bundle.start();
		else {
			PermissionInfo[] defPis = permAdmin.getDefaultPermissions();
			String[] locations = permAdmin.getLocations();
			Map<String,PermissionInfo[]> bundlePisMap = null;
			if (locations != null) {
				bundlePisMap = new HashMap<>(locations.length);
				for (int i = 0; i < locations.length; i++) {
					bundlePisMap.put(locations[i],
							permAdmin.getPermissions(locations[i]));
				}
			}
			this.resetPermissions();
			bundle.start();
			// this.printoutPermissions();
			this.setPreviousPermissions(defPis, bundlePisMap);
		}
	}

	private void setPreviousPermissions(PermissionInfo[] defPis,
			Map<String,PermissionInfo[]> bundlePisMap) {

		PermissionInfo[] pis = null;
		if (bundlePisMap != null)
			for (Iterator<String> keys = bundlePisMap.keySet().iterator(); keys
					.hasNext();) {
				String location = keys.next();
				if (location.equals(thisLocation))
					pis = bundlePisMap.get(location);
				else
					permAdmin.setPermissions(location,
							bundlePisMap.get(location));
			}
		permAdmin.setDefaultPermissions(defPis);
		if (pis != null)
			permAdmin.setPermissions(thisLocation, pis);
	}

	private void setInappropriatePermission() throws BundleException {
		if (permAdmin == null)
			return;
		this.resetPermissions();
		list.clear();
		add(list, PropertyPermission.class.getName(), "*", "READ,WRITE");
		add(list, PP, "*", "IMPORT,EXPORTONLY");
		add(list, SP, "*", "GET,REGISTER");
		// add(list, CP, "*", "CONFIGURE");
		add(list, AP, "*", "*");
		permissionFlag = false;
		this.setBundlePermission(super.context.getBundle(), list);
	}

	private void setAppropriatePermission() throws BundleException {
		if (permAdmin == null)
			return;
		this.resetPermissions();
		list.clear();
		add(list, PropertyPermission.class.getName(), "*", "READ,WRITE");
		add(list, PP, "*", "IMPORT,EXPORTONLY");
		add(list, SP, "*", "GET,REGISTER");
		add(list, CP, "*", "CONFIGURE");
		add(list, AP, "*", "*");
		permissionFlag = true;
		this.setBundlePermission(super.context.getBundle(), list);

	}

	/**
	 * TODO comments
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.update()
	 * @spec Configuration.getProperties()
	 *
	 * @throws Exception
	 */
	@Test
	public void testUpdate() throws Exception {
		String pid = Util.createPid();
		Configuration conf = cm.getConfiguration(pid);
		Dictionary<String,Object> props = conf.getProperties();
		assertThat(props).as("Properties in conf").isNull();

		Hashtable<String,Object> newprops = new Hashtable<String,Object>();
		newprops.put("somekey", "somevalue");
		conf.update(newprops);
		props = conf.getProperties();
		assertThat(props).as("Properties in conf").isNotNull();
		assertThat(props.get("somekey")).as("conf property 'somekey'").isEqualTo("somevalue");
		Configuration conf2 = cm.getConfiguration(pid);
		Dictionary<String,Object> props2 = conf2.getProperties();
		assertThat(props2).as("Properties in conf2").isNotNull();
		assertThat(props2.get("somekey")).as("conf2 property 'somekey'").isEqualTo("somevalue");
		// assertThat(conf2).as("Same configurations").isSameAs(conf);
		// assertThat(conf2).as("Equal configurations").isEqualTo(conf);
		// assertThat(conf2.getPid()).as("Equal pids").isEqualTo(conf.getPid());
		assertThat(equals(conf, conf2)).as("Equal configurations").isTrue();

		/* check returned properties are copied ones. */
		Dictionary<String,Object> props3 = conf2.getProperties();
		props3.put("KeyOnly3", "ValueOnly3");
		assertThat(props2.get("KeyOnly3") == null).as("Properties are copied").isTrue();

		/* Try to update with illegal configuration types */
		Hashtable<String,Object> illegalprops = new Hashtable<>();
		Collection<Object> v = new ArrayList<>();
		v.add("a string");
		v.add(Locale.getDefault());
		illegalprops.put("somekey", "somevalue");
		illegalprops.put("anotherkey", v);
		String message = "updating with illegal properties";
		try {
			conf2.update(illegalprops);
			/* A IllegalArgumentException should have been thrown */
			failException(message, IllegalArgumentException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, IllegalArgumentException.class, e);
		}

		/* contains case variants of the same key name */
		Hashtable<String,Object> casevariants = new Hashtable<>();
		casevariants.put("somekey", "somevalue");
		casevariants.put("SomeKey", "SomeValue");
		message = "updating with illegal properties (case variants of the same key)";
		try {
			conf2.update(casevariants);
			/* A IllegalArgumentException should have been thrown */
			failException(message, IllegalArgumentException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, IllegalArgumentException.class, e);
		}

	}

	/**
	 * Test the update if different method.
	 * Tests synchronous listeners and managed service.
	 */
	@Test
	public void testUpdateIfDifferent() throws Exception {
		final String pid = Util.createPid("uid");
		final List<ConfigurationEvent> events = new ArrayList<>();
		final List<Dictionary<String, ?>> updates = new ArrayList<>();
		final CyclicBarrier barrier = new CyclicBarrier(2);
		final SynchronousConfigurationListener listener = new SynchronousConfigurationListener() {

			@Override
			public void configurationEvent(final ConfigurationEvent event) {
				if (pid.equals(event.getPid())) {
					synchronized (events) {
						events.add(event);
					}
				}

			}
		};
		final ManagedService ms = new ManagedService() {

			@Override
			public void updated(final Dictionary<String, ?> properties) throws ConfigurationException {
				System.out.println("Updating managed service " + properties);
    			updates.add(properties);
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					throw new RuntimeException(e);
				}
			}

		};
		this.registerService(
				SynchronousConfigurationListener.class.getName(),
				listener, null);
	    final Hashtable<String,Object> msProps = new Hashtable<String,Object>();
	    msProps.put(Constants.SERVICE_PID, pid);
		this.registerService(
				ManagedService.class.getName(),
				ms, msProps);
		// wait for managed service to get configured with null
		barrier.await();
		barrier.reset();
		try {
			final Configuration conf = cm.getConfiguration(pid);

			final long startLevel = conf.getChangeCount();
			final Hashtable<String,Object> props1 = new Hashtable<String,Object>();
			props1.put("scalar", "somevalue");
			props1.put("array", new long[] {1, 2});
			props1.put("collection", Arrays.asList(1, 2));
			assertThat(conf.updateIfDifferent(props1)).isTrue();
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 1);

			assertThat(events).hasSize(1);
			// wait for managed service
			barrier.await();
			barrier.reset();

			// update again with same props
			assertThat(conf.updateIfDifferent(props1)).isFalse();
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 1);
			assertThat(events).hasSize(1);
            
			// update scalar prop
			final Hashtable<String,Object> props2 = new Hashtable<String,Object>(props1);
			props2.put("scalar", "newvalue");
			assertThat(conf.updateIfDifferent(props2)).isTrue();
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 2);

			assertThat(events).hasSize(2);
			
			// wait for managed service
			barrier.await();
			barrier.reset();

			// update array prop
			final Hashtable<String,Object> props3 = new Hashtable<String,Object>(props2);
			props3.put("array",  new long[] {3, 4});
			assertThat(conf.updateIfDifferent(props3)).isTrue();
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 3);
			assertThat(events).hasSize(3);
			
			// wait for managed service
			barrier.await();
			barrier.reset();

			// update collection prop
			final Hashtable<String,Object> props4 = new Hashtable<String,Object>(
					props3);
			props4.put("collection", Arrays.asList(3, 4));
			assertThat(conf.updateIfDifferent(props4)).isTrue();
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 4);
			assertThat(events).hasSize(4);
			
			// check updates to managed service
			System.out.println("Waiting for managed service updates");
			// wait for managed service
			barrier.await();
			barrier.reset();

			assertThat(updates.get(0)).isNull();
			assertThat(updates.get(1).get("scalar")).isEqualTo("somevalue");
			assertThat(updates.get(2).get("scalar")).isEqualTo("newvalue");
			long[] array = (long[]) updates.get(3).get("array");
			assertThat(array[0]).isEqualTo(3);
			assertThat(array[1]).isEqualTo(4);
			ArrayList< ? > collection = new ArrayList<>(
					(Collection< ? >) updates.get(4).get("collection"));
			assertThat(collection).isEqualTo(Arrays.asList(3, 4));

		} finally {
			this.unregisterService(listener);
			this.unregisterService(ms);
		}
	}

	/**
	 * Tests if we really get the same configuration.
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.update()
	 * @spec Configuration.getPid()
	 * @throws Exception
	 */
	@Test
	public void testEquals() throws Exception {
        String pid = Util.createPid();
        try {
            Configuration conf1 = cm.getConfiguration(pid);
            Configuration conf2 = cm.getConfiguration(pid);
            assertThat(conf2).as("Equal configurations").isEqualTo(conf1);
            assertThat(equals(conf1, conf2)).as("Equal configurations").isTrue();
        } finally {
            cm.getConfiguration(pid).delete();
        }
	}

	/**
	 * Tests listing of configurations.
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec ConfigurationAdmin.listConfigurations(String)
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @throws Exception
	 */
	@Test
	public void testListConfigurations() throws Exception {
		this.setAppropriatePermission();
		/* Create configurations */
		/* pid starts with the same prefix */
		final String pid11 = Util.createPid("pid11");
		final String pid12 = Util.createPid("pid12");
		final String pid21 = Util.createPid("pid21");
		final String pid22 = Util.createPid("pid22");
		final String pid31 = Util.createPid("pid31");
		final String pid32 = Util.createPid("pid32");
		/* pid does not start with the same prefix */
		final String otherPid = "otherPid";
		// this.cleanCM();

		List<Configuration> configs = new ArrayList<>(2);
		configs.add(cm.getConfiguration(pid11));
		configs.add(cm.getConfiguration(pid12));

		List<Configuration> updatedConfigs2 = new ArrayList<>(2);
		updatedConfigs2.add(cm.getConfiguration(pid21));
		updatedConfigs2.add(cm.getConfiguration(pid22));

		Configuration otherConf = cm.getConfiguration(otherPid);

		/* location is different */
		List<Configuration> updatedConfigs3 = new ArrayList<>(2);
		updatedConfigs3.add(cm.getConfiguration(pid31, neverlandLocation));
		updatedConfigs3.add(cm.getConfiguration(pid32, neverlandLocation));

		/*
		 * Update properties on some of configurations (to make them "active")
		 */
		for (int i = 0; i < updatedConfigs2.size(); i++) {
			Hashtable<String,Object> props = new Hashtable<>();
			props.put("someprop" + i, "somevalue" + i);
			updatedConfigs2.get(i).update(props);
		}

		for (int i = 0; i < updatedConfigs3.size(); i++) {
			Hashtable<String,Object> props = new Hashtable<>();
			props.put("someprop" + i, "somevalue" + i);
			updatedConfigs3.get(i).update(props);
		}

		try {
			Configuration[] confs = cm.listConfigurations("(service.pid="
					+ Util.createPid() + "*)");
			/*
			 * Returned list must contain all of updateConfigs2 and
			 * updateConfigs3.
			 */
			checkIfAllUpdatedConfigs2and3isListed(confs, updatedConfigs2,
					updatedConfigs3, null);

			/* Inappropriate Permission */
			this.setInappropriatePermission();
			confs = cm.listConfigurations("(service.pid=" + Util.createPid()
					+ "*)");
			if (System.getSecurityManager() != null)
				/* Returned list must contain all of updateConfigs2. */
				checkIfAllUpdatedConfigs2isListed(confs, updatedConfigs2,
						updatedConfigs3, null);
			else
				/*
				 * Returned list must contain all of updateConfigs2,
				 * updateConfigs3 and otherConf.
				 */
				checkIfAllUpdatedConfigs2and3isListed(confs, updatedConfigs2,
						updatedConfigs3, otherConf);

			/* Appropriate Permission */
			this.setAppropriatePermission();
			confs = cm.listConfigurations(null);
			/*
			 * Returned list must contain all of updateConfigs2, updateConfigs3
			 * and otherConf.
			 */
			checkIfAllUpdatedConfigs2and3isListed(confs, updatedConfigs2,
					updatedConfigs3, otherConf);

			/* Inappropriate Permission */
			this.setInappropriatePermission();
			confs = cm.listConfigurations(null);
			if (System.getSecurityManager() != null)
				/*
				 * Returned list must contain all of updateConfigs2 and
				 * otherConf.
				 */
				checkIfAllUpdatedConfigs2isListed(confs, updatedConfigs2,
						updatedConfigs3, otherConf);
			else
				/*
				 * Returned list must contain all of updateConfigs2,
				 * updateConfigs3 and otherConf.
				 */
				checkIfAllUpdatedConfigs2and3isListed(confs, updatedConfigs2,
						updatedConfigs3, otherConf);

			/* if the filter string is in valid */
			/* must fail because of invalid filter. */
			String message = "try to list configurations by invalid filter string.";
			try {
				cm.listConfigurations("(service.pid=" + Util.createPid() + "*");
				/* A InvalidSyntaxException should have been thrown */
				failException(message, InvalidSyntaxException.class);
			} catch (AssertionError e) {
				throw e;
			} catch (Throwable e) {
				/* Check that we got the correct exception */
				assertException(message, InvalidSyntaxException.class, e);
			}

		}
		/* Delete all used configurations */
		finally {
			for (int i = 0; i < configs.size(); i++) {
				configs.get(i).delete();
			}
			for (int i = 0; i < updatedConfigs2.size(); i++) {
				updatedConfigs2.get(i).delete();
			}
			for (int i = 0; i < updatedConfigs3.size(); i++) {
				updatedConfigs3.get(i).delete();
			}
			otherConf.delete();
		}
		this.setAppropriatePermission();
		/* List all configurations and make sure they are all gone */
		Configuration[] leftConfs = cm
				.listConfigurations("(|(service.pid=" + pid11
						+ ")(service.pid=" + pid12 + ")(service.pid=" + pid21
						+ ")(service.pid=" + pid22 + ")(service.pid=" + pid31
						+ ")(service.pid=" + pid32 + ")(service.pid="
						+ otherPid + "))");
		assertThat(leftConfs).as("Left configurations").isNull();

	}

	private void checkIfAllUpdatedConfigs2isListed(Configuration[] confs,
			List<Configuration> updatedConfigs2,
			List<Configuration> updatedConfigs3, Configuration otherConf)
			throws IOException, InvalidSyntaxException {
		boolean otherFlag = false;
		List<Configuration> removedConfigs2 = new ArrayList<>(
				updatedConfigs2.size());
		confs = cm
				.listConfigurations("(service.pid=" + Util.createPid() + "*)");
		if (confs == null) {
			fail("No configurations returned");
		}
		for (int i = 0; i < confs.length; i++) {
			int index = isPartOf(confs[i], updatedConfigs2);
			if (index != -1) {
				System.out.println(confs[i].getPid() + " was returned");
				removedConfigs2.add(confs[i]);
			} else if (otherConf != null && confs[i].equals(otherConf)) {
				System.out.println(confs[i].getPid() + " was returned");
				otherFlag = true;
			} else {
				fail(confs[i].getPid() + " should not have been listed");
			}
		}
		if (removedConfigs2.size() != updatedConfigs2.size() || otherFlag) {
			fail("All config with nun-null properties and bound to the bundle location cannot be retrieved by listConfigurations().");
		}
	}

	private void checkIfAllUpdatedConfigs2and3isListed(Configuration[] confs,
			List<Configuration> updatedConfigs2,
			List<Configuration> updatedConfigs3, Configuration otherConf)
			throws IOException, InvalidSyntaxException {
		/*
		 * List all configurations and make sure that only the active
		 * configurations are returned
		 */
		List<Configuration> removedConfigs2 = new ArrayList<>(
				updatedConfigs2.size());
		List<Configuration> removedConfigs3 = new ArrayList<>(
				updatedConfigs3.size());
		boolean otherFlag = false;
		if (confs == null) {
			fail("No configurations returned");
		}

		for (int i = 0; i < confs.length; i++) {
			int index = isPartOf(confs[i], updatedConfigs2);
			if (index != -1) {
				System.out.println(confs[i].getPid() + " was returned");
				removedConfigs2.add(confs[i]);
				continue;
			}
			index = isPartOf(confs[i], updatedConfigs3);
			if (index != -1) {
				System.out.println(confs[i].getPid() + " was returned");
				removedConfigs3.add(confs[i]);
			} else if (otherConf != null && equals(confs[i], otherConf)) {
				System.out.println(confs[i].getPid() + " was returned");
				otherFlag = true;
            } else if (!existingConfigs.contains(confs[i].getPid())) {
				fail(confs[i].getPid() + " should not have been listed");
			}
		}
		if (removedConfigs2.size() != updatedConfigs2.size()
				|| removedConfigs3.size() != updatedConfigs3.size()
				|| otherFlag) {
			fail("All config with nun-null properties cannot be retrieved by listConfigurations().");
		}
	}

	/**
	 * Tests to register a ManagedService when a configuration is existing for
	 * it.
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.getBundleLocation()
	 */
	@Test
	public void testManagedServiceRegistration() throws Exception {
		this.setAppropriatePermission();
		final String pid = Util.createPid("somepid");

		/* create a configuration in advance, then register ManagedService */
		Configuration conf = cm.getConfiguration(pid);

		System.out.println("created configuration has null properties.");
		System.out.println("Create and register a new the ManagedService");

		Semaphore semaphore = new Semaphore(0);
		ManagedServiceImpl ms = createManagedService(pid, semaphore);
		System.out.println("Wait until the ManagedService has gotten the update");
		boolean calledBack = semaphore.tryAcquire(SIGNAL_WAITING_TIME,
				TimeUnit.MILLISECONDS);
		assertThat(calledBack).as("ManagedService is called back").isTrue();

		System.out.println("Update done!");
		conf.delete();

		final String pid2 = Util.createPid("somepid2");
		Configuration conf2 = cm.getConfiguration(pid2);

		Hashtable<String,Object> props = new Hashtable<>();
		props.put("somekey", "somevalue");
		props.put("CAPITALkey", "CAPITALvalue");
		conf2.update(props);
		System.out.println("created configuration has non-null properties.");
		System.out.println("Create and register a new the ManagedService");
		System.out.println("Wait until the ManagedService has gotten the update");
		semaphore = new Semaphore(0);
		ms = createManagedService(pid2, semaphore);
		semaphore.acquire();
		System.out.println("Update done!");

		/*
		 * Add the two properties added by the CM and then check for equality in
		 * the properties
		 */
		props.put(Constants.SERVICE_PID, pid2);
		// props.put(SERVICE_BUNDLE_LOCATION, "cm_TBC"); R3 does not include
		// service.bundleLocation anymore!
		assertEqualProperties("Properties equal?", props, ms.getProperties());
		System.out.println((String) ms.getProperties().get("service.pid"));
		// System.out.println((String) ms.getProperties().get("service.bundleLocation"));
		System.out.println(this.getBundleLocationForCompare(conf2));
		/* OUTSIDE_OF_SPEC */
		// assertThat(ms.getProperties()).as("Properties same?").isNotSameAs(props);
		conf2.delete();

	}

	private void printoutPropertiesForDebug(SynchronizerImpl sync) {
		Dictionary<String,Object> props1 = sync.getProps();
		if (props1 == null) {
			System.out.println("props = null");
		} else {
			System.out.println("props = ");
			for (Enumeration<String> enums = props1.keys(); enums
					.hasMoreElements();) {
				Object key = enums.nextElement();
				System.out.println("\t(" + key + ", " + props1.get(key) + ")");
			}
		}
	}

	/**
	 * Register ManagedService Test 2.
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistration2() throws Exception {
		this.setAppropriatePermission();
		Bundle bundle = context.installBundle(
				getWebServer() + "targetb1.jar");
		final String bundlePid = Util.createPid("bundlePid1");
		ServiceRegistration<Synchronizer> reg = null;
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		System.out.println("###################### A. testManagedServiceRegistration2.");
		try {
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle);
			System.out.println("Wait for signal.");
			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			assertThat(calledback).as("ManagedService MUST be called back even if no configuration.").isTrue();
			assertThat(sync.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle.getLocation());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-A");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();

			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
			assertThat(props.get("StringKey")).as("pid").isEqualTo(testMethodName + "-A");
			assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			assertThat(props.size()).as("Size of props must be 2").isEqualTo(2);

			/* stop and restart target bundle */
			bundle.stop();
			bundle.start();
			System.out.println("The target bundle has been started. Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			// printoutPropertiesForDebug(sync);

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("called back with null props").isNull();
			Configuration[] confs = cm.listConfigurations(null);
            assertThat(confs == null
                || (existingConfigs.size() > 0 && confs.length == existingConfigs.size())).as("confs must be empty:").isTrue();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			bundle.stop();
            cm.getConfiguration(bundlePid).delete();
		}

		/*
		 * B1. (1)Register ManagedService in advance. (2)create Configuration
		 * with different location and null props (3)setBundleLocation to the
		 * target bundle.
		 */
		System.out.println("###################### B1. testManagedServiceRegistration2.");
		try {
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle);
			System.out.println("Wait for signal.");
			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			assertThat(calledback).as("ManagedService must be called back even if no configuration.").isTrue();
			assertThat(sync.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration with different location is being created ");
			Configuration conf = cm.getConfiguration(bundlePid,
					neverlandLocation);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			this.printoutPropertiesForDebug(sync);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();
			/* The conf has null props. */
			System.out.println("The configuration is being set to the target bundle");
			conf.setBundleLocation(bundle.getLocation());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);

			/*
			 * The spec seems unclear whether the ManagedService registered by
			 * the newly bound bundle should be called back with null or not
			 * called back. The implementator of this TCK (Ikuo) thinks TCK should
			 * accept either case of (a) No callback. (b) callback with null.
			 * Otherwise, it fails. Spec of version 1.3
			 */
			if (calledback) {
				count++;
				assertThat(sync.getProps()).as("The props called back MUST be null, if the ManagedService is called back.").isNull();
			}
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B1");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
			assertThat(props.get("StringKey")).as("pid").isEqualTo(testMethodName + "-B1");
			assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			assertThat(props.size()).as("Size of props must be 2").isEqualTo(2);

			System.out.println("The configuration is being updated to null.");
			/* props is reset */
			conf.update(new Hashtable<String,Object>(0));
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
			assertThat(props.size()).as("Size of props must be 1").isOne();
			conf.update(props);
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();

			System.out.println("The configuration is being set to different location");
			conf.setBundleLocation(neverlandLocation);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must Not be called back").isFalse();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			bundle.stop();
            cm.getConfiguration(bundlePid).delete();
		}
		/*
		 * B2. (1)create Configuration with different location and non-null
		 * props. (2) Register ManagedService. (3)setBundleLocation to the
		 * target bundle.
		 */
		System.out.println("###################### B2. testManagedServiceRegistration2.");
		try {
			System.out.println("The configuration with different location is being created ");
			Configuration conf = cm.getConfiguration(bundlePid,
					neverlandLocation);
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B2");
			conf.update(props);
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle);
			System.out.println("Wait for signal.");

			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			//assertThat(//		calledback).as(//		"ManagedService must NOT be called back even if no configuration.").isFalse();
			assertThat(calledback).as("ManagedService MUST be called back with null parameter when there is no configuration.").isTrue();

			System.out.println("The configuration is being updated ");
			props.put("StringKey", "stringvalue2");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			this.printoutPropertiesForDebug(sync);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

			/* The conf has non-null props. */
			System.out.println("The configuration is being set to the target bundle");
			conf.setBundleLocation(bundle.getLocation());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			/*
			 * XXX Felix and Ikuo think the ManagedService registered by the
			 * newly bound bundle should be called back with the props. However,
			 * BJ thinks no. The implementator of this TCK (Ikuo) thinks TCK
			 * should not check it because it is unclear in the spec of version
			 * 1.3.
			 */
			// assertThat(calledback).as("ManagedService must be called back").isTrue();
			// assertThat(// calledback).as("ManagedService must NOT be called back").isFalse();
			if (calledback) {
				++count;
				assertThat(sync.getProps()).as("null props must be called back").isNotNull();
				props = sync.getProps();
				assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
				assertThat(props.get("StringKey")).as("pid").isEqualTo("stringvalue2");
				assertThat(props.size()).as("Size of props must be 2").isEqualTo(2);
			}

			System.out.println("The configuration is being updated ");
			props.put("StringKey", "stringvalue3");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
			assertThat(props.get("StringKey")).as("pid").isEqualTo("stringvalue3");
			assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			assertThat(props.size()).as("Size of props must be 2").isEqualTo(2);

			System.out.println("The configuration is being set to different location");
			conf.setBundleLocation(neverlandLocation);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			/*
			 * XXX Felix and Ikuo think the ManagedService registered by the
			 * newly bound bundle should be called back with null. However, BJ
			 * thinks no. The implementator of this TCK (Ikuo) thinks TCK should
			 * not check it strictly because it is unclear in the spec of
			 * version 1.3.
			 */
			// assertThat(calledback).as("ManagedService must be called back").isTrue();
			if (calledback) {
				++count;
				assertThat(sync.getProps()).as("called back with null props").isNull();
			}
			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must Not be called back").isFalse();

		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			bundle.stop();
            cm.getConfiguration(bundlePid).delete();
		}
		/*
		 * B3. (1)create Configuration with different location and null props.
		 * (2) Register ManagedService. (3)setBundleLocation to the target
		 * bundle.
		 */
		System.out.println("###################### B3. testManagedServiceRegistration2.");
		try {
			System.out.println("The configuration with different location is being created ");
			Configuration conf = cm.getConfiguration(bundlePid,
					neverlandLocation);
			System.out.println("The configuration is being updated ");
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle);
			System.out.println("Wait for signal.");

			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			//assertThat(//		calledback).as(//		"ManagedService must NOT be called back even if no configuration.").isFalse();
			assertThat(calledback).as("ManagedService MUST be called back with null parameter when there is no configuration.").isTrue();
			assertThat(sync.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B3");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			this.printoutPropertiesForDebug(sync);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

			/* The conf has non-null props. */
			System.out.println("The configuration is being set to the target bundle");
			conf.setBundleLocation(bundle.getLocation());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid);
			assertThat(props.get("StringKey")).as("pid").isEqualTo(testMethodName + "-B3");
			assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			assertThat(props.size()).as("Size of props must be 2").isEqualTo(2);

			System.out.println("The configuration is being set to different location");
			conf.setBundleLocation(neverlandLocation);

			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			/*
			 * XXX Felix and Ikuo think the ManagedService registered by the
			 * newly bound bundle should be called back with null. However, BJ
			 * thinks no. The implementator of this TCK (Ikuo) thinks TCK should
			 * not check it strictly because it is unclear in the spec of
			 * version 1.3.
			 */
			// assertThat(calledback).as("ManagedService must be called back").isTrue();
			if (calledback) {
				++count;
				assertThat(sync.getProps()).as("called back with null props").isNull();
			}
			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			bundle.stop();
            cm.getConfiguration(bundlePid).delete();
		}
		/*
		 * C. Configuration Admin Service is stopped once. After a while, it
		 * restarts.
		 *
		 * 1.Register ManagedService in advance. 2.create Configuration with
		 * different location. 3.setBundleLocation to the target bundle.
		 */
		Bundle cmBundle = stopCmBundle();
		try {
			int count = 0;
			SynchronizerImpl sync = new SynchronizerImpl();
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle);

			/* restart where no configuration. */
			System.out.println("Wait for restart cm bundle.");
			settle(SIGNAL_WAITING_TIME);
			startCmBundle(cmBundle);
			System.out.println("Wait for signal.");
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			assertThat(calledback).as("ManagedService is Called back even if no configuration.").isTrue();

			this.cm = getService(ConfigurationAdmin.class);

			/* Create configuration and stop/start cm. */
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle.getLocation());
			assertThat(conf).isNotNull();
            cmBundle = stopCmBundle();
			System.out.println("Wait for restart cm bundle.");
			settle(SIGNAL_WAITING_TIME);
			startCmBundle(cmBundle);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService is Called back.").isTrue();
			this.cm = getService(ConfigurationAdmin.class);
			//conf.delete();
			cm.getConfiguration(bundlePid).delete();

			/* Create configuration with null location and stop/start cm. */
			conf = cm.getConfiguration(bundlePid, null);
			assertThat(conf).isNotNull();
			cmBundle = stopCmBundle();
			System.out.println("Wait for restart cm bundle.");
			settle(SIGNAL_WAITING_TIME);
			startCmBundle(cmBundle);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService is Called back even if configuration with null.").isTrue();
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			bundle.stop();
            cm.getConfiguration(bundlePid).delete();
		}
	}

	/**
	 * Register ManagedService in advance. ManagedService has multiple pids.
	 * Then create Configuration.
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistrationWithMultiplPIDs()
			throws Exception {
		this.setAppropriatePermission();

		try {
			System.out.println("########## 1(array) testManagedServiceRegistrationWithMultiplPIDs");
			System.setProperty(
					org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_MODE,
					org.osgi.test.cases.cm.shared.Constants.MODE_ARRAY);
			internalTestRegisterManagedServiceWithMultiplePIDs();

			System.out.println("########## 1(list) testManagedServiceRegistrationWithMultiplPIDs");
			System.setProperty(
					org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_MODE,
					org.osgi.test.cases.cm.shared.Constants.MODE_LIST);
			internalTestRegisterManagedServiceWithMultiplePIDs();

			System.out.println("########## 1(set) testManagedServiceRegistrationWithMultiplPIDs");
			System.setProperty(
					org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_MODE,
					org.osgi.test.cases.cm.shared.Constants.MODE_SET);
			internalTestRegisterManagedServiceWithMultiplePIDs();
		} finally {
			System.setProperty(
					org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_MODE,
					org.osgi.test.cases.cm.shared.Constants.MODE_UNARY);
		}
	}

	private void internalTestRegisterManagedServiceWithMultiplePIDs()
			throws BundleException, IOException {

		final String bundlePid1 = Util.createPid("bundlePid1");
		final String bundlePid2 = Util.createPid("bundlePid2");
		ServiceRegistration<Synchronizer> reg = null;
		Bundle bundle1 = null;
		try {
			bundle1 = context.installBundle(
					getWebServer() + "targetb1.jar");

			SynchronizerImpl sync = new SynchronizerImpl();
			System.out.println("1 sync.getCount()=" + sync.getCount());
			reg = context.registerService(Synchronizer.class,
					sync, propsForSync1);
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			int count = 0;
			boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME,
					++count);
			assertThat(calledback).as("ManagedService MUST be called back even if no configuration.").isTrue();
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			if (!calledback)
				--count;
			System.out.println("sync.getCount()=" + sync.getCount());
			System.out.println("The configuration1 is being created");
			System.out.println("sync.getCount()=" + sync.getCount());
			Configuration conf1 = cm.getConfiguration(bundlePid1,
					bundle1.getLocation());
			System.out.println("sync.getCount()=" + sync.getCount());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration2 is being created");
			Configuration conf2 = cm.getConfiguration(bundlePid2,
					bundle1.getLocation());
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
			System.out.println("sync.getCount()=" + sync.getCount());
			assertThat(calledback).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration1 is being updated ");
			Dictionary<String,Object> props1 = new Hashtable<>();
			props1.put("StringKey1", "stringvalue1");
			conf1.update(props1);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			System.out.println("sync.getCount()=" + sync.getCount());
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			Dictionary<String,Object> props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid1);
			assertThat(props.get("StringKey1")).as("pid").isEqualTo("stringvalue1");

			System.out.println("The configuration2 is being updated ");
			Dictionary<String,Object> props2 = new Hashtable<>();
			props2.put("StringKey1", "stringvalue1");
			props2.put("StringKey2", "stringvalue2");
			conf2.update(props2);
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();

			assertThat(sync.getProps()).as("null props must be called back").isNotNull();
			props = sync.getProps();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isEqualTo(bundlePid2);
			assertThat(props.get("StringKey1")).as("pid").isEqualTo("stringvalue1");
			assertThat(props.get("StringKey2")).as("pid").isEqualTo("stringvalue2");
			assertThat(props.size()).as("Size of props must be 3").isEqualTo(3);

			System.out.println("The configuration1 is being deleted ");
			conf1.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
			System.out.println("The configuration2 is being deleted ");
			conf2.delete();
			System.out.println("Wait for signal.");
			calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
			assertThat(calledback).as("ManagedService must be called back").isTrue();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (reg != null)
				reg.unregister();
			reg = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
		}
	}

	/**
	 * Register ManagedService in advance. Then create Configuration.
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistrationDuplicatedTargets()
			throws Exception {
		this.setAppropriatePermission();
		Bundle bundle2 = context.installBundle(
				getWebServer() + "targetb2.jar");

		final String bundlePid = Util.createPid("bundlePid1");
		ServiceRegistration<Synchronizer> reg2 = null;

		/*
		 * A. One bundle registers duplicated ManagedService. Both of them must
		 * be called back.
		 */
		System.out.println("################## A testManagedServiceRegistrationDuplicatedTargets()");
		try {
			SynchronizerImpl sync2 = new SynchronizerImpl();
			reg2 = context.registerService(Synchronizer.class,
					sync2, propsForSync2);
			System.setProperty(
					org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_DUPCOUNT,
					"2");
			this.startTargetBundle(bundle2);
			System.out.println("Wait for signal.");
			boolean calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 2);
			assertThat(calledback2).as("Both ManagedService MUST be called back even if no configuration.").isTrue();
			assertThat(sync2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle2.getLocation());
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 3);
			assertThat(calledback2).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-A");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 4);
			assertThat(calledback2).as("Both ManagedService must be called back").isTrue();

			/* stop and restart target bundle */
			bundle2.stop();
			bundle2.start();
			System.out.println("The target bundle has been stopped and re-started. Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 6);
			assertThat(calledback2).as("Both ManagedService must be called back").isTrue();
			// printoutPropertiesForDebug(sync);

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 8);
			assertThat(calledback2).as("Both ManagedService must be called back").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();
		} finally {
			if (reg2 != null)
				reg2.unregister();
			reg2 = null;
			if (bundle2 != null && bundle2.getState() != Bundle.UNINSTALLED)
				bundle2.uninstall();
			bundle2 = null;
		}

		/*
		 * B1. (1)No Configuration. (2)two bundles register duplicated
		 * ManagedService. ==> Only one, firstly registered, must be called
		 * back.
		 */
		System.out.println("################## B1 testManagedServiceRegistrationDuplicatedTargets()");

		reg2 = null;
		ServiceRegistration<Synchronizer> reg1 = null;
		Bundle bundle1 = context.installBundle(
				getWebServer() + "targetb1.jar");
		System.setProperty(
				org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_DUPCOUNT,
				"1");
		bundle2 = context.installBundle(getWebServer() + "targetb2.jar");
		try {
			SynchronizerImpl sync2 = new SynchronizerImpl("ID2");
			reg2 = context.registerService(Synchronizer.class,
					sync2, propsForSync2);
			this.startTargetBundle(bundle2);
			System.out.println("Wait for signal.");
			boolean calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 1);
			assertThat(calledback2).as("ManagedService MUST be called back even if no configuration.").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();

			SynchronizerImpl sync1 = new SynchronizerImpl("ID1");
			reg1 = context.registerService(Synchronizer.class,
					sync1, propsForSync1);
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			boolean calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, 1);
			assertThat(calledback1).as("ManagedService MUST be called back even if no configuration.").isTrue();

			System.out.println("The configuration is being created");
			Configuration[] confs = cm.listConfigurations(null);
            assertThat(confs == null
                || (existingConfigs.size() > 0 && confs.length == existingConfigs.size())).as("confs must be empty:").isTrue();
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle2.getLocation());
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 2);
			this.printoutPropertiesForDebug(sync2);

			assertThat(calledback2).as("ManagedService must NOT be called back").isFalse();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, 2);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B1");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 2);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, 2);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, 3);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, 2);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

		} finally {
			if (reg1 != null)
				reg1.unregister();
			reg1 = null;
			if (reg2 != null)
				reg2.unregister();
			reg2 = null;
			if (bundle2 != null && bundle2.getState() != Bundle.UNINSTALLED)
				bundle2.uninstall();
			bundle2 = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;

		}

		/*
		 * B2. (1) Create new Conf with null props. (2)two bundles register
		 * duplicated ManagedService. ==> Only one, firstly registered, must be
		 * called back.
		 */
		System.out.println("################## B2 testManagedServiceRegistrationDuplicatedTargets()");

		reg2 = null;
		reg1 = null;
		bundle1 = context.installBundle(getWebServer() + "targetb1.jar");
		System.setProperty(
				org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_DUPCOUNT,
				"1");
		bundle2 = context.installBundle(getWebServer() + "targetb2.jar");
		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle2.getLocation());
			SynchronizerImpl sync2 = new SynchronizerImpl("ID2");
			reg2 = context.registerService(Synchronizer.class,
					sync2, propsForSync2);
			this.startTargetBundle(bundle2);
			System.out.println("Wait for signal.");
			int count2 = 0;
			boolean calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME,
					++count2);
			assertThat(calledback2).as("ManagedService MUST be called back even if configuration has no props.").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();

			SynchronizerImpl sync1 = new SynchronizerImpl("ID1");
			reg1 = context.registerService(Synchronizer.class,
					sync1, propsForSync1);
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			int count1 = 0;
			boolean calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME,
					++count1);
			//assertThat(//		calledback1).as("ManagedService MUST NOT be called back even.").isFalse();
			assertThat(calledback1).as("ManagedService MUST be called back even if deffirent bound location.").isTrue();
			assertThat(sync1.getProps()).as("null props must be called back").isNull();

			Configuration[] confs = cm.listConfigurations(null);
            assertThat(confs == null
                || (existingConfigs.size() > 0 && confs.length == existingConfigs.size())).as("confs must be empty:").isTrue();

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B2");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, count1 + 1);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, count1 + 1);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

		} finally {
			if (reg1 != null)
				reg1.unregister();
			reg1 = null;
			if (reg2 != null)
				reg2.unregister();
			reg2 = null;
			if (bundle2 != null && bundle2.getState() != Bundle.UNINSTALLED)
				bundle2.uninstall();
			bundle2 = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
		}

		/*
		 * B3. (1) Create new Conf with non-null props. (2)two bundles register
		 * duplicated ManagedService. ==> Only one, firstly registered, must be
		 * called back.
		 */
		System.out.println("################## B3 testManagedServiceRegistrationDuplicatedTargets()");

		reg2 = null;
		reg1 = null;
		bundle1 = context.installBundle(getWebServer() + "targetb1.jar");
		System.setProperty(
				org.osgi.test.cases.cm.shared.Constants.SYSTEMPROP_KEY_DUPCOUNT,
				"1");
		bundle2 = context.installBundle(getWebServer() + "targetb2.jar");
		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(bundlePid,
					bundle2.getLocation());
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", testMethodName + "-B3");
			conf.update(props);
			SynchronizerImpl sync2 = new SynchronizerImpl("ID2");
			reg2 = context.registerService(Synchronizer.class,
					sync2, propsForSync2);
			this.startTargetBundle(bundle2);
			System.out.println("Wait for signal.");
			int count2 = 0;
			boolean calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME,
					++count2);
			assertThat(calledback2).as("ManagedService MUST be called back.").isTrue();
			assertThat(sync2.getProps()).as("called back with null props").isNotNull();

			SynchronizerImpl sync1 = new SynchronizerImpl("ID1");
			reg1 = context.registerService(Synchronizer.class,
					sync1, propsForSync1);
			this.startTargetBundle(bundle1);
			System.out.println("Wait for signal.");
			int count1 = 0;
			boolean calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME,
					++count1);
			//assertThat(calledback1).as("ManagedService MUST NOT be called back.").isFalse();
			assertThat(calledback1).as("ManagedService MUST be called back even if deffirent bound location.").isTrue();
			assertThat(sync1.getProps()).as("null props must be called back").isNull();

			Configuration[] confs = cm.listConfigurations(null);
			assertThat(confs).as("confs must NOT be empty:").isNotNull();

			System.out.println("The configuration is being updated ");
			props.put("StringKey2", "stringvalue2");
			conf.update(props);
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, count1 + 1);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

			System.out.println("The configuration is being deleted ");
			conf.delete();
			System.out.println("Wait for signal.");
			calledback2 = sync2.waitForSignal(SIGNAL_WAITING_TIME, ++count2);
			assertThat(calledback2).as("ManagedService firstly registered must be called back").isTrue();
			assertThat(sync2.getProps()).as("null props must be called back").isNull();
			calledback1 = sync1.waitForSignal(SIGNAL_WAITING_TIME, count1 + 1);
			assertThat(calledback1).as("ManagedService must NOT be called back").isFalse();

		} finally {
			if (reg1 != null)
				reg1.unregister();
			reg1 = null;
			if (reg2 != null)
				reg2.unregister();
			reg2 = null;
			if (bundle2 != null && bundle2.getState() != Bundle.UNINSTALLED)
				bundle2.uninstall();
			bundle2 = null;
			if (bundle1 != null && bundle1.getState() != Bundle.UNINSTALLED)
				bundle1.uninstall();
			bundle1 = null;
		}
	}

    private Bundle stopCmBundle() {
		ServiceReference<ConfigurationAdmin> reference = this.context
				.getServiceReference(ConfigurationAdmin.class);
        Bundle cmBundle = reference.getBundle();
        try {
            cmBundle.stop();
        } catch (BundleException be) {
            fail("Error stopping CM Bundle: " + be);
        }
        cm = null;
        return cmBundle;
    }

    private void assignCm() {
        cm = getService(ConfigurationAdmin.class);
    }

    private void startCmBundle(Bundle cmBundle) {
        try {
            cmBundle.start();
        } catch (BundleException be) {
            fail("Error starting CM Bundle: " + be);
        }
        assignCm();
    }

	private Dictionary<String,Object> getManagedProperties(String pid)
			throws Exception {
		Semaphore semaphore = new Semaphore(0);
		ManagedServiceImpl ms = createManagedService(pid, semaphore);
		semaphore.acquire();
		return ms.getProperties();
	}

	/**
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getProperties()
	 * @throws Exception
	 */
	@Test
	public void testManagedProperties() throws Exception {
		String pid = Util.createPid("somepid");
		/* Set up the configuration */
		Configuration conf = cm.getConfiguration(pid);
		/* Put all legal types in the properties and update */
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("StringKey", testMethodName);
		props.put("IntegerKey", Integer.valueOf(12));
		props.put("LongKey", Long.valueOf(-29));
		props.put("FloatKey", Float.valueOf(921.14f));
		props.put("DoubleKey", Double.valueOf(1827.234));
		props.put("ByteKey", Byte.valueOf((byte) 127));
		props.put("ShortKey", Short.valueOf((short) 1));
		// props.put("BigIntegerKey", new BigInteger("123"));
		// props.put("BigDecimalkey", new BigDecimal(9872.7643));
		props.put("CharacterKey", Character.valueOf('N'));
		props.put("BooleanKey", Boolean.valueOf(true));

		Collection<Object> v = new ArrayList<>();
		v.add(testMethodName);
		// ### is now invalid ....
		// v.addElement(Integer.valueOf(12));
		// v.addElement(Long.valueOf(-29));
		// v.addElement(Float.valueOf(921.14));
		// v.addElement(Double.valueOf(1827.234));
		// v.addElement(Byte.valueOf((byte) 127));
		// v.addElement(Short.valueOf((short) 1));
		// v.addElement(new BigInteger("123"));
		// v.addElement(new BigDecimal(9872.7643));
		// v.addElement(Character.valueOf('N'));
		// v.addElement(Boolean.valueOf(true));
		// v.addElement(new String[] {"firststring", "secondstring"});
		// ### end invalid
		props.put("collectionkey", v);
		props.put("StringArray", new String[] { "string1", "string2" });
		props.put("IntegerArray", new Integer[] { Integer.valueOf(1),
				Integer.valueOf(2) });
		props.put("LongArray", new Long[] { Long.valueOf(1), Long.valueOf(2) });
		props.put("FloatArray", new Float[] {
				Float.valueOf(1.1f), Float.valueOf(2.2f)
		});
		props.put("DoubleArray", new Double[] { Double.valueOf(1.1),
				Double.valueOf(2.2) });
		props.put("ByteArray", new Byte[] { Byte.valueOf((byte) -1),
				Byte.valueOf((byte) -2) });
		props.put("ShortArray", new Short[] { Short.valueOf((short) 1),
				Short.valueOf((short) 2) });
		// props.put("BigIntegerArray", new BigInteger[] {
		// new BigInteger("1"), new BigInteger("2")
		// }
		//
		// );
		// props.put("BigDecimalArray", new BigDecimal[] {
		// new BigDecimal(1.1), new BigDecimal(2.2)
		// }
		//
		// );
		props.put("CharacterArray", new Character[] { Character.valueOf('N'),
				Character.valueOf('O') });
		props.put("BooleanArray", new Boolean[] { Boolean.valueOf(true),
				Boolean.valueOf(false) });

		// ### invalid
		// Vector v1 = new Vector();
		// v1.addElement(new Vector());
		// v1.addElement("Anystring");
		// props.put("VectorArray", new Vector[] {v1, new Vector()});

		props.put("CAPITALkey", "CAPITALvalue");
		conf.update(props);
		/* Register a managed service and get the properties */
		Dictionary<String,Object> msprops = getManagedProperties(pid);
		/*
		 * Add the two properties added by the CM and then check for equality in
		 * the properties (including preserved case)
		 */
		props.put(Constants.SERVICE_PID, pid);
		// props.put(SERVICE_BUNDLE_LOCATION, "cm_TBC"); R3 does not include
		// service.bundleLocation anymore!
		assertEqualProperties("Properties equal?", props, msprops);

		/* Check if the properties are case independent */
		String s = (String) msprops.get("sTringkeY");
		assertThat(s).as("case independant properties").isEqualTo(testMethodName);
		Hashtable<String,Object> illegalprops = new Hashtable<>();
		illegalprops.put("exception", new Exception());
		String message = "Exception is not a legal type";
		try {
			conf.update(illegalprops);
			/* A IllegalArgumentException should have been thrown */
			failException(message, IllegalArgumentException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			/* Check that we got the correct exception */
			assertException(message, IllegalArgumentException.class, e);
		}

		/* TODO: Add more illegal types (inside collections etc) */
	}

	// public void testUpdatedProperties() throws Exception {
	// /* Put all legal types in the properties and update */
	// /* Get the properties again */
	// /* Check if the properties are equals */
	// /* Check if the properties have preserved the case */
	// /* Check if the properties are case independent */
	// }

	/**
	 * Test created Factory configuration without location.
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getPid()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.getBundleLocation()
	 * @throws Exception
	 */
	@Test
	public void testCreateFactoryConfiguration() throws Exception {
		commonTestCreateFactoryConfiguration(false, getLocation());
	}

	/**
	 * Test created Factory configuration with location.
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String,String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getPid()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.getBundleLocation()
	 * @throws Exception
	 */
	@Test
	public void testCreateFactoryConfigurationWithLocation() throws Exception {
		commonTestCreateFactoryConfiguration(true, neverlandLocation);
	}

	/**
	 * Test created Factory configuration with null location.
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String,String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getPid()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getProperties()
	 * @spec Configuration.getBundleLocation()
	 * @throws Exception
	 */
	@Test
	public void testCreateFactoryConfigurationWithNullLocation()
			throws Exception {
		commonTestCreateFactoryConfiguration(true, null);
	}

	/**
	 * Test creating a factory configuration with a name
	 * 
	 * @throws Exception
	 * @since 1.6
	 */
	@Test
	public void testGetFactoryConfigurationWithName() throws Exception {
		final int NUMBER_OF_CONFIGS = 3;
		final String factorypid = Util.createPid("factorypidforname");
		final List<String> pids = new ArrayList<>();
		final List<Configuration> configs = new ArrayList<>();

		this.setAppropriatePermission();
		pids.add(factorypid);
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = cm.getFactoryConfiguration(factorypid,
					String.valueOf(i), null);
			final Dictionary<String,Object> props = new Hashtable<>();
			props.put("val", String.valueOf(i));
			conf.update(props);
			configs.add(conf);
			System.out.println("pid: " + conf.getPid());
			assertThat(!pids.contains(conf.getPid())).as("Unique pid").isTrue();
			assertThat(conf.getFactoryPid()).as("Correct factory pid").isEqualTo(factorypid);
			assertThat(conf.getPid()).as("Correct pid").isEqualTo(factorypid + "~" + String.valueOf(i));
			assertThat(getBundleLocationForCompare(conf)).as("Correct location").isEqualTo(null);

			pids.add(conf.getPid());
		}
		assertThat(cm
			.listConfigurations(
				"(service.factoryPid=" + factorypid + ")")).hasSize(NUMBER_OF_CONFIGS);
		for (int i = 0; i < configs.size(); i++) {
			Configuration[] cfgs = cm.listConfigurations(
					"(service.pid=" + configs.get(i).getPid() + ")");
			assertThat(cfgs).hasSize(1);
			assertThat(cfgs[0].getFactoryPid()).isEqualTo(configs.get(i).getFactoryPid());
			assertThat(cfgs[0].getPid()).isEqualTo(configs.get(i).getPid());
		}
		// get factory configuration with same name again
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = cm.getFactoryConfiguration(factorypid,
					String.valueOf(i), null);
			assertThat(conf.getFactoryPid()).as("Correct factory pid").isEqualTo(factorypid);
			assertThat(conf.getPid()).as("Correct pid").isEqualTo(factorypid + "~" + String.valueOf(i));
			assertThat(conf.getProperties().get("val")).isEqualTo(String.valueOf(i));
		}
		// and verify count of factory configurations
		// (should still be NUMBER_OF_CONFIGS)
		assertThat(cm.listConfigurations(
			"(service.factoryPid=" + factorypid + ")")).hasSize(NUMBER_OF_CONFIGS);

		// delete configurations
		for (int i = 0; i < configs.size(); i++) {
			Configuration conf = configs.get(i);
			conf.delete();
		}
		// ensure that no factory configurations are available anymore
		for (final String p : pids) {
			Configuration[] cfgs = cm.listConfigurations(
					"(service.pid=" + p + ")");
			assertThat(cfgs).isNull();
		}
		assertThat(cm
				.listConfigurations("(service.factoryPid=" + factorypid + ")")).isNull();
	}

	/**
	 * Test whether repeated gets of a factory config returns the same object
	 * 
	 * @throws Exception
	 * @since 1.6
	 */
	@Test
	public void testRepeatedGetFactoryConfigurationWithName() throws Exception {
		final String factorypid = Util.createPid("factorypidforname");
		final String factoryname = "aname";

		this.setAppropriatePermission();

		final Configuration conf1 = cm.getFactoryConfiguration(factorypid,
				factoryname, null);
		final Configuration conf2 = cm.getFactoryConfiguration(factorypid,
				factoryname, null);
		final Dictionary<String,Object> props1 = new Hashtable<>();
		props1.put("val", "1");
		conf1.update(props1);

		final Dictionary<String,Object> props2 = new Hashtable<>();
		props2.put("val", "2");
		conf2.update(props2);

		assertThat(cm.getFactoryConfiguration(factorypid, factoryname, null)
						.getProperties()
						.get("val")).isEqualTo("2");

		final Dictionary<String,Object> props22 = new Hashtable<>();
		props22.put("val", "22");
		conf2.update(props22);

		assertThat(cm.getFactoryConfiguration(factorypid, factoryname, null)
						.getProperties()
						.get("val")).isEqualTo("22");
	}

	private void commonTestCreateFactoryConfiguration(boolean withLocation,
			String location) throws Exception {
		final int NUMBER_OF_CONFIGS = 3;
		final String factorypid = Util.createPid("somefactorypid");
		final List<String> pids = new ArrayList<>();
		final List<Configuration> configs = new ArrayList<>();

		// Inappropriate Permission
		this.setInappropriatePermission();
		pids.add(factorypid);
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = null;
			if (withLocation) {
				/*
				 * Without appropriate ConfigurationPermission, create
				 * FactoryConfiguration with specfied location.
				 */
				String message = "try to create factory configuration without appropriate ConfigurationPermission.";
				try {
					conf = cm.createFactoryConfiguration(factorypid, location);
					/*
					 * A SecurityException should have been thrown if security
					 * is enabled
					 */
					if (System.getSecurityManager() != null)
						failException(message, SecurityException.class);
				} catch (AssertionError e) {
					throw e;
				} catch (Throwable e) {
					/* Check that we got the correct exception */
					assertException(message, SecurityException.class, e);
					/*
					 * A SecurityException should not have been thrown if
					 * security is not enabled
					 */
					if (System.getSecurityManager() == null)
						fail("Security is not enabled", e);
				}
				continue;
			} else {
				/*
				 * Even appropriate ConfigurationPermission,
				 * createFactoryConfiguration(factorypid) must be succeed
				 */
				conf = cm.createFactoryConfiguration(factorypid);
			}
			configs.add(conf);
			System.out.println("pid: " + conf.getPid());
			assertThat(!pids.contains(conf.getPid())).as("Unique pid").isTrue();
			assertThat(conf.getFactoryPid()).as("Correct factory pid").isEqualTo(factorypid);
			assertThat(conf.getProperties()).as("No properties").isNull();
			assertThat(getBundleLocationForCompare(conf)).as("Correct location").isEqualTo(location);
			/* Add the pid to the list */
			pids.add(conf.getPid());
		}
		for (int i = 0; i < configs.size(); i++) {
			Configuration conf = configs.get(i);
			conf.delete();
		}

		// Appropriate Permission
		pids.clear();
		configs.clear();
		this.setAppropriatePermission();
		pids.add(factorypid);
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = null;
			if (withLocation) {
				conf = cm.createFactoryConfiguration(factorypid, location);
			} else {
				conf = cm.createFactoryConfiguration(factorypid);
			}
			configs.add(conf);
			System.out.println("pid: " + conf.getPid());
			assertThat(!pids.contains(conf.getPid())).as("Unique pid").isTrue();
			assertThat(conf.getFactoryPid()).as("Correct factory pid").isEqualTo(factorypid);
			assertThat(conf.getProperties()).as("No properties").isNull();
			assertThat(this.getBundleLocationForCompare(conf)).as("Correct location").isEqualTo(location);
			/* Add the pid to the list */
			pids.add(conf.getPid());
		}
		for (int i = 0; i < configs.size(); i++) {
			Configuration conf = configs.get(i);
			conf.delete();
		}
	}

	private String getBundleLocationForCompare(Configuration conf)
			throws BundleException {
		String location = null;
        if (this.permissionFlag) {
            try {
                location = conf.getBundleLocation();
            } catch (SecurityException se) {
                // Bug 2539: Need to be hard on granting appropriate permission
                System.out.println("Temporarily grant CONFIGURE(" + thisLocation
                    + ") to get location of configuration " + conf.getPid());
				List<PermissionInfo> perms = getBundlePermission(thisBundle);
                try {
                    setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
                    location = conf.getBundleLocation();
                } catch (SecurityException se2) {
                    throw se;
                } finally {
                    System.out.println("Resetting permissions for " + thisLocation + " to: " + perms);
                    resetBundlePermission(thisBundle, perms);
                }
            }
        } else {
			this.setAppropriatePermission();
			location = conf.getBundleLocation();
			this.setInappropriatePermission();
		}
		return location;

	}

	@Test
	public void testFactoryConfigurationCollision()
			throws IOException, InvalidSyntaxException, BundleException {
        final String factoryPid = Util.createPid("factoryPid1");

        final Configuration cf = cm.createFactoryConfiguration( factoryPid, null );
        assertThat(cf).isNotNull();
        final String pid = cf.getPid();

		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
        Bundle bundle = context.installBundle(getWebServer() + "bundleT1.jar");
        try
        {
            SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class, sync1_1,
					propsForSyncF1_1));

            this.startTargetBundle(bundle);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			assertNoCallback(sync1_1, count1_1);

            assertThat(pid).as("Configuration must have PID").isNotNull();
            assertThat(cf.getFactoryPid()).as("Factory configuration must have requested factory PID").isEqualTo(factoryPid);

            // assert getConfiguration returns the same configurtion
            final Configuration c1 = cm.getConfiguration( pid, null );
            assertThat(c1.getPid()).as("getConfiguration must retrieve required PID").isEqualTo(pid);
            assertThat(c1.getFactoryPid()).as("getConfiguration must retrieve new factory configuration").isEqualTo(factoryPid);
            assertThat(c1.getProperties()).as("Configuration must not have properties").isNull();

            assertNoCallback(sync1_1, count1_1);

            // restart config admin and verify getConfiguration persisted
            // the new factory configuration as such
            restartCM();
            assertThat(cm).as("Config Admin Service missing").isNotNull();

			assertNoCallback(sync1_1, count1_1);

            final Configuration c2 = cm.getConfiguration( pid, null );
            assertThat(c2.getPid()).as("getConfiguration must retrieve required PID").isEqualTo(pid);
            assertThat(c2.getFactoryPid()).as("getConfiguration must retrieve new factory configuration from persistence").isEqualTo(factoryPid);
            assertThat(c2.getProperties()).as("Configuration must not have properties").isNull();

			Dictionary<String,Object> props = new Hashtable<>();
            props.put("StringKey", "stringvalue");
            c2.update( props );

            count1_1 = assertCallback(sync1_1, count1_1);
			props = sync1_1.getProps();

            assertThat(props.get( "StringKey" )).isEqualTo("stringvalue");

            final Configuration[] cfs = cm.listConfigurations( "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + factoryPid + ")" );
            assertThat(cfs).as("Expect at least one configuration").isNotNull();
			assertThat(cfs.length).as("Expect exactly one configuration").isOne();
            assertThat(cfs[0].getPid()).isEqualTo(cf.getPid());
            assertThat(cfs[0].getFactoryPid()).isEqualTo(cf.getFactoryPid());
        }
        finally
        {
            // make sure no configuration survives ...
            this.cleanUpForCallbackTest(bundle, null, null, list);
            cm.getConfiguration( pid, null ).delete();
        }
    }

	/**
	 * Test Managed Service Factory.
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String)
	 * @spec Configuration.update(Dictionary)
	 * @spec Configuration.getPid()
	 * @spec Configuration.getFactoryPid()
	 * @spec Configuration.getProperties()
	 * @spec ManagedServiceFactory.updated(String,Dictionary)
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceFactory() throws Exception {
		final int NUMBER_OF_CONFIGS = 3;
		String factorypid = Util.createPid("somefactorypid");
		Hashtable<String,Object> configs = new Hashtable<>();
		// Create some factory configurations
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = cm.createFactoryConfiguration(factorypid);
			Hashtable<String,Object> ht = new Hashtable<>();
			ht.put("test.field", i + "");
			conf.update(ht);
			System.out.println("pid: " + conf.getPid());
			configs.put(conf.getPid(), conf);
		}
		// Create some more factory configurations using names
		for (int i = 0; i < NUMBER_OF_CONFIGS; i++) {
			Configuration conf = cm.getFactoryConfiguration(factorypid,
					String.valueOf(i));
			Hashtable<String,Object> ht = new Hashtable<>();
			ht.put("test.field", String.valueOf(NUMBER_OF_CONFIGS + i) + "");
			conf.update(ht);
			System.out.println("pid: " + conf.getPid());
			configs.put(conf.getPid(), conf);
		}
		try {
			Semaphore semaphore = new Semaphore(0);
			// Register a factory
			ManagedServiceFactoryImpl msf = new ManagedServiceFactoryImpl(
					"msf", "testprop", semaphore);
			Hashtable<String,Object> properties = new Hashtable<>();
			properties.put(Constants.SERVICE_PID, factorypid);
			properties.put(ConfigurationAdmin.SERVICE_FACTORYPID, factorypid);
			registerService(ManagedServiceFactory.class.getName(), msf,
					properties);
			for (int i = 0; i < 2 * NUMBER_OF_CONFIGS; i++) {
				System.out.println("Wait for signal #" + i);
				semaphore.acquire();
				System.out.println("Signal #" + i + " arrived");
			}
			System.out.println("All signals have arrived");
		} finally {
			Enumeration<String> keys = configs.keys();
			while (keys.hasMoreElements()) {
				Configuration conf = (Configuration) configs.get(keys
						.nextElement());
				conf.delete();
			}
		}

	}

	/**
	 * Tests a configuration listener update event notification from a
	 * configuration service. The event data should match the data that
	 * originated the event (pid, factorypid...).
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.update(Dictionary)
	 * @spec ConfigurationListener.configurationEvent(ConfigurationEvent)
	 * @spec ConfigurationEvent.getPid()
	 * @spec ConfigurationEvent.getFactoryPid()
	 * @spec ConfigurationEvent.getReference()
	 * @spec ConfigurationEvent.getType()
	 * @spec Configuration.update(Dictionary)
	 * @throws Exception
	 */
	@Test
	public void testUpdateConfigEvent() throws Exception {
		ConfigurationListenerImpl cl = null;
		String pid = Util
				.createPid(ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
		SynchronizerImpl synchronizer = new SynchronizerImpl();
		/* Set up the configuration */
		Configuration conf = cm.getConfiguration(pid);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("key", "value1");
		System.out.println("Create and register a new ConfigurationListener");
		cl = createConfigurationListener(synchronizer);
		try {
			conf.update(props);
			System.out.println("Wait until the ConfigurationListener has gotten the update");
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			assertThat(cl.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);
			assertThat(cl.getFactoryPid()).as("Config Factory event pid null").isNull();
			assertThat(cl.getReference()).as("Config event reference null").isNotNull();
			ConfigurationAdmin admin = context
					.getService(cl.getReference());
			try {
				assertThat(admin).as("Configuration Admin from event").isNotNull();
				Configuration config = admin.getConfiguration(cl.getPid());
				assertThat(config).as("Configuration from event").isNotNull();
				assertEqualProperties("Properties match", conf.getProperties(),
						config.getProperties());
			} finally {
				context.ungetService(cl.getReference());
			}
		} finally {
			removeConfigurationListener(cl);
		}
	}

	/**
	 * Tests a configuration listener update event notification from a
	 * configuration service factory. The event data should match the data that
	 * originated the event (pid, factorypid...).
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String)
	 * @spec Configuration.getPid()
	 * @spec Configuration.update(Dictionary)
	 * @spec ConfigurationListener.configurationEvent(ConfigurationEvent)
	 * @spec ConfigurationEvent.getPid()
	 * @spec ConfigurationEvent.getFactoryPid()
	 * @spec ConfigurationEvent.getReference()
	 * @spec ConfigurationEvent.getType()
	 * @throws Exception
	 *             if an error occurs or an assertion fails in the test.
	 */
	@Test
	public void testUpdateConfigFactoryEvent() throws Exception {
		ConfigurationListenerImpl cl = null;
		String factorypid = Util
				.createPid(ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
		SynchronizerImpl synchronizer = new SynchronizerImpl();
		/* Set up the configuration */
		Configuration conf = cm.createFactoryConfiguration(factorypid);
		String pid = conf.getPid();
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("key", "value1");
		System.out.println("Create and register a new ConfigurationListener");
		cl = createConfigurationListener(synchronizer);
		conf.update(props);
		System.out.println("Wait until the ConfigurationListener has gotten"
				+ "the config factory update");
		try {
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			assertThat(cl.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);
			assertThat(cl.getFactoryPid()).as("Config Factory event pid match").isEqualTo(factorypid);
			assertThat(cl.getReference()).as("Config Factory event reference null").isNotNull();
			ConfigurationAdmin admin = context
					.getService(cl.getReference());
			try {
				assertThat(admin).as("Configuration Admin from event").isNotNull();
				Configuration config = admin.getConfiguration(cl.getPid());
				assertThat(config).as("Configuration from event").isNotNull();
				assertEqualProperties("Properties match", conf.getProperties(),
						config.getProperties());
			} finally {
				context.ungetService(cl.getReference());
			}
		} finally {
			removeConfigurationListener(cl);
		}
	}

	/**
	 * Tests a configuration listener delete event notification from a
	 * configuration service. The deleted <code>Configuration</code> should be
	 * empty (<code>ConfigurationAdmin.listConfigurations(null)</code> must not
	 * contain the deleted <code>Configuration</code>).
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.update(Dictionary)
	 * @spec ConfigurationListener.configurationEvent(ConfigurationEvent)
	 * @spec ConfigurationEvent.getPid()
	 * @spec ConfigurationEvent.getFactoryPid()
	 * @spec ConfigurationEvent.getReference()
	 * @spec ConfigurationEvent.getType()
	 * @spec ConfigurationAdmin.listConfigurations(String)
	 * @throws Exception
	 *             if an error occurs or an assertion fails in the test.
	 */
	@Test
	public void testDeleteConfigEvent() throws Exception {
		ConfigurationListenerImpl cl = null;
		String pid = Util
				.createPid(ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
		SynchronizerImpl synchronizer = new SynchronizerImpl();
		/* Set up the configuration */
		Configuration conf = cm.getConfiguration(pid);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put("key", "value1");
		System.out.println("Create and register a new ConfigurationListener");
		try {
			cl = createConfigurationListener(synchronizer, 2);
			conf.update(props);
			System.out.println("Wait until the ConfigurationListener has gotten the update");
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();

			conf.delete();

			System.out.println("Wait until the ConfigurationListener has gotten the delete");

			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME, 2)).as("Delete done").isTrue();
			assertThat(cl.getPid(2)).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType(2)).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);
			assertThat(cl.getFactoryPid(2)).as("Config Factory event pid null").isNull();
			assertThat(cl.getReference(2)).as("Config Factory event reference null").isNotNull();
			try {
				ConfigurationAdmin admin = context
						.getService(cl.getReference(2));
				assertThat(admin).as("Configuration Admin from event").isNotNull();
				Configuration[] configs = admin
						.listConfigurations("(service.pid=" + pid + ")");
				assertThat(configs).as("The configuration exists in CM!").isNull();
			} finally {
				context.ungetService(cl.getReference(2));
			}
		} finally {
			removeConfigurationListener(cl);
		}
	}

	/**
	 * Tests a configuration listener delete event notification from a
	 * configuration service factory. The deleted <code>Configuration</code>
	 * should be empty (
	 * <code>ConfigurationAdmin.listConfigurations(null)</code> must not contain
	 * the deleted <code>Configuration</code>).
	 *
	 * @spec ConfigurationAdmin.createFactoryConfiguration(String)
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.update(Dictionary)
	 * @spec ConfigurationListener.configurationEvent(ConfigurationEvent)
	 * @spec ConfigurationEvent.getPid()
	 * @spec ConfigurationEvent.getFactoryPid()
	 * @spec ConfigurationEvent.getReference()
	 * @spec ConfigurationEvent.getType()
	 * @spec ConfigurationAdmin.listConfigurations(String)
	 * @throws Exception
	 *             if an error occurs or an assertion fails in the test.
	 */
	@Test
	public void testDeleteConfigFactoryEvent() throws Exception {
		ConfigurationListenerImpl cl = null;
		String factorypid = Util
				.createPid(ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
		SynchronizerImpl synchronizer = new SynchronizerImpl();
		/* Set up the configuration */
		Configuration conf = cm.createFactoryConfiguration(factorypid);
		String pid = conf.getPid();
		System.out.println("Create and register a new ConfigurationListener");
		cl = createConfigurationListener(synchronizer);
		conf.delete();
		System.out.println("Wait until the ConfigurationListener has gotten"
				+ "the config factory delete");
		try {
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			assertThat(cl.getPid()).as("Config event pid match").isEqualTo(pid);
			assertThat(cl.getType()).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);
			assertThat(cl.getFactoryPid()).as("Config Factory event pid match").isEqualTo(factorypid);
			assertThat(cl.getReference()).as("Config Factory event reference null").isNotNull();
			ConfigurationAdmin admin = context
					.getService(cl.getReference());
			try {
				assertThat(admin).as("Configuration Admin from event").isNotNull();
				Configuration[] configs = admin
						.listConfigurations("(service.factoryPid=" + factorypid
								+ ")");
				assertThat(configs).as("The configuration exists in CM!").isNull();
			} finally {
				context.ungetService(cl.getReference());
			}
		} finally {
			removeConfigurationListener(cl);
		}
	}

	/**
	 * Tests a configuration listener permission. The bundle does not have
	 * <code>ServicePermission[ConfigurationListener,REGISTER]</code> and will
	 * try to register a <code>ConfigurationListener</code>. An exception must
	 * be thrown.
	 *
	 * @spec BundleContext.installBundle(String)
	 * @spec Bundle.start()
	 *
	 * @throws Exception
	 *             if an error occurs or an assertion fails in the test.
	 */
	@Test
	public void testConfigListenerPermission() throws Exception {
		Bundle tb = context.installBundle(getWebServer() + "tb1.jar");
		String message = "registering config listener without permission";
		try {
			tb.start();
			/* A BundleException should have been thrown if security is enabled */
			if (System.getSecurityManager() != null)
				failException(message, BundleException.class);
		} catch (BundleException e) {
			/* Check that we got the correct exception */
			assertException(message, BundleException.class, e);
			/*
			 * A BundleException should not have been thrown if security is not
			 * enabled
			 */
			if (System.getSecurityManager() == null)
				fail("Security is not enabled", e);
		} finally {
			tb.uninstall();
		}
	}

	/**
	 * Tests an event from a different bundle. The
	 * <code>ConfigurationListener</code> should get the event even if it was
	 * generated from a different bundle.
	 *
	 * @spec ConfigurationAdmin.getConfiguration(String)
	 * @spec Configuration.getPid()
	 * @spec Configuration.delete()
	 * @spec Configuration.update(Dictionary)
	 * @spec ConfigurationListener.configurationEvent(ConfigurationEvent)
	 * @spec ConfigurationEvent.getPid()
	 * @spec ConfigurationEvent.getFactoryPid()
	 * @spec ConfigurationEvent.getReference()
	 * @spec ConfigurationEvent.getType()
	 * @throws Exception
	 *             if an error occurs or an assertion fails in the test.
	 */
	@Test
	public void testConfigEventFromDifferentBundle() throws Exception {
		System.out.println("Create and register a new ConfigurationListener");
		SynchronizerImpl synchronizer = new SynchronizerImpl();
		ConfigurationListenerImpl cl = createConfigurationListener(
				synchronizer, 4);
		Bundle tb = context.installBundle(getWebServer() + "tb2.jar");
		tb.start();
		System.out.println("Wait until the ConfigurationListener has gotten the update");

		try {
			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME)).as("Update done").isTrue();
			assertThat(cl.getPid(1)).as("Config event pid match").isEqualTo(PACKAGE + ".tb2pid."
					+ ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
			assertThat(cl.getType(1)).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);
			assertThat(cl.getFactoryPid(1)).as("Config Factory event pid null").isNull();

			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME, 2)).as("Update done").isTrue();
			assertThat(cl.getPid(2)).as("Config event pid match").isEqualTo(PACKAGE + ".tb2pid."
					+ ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
			assertThat(cl.getType(2)).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);
			assertThat(cl.getFactoryPid(2)).as("Config Factory event pid null").isNull();

			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME, 3)).as("Update done").isTrue();
			assertThat(cl.getFactoryPid(3)).as("Config event facotory pid match").isEqualTo(PACKAGE
					+ ".tb2factorypid."
					+ ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
			assertThat(cl.getType(3)).as("Config event type match").isEqualTo(ConfigurationEvent.CM_UPDATED);

			assertThat(synchronizer.waitForSignal(SIGNAL_WAITING_TIME, 4)).as("Update done").isTrue();
			assertThat(cl.getFactoryPid(4)).as("Config event factory pid match").isEqualTo(PACKAGE
					+ ".tb2factorypid."
					+ ConfigurationListenerImpl.LISTENER_PID_SUFFIX);
			assertThat(cl.getType(4)).as("Config event type match").isEqualTo(ConfigurationEvent.CM_DELETED);

		} finally {
			removeConfigurationListener(cl);
			tb.uninstall();
		}
	}



	/** *** Helper methods **** */
	/**
	 * creates and registers a configuration listener that expects just one
	 * event.
	 */
	private ConfigurationListenerImpl createConfigurationListener(
			SynchronizerImpl synchronizer) throws Exception {
		return createConfigurationListener(synchronizer, 1);
	}

	/**
	 * creates and registers a configuration listener that expects eventCount
	 * events.
	 */
	private ConfigurationListenerImpl createConfigurationListener(
			SynchronizerImpl synchronizer, int eventCount) throws Exception {
		ConfigurationListenerImpl listener = new ConfigurationListenerImpl(
				synchronizer, eventCount);
		registerService(ConfigurationListener.class.getName(), listener, null);
		return listener;
	}

	/**
	 * unregisters a configuration listener.
	 */
	private void removeConfigurationListener(ConfigurationListener cl)
			throws Exception {
		unregisterService(cl);
	}

	private ManagedServiceImpl createManagedService(String pid, Semaphore s)
			throws Exception {
		ManagedServiceImpl ms = new ManagedServiceImpl(s);
		Hashtable<String,Object> props = new Hashtable<>();
		props.put(Constants.SERVICE_PID, pid);
		/* TODO: Testa registered service.pid with other String */
		registerService(ManagedService.class.getName(), ms, props);
		System.out.println("ManagedService is registered with pid:" + pid);
		return ms;
	}

	private void checkConfiguration(Configuration conf, String message,
			String pid, String location) throws BundleException {
		assertThat(conf).as(message).isNotNull();
		assertThat(conf.getPid()).as("Pid").isEqualTo(pid);
		assertThat(conf.getFactoryPid()).as("FactoryPid").isNull();
		assertThat(conf.getProperties()).as("Properties").isNull();
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(location);

	}

	/**
	 * See if a configuration is part of a list.
	 *
	 * @return index of the list if the configuration is a part of the
	 *         list.Otherwise, return -1nd.
	 */
	private int isPartOf(Configuration theConf, List<Configuration> configs) {

		for (int i = 0; i < configs.size(); i++)
			if (equals(configs.get(i), theConf))
				return i;

		return -1;
	}

	/**
	 * Compares two Configurations for equality. Configuration.equals() is not
	 * specified in the spec, so this is a helper method that compares pids.
	 * <p>
	 * Two Configurations are considered equal if the got the same pid or if
	 * both are null.
	 */
	private boolean equals(Configuration c1, Configuration c2) {
		boolean result = false;
		/* If both are null, they are equal */
		if ((c1 == null) && (c2 == null)) {
			result = true;
		}
		/* If none of them is null, and got the same pid, they are equal */
		if ((c1 != null) && (c2 != null)) {
			result = c1.getPid().equals(c2.getPid());
		}
		return result;
	}

	public final static String PACKAGE = "org.osgi.test.cases.cm.tbc";

	private String getLocation() {
		return context.getBundle().getLocation();
	}

	/**
	 * Removes any configurations made by this bundle.
	 */
	private void cleanCM(Set<String> existingConfigs) throws Exception {
        if (cm != null) {
            Configuration[] configs = cm.listConfigurations(null);
            if (configs != null) {
                // System.out.println("  cleanCM() -- Checking " + configs.length + " configs");
                for (int i = 0; i < configs.length; i++) {
                    Configuration config = configs[i];
                    if (!existingConfigs.contains(config.getPid())) {
                        // System.out.println("               Delete " + config.getPid());
                        config.delete();
                    } else {
                        // System.out.println("               Keep   " + config.getPid());
                    }
                }
            } else {
                // System.out.println("  cleanCM() -- No Configurations to clear");
            }
        } else {
            // System.out.println("  cleanCM() -- No CM !");
        }
	}

    class SyncEventListener implements SynchronousConfigurationListener {

        private final Synchronizer sync;

        public SyncEventListener(final Synchronizer sync) {
            this.sync = sync;
        }

        public void configurationEvent(ConfigurationEvent event) {
            this.sync.signal();
        }

    }

	/*
	 * Shigekuni KONDO, Ikuo YAMASAKI, (Yushi Kuroda), NTT Corporation adds
	 * tests for specification version 1.4
	 */
	private final String locationA = "location.a";
	private final String locationB = "location.b";
	private final String regionA = "?RegionA";
	private String regionB = "?RegionB";

	@Test
	public void testGetConfigurationWithLocation_2_01() throws Exception {
		final String locationOld = null;
		this.internalGetConfigurationWithLocation_2_02To08(1, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_02() throws Exception {
		final String locationOld = locationA;
		this.internalGetConfigurationWithLocation_2_02To08(2, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_03() throws Exception {
		final String locationOld = locationA + "*";
		this.internalGetConfigurationWithLocation_2_02To08(3, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_04() throws Exception {
		final String locationOld = locationB;
		this.internalGetConfigurationWithLocation_2_02To08(4, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_06() throws Exception {
		final String locationOld = "?*";
		this.internalGetConfigurationWithLocation_2_02To08(6, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_07() throws Exception {
		final String locationOld = regionA;
		this.internalGetConfigurationWithLocation_2_02To08(7, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_08() throws Exception {
		final String locationOld = regionB;
		this.internalGetConfigurationWithLocation_2_02To08(8, locationOld);
	}

	public void internalGetConfigurationWithLocation_2_02To08(final int minor,
			final String locationOld) throws BundleException, IOException {

		final String header = "testGetConfigurationWithLocation_2_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		this.setAppropriatePermission();
		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		Dictionary<String,Object> props = new Hashtable<>();

		// Get a brand new configuration
		conf = cm.getConfiguration(pid1, locationOld);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
		assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

		// micro 2
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
		conf = cm.getConfiguration(pid1, locationOld);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
		assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

		// micro 1
		testId = traceTestId(header, ++micro);
		props.put("StringKey", "stringvalue");
		conf.update(props);
		conf = cm.getConfiguration(pid1, locationOld);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
		assertThat(conf.getProperties()
				.size()).as("Check Configuration props").isEqualTo(2);
		assertThat(conf
				.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
		conf.delete();

		resetPermissions();
		conf = cm.getConfiguration(pid1, locationOld);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
		assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

		// micro 4
		testId = traceTestId(header, ++micro);
		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 2) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}

		// micro 3
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 2) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			assertThat(conf
					.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}
		conf.delete();

		resetPermissions();
		conf = cm.getConfiguration(pid1, locationOld);

		// micro 8
		testId = traceTestId(header, ++micro);
		setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 6 || minor == 7 || minor == 8) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}

		// micro 7
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 6 || minor == 7 || minor == 8) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			assertThat(conf
					.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}
		conf.delete();

		resetPermissions();
		conf = cm.getConfiguration(pid1, locationOld);

		// micro 10
		testId = traceTestId(header, ++micro);
		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 7) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}

		// micro 9
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 7) {
			conf = cm.getConfiguration(pid1, locationOld);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			assertThat(conf
					.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1,
					locationOld);
		}
		conf.delete();

		resetPermissions();
		conf = cm.getConfiguration(pid1, locationOld);

		// micro 12
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
		assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, locationOld);

		// micro 11
		testId = traceTestId(header, ++micro);
		conf.update(props);
		assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, locationOld);
		conf.delete();

		resetPermissions();
		conf = cm.getConfiguration(pid1, locationOld);

		// micro 13
		testId = traceTestId(header, ++micro);
		List<String> cList = new ArrayList<>();
		cList.add("?");
		cList.add(regionA);
		setCPListtoBundle(cList, null, thisBundle);
		conf.update(props);
		if (minor == 7) {
			conf = cm.getConfiguration(pid1, "?");
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			assertThat(conf
					.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, "?");
		}
		conf.delete();

		if (minor == 2) {
			resetPermissions();
			conf = cm.getConfiguration(pid1, thisLocation);

			// micro 16
			testId = traceTestId(header, ++micro);
			setCPtoBundle(null, null, thisBundle);
			conf = cm.getConfiguration(pid1, thisLocation);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

			// micro 15
			testId = traceTestId(header, ++micro);
			conf.update(props);
			conf = cm.getConfiguration(pid1, thisLocation);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			assertThat(conf
					.getProperties().get("StringKey")).as("Check Configuration props").isEqualTo("stringvalue");
			conf.delete();
		}
	}

	// TODO confirm
	@Test
	public void testGetConfigurationWithLocation_2_09() throws Exception {
		final String locationOld = null;
		this.internalGetConfigurationWithLocation_2_09To13(9, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_10() throws Exception {
		final String locationOld = locationA;
		this.internalGetConfigurationWithLocation_2_09To13(10, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_12() throws Exception {
		final String locationOld = regionA;
		this.internalGetConfigurationWithLocation_2_09To13(12, locationOld);
	}

	@Test
	public void testGetConfigurationWithLocation_2_13() throws Exception {
		final String locationOld = regionA + "*";
		this.internalGetConfigurationWithLocation_2_09To13(13, locationOld);
	}

	public void internalGetConfigurationWithLocation_2_09To13(final int minor,
			final String location) throws BundleException, IOException {
		final String header = "testGetConfigurationWithLocation_2_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		this.setAppropriatePermission();
		final String pid1 = Util.createPid("1");
		Configuration conf = null;

		// 1
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
		conf = cm.getConfiguration(pid1, location);
		assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(location);
		assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		conf.delete();

		// 2
		testId = traceTestId(header, ++micro);
		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 10) {
			conf = cm.getConfiguration(pid1, location);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(location);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);
		}

		// 3
		testId = traceTestId(header, ++micro);
		setCPtoBundle(locationB, ConfigurationPermission.CONFIGURE, thisBundle);
		assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);

		// 4
		testId = traceTestId(header, ++micro);
		setCPtoBundle("?", ConfigurationPermission.CONFIGURE, thisBundle);
		assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);

		// 5
		testId = traceTestId(header, ++micro);
		setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 12 || minor == 13) {
			conf = cm.getConfiguration(pid1, location);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(location);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);
		}

		// 6
		testId = traceTestId(header, ++micro);
		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 12) {
			conf = cm.getConfiguration(pid1, location);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(location);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();
		} else {
			assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);
		}

		// 7
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
		assertThrowsSEbyGetConfigurationWithLocation(testId, pid1, location);

		if (minor == 10) {
			// thisLocation no permission
			testId = traceTestId(header, ++micro);
			setCPtoBundle(null, null, thisBundle);
			conf = cm.getConfiguration(pid1, thisLocation);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();

			// thisLocation with CONFIGURE
			testId = traceTestId(header, ++micro);
			setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE,
					thisBundle);
			conf = cm.getConfiguration(pid1, thisLocation);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();

			// thisLocation with TARGET
			testId = traceTestId(header, ++micro);
			setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
			conf = cm.getConfiguration(pid1, thisLocation);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
			conf.delete();
		}
	}

	private void setCPtoBundle(String name, String action, Bundle bundle)
			throws BundleException {
		this.setCPtoBundle(name, action, bundle, true);
	}

	private void setCPtoBundle(String name, String action, Bundle bundle,
			boolean resetAll) throws BundleException {
		if (resetAll)
			this.resetPermissions();
		list.clear();

		add(list, PropertyPermission.class.getName(), "*", "READ,WRITE");
		add(list, PP, "*", "IMPORT,EXPORTONLY");
		add(list, SP, "*", "GET,REGISTER");

		if (name != null && action != null)
			add(list, CP, name, action);
		add(list, AP, "*", "*");
		permissionFlag = true;
		this.setBundlePermission(bundle, list);
	}

	private void resetBundlePermission(Bundle b, List<PermissionInfo> list)
			throws BundleException {
        this.resetPermissions();
        if (list != null) {
            this.setBundlePermission(b, list);
        }
    }

	private String traceTestId(final String header, int micro) {
		String testId = header + String.valueOf(micro);
		System.out.println(testId);
		return testId;
	}

	private void assertThrowsSEbyGetConfigurationWithLocation(String testId,
			String pid, String location) {
		String message = testId
				+ ":try to get configuration without appropriate ConfigurationPermission.";
		try {
			cm.getConfiguration(pid, location);
			// A SecurityException should have been thrown
			failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// Check that we got the correct exception
			assertException(message, SecurityException.class, e);
		}

	}

	@Test
	public void testGetConfiguration_3_01() throws Exception {
		final String locationOld = null;
		this.internalGetConfiguration_3_01To07(1, locationOld);
	}

	@Test
	public void testGetConfiguration_3_02() throws Exception {
		final String locationOld = locationA;
		this.internalGetConfiguration_3_01To07(2, locationOld);
	}

	@Test
	public void testGetConfiguration_3_03() throws Exception {
		final String locationOld = locationA + "*";
		this.internalGetConfiguration_3_01To07(3, locationOld);
	}

	@Test
	public void testGetConfiguration_3_04() throws Exception {
		final String locationOld = locationB;
		this.internalGetConfiguration_3_01To07(4, locationOld);
	}

	@Test
	public void testGetConfiguration_3_06() throws Exception {
		final String locationOld = regionA;
		this.internalGetConfiguration_3_01To07(6, locationOld);
	}

	@Test
	public void testGetConfiguration_3_07() throws Exception {
		final String locationOld = regionA + "*";
		this.internalGetConfiguration_3_01To07(7, locationOld);
	}

	public void internalGetConfiguration_3_01To07(int minor, String locationOld)
			throws BundleException, IOException {
		final String header = "testGetConfiguration_3_" + String.valueOf(minor)
				+ "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		Dictionary<String,Object> props = new Hashtable<>();

		this.setAppropriatePermission();
		conf = cm.getConfiguration(pid1, locationOld);
		String message = testId
				+ ":try to get configuration with appropriate ConfigurationPermission.";

		// 2
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
		conf = cm.getConfiguration(pid1);
		if (minor == 1) {
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		}

		// 1
		testId = traceTestId(header, ++micro);
		props.put("StringKey", "stringvalue");
		conf.update(props);
		conf = cm.getConfiguration(pid1);
		if (minor == 1) {
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else {
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		}
		conf.delete();

		this.setAppropriatePermission();
		conf = cm.getConfiguration(pid1, locationOld);

		// 4
		testId = traceTestId(header, ++micro);
		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else if (minor == 2) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}

		// 3
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else if (minor == 2) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}
		conf.delete();

		this.setAppropriatePermission();
		conf = cm.getConfiguration(pid1, locationOld);

		// 8
		testId = traceTestId(header, ++micro);
		setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else if (minor == 6 || minor == 7) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}

		// 7
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else if (minor == 6 || minor == 7) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}
		conf.delete();

		this.setAppropriatePermission();
		conf = cm.getConfiguration(pid1, locationOld);

		// 10
		testId = traceTestId(header, ++micro);
		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else if (minor == 6) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}

		// 9
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else if (minor == 6) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(locationOld);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}
		conf.delete();

		this.setAppropriatePermission();
		conf = cm.getConfiguration(pid1, locationOld);

		// 12
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}

		// 11
		testId = traceTestId(header, ++micro);
		conf.update(props);
		if (minor == 1) {
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
		} else {
			assertThrowsSEbyGetConfiguration(pid1, message);
		}
		conf.delete();

		if (minor == 1) {
			this.setAppropriatePermission();
			conf = cm.getConfiguration(pid1, locationOld);

			// 15
			testId = traceTestId(header, ++micro);
			setCPtoBundle(null, null, thisBundle);
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

			// 14
			testId = traceTestId(header, ++micro);
			conf.update(props);
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisBundle.getLocation());
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			conf.delete();
		}

		if (minor == 2) {
			this.setAppropriatePermission();
			conf = cm.getConfiguration(pid1, thisLocation);

			// 15
			testId = traceTestId(header, ++micro);
			setCPtoBundle(null, null, thisBundle);
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()).as("Configuration props MUST be null").isNull();

			// 14
			testId = traceTestId(header, ++micro);
			conf.update(props);
			conf = cm.getConfiguration(pid1);
			assertThat(this.getBundleLocationForCompare(conf)).as("Location").isEqualTo(thisLocation);
			assertThat(conf.getProperties()
					.size()).as("Check Configuration props").isEqualTo(2);
			conf.delete();
		}
	}

	private void assertThrowsSEbyGetConfiguration(final String pid,
			String message) throws AssertionError {
		try {
			cm.getConfiguration(pid);
			// A SecurityException should have been thrown
			failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// Check that we got the correct exception
			assertException(message, SecurityException.class, e);
		}
	}

	// TODO confirm
	@Test
	public void testCreateFactoryConfiguration_4_01() throws Exception {
		String location = null;
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(1,
				location);
	}

	@Test
	public void testCreateFactoryConfiguration_4_02() throws Exception {
		String location = locationA;
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(2,
				location);
	}

	@Test
	public void testCreateFactoryConfiguration_4_03() throws Exception {
		String location = locationA + "*";
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(3,
				location);
	}

	@Test
	public void testCreateFactoryConfiguration_4_04() throws Exception {
		String location = locationB;
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(4,
				location);
	}

	@Test
	public void testCreateFactoryConfiguration_4_06() throws Exception {
		String location = regionA;
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(6,
				location);
	}

	@Test
	public void testCreateFactoryConfiguration_4_07() throws Exception {
		String location = regionA + "*";
		this.internalCreateFactoryConfigurationWithLocation_4_01To07(7,
				location);
	}

	public void internalCreateFactoryConfigurationWithLocation_4_01To07(
			int minor, String location) throws BundleException, IOException {
		final String header = "testCreateFactoryConfigurationWithLocation_4_"
				+ String.valueOf(minor) + "_";
		;

		String fpid = Util.createPid("factory1");
		String testId = null;
		int micro = 0;

		String message = testId
				+ ":try to create factory configuration with location with inappropriate ConfigurationPermission.";

		// 1
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
		Configuration conf1 = cm.createFactoryConfiguration(fpid, location);
		Configuration conf2 = cm.createFactoryConfiguration(fpid, location);
		assertThat(conf2.getFactoryPid()).as("Check conf fpid.").isEqualTo(conf1.getFactoryPid());
		assertThat(conf1.getPid().equals(conf2.getPid())).as("Check conf pid does not same.").isFalse();
		assertThat(this.getBundleLocationForCompare(conf1)).as("Check conf location.").isEqualTo(location);
		assertThat(this.getBundleLocationForCompare(conf2)).as("Check conf location.").isEqualTo(location);

		// 2
		testId = traceTestId(header, ++micro);
		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 2) {
			conf1 = cm.createFactoryConfiguration(fpid, location);
			conf2 = cm.createFactoryConfiguration(fpid, location);
			assertThat(conf2.getFactoryPid()).as("Check conf fpid.").isEqualTo(conf1.getFactoryPid());
			assertThat(conf1.getPid().equals(conf2.getPid())).as("Check conf pid does not same.").isFalse();
			assertThat(this.getBundleLocationForCompare(conf1)).as("Check conf location.").isEqualTo(location);
			assertThat(this.getBundleLocationForCompare(conf2)).as("Check conf location.").isEqualTo(location);
		} else {
			this.assertThrowsSEbyCreateFactoryConf(fpid, location, message);
		}

		// 4
		testId = traceTestId(header, ++micro);
		setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 6 || minor == 7) {
			conf1 = cm.createFactoryConfiguration(fpid, location);
			conf2 = cm.createFactoryConfiguration(fpid, location);
			assertThat(conf2.getFactoryPid()).as("Check conf fpid.").isEqualTo(conf1.getFactoryPid());
			assertThat(conf1.getPid().equals(conf2.getPid())).as("Check conf pid does not same.").isFalse();
			assertThat(this.getBundleLocationForCompare(conf1)).as("Check conf location.").isEqualTo(location);
			assertThat(this.getBundleLocationForCompare(conf2)).as("Check conf location.").isEqualTo(location);
		} else {
			this.assertThrowsSEbyCreateFactoryConf(fpid, location, message);
		}

		// 5
		testId = traceTestId(header, ++micro);
		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
		if (minor == 6) {
			conf1 = cm.createFactoryConfiguration(fpid, location);
			conf2 = cm.createFactoryConfiguration(fpid, location);
			assertThat(conf2.getFactoryPid()).as("Check conf fpid.").isEqualTo(conf1.getFactoryPid());
			assertThat(conf1.getPid().equals(conf2.getPid())).as("Check conf pid does not same.").isFalse();
			assertThat(this.getBundleLocationForCompare(conf1)).as("Check conf location.").isEqualTo(location);
			assertThat(this.getBundleLocationForCompare(conf2)).as("Check conf location.").isEqualTo(location);
		} else {
			this.assertThrowsSEbyCreateFactoryConf(fpid, location, message);
		}

		// 6
		testId = traceTestId(header, ++micro);
		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
		this.assertThrowsSEbyCreateFactoryConf(fpid, location, message);

		// 7
		if (minor == 6) {
			testId = traceTestId(header, ++micro);
			setCPtoBundle(regionB, ConfigurationPermission.CONFIGURE,
					thisBundle);
			this.assertThrowsSEbyCreateFactoryConf(fpid, location, message);
		}

		// 8
		if (minor == 2) {
			traceTestId(header, ++micro);
			this.resetPermissions();
			setCPtoBundle(null, null, thisBundle);
			conf1 = cm.createFactoryConfiguration(fpid, thisLocation);
			conf2 = cm.createFactoryConfiguration(fpid, thisLocation);
			assertThat(conf2.getFactoryPid()).as("Check conf fpid.").isEqualTo(conf1.getFactoryPid());
			assertThat(conf1.getPid().equals(conf2.getPid())).as("Check conf pid does not same.").isFalse();
			assertThat(this.getBundleLocationForCompare(conf1)).as("Check conf location.").isEqualTo(thisLocation);
			assertThat(this.getBundleLocationForCompare(conf2)).as("Check conf location.").isEqualTo(thisLocation);
		}
	}

	private void assertThrowsSEbyCreateFactoryConf(final String fPid,
			final String location, String message) throws AssertionError {
		try {
			cm.createFactoryConfiguration(fPid, location);
			// A SecurityException should have been thrown
			failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// Check that we got the correct exception
			assertException(message, SecurityException.class, e);
		}
	}

	@Test
	public void testListConfigurations_6_01() throws Exception {
		String filter = null;
		this.internalListConfigurations(1, filter);
	}

	@Test
	public void testListConfigurations_6_02() throws Exception {
		String filter = "(service.pid=pid1)";
		this.internalListConfigurations(2, filter);
	}

	@Test
	public void testListConfigurations_6_03() throws Exception {
		String filter = "(service.bundleLocation=location.a)";
		this.internalListConfigurations(3, filter);
	}

	@Test
	public void testListConfigurations_6_04() throws Exception {
		String filter = "(service.bundleLocation=?RegionA)";
		this.internalListConfigurations(4, filter);
	}

	@Test
	public void testListConfigurations_6_05() throws Exception {
		String filter = "(&(service.bundleLocation=?RegionA)(service.pid=pid2))";
		this.internalListConfigurations(5, filter);
	}

	public void internalListConfigurations(int minor, String filter)
			throws IOException, InvalidSyntaxException, BundleException {
		final String header = "testListConfigurations_6_"
				+ String.valueOf(minor) + "_";
		int micro = 0;

		String testId = null;
		String pid1 = "pid1";
		String pid2 = "pid2";
		String pid3 = "pid3";
		Configuration conf1 = null;
		Configuration conf2 = null;
		Configuration conf3 = null;

		Dictionary<String,Object> prop = new Hashtable<>();
		prop.put("StringKey", "Stringvalue");

		String message = testId + ":try listConfigurations ";
		this.setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
		conf1 = cm.getConfiguration(pid1, locationA);
		conf1.update(prop);
		conf2 = cm.getConfiguration(pid2, regionA);
		conf2.update(prop);
		conf3 = cm.getConfiguration(pid3, "?");
		conf3.update(prop);

		testId = traceTestId(header, ++micro);
		this.setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE,
				thisBundle);
		Configuration[] conflist = cm.listConfigurations(filter);
		if (minor == 4 || minor == 5) {
			assertThat(conflist).as("Returned list of configuration MUST be null.").isNull();
		} else {
			assertThat(conflist.length).as("number of Configuration Object").isOne();
			assertThat(conf1).as(message).isEqualTo(conflist[0]);
		}

		testId = traceTestId(header, ++micro);
		this.setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE,
				thisBundle);
		conflist = cm.listConfigurations(filter);
		if (minor == 1 || minor == 4 || minor == 5) {
			assertThat(conflist.length).as("number of Configuration Object").isOne();
			assertThat(conf2).as(message).isEqualTo(conflist[0]);
		} else {
			assertThat(conflist).as("Returned list of configuration MUST be null.").isNull();
		}
	}

	// TODO
	@Test
	public void testGetBundleLocation_7_01() throws Exception {
		String locationOld = null;
		this.internalGetBundleLocation_7_01to08(1, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_02() throws Exception {
		String locationOld = locationA;
		this.internalGetBundleLocation_7_01to08(2, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_03() throws Exception {
		String locationOld = locationA + "*";
		this.internalGetBundleLocation_7_01to08(3, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_05() throws Exception {
		String locationOld = "?*";
		this.internalGetBundleLocation_7_01to08(5, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_06() throws Exception {
		String locationOld = regionA;
		this.internalGetBundleLocation_7_01to08(6, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_07() throws Exception {
		String locationOld = regionA + "*";
		this.internalGetBundleLocation_7_01to08(7, locationOld);
	}

	@Test
	public void testGetBundleLocation_7_08() throws Exception {
		String locationOld = thisLocation;
		this.internalGetBundleLocation_7_01to08(8, locationOld);
	}

	public void internalGetBundleLocation_7_01to08(final int minor,
			final String locationOld) throws Exception {

		final String header = "testGetBundleLocation_7_"
				+ String.valueOf(minor) + "_";

		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;

		try {
			this.setAppropriatePermission();
			conf = cm.getConfiguration(pid1, locationOld);

			// 1
			testId = traceTestId(header, ++micro);
			setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
			String loc = conf.getBundleLocation();
			assertThat(loc).as("Check conf location").isEqualTo(locationOld);

			// 2
			testId = traceTestId(header, ++micro);
			setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE,
					thisBundle);
            if (minor == 2) {
				loc = conf.getBundleLocation();
				assertThat(loc).as("Check conf location").isEqualTo(locationOld);
			} else {
				this.assertThrowsSEbyGetLocation(conf, testId);
			}

			// 3
			testId = traceTestId(header, ++micro);
			setCPtoBundle("?", ConfigurationPermission.CONFIGURE, thisBundle);
            this.assertThrowsSEbyGetLocation(conf, testId);

			// 4
			testId = traceTestId(header, ++micro);
			setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
            if (minor == 5 || minor == 6 || minor == 7) {
				loc = conf.getBundleLocation();
				assertThat(loc).as("Check conf location").isEqualTo(locationOld);
			} else {
				this.assertThrowsSEbyGetLocation(conf, testId);
			}

			// 5
			testId = traceTestId(header, ++micro);
			setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE,
					thisBundle);
            if (minor == 6) {
				loc = conf.getBundleLocation();
				assertThat(loc).as("Check conf location").isEqualTo(locationOld);
			} else {
				this.assertThrowsSEbyGetLocation(conf, testId);
			}

			// 6
			testId = traceTestId(header, ++micro);
			setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
            this.assertThrowsSEbyGetLocation(conf, testId);

			// 7
			testId = traceTestId(header, ++micro);
			setCPtoBundle("*", ConfigurationPermission.CONFIGURE + ","
					+ ConfigurationPermission.TARGET, thisBundle);
			loc = conf.getBundleLocation();
			assertThat(loc).as("Check conf location").isEqualTo(locationOld);

			// 8
			if (minor == 6 || minor == 7) {
				testId = traceTestId(header, ++micro);
				setCPtoBundle(regionB, ConfigurationPermission.CONFIGURE,
						thisBundle);
				this.assertThrowsSEbyGetLocation(conf, testId);
			}
			++micro;

			// 9
			if (minor == 6) {
				testId = traceTestId(header, ++micro);
				setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE + ","
						+ ConfigurationPermission.TARGET, thisBundle);
				loc = conf.getBundleLocation();
				assertThat(loc).as("Check conf location").isEqualTo(locationOld);
			}
			++micro;

			// 10
			testId = traceTestId(header, ++micro);
			conf.delete();
			resetPermissions();
			conf = cm.getConfiguration(pid1, thisLocation);
            // Bug2539: need to have CONFIGURE(thisLocation)
            setCPtoBundle(thisLocation, ConfigurationPermission.CONFIGURE, thisBundle);
            loc = conf.getBundleLocation();
			assertThat(loc).as("Check conf location").isEqualTo(thisLocation);
		} finally {
			this.resetPermissions();
			if (conf != null)
				conf.delete();
		}
	}

	private void assertThrowsSEbyGetLocation(final Configuration conf,
			String testId) throws AssertionError {
		String message = testId
				+ ":try to get bundle location without appropriate ConfigurationPermission.";
		try {
			conf.getBundleLocation();
			// A SecurityException should have been thrown
			failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// Check that we got the correct exception
			assertException(message, SecurityException.class, e);
		}
	}

	@Test
	public void testSetBundleLocation_8_01() throws Exception {
		String locationOld = null;
		String location = null;
		this.internalSetBundleLocation_8_01to07(1, locationOld, location);
	}

	@Test
	public void testSetBundleLocation_8_02() throws Exception {
		String locationOld = null;
		String location = locationA;
		this.internalSetBundleLocation_8_01to07(2, locationOld, location);
	}

	@Test
	public void testSetBundleLocation_8_04() throws Exception {
		String locationOld = null;
		String location = "?*";
		this.internalSetBundleLocation_8_01to07(4, locationOld, location);
	}

	@Test
	public void testSetBundleLocation_8_05() throws Exception {
		String locationOld = null;
		String location = regionA;
		this.internalSetBundleLocation_8_01to07(5, locationOld, location);
	}

	@Test
	public void testSetBundleLocation_8_06() throws Exception {
		String locationOld = locationA;
		String location = null;
		this.internalSetBundleLocation_8_01to07(6, locationOld, location);
	}

	@Test
	public void testSetBundleLocation_8_07() throws Exception {
		String locationOld = locationA;
		String location = locationA;
		this.internalSetBundleLocation_8_01to07(7, locationOld, location);
	}

	public void internalSetBundleLocation_8_01to07(final int minor,
			final String locationOld, final String location) throws Exception {

		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";

		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 2
    		testId = traceTestId(header, ++micro);
    		if (minor == 6 || minor == 7) {
    			setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE,
    					thisBundle);
    			if (minor == 7) {
    				conf.setBundleLocation(location);
    				assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);
    			} else
    				assertThrowsSEbySetLocation(conf, location, testId);
    		} else {
    			setCPtoBundle("?*", ConfigurationPermission.CONFIGURE, thisBundle);
    			assertThrowsSEbySetLocation(conf, location, testId);
    		}

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
		} finally {
		    if (conf != null) {
		        conf.delete();
		    }
		}
	}

	@Test
	public void testSetBundleLocation_8_08() throws Exception {

		int minor = 8;
		String locationOld = locationA;
		String location = locationB;
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
			List<String> cList = new ArrayList<>();
    		cList.add(locationA);
    		cList.add(locationB);
    		setCPListtoBundle(cList, null, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(locationOld);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(locationOld);

    		// 4
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(locationB, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(locationOld);

    		// 5
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(locationA, ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_10() throws Exception {

		int minor = 10;
		String locationOld = locationA;
		String location = regionA;
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
			List<String> cList = new ArrayList<>();
    		cList.add(locationA);
    		cList.add(regionA);
    		setCPListtoBundle(cList, null, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 4
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_15() throws Exception {

		int minor = 15;
		String locationOld = regionA;
		String location = null;
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_16() throws Exception {

		int minor = 16;
		String locationOld = regionA;
		String location = locationA;
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
			List<String> cList = new ArrayList<>();
    		cList.add(locationA);
    		cList.add(regionA);
    		setCPListtoBundle(cList, null, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(locationA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 4
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_17() throws Exception {

		int minor = 17;
		String locationOld = regionA;
		String location = "?";
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
			List<String> cList = new ArrayList<>();
    		cList.add("?");
    		cList.add(regionA);
    		setCPListtoBundle(cList, null, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("?", ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		this.setAppropriatePermission();
    		conf.setBundleLocation(locationOld);

    		// 4
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_18() throws Exception {

		int minor = 18;
		String locationOld = regionA;
		String location = regionA;
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle(regionA, ConfigurationPermission.CONFIGURE, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		// 2
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("?", ConfigurationPermission.CONFIGURE, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	@Test
	public void testSetBundleLocation_8_19() throws Exception {

		int minor = 19;
		String locationOld = locationA + "*";
		String location = regionA + "*";
		final String header = "testSetBundleLocation_8_"
				+ String.valueOf(minor) + "_";
		String testId = null;
		int micro = 0;

		final String pid1 = Util.createPid("1");
		Configuration conf = null;
		try {
    		this.setAppropriatePermission();
    		conf = cm.getConfiguration(pid1, locationOld);
            assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(locationOld);

    		// 1
    		testId = traceTestId(header, ++micro);
			List<String> cList = new ArrayList<>();
    		cList.add(locationA + "*");
    		cList.add(regionA + "*");
    		setCPListtoBundle(cList, null, thisBundle);
    		conf.setBundleLocation(location);
    		assertThat(this.getBundleLocationForCompare(conf)).as("Check Conf location.").isEqualTo(location);

    		// 2
    		testId = traceTestId(header, ++micro);
			cList = new ArrayList<>();
    		cList.add(locationA + ".com");
    		cList.add(regionA + ".com");
    		setCPListtoBundle(cList, null, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);

    		// 3
    		testId = traceTestId(header, ++micro);
    		setCPtoBundle("*", ConfigurationPermission.TARGET, thisBundle);
    		assertThrowsSEbySetLocation(conf, location, testId);
        } finally {
            if (conf != null) {
                conf.delete();
            }
        }
	}

	private void setCPListtoBundle(List<String> nameforConfigure,
			List<String> nameForTarget,
			Bundle bundle) throws BundleException {
		this.resetPermissions();
		list.clear();

		add(list, PropertyPermission.class.getName(), "*", "READ,WRITE");
		add(list, PP, "*", "IMPORT,EXPORTONLY");
		add(list, SP, "*", "GET,REGISTER");
		add(list, AP, "*", "*");

		if (nameforConfigure != null)
			if (!nameforConfigure.isEmpty()) {
				for (Iterator<String> itc = nameforConfigure.iterator(); itc
						.hasNext();) {
					add(list, CP, itc.next(),
							ConfigurationPermission.CONFIGURE);
				}
			}
		if (nameForTarget != null)
			if (!nameForTarget.isEmpty()) {
				for (Iterator<String> itt = nameforConfigure.iterator(); itt
						.hasNext();) {
					add(list, CP, itt.next(),
							ConfigurationPermission.TARGET);
				}
			}
		permissionFlag = true;
		this.setBundlePermission(bundle, list);
	}

	private void assertThrowsSEbySetLocation(final Configuration conf,
			final String location, String testId) throws AssertionError {
		String message = testId
				+ ":try to set bundle location without appropriate ConfigurationPermission.";
		try {
			conf.setBundleLocation(location);
			// A SecurityException should have been thrown
			failException(message, SecurityException.class);
		} catch (AssertionError e) {
			throw e;
		} catch (Throwable e) {
			// Check that we got the correct exception
			assertException(message, SecurityException.class, e);
		}
	}

	/* Ikuo YAMASAKI */

	private int assertCallback(SynchronizerImpl sync, int count) {
		boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count);
		assertThat(calledback).as("ManagedService#updated(props)/ManagedServiceFactory#updated(pid,props) must be called back").isTrue();
		return count;
	}

	private void assertNoCallback(SynchronizerImpl sync, int count) {
		boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1);
		assertThat(calledback).as("ManagedService#updated(props)/ManagedServiceFactory#updated(pid,props) must NOT be called back").isFalse();
	}

	private int assertDeletedCallback(SynchronizerImpl sync, int count) {
		boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, ++count,
				true);
		assertThat(calledback).as("ManagedServiceFactory#deleted(pid) must be called back").isTrue();
		return count;
	}

	private void assertDeletedNoCallback(SynchronizerImpl sync, int count) {
		boolean calledback = sync.waitForSignal(SIGNAL_WAITING_TIME, count + 1,
				true);
		assertThat(calledback).as("ManagedServiceFactory#deleted(pid) must NOT be called back").isFalse();
	}

	private void cleanUpForCallbackTest(final Bundle bundleT1,
			final Bundle bundleT2, final Bundle bundleT3,
			List<ServiceRegistration< ? >> list)
			throws BundleException {
		this.cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, null, list);
	}

	private void cleanUpForCallbackTest(final Bundle bundleT1,
			final Bundle bundleT2, final Bundle bundleT3,
			final Bundle bundleT4, List<ServiceRegistration< ? >> list)
			throws BundleException {
		for (Iterator<ServiceRegistration< ? >> regs = list.iterator(); regs
				.hasNext();)
			regs.next().unregister();
		list.clear();
		if (bundleT1 != null)
			bundleT1.uninstall();
		if (bundleT2 != null)
			bundleT2.uninstall();
		if (bundleT3 != null)
			bundleT3.uninstall();
		if (bundleT4 != null)
			bundleT4.uninstall();
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistration9_1_1() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testSetBundleLocation_9_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.setCPtoBundle(null, null, bundleT1, false);

			this.startTargetBundle(bundleT1);
			// this.setInappropriatePermission();

			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();

			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			this.setAppropriatePermission();
			Configuration conf = cm.getConfiguration(pid1,
					bundleT1.getLocation());
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_1);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("Conf is being deleted.");
			conf.delete();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	private void restartCM() throws BundleException {
		Bundle cmBundle = stopCmBundle();
		cmBundle.stop();
		System.out.println("CM has been stopped.");
		startCmBundle(cmBundle);
		System.out.println("CM has been started.");
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistration9_1_2() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
		/*
		 * A. create Configuration in advance, Then Register ManagedService .
		 */
		final String header = "testSetBundleLocation_9_1_2";

		int micro = 0;
		traceTestId(header, ++micro);
		System.out.println("The configuration is being created");
		Configuration conf = cm.getConfiguration(pid1, bundleT1.getLocation());
		System.out.println("The configuration is being updated ");
		Dictionary<String,Object> props = new Hashtable<>();
		props.put("StringKey", "stringvalue");
		conf.update(props);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class,
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.setCPtoBundle(null, null, bundleT1, false);

			this.startTargetBundle(bundleT1);

			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("null props must be called back").isNotNull();
			assertThat(sync1_1.getProps().get("StringKey")).as("Check props").isEqualTo("stringvalue");
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("Conf is being deleted.");
			conf.delete();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	// public void testManagedServiceRegistration9_2_1() throws Exception {
	// final Bundle bundleT1 = context.installBundle(
	// getWebServer() + "bundleT1.jar");
	// final Bundle bundleT2 = context.installBundle(
	// getWebServer() + "bundleT2.jar");
	// final String locationT2 = bundleT2.getLocation();
	// final String pid1 = Util.createPid("pid1");
	// List list = new ArrayList(3);
	// /*
	// * A. Register ManagedService in advance. Then create Configuration.
	// */
	// final String header = "testSetBundleLocation_9_2_1";
	//
	// String testId = null;
	// int micro = 0;
	// testId = traceTestId(header, ++micro);
	//
	// this.setCPtoBundle(locationT2, "target", bundleT1);
	//
	// try {
	// SynchronizerImpl sync1_1 = new SynchronizerImpl();
	// list.add(context.registerService(Synchronizer.class.getName(),
	// sync1_1, propsForSync1_1));
	// SynchronizerImpl sync1_2 = new SynchronizerImpl();
	// list.add(context.registerService(Synchronizer.class.getName(),
	// sync1_2, propsForSync1_2));
	// this.startTargetBundle(bundleT1);
	// System.out.println("Wait for signal.");
	// int count1_1 = 0;
	// count1_1 = assertCallback(sync1_1, count1_1);
	// assertThat(sync1_1.getProps()).as("called back with null props").isNull();
	// int count1_2 = 0;
	// count1_2 = assertCallback(sync1_2, count1_2);
	// assertThat(sync1_2.getProps()).as("called back with null props").isNull();
	//
	// System.out.println("The configuration is being created");
	//
	// Configuration conf = cm.getConfiguration(pid1, bundleT2
	// .getLocation());
	// System.out.println("Wait for signal.");
	// assertNoCallback(sync1_1, count1_1);
	//
	// System.out.println("The configuration is being updated ");
	// Dictionary<String,Object> props = new Hashtable<>();
	// props.put("StringKey", "stringvalue");
	// conf.update(props);
	// System.out.println("Wait for signal.");
	// assertNoCallback(sync1_1, count1_1);
	// assertNoCallback(sync1_2, count1_2);
	//
	// // restartCM();
	// // conf = cm.getConfiguration(pid1, bundleT2.getLocation());
	// // System.out.println("Wait for signal.");
	// // count1_1 = assertCallback(sync1_1, count1_1);
	// // count1_2 = assertCallback(sync1_2, count1_2);
	// // assertThat(sync1_2.getProps()).as("called back with null props").isNull();
	// //
	// // System.out.println("conf is going to be deleted.");
	// // conf.delete();
	// // count1_1 = assertCallback(sync1_1, count1_1);
	// // assertThat(sync1_1.getProps()).as("called back with null props").isNull();
	// // this.assertNoCallback(sync1_2, count1_2);
	//
	// } finally {
	// this.cleanUpForCallbackTest(bundleT1, null, null, list);
	// }
	// }
	//

	@Test
	public void testManagedServiceRegistration9_2_1() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		internalManagedServiceRegistration9_2_1to2(2, bundleT2.getLocation(),
				ConfigurationPermission.TARGET, bundleT1, bundleT2);
	}

	@Test
	public void testManagedServiceRegistration9_2_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		internalManagedServiceRegistration9_2_1to2(3, "*",
				ConfigurationPermission.TARGET, bundleT1, bundleT2);
	}

	private void internalManagedServiceRegistration9_2_1to2(final int micro,
			final String target, final String actions, final Bundle bundleT1,
			final Bundle bundleT2) throws Exception {

		bundleT2.getLocation();
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testSetBundleLocation_9_2_"
				+ String.valueOf(micro);

		int micro1 = 0;
		traceTestId(header, ++micro1);

		this.setCPtoBundle(actions, target, bundleT1);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl();
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");

			Configuration conf = cm.getConfiguration(pid1,
					bundleT2.getLocation());
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("conf is going to be deleted.");
			conf.delete();
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistration9_2_4() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		internalManagedServiceRegistration9_2_4to5(4, bundleT2.getLocation(),
				"target", bundleT1, bundleT2);
	}

	@Test
	public void testManagedServiceRegistration9_2_5() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		internalManagedServiceRegistration9_2_4to5(5, "*", "target", bundleT1,
				bundleT2);
	}

	public void internalManagedServiceRegistration9_2_4to5(final int micro,
			final String target, final String actions, final Bundle bundleT1,
			final Bundle bundleT2) throws Exception {

		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testSetBundleLocation_9_2_"
				+ String.valueOf(micro);

		int micro1 = 0;
		traceTestId(header, ++micro1);

		System.out.println("The configuration is being created");
		Configuration conf = cm.getConfiguration(pid1, bundleT2.getLocation());

		System.out.println("The configuration is being updated ");
		Dictionary<String,Object> props = new Hashtable<>();
		props.put("StringKey", "stringvalue");
		conf.update(props);
		this.setCPtoBundle(target, actions, bundleT1);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("Conf is being deleted.");
			conf.delete();
			this.assertNoCallback(sync1_1, count1_1);
			this.assertNoCallback(sync1_2, count1_2);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, null, list);
		}
	}

	/**
	 * Register ManagedService in advance. Then create Configuration.
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_1_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final Bundle bundleT4 = context.installBundle(
				getWebServer() + "bundleT4.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(7);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testManagedServiceRegistrationMultipleTargets_10_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSync2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSync3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSync3_2));
			SynchronizerImpl sync4_1 = new SynchronizerImpl("4-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_1, propsForSync4_1));
			SynchronizerImpl sync4_2 = new SynchronizerImpl("4-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_2, propsForSync4_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?*", "target", bundleT2, false);
			this.setCPtoBundle("?RegionA", "target", bundleT3, false);
			this.setCPtoBundle("?RegionA", "target", bundleT4, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT2);
			int count2_1 = 0;
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT3);
			int count3_1 = 0;
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			int count3_2 = 0;
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT4);
			// TODO Called back twice each pid.
			// int count4_1 = 0;
			int count4_1 = 1;
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with null props").isNull();
			// count4_1 = 2
			// int count4_2 = 0;
			int count4_2 = 1;
			count4_2 = assertCallback(sync4_2, count4_2);
			assertThat(sync4_2.getProps()).as("called back with null props").isNull();
			// count4_2 = 2

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			System.out.println("Wait for signal.");

			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);
			assertNoCallback(sync4_1, count4_1);
			assertNoCallback(sync4_2, count4_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync4_2, count4_2);

			System.out.println("conf is going to be deleted.");
			conf.delete();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with null props").isNull();
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync4_2, count4_2);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, bundleT4, list);
		}

	}

	/**
	 * Register ManagedService in advance. Then create Configuration.
	 *
	 * @throws Exception
	 */
	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_1_2()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final Bundle bundleT4 = context.installBundle(
				getWebServer() + "bundleT4.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testManagedServiceRegistrationMultipleTargets_10_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSync2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSync3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSync3_2));
			SynchronizerImpl sync4_1 = new SynchronizerImpl("4-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_1, propsForSync4_1));
			SynchronizerImpl sync4_2 = new SynchronizerImpl("4-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_2, propsForSync4_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?RegionB", "target", bundleT2, false);
			this.setCPtoBundle("?RegionB", "target", bundleT3, false);
			this.setCPtoBundle("?RegionA", "target", bundleT4, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT2);
			int count2_1 = 0;
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT3);
			int count3_1 = 0;
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			int count3_2 = 0;
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT4);
			// int count4_1 = 0;
			int count4_1 = 1;
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with null props").isNull();
			// int count4_2 = 0;
			int count4_2 = 1;
			count4_2 = assertCallback(sync4_2, count4_2);
			assertThat(sync4_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);
			assertNoCallback(sync4_1, count4_1);
			assertNoCallback(sync4_2, count4_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync4_2, count4_2);

			System.out.println("conf is going to be deleted.");
			conf.delete();
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync4_2, count4_2);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, bundleT4, list);
		}

	}

	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_2_1()
			throws Exception {
	    System.setProperty("org.osgi.test.cases.cm.bundleT4.mode","Array" );
	    try {
	        this.internalTestManagedServiceRegistrationMultipleTargets_10_2_1to3();
	    } finally {
	        System.getProperties().remove("org.osgi.test.cases.cm.bundleT4.mode");
	    }
	}

	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_2_2()
			throws Exception {
		System.setProperty("org.osgi.test.cases.cm.bundleT4.mode", "Vector");
		try {
    		this.internalTestManagedServiceRegistrationMultipleTargets_10_2_1to3();
        } finally {
            System.getProperties().remove("org.osgi.test.cases.cm.bundleT4.mode");
        }
	}

	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_2_3()
			throws Exception {
		System.setProperty("org.osgi.test.cases.cm.bundleT4.mode", "List");
		try {
		    this.internalTestManagedServiceRegistrationMultipleTargets_10_2_1to3();
        } finally {
            System.getProperties().remove("org.osgi.test.cases.cm.bundleT4.mode");
        }
	}

	private void internalTestManagedServiceRegistrationMultipleTargets_10_2_1to3()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final Bundle bundleT4 = context.installBundle(
				getWebServer() + "bundleT4.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceRegistrationMultipleTargets_10_2_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSync2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSync3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSync3_2));
			SynchronizerImpl sync4_1 = new SynchronizerImpl("4-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_1, propsForSync4_1));
			SynchronizerImpl sync4_2 = new SynchronizerImpl("4-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_2, propsForSync4_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?*", "target", bundleT2, false);
			this.setCPtoBundle("?RegionA", "target", bundleT3, false);
			this.setCPtoBundle("?RegionA", "target", bundleT4, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT2);
			int count2_1 = 0;
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with NON-null props").isNotNull();
			this.startTargetBundle(bundleT3);
			int count3_1 = 0;
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with NON-null props").isNotNull();
			int count3_2 = 0;
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with NON-null props").isNotNull();
			this.startTargetBundle(bundleT4);
			System.out.println("Wait for signal.");

			// MS for pid2, pid1; two callbacks expected
			int count4_1 = 0;
			count4_1 = assertCallback(sync4_1, count4_1+1);
			assertThat(2).as("expect two callbacks").isEqualTo(count4_1);
			assertThat(sync4_1.getProps()).as("called back with NON-null props").isNotNull();

	 		// MS for pid2, pid3; two callbacks expected
			int count4_2 = 0;
			count4_2 = assertCallback(sync4_2, count4_2+1);
			assertThat(2).as("expect two callbacks").isEqualTo(count4_2);
			assertThat(sync4_2.getProps()).as("called back with null props").isNull();

			// System.out.println("conf is going to be deleted.");
			// conf.delete();
			// assertNoCallback(sync1_1, count1_1);
			// assertNoCallback(sync1_2, count1_2);
			// count2_1 = assertCallback(sync2_1, count2_1);
			// assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			// count3_1 = assertCallback(sync3_1, count3_1);
			// assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			// count3_2 = assertCallback(sync3_2, count3_2);
			// assertThat(sync3_2.getProps()).as("called back with null props").isNull();
		} finally {

			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, bundleT4, list);
		}
	}

	@Test
	public void testManagedServiceRegistrationMultipleTargets_10_2_4()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final Bundle bundleT4 = context.installBundle(
				getWebServer() + "bundleT4.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testManagedServiceRegistrationMultipleTargets_10_2_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSync2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSync3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSync3_2));
			SynchronizerImpl sync4_1 = new SynchronizerImpl("4-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_1, propsForSync4_1));
			SynchronizerImpl sync4_2 = new SynchronizerImpl("4-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync4_2, propsForSync4_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?RegionB", "target", bundleT2, false);
			this.setCPtoBundle("?RegionB", "target", bundleT3, false);
			this.setCPtoBundle("?RegionA", "target", bundleT4, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT2);
			System.out.println("Wait for signal.");
			int count2_1 = 0;
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT3);
			System.out.println("Wait for signal.");
			int count3_1 = 0;
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			// assertThat(// sync3_1.getProps()).as("called back with NON-null props").isNotNull();
			int count3_2 = 0;
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with null props").isNull();

			String mode = System
					.getProperty("org.osgi.test.cases.cm.bundleT4.mode");
			System.out.println("##########################" + mode);

			this.startTargetBundle(bundleT4);
			System.out.println("Wait for signal.");
			int count4_1 = 0;
			count4_1 = assertCallback(sync4_1, count4_1);
			// ignore actual call back properties, might already have been overwritten
			count4_1 = assertCallback(sync4_1, count4_1);
			assertThat(sync4_1.getProps()).as("called back with NON-null props").isNotNull();
			int count4_2 = 0;
			count4_2 = assertCallback(sync4_2, count4_2);
			// ignore actual call back properties, might already have been overwritten
			count4_2 = assertCallback(sync4_2, count4_2);
			assertThat(sync4_2.getProps()).as("called back with null props").isNull();

			// System.out.println("conf is going to be deleted.");
			// conf.delete();
			// assertNoCallback(sync1_1, count1_1);
			// assertNoCallback(sync1_2, count1_2);
			// count2_1 = assertCallback(sync2_1, count2_1);
			// assertThat(sync2_1.getProps()).as("called back with null props").isNull();
			// count3_1 = assertCallback(sync3_1, count3_1);
			// assertThat(sync3_1.getProps()).as("called back with null props").isNull();
			// count3_2 = assertCallback(sync3_2, count3_2);
			// assertThat(sync3_2.getProps()).as("called back with null props").isNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, bundleT4, list);
		}
	}

	@Test
	public void testManagedServiceRegistrationMultipleTargets_11_1_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		/*
		 * A. Register ManagedService in advance. Then create Configuration.
		 */
		final String header = "testManagedServiceRegistrationMultipleTargets_11_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSync2_1));

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			this.startTargetBundle(bundleT2);
			int count2_1 = 0;
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with null props").isNull();

			this.printoutPermissions();
			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("*", "target", bundleT2, false);

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, null);
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);
			settle(3000);

			if(conf.getBundleLocation().equals(bundleT1.getLocation())){
				count1_1 = assertCallback(sync1_1, count1_1);
				assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
				assertNoCallback(sync1_2, count1_2);
				assertNoCallback(sync2_1, count2_1);
				assertThat(bundleT1.getLocation()).as("conf.location must be dynamically set to location of T1.").isEqualTo(conf.getBundleLocation());
				bundleT1.stop();
				bundleT1.uninstall();
				props.put("StringKey", "stringvalueNew");
				System.out.println("The configuration is being updated ");
				conf.update(props);
				count2_1 = assertCallback(sync2_1, count2_1);
				assertThat(sync2_1.getProps()).as("called back with NON-null props").isNotNull();
				bundleT2.stop();
				bundleT2.uninstall();
			}else if(conf.getBundleLocation().equals(bundleT2.getLocation())){
				assertNoCallback(sync1_1, count1_1);
				assertNoCallback(sync1_2, count1_2);
				count2_1 = assertCallback(sync2_1, count2_1);
				assertThat(sync2_1.getProps()).as("called back with NON-null props").isNotNull();
				assertThat(bundleT2.getLocation()).as("conf.location must be dynamically set to location of T2.").isEqualTo(conf.getBundleLocation());
				System.out.println("The bound bundleT2 is going to unregister MS.");
				bundleT2.stop();
				bundleT2.uninstall();
				props.put("StringKey", "stringvalueNew");
				System.out.println("The configuration is being updated ");
				conf.update(props);
				count1_1 = assertCallback(sync1_1, count1_1);
				assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
				assertNoCallback(sync1_2, count1_2);
				bundleT1.stop();
				bundleT1.uninstall();
			}else{
				fail("");
			}
		} finally {
			cleanUpForCallbackTest(null, null, null, list);
		}
	}

	@Test
	public void testManagedServiceChangeLocation_12_1_1() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceChangeLocation_12_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1,
					bundleT2.getLocation());
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("?RegionA", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("conf.setBundleLocation() is being called.");
			conf.setBundleLocation("?RegionA");

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			// count1_2 = assertCallback(sync1_2, count1_2);
			// assertThat(sync1_2.getProps()).as("called back with null props").isNull();

		} finally {

			cleanUpForCallbackTest(bundleT1, bundleT2, null, list);
		}
	}

	@Test
	public void testManagedServiceChangeLocation_12_1_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceChangeLocation_12_1_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("?RegionA", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			assertThat("?RegionA").as("conf.location must be set to \"?RegionA\".").isEqualTo(conf.getBundleLocation());

			System.out.println("conf.setBundleLocation() is being called.");
			conf.setBundleLocation(bundleT2.getLocation());

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
		} finally {

			cleanUpForCallbackTest(bundleT1, bundleT2, null, list);
		}
	}

	@Test
	public void testManagedServiceChangeLocation_12_1_3() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceChangeLocation_12_1_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("?RegionA", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
			assertThat("?RegionA").as("conf.location must be set to \"?RegionA\".").isEqualTo(conf.getBundleLocation());

			System.out.println("conf.setBundleLocation() is being called.");
			conf.setBundleLocation("?RegionB");

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, null, list);
		}
	}

	@Test
	public void testManagedServiceChangeCP_12_2_1() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceChangeCP_12_2_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			this.setCPtoBundle("?RegionA", "target", bundleT1, false);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			props.put("StringKey", "stringvalueNew");
			System.out.println("The configuration is being updated ");
			conf.update(props);
			//count1_1 = assertCallback(sync1_1, count1_1);
			//assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceChangeCP_12_2_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceChangeCP_12_2_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("?RegionA", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			this.setCPtoBundle("*", "target", bundleT1, false);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			props.put("StringKey", "stringvalueNew");
			System.out.println("The configuration is being updated ");
			conf.update(props);
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceStartCM_12_3_1() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceStartCM_12_3_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			Bundle cmBundle = this.stopCmBundle();

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			int count1_2 = 0;
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("CM is going to start");
			startCmBundle(cmBundle);
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceStartCM_12_3_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceStartCM_12_3_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			assertThat(conf).isNotNull();
			// Dictionary<String,Object> props = new Hashtable<>();
			// props.put("StringKey", "stringvalue");
			// System.out.println("The configuration is being updated ");
			// conf.update(props);
            Bundle cmBundle = this.stopCmBundle();

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			int count1_2 = 0;
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("CM is going to start");
			startCmBundle(cmBundle);
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceStartCM_12_3_3() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceStartCM_12_3_3";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?RegionA");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);
            Bundle cmBundle = this.stopCmBundle();

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			int count1_2 = 0;
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("CM is going to start");
			startCmBundle(cmBundle);
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceModifyPid_12_4_1() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		final String pid2 = Util.createPid("pid2");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceModifyPid_12_4_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			// modify PID
			System.out.println("modify PID from " + pid1 + " to " + pid2);
			modifyPid(ManagedService.class.getName(), pid1, pid2);
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			assertNoCallback(sync1_2, count1_2);

		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceModifyPid_12_4_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String pid1 = Util.createPid("pid1");
		final String pid2 = Util.createPid("pid2");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);

		final String header = "testManagedServiceModifyPid_12_4_1";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSync1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSync1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with null props").isNull();
			int count1_2 = 0;
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with null props").isNull();

			System.out.println("The configuration is being created");
			Configuration conf = cm.getConfiguration(pid1, "?");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			System.out.println("The configuration is being updated ");
			conf.update(props);

			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			// modify PID
			System.out.println("modify PID from " + pid2 + " to " + pid1);
			modifyPid(ManagedService.class.getName(), pid2, pid1);
			assertNoCallback(sync1_1, count1_1);
			count1_1 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with NON-null props").isNotNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	private void modifyPid(String clazz, String oldPid, String newPid) {
		ServiceReference< ? > reference1 = this.context
				.getServiceReference(
				ModifyPid.class.getName());
		ModifyPid modifyPid = (ModifyPid) this.context.getService(
				reference1);

		ServiceReference< ? >[] references = null;
		try {
			references = this.context.getServiceReferences(clazz,
					"(service.pid=" + oldPid + ")");
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("The pid of MS1-1 is being changed from " + oldPid + " to "
				+ newPid);
		modifyPid.changeMsPid(
				(Long) (references[0].getProperty(Constants.SERVICE_ID)),
				newPid);
	}

	@Test
	public void testManagedServiceFactoryRegistration13_1_1() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryRegistration13_1_1";

		int micro = 0;
		traceTestId(header, ++micro);

		this.setCPtoBundle(null, null, bundleT1, false);

		checkMsf_FirstRegThenCreateConf(bundleT1, fpid1, list,
				bundleT1.getLocation(), true);
	}

	private void checkMsf_FirstRegThenCreateConf(Bundle bundleT1,
			final String fpid1, List<ServiceRegistration< ? >> list,
			final String location,
			boolean toBeCalledBack) throws BundleException, IOException {
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			int count1_1 = 0;
			int count1_2 = 0;
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1, location);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			if (toBeCalledBack) {
				count1_1 = assertCallback(sync1_1, count1_1);
				props = sync1_1.getProps();
				assertThat(props).as("null props must be called back").isNotNull();
				assertThat(props.get(Constants.SERVICE_PID)).as("pid").isNotNull();
				assertThat(props.get(Constants.SERVICE_PID).equals(fpid1)).as("pid").isFalse();
				assertThat(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)).as("fpid").isEqualTo(fpid1);
				assertThat(props.get("stringkey")).as("value").isEqualTo("stringvalue");
				assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
				assertThat(props.size()).as("Size of props must be 3").isEqualTo(3);
			} else {
				assertNoCallback(sync1_1, count1_1);
			}
			assertNoCallback(sync1_2, count1_2);

			restartCM();
			if (toBeCalledBack) {
				count1_1 = assertCallback(sync1_1, count1_1);
				props = sync1_1.getProps();
				assertThat(props).as("null props must be called back").isNotNull();
				assertThat(props.get(Constants.SERVICE_PID)).as("pid").isNotNull();
				assertThat(props.get(Constants.SERVICE_PID).equals(fpid1)).as("pid").isFalse();
				assertThat(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)).as("fpid").isEqualTo(fpid1);
				assertThat(props.get("stringkey")).as("value").isEqualTo("stringvalue");
				assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
				assertThat(props.size()).as("Size of props must be 3").isEqualTo(3);
			} else {
				assertNoCallback(sync1_1, count1_1);
			}
			assertNoCallback(sync1_2, count1_2);

		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistration13_1_2() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryRegistration11_1_2";

		int micro = 0;
		traceTestId(header, ++micro);

		this.setCPtoBundle(null, null, bundleT1, false);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					bundleT1.getLocation());

			int count1_1 = 0;
			int count1_2 = 0;

			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("Bundle is going to stop.");
			bundleT1.stop();
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			props = sync1_1.getProps();
			assertThat(props).as("null props must be called back").isNotNull();
			assertThat(props.get(Constants.SERVICE_PID)).as("pid").isNotNull();
			assertThat(props.get(Constants.SERVICE_PID).equals(fpid1)).as("pid").isFalse();
			assertThat(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)).as("fpid").isEqualTo(fpid1);
			assertThat(props.get("stringkey")).as("value").isEqualTo("stringvalue");
			assertThat(props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			assertThat(props.size()).as("Size of props must be 3").isEqualTo(3);

			System.out.println("The configuration is being deleted. Wait for signal.");
			conf.delete();
			this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);
		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistration13_2_2() throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);
		/*
		 * A. Register in advance. Then create Configuration.
		 */
		final String header = "testManagedServiceFactoryRegistration13_2_2";

		int micro = 0;
		traceTestId(header, ++micro);

		this.setCPtoBundle("*", "target", bundleT1, false);

		checkMsf_FirstRegThenCreateConf(bundleT1, fpid1, list,
				bundleT2.getLocation(), false);
	}

	@Test
	public void testManagedServiceFactoryRegistration13_2_5() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryRegistration13_2_5";

		int micro = 0;
		traceTestId(header, ++micro);

		this.setCPtoBundle(null, null, bundleT1, false);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					bundleT2.getLocation());
			int count1_1 = 0;
			int count1_2 = 0;
			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1);
			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("Bundle is going to stop.");
			bundleT1.stop();
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			// count1_1 = assertCallback(sync1_1, count1_1);
			// assertThat(// sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			// props = sync1_1.getProps();
			// assertThat(props).as("null props must be called back").isNotNull();
			// assertThat(props.get(Constants.SERVICE_PID)).as("pid").isNotNull();
			// assertThat(// props.get(Constants.SERVICE_PID).equals(fpid1)).as("pid").isFalse();
			// assertThat(// props.get(ConfigurationAdmin.SERVICE_FACTORYPID)).as("fpid").isEqualTo(fpid1);
			// assertThat(props.get("stringkey")).as("value").isEqualTo("stringvalue");
			// assertThat(// props.get("service.bundleLocation")).as("bundleLocation must be not included").isNull();
			// assertThat(props.size()).as("Size of props must be 3").isEqualTo(3);

			System.out.println("The configuration is being deleted. Wait for signal.");
			conf.delete();
			this.assertDeletedNoCallback(sync1_1, 0);
			// count1_1 = this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);

		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_14_1_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		// final Bundle bundleT4 = context.installBundle(
		// getWebServer() + "bundleT4.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_14_1_1";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?*", "target", bundleT2, false);
			this.setCPtoBundle("?RegionA", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();

			System.out.println("conf is going to be deleted.");
			conf.delete();
			this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);
			this.assertDeletedCallback(sync2_1, 0);
			this.assertDeletedCallback(sync3_1, 0);
			this.assertDeletedCallback(sync3_2, 0);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_14_1_2()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		// final Bundle bundleT4 = context.installBundle(
		// getWebServer() + "bundleT4.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_14_1_2";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?RegionB", "target", bundleT2, false);
			this.setCPtoBundle("?RegionB", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("conf is going to be deleted.");
			conf.delete();
			this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);
			this.assertDeletedNoCallback(sync2_1, 0);
			this.assertDeletedNoCallback(sync3_1, 0);
			this.assertDeletedNoCallback(sync3_2, 0);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_14_2_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_14_2_1";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?*", "target", bundleT2, false);
			this.setCPtoBundle("?RegionA", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			this.startTargetBundle(bundleT1);
			settle(1000);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			this.startTargetBundle(bundleT3);
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();

			System.out.println("conf is going to be deleted.");
			conf.delete();
			this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);
			this.assertDeletedCallback(sync2_1, 0);
			this.assertDeletedCallback(sync3_1, 0);
			this.assertDeletedCallback(sync3_2, 0);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_14_2_2()
			throws Exception {

		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_14_2_2";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("?RegionB", "target", bundleT2, false);
			this.setCPtoBundle("?RegionB", "target", bundleT3, false);
			// this.setCPtoBundle("?*", "target", bundleT2, false);
			// this.setCPtoBundle("?RegionA", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			this.startTargetBundle(bundleT1);
			settle(1000);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("conf is going to be deleted.");
			conf.delete();
			this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);
			this.assertDeletedNoCallback(sync2_1, 0);
			this.assertDeletedNoCallback(sync3_1, 0);
			this.assertDeletedNoCallback(sync3_2, 0);
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_15_1_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		//final Bundle bundleT3 = context.installBundle(
		//		getWebServer() + "bundleT3.jar");
		// final Bundle bundleT4 = context.installBundle(
		// getWebServer() + "bundleT4.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_15_1_1";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			//SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			//list.add(context.registerService(Synchronizer.class.getName(),
			//		sync3_1, propsForSyncF3_1));
			//SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			//list.add(context.registerService(Synchronizer.class.getName(),
			//		sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle("*", "target", bundleT2, false);
			//this.setCPtoBundle("*", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			//int count3_1 = 0;
			//int count3_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			//this.startTargetBundle(bundleT3);
			//assertNoCallback(sync3_1, count3_1);
			//assertNoCallback(sync3_2, count3_2);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1, null);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			//assertNoCallback(sync3_1, count3_1);
			//assertNoCallback(sync3_2, count3_2);
//			assertThat(//					bundleT2.getLocation()).isCloseTo(//					"The location of the conf must be dynamically bound to bundleT2", within(conf.getBundleLocation()));

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			settle(1000);
			if(conf.getBundleLocation().equals(bundleT1.getLocation())){
				System.out.println("Wait for signal.");
				count1_1 = assertCallback(sync1_1, count1_1);
				assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
				assertNoCallback(sync1_2, count1_2);
				assertNoCallback(sync2_1, count2_1);
				assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically bound to bundleT1").isEqualTo(bundleT1.getLocation());
				System.out.println("Bound bundleT1 is going to be stopped.");
				bundleT1.stop();
				bundleT1.uninstall();
				settle(1000);
				assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically bound to bundleT2").isEqualTo(bundleT2.getLocation());
				count2_1 = assertCallback(sync2_1, count2_1);
				System.out.println("conf is going to be deleted.");
				conf.delete();
				this.assertDeletedCallback(sync2_1, 0);
				bundleT2.stop();
				bundleT2.uninstall();
			}else if(conf.getBundleLocation().equals(bundleT2.getLocation())){
				System.out.println("Wait for signal.");
				assertNoCallback(sync1_1, count1_1);
				assertNoCallback(sync1_2, count1_2);
				count2_1 = assertCallback(sync2_1, count2_1);
				assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
				assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically bound to bundleT2").isEqualTo(bundleT2.getLocation());
				System.out.println("Bound bundleT2 is going to be stopped.");
				bundleT2.stop();
				bundleT2.uninstall();
				settle(1000);
				assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically bound to bundleT1").isEqualTo(bundleT1.getLocation());
				count1_1 = assertCallback(sync1_1, count1_1);
				assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
				assertNoCallback(sync1_2, count1_2);
				System.out.println("conf is going to be deleted.");
				conf.delete();
				this.assertDeletedCallback(sync1_1, 0);
				this.assertDeletedNoCallback(sync1_2, 0);
				bundleT1.stop();
				bundleT1.uninstall();
			}else{
				fail("");
			}

			//System.out.println("Wait for signal.");
			//assertNoCallback(sync1_1, count1_1);
			//assertNoCallback(sync1_2, count1_2);
			//count2_1 = assertCallback(sync2_1, count2_1);
			//assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			//assertNoCallback(sync3_1, count3_1);
			//assertNoCallback(sync3_2, count3_2);

			//System.out.println("Bound bundleT2 is going to be stopped.");
			//bundleT2.stop();
			//assertThat(//		bundleT3.getLocation()).isCloseTo(//		"The location of the conf must be dynamically bound to bundleT3", within(conf.getBundleLocation()));

			// TODO confirm the behavior when the MSF gets unvisible. Will
			// deleted(pid) be called back ?
			//assertNoCallback(sync1_1, count1_1);
			//assertNoCallback(sync1_2, count1_2);
			//int countDeleted2_1 = this.assertDeletedCallback(sync2_1, 0);
			//count3_1 = assertCallback(sync3_1, count3_1);
			//assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			//count3_1 = assertCallback(sync3_2, count3_2);
			//assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();

			//System.out.println("conf is going to be deleted.");
			//conf.delete();
			//this.assertDeletedNoCallback(sync1_1, 0);
			//this.assertDeletedNoCallback(sync1_2, 0);
			//this.assertDeletedNoCallback(sync2_1, countDeleted2_1);
			//this.assertDeletedCallback(sync3_1, 0);
			//this.assertDeletedCallback(sync3_2, 0);
		} finally {
			cleanUpForCallbackTest(null, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_15_1_2()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_15_1_2";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			int count1_1 = 0;
			int count1_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1, null);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically bound to bundleT1").isEqualTo(bundleT1.getLocation());

			System.out.println("Bound bundleT1 is going to be stopped.");
			bundleT1.stop();
			bundleT1.uninstall();
			settle(1000);
			assertThat(conf.getBundleLocation()).as("The location of the conf must be dynamically unbound to null.").isNull();
		} finally {
			cleanUpForCallbackTest(null, null, null, list);
		}
	}


	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_15_2_1()
			throws Exception {
		// TODO impl
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleTargets_15_3_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		// final Bundle bundleT4 = context.installBundle(
		// getWebServer() + "bundleT4.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleTargets_15_3_1";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("*", "target", bundleT1, false);
			this.setCPtoBundle(bundleT2.getLocation(), "target", bundleT2,
					false);
			this.setCPtoBundle("?", "target", bundleT3, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1, "?");
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			this.printoutPermissions();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			this.startTargetBundle(bundleT1);
			settle(1000);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			settle(1000);
			System.out.println("Wait for signal.");
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			settle(1000);
			System.out.println("Wait for signal.");
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();

		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleCF_16_1_1()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleCF_16_1_1";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("?RegionA", "target", bundleT1, false);
			this.setCPtoBundle("?RegionB", "target", bundleT2, false);
			this.setCPtoBundle(null, null, bundleT3, false);
			// this.setCPtoBundle(null, null, bundleT2, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configurations are being created");
			Configuration conf1 = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			Configuration conf2 = cm.createFactoryConfiguration(fpid1, "?");
			Configuration conf3 = cm.createFactoryConfiguration(fpid1,
					bundleT3.getLocation());
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf1 is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf1.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf2 is being updated ");
			conf2.update(props);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			// count2_1 = assertCallback(sync2_1, count2_1);
			// assertThat(// sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf3 is being updated ");
			conf3.update(props);
			System.out.println("Wait for signal.");
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryRegistrationMultipleCF_16_1_2()
			throws Exception {
		final Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final Bundle bundleT2 = context.installBundle(
				getWebServer() + "bundleT2.jar");
		final Bundle bundleT3 = context.installBundle(
				getWebServer() + "bundleT3.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(5);
		final String header = "testManagedServiceFactoryRegistrationMultipleCF_16_1_2";

		int micro = 0;
		traceTestId(header, ++micro);
		try {
			SynchronizerImpl sync1_1 = new SynchronizerImpl("1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));
			SynchronizerImpl sync2_1 = new SynchronizerImpl("2-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync2_1, propsForSyncF2_1));

			SynchronizerImpl sync3_1 = new SynchronizerImpl("3-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_1, propsForSyncF3_1));
			SynchronizerImpl sync3_2 = new SynchronizerImpl("3-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync3_2, propsForSyncF3_2));

			this.resetPermissions();
			this.setCPtoBundle("?*", "target", bundleT1, false);
			this.setCPtoBundle("?", "target", bundleT2, false);
			this.setCPtoBundle(null, null, bundleT3, false);
			// this.setCPtoBundle(null, null, bundleT2, false);
			int count1_1 = 0;
			int count1_2 = 0;
			int count2_1 = 0;
			int count3_1 = 0;
			int count3_2 = 0;

			this.startTargetBundle(bundleT1);
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			this.startTargetBundle(bundleT2);
			assertNoCallback(sync2_1, count2_1);
			this.startTargetBundle(bundleT3);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The configurations are being created");
			Configuration conf1 = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			Configuration conf2 = cm.createFactoryConfiguration(fpid1, "?");
			Configuration conf3 = cm.createFactoryConfiguration(fpid1,
					bundleT3.getLocation());
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf1 is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf1.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);
			assertNoCallback(sync2_1, count2_1);
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf2 is being updated ");
			conf2.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with Non-null props").isNotNull();
			// assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);
			count2_1 = assertCallback(sync2_1, count2_1);
			assertThat(sync2_1.getProps()).as("called back with Non-null props").isNotNull();
			assertNoCallback(sync3_1, count3_1);
			assertNoCallback(sync3_2, count3_2);

			System.out.println("The conf3 is being updated ");
			conf3.update(props);
			System.out.println("Wait for signal.");
			count3_1 = assertCallback(sync3_1, count3_1);
			assertThat(sync3_1.getProps()).as("called back with Non-null props").isNotNull();
			count3_2 = assertCallback(sync3_2, count3_2);
			assertThat(sync3_2.getProps()).as("called back with Non-null props").isNotNull();
		} finally {
			cleanUpForCallbackTest(bundleT1, bundleT2, bundleT3, list);
		}
	}

	@Test
	public void testManagedServiceFactoryCmRestart18_3_2() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryCmRestart18_3_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					bundleT1.getLocation());
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			System.out.println("CM Bundle is going to start.");
			Bundle cmBundle = stopCmBundle();

			this.setCPtoBundle("*", "target", bundleT1, false);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			int count1_1 = 0;
			int count1_2 = 0;

			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			assertNoCallback(sync1_2, count1_2);

			System.out.println("CM Bundle is going to start.");
			this.startTargetBundle(cmBundle);
			assignCm();
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryModifyPid18_4_2() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		final String fpid2 = Util.createPid("factoryPid2");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryModifyPid18_4_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {

			this.setCPtoBundle("*", "target", bundleT1, false);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			int count1_1 = 0;
			int count1_2 = 0;

			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);

			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1, "?");
			// Configuration conf =
			// cm.createFactoryConfiguration(fpid1,bundleT1.getLocation());
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			// modify PID
			modifyPid(ManagedServiceFactory.class.getName(), fpid2, fpid1);

			// ServiceReference[] references = this.context
			// .getServiceReferences(
			// ManagedServiceFactory.class.getName(), null);

			System.out.println("Wait for signal.");
			assertNoCallback(sync1_1, count1_1);
			count1_2 = assertCallback(sync1_2, count1_2);
			assertThat(sync1_2.getProps()).as("called back with NON-null props").isNotNull();

		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	@Test
	public void testManagedServiceFactoryDeletion18_5_2() throws Exception {
		Bundle bundleT1 = context.installBundle(
				getWebServer() + "bundleT1.jar");
		final String fpid1 = Util.createPid("factoryPid1");
		List<ServiceRegistration< ? >> list = new ArrayList<>(3);

		final String header = "testManagedServiceFactoryDeletion18_5_2";

		int micro = 0;
		traceTestId(header, ++micro);

		try {
			System.out.println("The configuration is being created");
			Configuration conf = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("The configuration is being updated ");
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("StringKey", "stringvalue");
			conf.update(props);

			this.setCPtoBundle("*", "target", bundleT1, false);

			SynchronizerImpl sync1_1 = new SynchronizerImpl("F1-1");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_1, propsForSyncF1_1));
			SynchronizerImpl sync1_2 = new SynchronizerImpl("F1-2");
			list.add(context.registerService(Synchronizer.class.getName(),
					sync1_2, propsForSyncF1_2));

			int count1_1 = 0;
			int count1_2 = 0;

			System.out.println("Bundle is going to start.");
			this.startTargetBundle(bundleT1);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			System.out.println("The configuration is being deleted. Wait for signal.");
			conf.delete();

			int countDeteted1_1 = this.assertDeletedCallback(sync1_1, 0);
			this.assertDeletedNoCallback(sync1_2, 0);

			System.out.println("The configuration is being created");
			Configuration conf2 = cm.createFactoryConfiguration(fpid1,
					"?RegionA");
			System.out.println("The configuration is being updated ");
			conf2.update(props);
			System.out.println("Wait for signal.");
			count1_1 = assertCallback(sync1_1, count1_1);
			assertThat(sync1_1.getProps()).as("called back with NON-null props").isNotNull();
			assertNoCallback(sync1_2, count1_2);

			System.out.println("Change permission of bundleT1 ");
			this.setCPtoBundle(null, null, bundleT1, false);
			System.out.println("The configuration is being deleted. Wait for signal.");
			conf2.delete();
			this.assertDeletedNoCallback(sync1_1, countDeteted1_1);
			this.assertDeletedNoCallback(sync1_2, 0);
		} finally {
			this.cleanUpForCallbackTest(bundleT1, null, null, list);
		}
	}

	/**
	 * Configuration attributes - locking
	 * @since 1.6
	 */
	@Test
	public void testConfigurationLocking() throws Exception {
		final String pid = Util.createPid("locking");
		final List<ConfigurationEvent> events = new ArrayList<>();
		final List<Dictionary<String, ?>> updates = new ArrayList<>();
		final CyclicBarrier barrier = new CyclicBarrier(2);

		final SynchronousConfigurationListener listener = new SynchronousConfigurationListener() {

			@Override
			public void configurationEvent(final ConfigurationEvent event) {
				if (pid.equals(event.getPid())) {
					synchronized (events) {
						events.add(event);
					}
				}

			}
		};
		final ManagedService ms = new ManagedService() {

			@Override
			public void updated(final Dictionary<String, ?> properties) throws ConfigurationException {
    			updates.add(properties);
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					throw new RuntimeException(e);
				}
			}

		};
		this.registerService(
				SynchronousConfigurationListener.class.getName(),
				listener, null);
	    final Hashtable<String,Object> msProps = new Hashtable<String,Object>();
	    msProps.put(Constants.SERVICE_PID, pid);
		this.registerService(
				ManagedService.class.getName(),
				ms, msProps);
		// wait for managed service to get null
		barrier.await();
		barrier.reset();
		try {
			final Configuration conf = cm.getConfiguration(pid);
			cm.getConfiguration(pid);
			final long startLevel = conf.getChangeCount();
			final Hashtable<String,Object> newprops = new Hashtable<String,Object>();
			newprops.put("somekey", "somevalue");
			conf.update(newprops);
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 1);
			assertThat(events).hasSize(1);

			// wait for managed service
			barrier.await();
			barrier.reset();

			// check attributes
			assertThat(conf.getAttributes().isEmpty()).isTrue();
			
			// lock configuration
			conf.addAttributes(Configuration.ConfigurationAttribute.READ_ONLY);
			assertThat(conf.getAttributes().size()).isEqualTo(1);
			assertThat(conf.getAttributes().contains(Configuration.ConfigurationAttribute.READ_ONLY)).isTrue();

			// try to update
			try {
				conf.update(newprops);
				fail("");
			} catch ( final ReadOnlyConfigurationException e) {
				// expected
			}
			// nothing should have changed
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 1);
			assertThat(events.size()).isEqualTo(1);

            // unlock
			conf.removeAttributes(Configuration.ConfigurationAttribute.READ_ONLY);
			assertThat(conf.getAttributes().isEmpty()).isTrue();
			
            // try update 
			conf.update(newprops);
			assertThat(conf.getChangeCount()).isEqualTo(startLevel + 2);
			assertThat(events).hasSize(2);
			
			// check updates to managed service
			barrier.await();

			assertThat(updates.get(0)).isNull();
			assertThat(updates.get(1)).isNotNull();
			assertThat(updates.get(2)).isNotNull();
		} finally {
			this.unregisterService(listener);
			this.unregisterService(ms);
		}		
	}

	@Test
	public void testProvideImplementationCapability() throws Exception {
	    // get the bundle revision for the CA bundle by obtaining the CA service
		final ServiceReference<ConfigurationAdmin> srA = context.getServiceReference(ConfigurationAdmin.class);
		final BundleRevision rev = srA.getBundle().adapt(BundleRevision.class);

		final List<Capability> capabilities = rev.getCapabilities(ImplementationNamespace.IMPLEMENTATION_NAMESPACE);

		boolean found = false;

		for (final Capability capability : capabilities) {
			final Map<String, Object> attributes = capability.getAttributes();
			final String name = (String) attributes.get(ImplementationNamespace.IMPLEMENTATION_NAMESPACE);

			if ("osgi.cm".equals(name) ) {
				final Version version = (Version)attributes.get("version");

				if (version != null && version.equals(new Version("1.6.0"))) {
    				final Map<String, String> directives = capability.getDirectives();
	    			final List<String> packages = Arrays.asList(directives.get("uses").split(","));

					assertThat(packages.contains("org.osgi.service.cm")).isTrue();

					found = true;
				}
			}
		}

		assertThat(found).isTrue();
	}

	@Test
	public void testProvideServiceCapability() throws Exception {
	    // get the bundle revision for the CA bundle by obtaining the CA service
		final ServiceReference<ConfigurationAdmin> srA = context.getServiceReference(ConfigurationAdmin.class);
		final BundleRevision rev = srA.getBundle().adapt(BundleRevision.class);
		final List<Capability> capabilities = rev.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE);

		boolean found = false;

		for (final Capability capability : capabilities) {
			final Map<String, Object> attributes = capability.getAttributes();
			@SuppressWarnings("unchecked")
			final List<String> objectClasses = (List<String>) attributes.get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE);

			if ((objectClasses != null) && objectClasses.contains(ConfigurationAdmin.class.getName())) {
				final Map<String, String> directives = capability.getDirectives();
				final List<String> packages = Arrays.asList(directives.get("uses").split(","));

				assertThat(packages.contains("org.osgi.service.cm")).isTrue();

				found = true;
			}
		}

		assertThat(found).isTrue();
	}
}
