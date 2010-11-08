/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */


package org.apache.isis.viewer.wicket.viewer.registries.pages;

import java.util.Map;

import org.apache.isis.core.commons.ensure.Ensure;
import org.apache.isis.viewer.wicket.model.util.Classes;
import org.apache.isis.viewer.wicket.ui.pages.PageClassList;
import org.apache.isis.viewer.wicket.ui.pages.PageClassRegistry;
import org.apache.isis.viewer.wicket.ui.pages.PageRegistrySpi;
import org.apache.isis.viewer.wicket.ui.pages.PageType;
import org.apache.wicket.Page;
import org.hamcrest.Matcher;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Default implementation of {@link PageClassRegistry}; just delegates to an underlying
 * {@link PageClassList}.
 */
@Singleton
public class PageClassRegistryDefault implements PageClassRegistry, PageRegistrySpi {

	private Map<PageType, Class<? extends Page>> pagesByType = Maps.newHashMap();
	
	/**
	 * {@link Inject}ed in {@link #PageClassRegistryDefault(PageClassList) constructor}.
	 */
	@SuppressWarnings("unused")
	private final PageClassList pageClassList;
	
	@Inject
	public PageClassRegistryDefault(final PageClassList pageClassList) {
		this.pageClassList = pageClassList;
		pageClassList.registerPages(this);
		ensureAllPageTypesRegistered();
	}

	private void ensureAllPageTypesRegistered() {
		for (PageType pageType : PageType.values()) {
			if (getPageClass(pageType) == null) {
				throw new IllegalStateException("No page registered for " + pageType);
			}
		}
	}
	
	///////////////////////////////////////////////////////////
	// API
	///////////////////////////////////////////////////////////

	@Override
	public final Class<? extends Page> getPageClass(PageType pageType) {
		return pagesByType.get(pageType);
	}
	
	///////////////////////////////////////////////////////////
	// API
	///////////////////////////////////////////////////////////

	public final void registerPage(PageType pageType, Class<? extends Page> pageClass) {
		ensureThatArg(pageClass, Classes.isSubclassOf(pageType.getPageClass()));
		pagesByType.put(pageType, pageClass);
	}

	///////////////////////////////////////////////////////////
	// Helpers
	///////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	private static void ensureThatArg(Class<? extends Page> cls, Matcher clsMatcher) {
		Ensure.ensureThatArg(cls, clsMatcher);		
	}


}
