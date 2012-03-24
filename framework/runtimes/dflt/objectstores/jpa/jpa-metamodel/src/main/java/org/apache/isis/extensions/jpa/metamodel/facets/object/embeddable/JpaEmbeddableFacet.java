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
package org.apache.isis.extensions.jpa.metamodel.facets.object.embeddable;

import javax.persistence.Embeddable;

import org.apache.isis.core.metamodel.facets.MarkerFacet;

/**
 * Corresponds to annotating the class with {@link Embeddable}.
 * <p>
 * The JPA {@link Embeddable} annotation has no attributes. However, in addition
 * to this facet it does also implicitly map to
 * {@link AggregatedFacetDerivedFromJpaEmbeddableAnnotation}.
 */
public interface JpaEmbeddableFacet extends MarkerFacet {

}
