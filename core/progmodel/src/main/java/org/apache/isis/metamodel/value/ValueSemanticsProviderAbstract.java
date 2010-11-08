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


package org.apache.isis.metamodel.value;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.isis.applib.adapters.DefaultsProvider;
import org.apache.isis.applib.adapters.EncoderDecoder;
import org.apache.isis.applib.adapters.Parser;
import org.apache.isis.applib.adapters.ValueSemanticsProvider;
import org.apache.isis.applib.clock.Clock;
import org.apache.isis.core.commons.exceptions.UnknownTypeException;
import org.apache.isis.core.commons.lang.LocaleUtils;
import org.apache.isis.metamodel.adapter.InvalidEntryException;
import org.apache.isis.metamodel.adapter.ObjectAdapter;
import org.apache.isis.metamodel.config.ConfigurationConstants;
import org.apache.isis.metamodel.config.IsisConfiguration;
import org.apache.isis.metamodel.facets.Facet;
import org.apache.isis.metamodel.facets.FacetAbstract;
import org.apache.isis.metamodel.facets.FacetHolder;
import org.apache.isis.metamodel.facets.properties.defaults.PropertyDefaultFacet;
import org.apache.isis.metamodel.runtimecontext.RuntimeContext;
import org.apache.isis.metamodel.spec.ObjectSpecification;
import org.apache.isis.metamodel.specloader.SpecificationLoader;


