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
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.PropertyPermission;

import org.junit.jupiter.api.Test;
import org.osgi.service.cm.ConfigurationPermission;

public class ConfigurationPermissionTests {

	@Test
	public void testInvalid() {
		invalidConfigurationPermission("a/b/c", "x");
		invalidConfigurationPermission("a/b/c", "   configure  ,  x   ");
		invalidConfigurationPermission("a/b/c", "");
		invalidConfigurationPermission("a/b/c", "      ");
		invalidConfigurationPermission("a/b/c", null);
		invalidConfigurationPermission("a/b/c", ",");
		invalidConfigurationPermission("a/b/c", ",xxx");
		invalidConfigurationPermission("a/b/c", "xxx,");
		invalidConfigurationPermission("a/b/c", "configure,");
		invalidConfigurationPermission("a/b/c", "target,   ");
		invalidConfigurationPermission("a/b/c", "attribute, ");
		invalidConfigurationPermission("a/b/c", "configureme,");
		invalidConfigurationPermission("a/b/c", "targetme,");
		invalidConfigurationPermission("a/b/c", "attributeme,");
		invalidConfigurationPermission("a/b/c", ",configure");
		invalidConfigurationPermission("a/b/c", ",target");
		invalidConfigurationPermission("a/b/c", ",attribute");
		invalidConfigurationPermission("a/b/c", "   configureme   ");
		invalidConfigurationPermission("a/b/c", "   targetme     ");
		invalidConfigurationPermission("a/b/c", "   attributeme     ");
		invalidConfigurationPermission("a/b/c", "   configur");
		invalidConfigurationPermission("a/b/c", "   targe");
		invalidConfigurationPermission("a/b/c", "   attribut");
		invalidConfigurationPermission("", "   target");
	}

	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read"); 

		ConfigurationPermission p10 = new ConfigurationPermission(
				"com/foo/service1", "    CONFIGURE,target , attribute   ");
		ConfigurationPermission p11 = new ConfigurationPermission(
				"com/foo/service1",
				"    CONFIGURE,target   ");
		ConfigurationPermission p12 = new ConfigurationPermission(
				"com/foo/service1",
 "TARGET  ,   configure");
		ConfigurationPermission p13 = new ConfigurationPermission(
				"com/foo/service1",
 "tarGET   ");
		ConfigurationPermission p14 = new ConfigurationPermission(
				"com/foo/service1",
 "    Configure    ");
		ConfigurationPermission p15 = new ConfigurationPermission(
				"com/foo/service1", " aTTribute ");

		assertThat(p10).implies(p10);
		assertThat(p10).implies(p11);
		assertThat(p10).implies(p12);
		assertThat(p10).implies(p13);
		assertThat(p10).implies(p14);
		assertThat(p10).implies(p15);

		assertThat(p11).implies(p11);
		assertThat(p11).implies(p12);
		assertThat(p11).implies(p13);
		assertThat(p11).implies(p14);

		assertThat(p12).implies(p11);
		assertThat(p12).implies(p12);
		assertThat(p12).implies(p13);
		assertThat(p12).implies(p14);

		assertThat(p13).implies(p13);
		assertThat(p14).implies(p14);

		assertThat(p13).doesNotImply(p10);
		assertThat(p13).doesNotImply(p11);
		assertThat(p13).doesNotImply(p14);
		assertThat(p13).doesNotImply(p15);

		assertThat(p14).doesNotImply(p10);
		assertThat(p14).doesNotImply(p11);
		assertThat(p14).doesNotImply(p13);
		assertThat(p14).doesNotImply(p15);

		assertThat(p15).doesNotImply(p10);
		assertThat(p15).doesNotImply(p11);
		assertThat(p15).doesNotImply(p13);
		assertThat(p15).doesNotImply(p14);

		assertThat(p11).doesNotImply(op);

		assertThat(p10).isEquivalentTo(p10);
		assertThat(p11).isEquivalentTo(p11);
		assertThat(p11).isEquivalentTo(p12);
		assertThat(p12).isEquivalentTo(p11);
		assertThat(p12).isEquivalentTo(p12);
		assertThat(p13).isEquivalentTo(p13);
		assertThat(p14).isEquivalentTo(p14);

		assertThat(p10).isNotEquivalentTo(p11);
		assertThat(p10).isNotEquivalentTo(p12);
		assertThat(p10).isNotEquivalentTo(p13);
		assertThat(p10).isNotEquivalentTo(p14);
		assertThat(p10).isNotEquivalentTo(p15);
		assertThat(p11).isNotEquivalentTo(p10);
		assertThat(p11).isNotEquivalentTo(p13);
		assertThat(p11).isNotEquivalentTo(p14);
		assertThat(p11).isNotEquivalentTo(p15);
		assertThat(p12).isNotEquivalentTo(p10);
		assertThat(p12).isNotEquivalentTo(p13);
		assertThat(p12).isNotEquivalentTo(p14);
		assertThat(p12).isNotEquivalentTo(p15);
		assertThat(p13).isNotEquivalentTo(p10);
		assertThat(p13).isNotEquivalentTo(p11);
		assertThat(p13).isNotEquivalentTo(p12);
		assertThat(p13).isNotEquivalentTo(p14);
		assertThat(p13).isNotEquivalentTo(p15);
		assertThat(p14).isNotEquivalentTo(p10);
		assertThat(p14).isNotEquivalentTo(p11);
		assertThat(p14).isNotEquivalentTo(p12);
		assertThat(p14).isNotEquivalentTo(p13);
		assertThat(p14).isNotEquivalentTo(p15);
		assertThat(p15).isNotEquivalentTo(p10);
		assertThat(p15).isNotEquivalentTo(p11);
		assertThat(p15).isNotEquivalentTo(p12);
		assertThat(p15).isNotEquivalentTo(p13);
		assertThat(p15).isNotEquivalentTo(p14);

