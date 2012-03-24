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
package org.apache.isis.viewer.json.viewer.util;

import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.adapter.oid.stringable.directly.OidWithSpecification;
import org.apache.isis.viewer.json.applib.RestfulResponse.HttpStatusCode;
import org.apache.isis.viewer.json.viewer.JsonApplicationException;
import org.apache.isis.viewer.json.viewer.ResourceContext;

public final class OidUtils {

    private OidUtils() {
    }

    public static ObjectAdapter getObjectAdapter(final ResourceContext resourceContext, final String oidEncodedStr) {
        final String oidStr = UrlDecoderUtils.urlDecode(oidEncodedStr);
        final Oid oid = resourceContext.getOidStringifier().deString(oidStr);
        final ObjectAdapter adapterFor = resourceContext.getObjectAdapterLookup().getAdapterFor(oid);
        if(adapterFor != null) {
            return adapterFor;
        }
        if(!(oid instanceof OidWithSpecification)) {
            throw JsonApplicationException.create(HttpStatusCode.NOT_IMPLEMENTED, "JSON viewer only supports objectstores that have Oids that are self-describing (implement OidWithSpecification)");
        }
        final OidWithSpecification oidWithSpec = (OidWithSpecification) oid;
        return resourceContext.getPersistenceSession().recreateAdapter(oidWithSpec);
    }

    public static String getOidStr(final ResourceContext resourceContext, final ObjectAdapter objectAdapter) {
        final Oid oid = objectAdapter.getOid();
        return oid != null ? resourceContext.getOidStringifier().enString(oid) : null;
    }

}