public abstract class ValueSemanticsProviderAbstract extends FacetAbstract implements ValueSemanticsProvider, EncoderDecoder,
        Parser, DefaultsProvider {

    private final Class<?> adaptedClass;
    private final int typicalLength;
    private final boolean immutable;
    private final boolean equalByContent;
    private final Object defaultValue;

    /**
     * Lazily looked up per {@link #getSpecification()}.
     */
    private ObjectSpecification specification;
    
	private final IsisConfiguration configuration;
	private final SpecificationLoader specificationLoader;
	private final RuntimeContext runtimeContext;

    public ValueSemanticsProviderAbstract(
            final Class<? extends Facet> adapterFacetType,
            final FacetHolder holder,
            final Class<?> adaptedClass,
            final int typicalLength,
            final boolean immutable,
            final boolean equalByContent,
            final Object defaultValue, 
            final IsisConfiguration configuration, 
            final SpecificationLoader specificationLoader, 
            final RuntimeContext runtimeContext) {
        super(adapterFacetType, holder, false);
        this.adaptedClass = adaptedClass;
        this.typicalLength = typicalLength;
        this.immutable = immutable;
        this.equalByContent = equalByContent;
        this.defaultValue = defaultValue;
        
        this.configuration = configuration;
        this.specificationLoader = specificationLoader;
        this.runtimeContext = runtimeContext;
    }

    public ObjectSpecification getSpecification() {
        if (specification == null) {
            specification = getSpecificationLoader().loadSpecification(getAdaptedClass());
        }
        return specification;
    }


    /**
     * The underlying class that has been adapted.
     * 
     * <p>
     * Used to determine whether an empty string can be parsed, (for primitive types a non-null entry is
     * required, see {@link #mustHaveEntry()}), and potentially useful for debugging.
     */
    public final Class<?> getAdaptedClass() {
        return adaptedClass;
    }

    /**
     * We don't replace any (none no-op) facets.
     * 
     * <p>
     * For example, if there is already a {@link PropertyDefaultFacet} then we shouldn't replace it.
     */
    @Override
    public boolean alwaysReplace() {
        return false;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // ValueSemanticsProvider implementation
    // ///////////////////////////////////////////////////////////////////////////

    public EncoderDecoder getEncoderDecoder() {
        return this;
    }

    public Parser getParser() {
        return this;
    }

    public DefaultsProvider getDefaultsProvider() {
        return this;
    }

    public boolean isEqualByContent() {
        return equalByContent;
    }

    public boolean isImmutable() {
        return immutable;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // Parser implementation
    // ///////////////////////////////////////////////////////////////////////////

    public Object parseTextEntry(final Object context, final String entry) {
        if (entry == null) {
            throw new IllegalArgumentException();
        }
        if (entry.trim().equals("")) {
            if (mustHaveEntry()) {
                throw new InvalidEntryException("An entry is required");
            } else {
                return null;
            }
        }
        return doParse(context, entry);
    }

    /**
     * @param original
     *            - the underlying object, or <tt>null</tt>.
     * @param entry
     *            - the proposed new object, as a string representation to be parsed
     */
    protected abstract Object doParse(Object original, String entry);

    /**
     * Whether a non-null entry is required, used by parsing.
     * 
     * <p>
     * Adapters for primitives will return <tt>true</tt>.
     */
    private final boolean mustHaveEntry() {
        return adaptedClass.isPrimitive();
    }

    public String displayTitleOf(final Object object) {
        if (object == null) {
            return "";
        }
        return titleString(object);
    }

    public String displayTitleOf(final Object object, final String usingMask) {
        if (object == null) {
            return "";
        }
        return titleStringWithMask(object, usingMask);
    }

    /**
     * Defaults to {@link #displayTitleOf(Object)}.
     */
    public String parseableTitleOf(final Object existing) {
        return displayTitleOf(existing);
    }

    protected String titleString(final Format formatter, final Object object) {
        return object == null ? "" : formatter.format(object);
    }

    /**
     * Return a string representation of aforesaid object.
     */
    protected abstract String titleString(Object object);

    public abstract String titleStringWithMask(final Object value, final String usingMask);

    public final int typicalLength() {
        return this.typicalLength;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // DefaultsProvider implementation
    // ///////////////////////////////////////////////////////////////////////////

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    // ///////////////////////////////////////////////////////////////////////////
    // EncoderDecoder implementation
    // ///////////////////////////////////////////////////////////////////////////

    public String toEncodedString(final Object object) {
        return doEncode(object);
    }

    public Object fromEncodedString(final String data) {
        return doRestore(data);
    }

    /**
     * Hook method to perform the actual encoding.
     */
    protected abstract String doEncode(Object object);

    /**
     * Hook method to perform the actual restoring.
     */
    protected abstract Object doRestore(String data);
    

    
    // ///////////////////////////////////////////////////////////////////////////
    // Helper: Locale handling
    // ///////////////////////////////////////////////////////////////////////////

    protected NumberFormat determineNumberFormat(String suffix) {
        final String formatRequired = getConfiguration().getString(ConfigurationConstants.ROOT + suffix);
        if (formatRequired != null) {
            return new DecimalFormat(formatRequired);
        } else {
            return NumberFormat.getNumberInstance(findLocale());
        }
    }

    private Locale findLocale() {
        final String localeStr = getConfiguration().getString(
                ConfigurationConstants.ROOT + "locale");

        Locale findLocale = LocaleUtils.findLocale(localeStr);
        return findLocale != null? findLocale: Locale.getDefault();
    }


    
    ////////////////////////////////////////////////////////////
    // Helper: createAdapter
    ////////////////////////////////////////////////////////////

    protected ObjectAdapter createAdapter(final Class<?> type, final Object object) {
	    final ObjectSpecification specification = getSpecificationLoader().loadSpecification(type);
	    if (specification.isNotCollection()) {
	        return getRuntimeContext().adapterFor(object);
	    } else {
	        throw new UnknownTypeException("not an object, is this a collection?");
	    }
	}


    ////////////////////////////////////////////////////////////
    // Dependencies (from constructor)
    ////////////////////////////////////////////////////////////

    protected IsisConfiguration getConfiguration() {
        return configuration;
    }

    protected SpecificationLoader getSpecificationLoader() {
        return specificationLoader;
    }

    protected RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }

    ////////////////////////////////////////////////////////////
    // Dependencies (from singleton)
    ////////////////////////////////////////////////////////////

    protected static Clock getClock() {
        return Clock.getInstance();
    }

}