		PermissionCollection pc = p13.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p10);

		assertThat(pc).accepts(p14);
		assertThat(pc).implies(p14);
		assertThat(pc).doesNotImply(p10);
		assertThat(pc).doesNotImply(p11);
		assertThat(pc).doesNotImply(p12);
		assertThat(pc).doesNotImply(p13);
		assertThat(pc).doesNotImply(p15);

		assertThat(pc).accepts(p13);
		assertThat(pc).doesNotImply(p10);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).doesNotImply(p15);

		assertThat(pc).accepts(p15);
		assertThat(pc).implies(p10);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);

		assertThat(pc).rejects(op);

		pc = p13.newPermissionCollection();

		assertThat(pc).accepts(p13);
		assertThat(pc).implies(p13);
		assertThat(pc).doesNotImply(p11);
		assertThat(pc).doesNotImply(p12);
		assertThat(pc).doesNotImply(p14);

		assertThat(pc).accepts(p14);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);

		pc = p10.newPermissionCollection();

		assertThat(pc).accepts(p10);
		assertThat(pc).implies(p10);
		assertThat(pc).implies(p11);
		assertThat(pc).implies(p12);
		assertThat(pc).implies(p13);
		assertThat(pc).implies(p14);
		assertThat(pc).implies(p15);

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p10).isSerializable();
		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p13).isSerializable();
		assertThat(p14).isSerializable();
		assertThat(p15).isSerializable();
	}

	@Test
	public void testNames() {
		ConfigurationPermission p21 = new ConfigurationPermission(
				"com/foo/service2",
 "configure");
		ConfigurationPermission p22 = new ConfigurationPermission("com/foo/*",
				"configure");
		ConfigurationPermission p23 = new ConfigurationPermission("com/*",
				"configure");
		ConfigurationPermission p24 = new ConfigurationPermission("*",
				"configure");

		assertThat(p21).implies(p21);
		assertThat(p22).implies(p21);
		assertThat(p23).implies(p21);
		assertThat(p24).implies(p21);

		assertThat(p22).implies(p22);
		assertThat(p23).implies(p22);
		assertThat(p24).implies(p22);

		assertThat(p23).implies(p23);
		assertThat(p24).implies(p23);

		assertThat(p24).implies(p24);

		assertThat(p21).doesNotImply(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);

		assertThat(p22).doesNotImply(p23);
		assertThat(p22).doesNotImply(p24);

		assertThat(p23).doesNotImply(p24);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		pc = p22.newPermissionCollection();

		assertThat(pc).accepts(p22);
		assertThat(pc).isSerializable();
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).isSerializable();
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);

		pc = p24.newPermissionCollection();

		assertThat(pc).accepts(p24);
		assertThat(pc).isSerializable();
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		assertThat(p21).isSerializable();
		assertThat(p22).isSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isSerializable();
	}
	
	@Test
	public void testWildcardNames() {
		ConfigurationPermission p21 = new ConfigurationPermission(
				"com/foo/service2", "configure");
		ConfigurationPermission p22 = new ConfigurationPermission("com/foo/*",
				"configure");
		ConfigurationPermission p23 = new ConfigurationPermission(
				"com/*/service2", "configure");
		ConfigurationPermission p24 = new ConfigurationPermission("*/service2",
				"configure");

		assertThat(p21).implies(p21);
		assertThat(p22).implies(p21);
		assertThat(p23).implies(p21);
		assertThat(p24).implies(p21);

		assertThat(p22).implies(p22);
		assertThat(p23).doesNotImply(p22);
		assertThat(p24).doesNotImply(p22);

		assertThat(p23).implies(p23);
		assertThat(p24).implies(p23);

		assertThat(p24).implies(p24);

		assertThat(p21).doesNotImply(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);

		assertThat(p22).doesNotImply(p23);
		assertThat(p22).doesNotImply(p24);

		assertThat(p23).doesNotImply(p24);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		pc = p22.newPermissionCollection();

		assertThat(pc).accepts(p22);
		assertThat(pc).implies(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);

		pc = p24.newPermissionCollection();

		assertThat(pc).accepts(p24);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		assertThat(p21).isSerializable();
		assertThat(p22).isSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isSerializable();
	}

	@Test
	public void testActionImplications() {
		ConfigurationPermission target = new ConfigurationPermission("*",
				"target");
		ConfigurationPermission configure = new ConfigurationPermission("*",
				"configure");
		ConfigurationPermission attribute = new ConfigurationPermission("*",
				"attribute");

		assertThat(target).implies(target);
		assertThat(target).doesNotImply(configure);
		assertThat(target).doesNotImply(attribute);

		assertThat(configure).doesNotImply(target);
		assertThat(configure).implies(configure);
		assertThat(configure).doesNotImply(attribute);

		assertThat(attribute).doesNotImply(target);
		assertThat(attribute).doesNotImply(configure);
		assertThat(attribute).implies(attribute);
	}

	private void invalidConfigurationPermission(String name, String actions) {
		try {
			ConfigurationPermission p = new ConfigurationPermission(name,
					actions);
			fail(p + " created with invalid actions"); 
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
}
