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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * @author Carsten Ziegeler, Adobe, Testing CM + Coordinator
 */
public class CMCoordinationTestCase extends CMTestSupport {


	private long				SIGNAL_WAITING_TIME;

	private ConfigurationAdmin cm;
	private PermissionAdmin permAdmin;
	private Bundle setAllPermissionBundle;

	private Set<String>								existingConfigs;

	@BeforeEach
	public void setUp() throws Exception {
		SIGNAL_WAITING_TIME = getLongProperty(
				"org.osgi.test.cases.cm.signal_waiting_time", 4000);
	    assignCm();

		if (System.getSecurityManager() != null) {
			permAdmin = getService(PermissionAdmin.class);
			setAllPermissionBundle = context.installBundle(
					getWebServer() + "setallpermission.jar");
		}

		// existing configurations
        Configuration[] configs = cm.listConfigurations(null);
		existingConfigs = new HashSet<>();
        if (configs != null) {
            for (int i = 0; i < configs.length; i++) {
                Configuration config = configs[i];
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

        unregisterAllServices();
		ungetService(cm);
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

	private void sleep() {
		try {
			Thread.sleep(SIGNAL_WAITING_TIME);
		} catch (InterruptedException e) {
			// ignore
		}
	}

    private void assignCm() {
        cm = getService(ConfigurationAdmin.class);
    }

	/**
	 * Removes any configurations made by this bundle.
	 */
	private void cleanCM(Set<String> existingConfigs) throws Exception {
        if (cm != null) {
            Configuration[] configs = cm.listConfigurations(null);
            if (configs != null) {
                for (int i = 0; i < configs.length; i++) {
                    Configuration config = configs[i];
                    if (!existingConfigs.contains(config.getPid())) {
                        config.delete();
                    }
                }
            }
        }
	}

	/**
	 * TEST CASES
	 */

	/**
	 * This test tests synchronous and plain configuration listeners. It
	 * <ol>
	 * <li>starts a coordination
	 * <li>adds a synchronous listener
	 * <li>adds a plain listener
	 * <li>creates/updates/deletes a configuration
	 * <li>ends the coordination
	 * <li>checks that synchronous listener is informed immediately
	 * <li>plain listener is informed once coordination ends
	 * </ol>
	 *
	 * @throws Exception
	 * @since 1.6
	 */
	@Test
	public void testListeners() throws Exception {
		// start a coordination
		final Coordinator c = this.getService(Coordinator.class);
		final Coordination coord = c.begin("cm-test", 0);
		final List<ConfigurationEvent> syncList = new ArrayList<>();
		final List<ConfigurationEvent> plainList = new ArrayList<>();

		final String pid = this.getClass().getName() + ".testpid";

		try {
			// add a synchronous listener

			this.registerService(
					SynchronousConfigurationListener.class.getName(),
					new SynchronousConfigurationListener() {

						@Override
						public void configurationEvent(
								ConfigurationEvent event) {
							if (pid.equals(event.getPid())) {
								syncList.add(event);
							}
						}
					}, null);

			// add a plain listener

			this.registerService(
					ConfigurationListener.class.getName(),
					new ConfigurationListener() {

						@Override
						public void configurationEvent(
								ConfigurationEvent event) {
							if (pid.equals(event.getPid())) {
								plainList.add(event);
							}
						}
					}, null);

			// create the configuration
			final Dictionary<String,Object> props = new Hashtable<>();
			props.put("key", "value");

			final Configuration conf = this.cm.getConfiguration(pid);
			conf.update(props);

			assertThat(syncList).hasSize(1);
			assertThat(syncList.get(0).getType()).isEqualTo(ConfigurationEvent.CM_UPDATED);

			assertThat(plainList).hasSize(0);

			sleep();
			assertThat(plainList).hasSize(0);

			// update configuration
			props.put("key2", "value2");
			conf.update(props);

			assertThat(syncList).hasSize(2);
			assertThat(syncList.get(1).getType()).isEqualTo(ConfigurationEvent.CM_UPDATED);
			assertThat(plainList).hasSize(0);

			sleep();
			assertThat(plainList).hasSize(0);

			// delete configuration
			conf.delete();
			assertThat(syncList).hasSize(3);
			assertThat(syncList.get(2).getType()).isEqualTo(ConfigurationEvent.CM_DELETED);
			assertThat(plainList).hasSize(0);

			sleep();
			assertThat(plainList).hasSize(0);
		} finally {
			coord.end();
		}

		// wait and verify listener
		sleep();
		assertThat(plainList).hasSize(3);
		assertThat(plainList.get(0).getType()).isEqualTo(ConfigurationEvent.CM_UPDATED);
		assertThat(plainList.get(1).getType()).isEqualTo(ConfigurationEvent.CM_UPDATED);
		assertThat(plainList.get(2).getType()).isEqualTo(ConfigurationEvent.CM_DELETED);

	}

	// Test opposite of:
	// If a Managed Service is registered and no configuration information is
	// available or the configuration is not visible then the Configuration
	// Admin service must call updated(Dictionary) with a null parameter.
	//
	// Configuration exists before ManagedService is registered. No update(null)
	// use cfg-dictionary directly

	@Test
	public void test_deliver_existing_Configuration_to_later_registered_ManagedService()
			throws Exception {
		// start a coordination
		final Coordinator c = this.getService(Coordinator.class);
		final Coordination coord = c.begin("cm-test1", 0);
		final List<String> events = new ArrayList<>();

		final String pid = this.getClass().getName() + ".mstestpid2";

		try {
			// create the configuration
			final Dictionary<String,Object> props = new Hashtable<>();
			props.put("key", "value");

			final Configuration conf = this.cm.getConfiguration(pid);
			conf.update(props);

			// add managed service
			final Dictionary<String,Object> msProps = new Hashtable<>();
			msProps.put(Constants.SERVICE_PID, pid);

			this.registerService(ManagedService.class.getName(),
					new ManagedService() {

						@Override
						public void updated(Dictionary<String, ? > properties)
								throws ConfigurationException {
							events.add((String) properties.get("key2"));
						}

					}, msProps);

			sleep();
			assertThat(events.size()).isEqualTo(0);

			// update configuration
			props.put("key2", "value2");
			conf.update(props);

			sleep();
			assertThat(events.size()).isEqualTo(0);
		} finally {
			coord.end();
		}

		// wait and verify listener
		sleep();

		assertThat(events).hasSize(1);
		// last update
		assertThat(events.get(0)).isEqualTo("value2");
	}

	@Test
	public void test_create_managedservice_delete() throws Exception {
		ConfigurationAdmin cm = getService(ConfigurationAdmin.class);
		Coordinator c = getService(Coordinator.class);
		Coordination coord = c.begin("cm-test1", 0);
		final List<Boolean> events = new ArrayList<>();

		String pid = getClass().getName() + ".mstestpid3";

		try {
			// create the configuration
			Dictionary<String,Object> props = new Hashtable<>();
			props.put("key", "value");

			Configuration conf = cm.getConfiguration(pid);
			conf.update(props);

			// add managed service
			Dictionary<String,Object> msProps = new Hashtable<>();
			msProps.put(Constants.SERVICE_PID, pid);

			this.registerService(ManagedService.class.getName(),
					new ManagedService() {

						@Override
						public void updated(Dictionary<String, ? > properties)
								throws ConfigurationException {
							events.add(properties != null);
						}

					}, msProps);

			// update configuration
			props.put("key2", "value2");
			conf.update(props);

			// delete configuration
			conf.delete();

			assertThat(events.size()).isEqualTo(0);
		} finally {
			coord.end();
		}

		// wait and verify listener
		sleep();

		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isFalse();// no configuration, update with null
	}

	@Test
	public void testCoordinatedConfigurationOnBeforeRegisteredManagedService()
			throws Exception {

		// written by the Configuration Admin thread, read by the test
		final List<Boolean> events = new CopyOnWriteArrayList<>();
		// completed by the first callback only
		final CompletableFuture<Boolean> initial = new CompletableFuture<>();
		final String pid = this.getClass().getName() + ".mstestpid4";
		// add managed service
		final Dictionary<String,Object> msProps = new Hashtable<>();
		msProps.put(Constants.SERVICE_PID, pid);

		this.registerService(ManagedService.class.getName(),
				new ManagedService() {

					@Override
					public void updated(Dictionary<String, ? > properties)
							throws ConfigurationException {
						events.add(properties != null);
						initial.complete(properties != null);
					}

				}, msProps);

		// the Managed Service is registered before the coordination: its
		// asynchronous updated(null) comes first, there is no configuration
		assertThat(initial)
				.succeedsWithin(Duration.ofMillis(SIGNAL_WAITING_TIME))
				.isEqualTo(false);

		// start a coordination
		final Coordinator c = this.getService(Coordinator.class);
		final Coordination coord = c.begin("cm-test2", 0);

		try {

			// create the configuration
			final Dictionary<String,Object> props = new Hashtable<>();
			props.put("key", "value");

			final Configuration conf = this.cm.getConfiguration(pid);
			conf.update(props);

			sleep();
			assertThat(events).hasSize(1);// update with null because
											// registered before coordination

			// update configuration
			props.put("key2", "value2");
			conf.update(props);

			sleep();
			assertThat(events).hasSize(1);

			// delete configuration
			conf.delete();

			sleep();
			assertThat(events).hasSize(1);
		} finally {
			coord.end();
		}

		// wait and verify listener
		sleep();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isFalse();
	}

	/**
	 * This test tests a managed service. It
	 * <ol>
	 * <li>starts a coordination
	 * <li>adds a synchronous listener
	 * <li>adds a plain listener
	 * <li>creates/updates/deletes a configuration
	 * <li>ends the coordination
	 * <li>checks that synchronous listener is informed immediately
	 * <li>plain listener is informed once coordination ends
	 * </ol>
	 *
	 * @throws Exception
	 * @since 1.6
	 */
	@Test
	public void testManagedService() throws Exception {
		// start a coordination
		final Coordinator c = this.getService(Coordinator.class);
		final Coordination coord = c.begin("cm-test", 0);
		final List<Boolean> events = new ArrayList<>();

		final String pid = this.getClass().getName() + ".mstestpid";

		boolean checkpoint = false;
		try {
			// add managed service
			final Dictionary<String,Object> msProps = new Hashtable<>();
			msProps.put(Constants.SERVICE_PID, pid);

			this.registerService(ManagedService.class.getName(),
					new ManagedService() {

						@Override
						public void updated(Dictionary<String, ? > properties)
								throws ConfigurationException {
							events.add(properties != null);
						}

					}, msProps);


			// create the configuration
			final Dictionary<String,Object> props = new Hashtable<>();
			props.put("key", "value");

			final Configuration conf = this.cm.getConfiguration(pid);
			conf.update(props);

			sleep();
			assertThat(events.size()).isEqualTo(0);

			// update configuration
			props.put("key2", "value2");
			conf.update(props);

			sleep();
			assertThat(events.size()).isEqualTo(0);

			// delete configuration
			conf.delete();

			sleep();
			assertThat(events.size()).isEqualTo(0);
			checkpoint = true;
		} catch (Exception e) {
			e.printStackTrace();
			c.fail(e);
		} finally {
			coord.end();
		}
		assertThat(checkpoint).isTrue();
		// wait and verify listener
		sleep();
		assertThat(events).hasSize(1);
		assertThat(events.get(0)).isFalse();
	}
}
