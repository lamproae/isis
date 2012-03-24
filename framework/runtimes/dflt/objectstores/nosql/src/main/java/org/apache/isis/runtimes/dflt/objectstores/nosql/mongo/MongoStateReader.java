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

package org.apache.isis.runtimes.dflt.objectstores.nosql.mongo;

import java.util.List;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import org.apache.log4j.Logger;

import org.apache.isis.core.commons.exceptions.UnexpectedCallException;
import org.apache.isis.runtimes.dflt.objectstores.nosql.StateReader;
import org.apache.isis.runtimes.dflt.runtime.persistence.ObjectNotFoundException;

public class MongoStateReader implements StateReader, PropertyNames {
     private static final Logger LOG = Logger.getLogger(MongoStateReader.class);
    private final DBObject instance;

    public MongoStateReader(final DB db, final String specName, final String key) {
        final DBCollection instances = db.getCollection(specName);
        instance = instances.findOne(key);
        if (instance == null) {
            throw new ObjectNotFoundException(key);
        }
        LOG.debug("loading " + instance);
    }

    public MongoStateReader(final DBObject instance) {
        this.instance = instance;
        LOG.debug("loading " + instance);
    }

    @Override
    public long readLongField(final String id) {
        final Object value = instance.get(id);
        if (value == null || value.equals("null")) {
            return 0;
        } else {
            return Long.valueOf((String) value);
        }
    }

    @Override
    public String readField(final String name) {
        final Object value = instance.get(name);
        if (value == null || value.equals("null")) {
            return null;
        } else {
            return (String) value;
        }
    }

    @Override
    public String readEncrytionType() {
        return (String) instance.get(ENCRYPT);
    }

    @Override
    public String readObjectType() {
        return (String) instance.get(TYPE);
    }

    @Override
    public String readId() {
        return readField(ID);
    }

    @Override
    public String readVersion() {
        return readField(VERSION);
    }

    @Override
    public String readUser() {
        return readField(USER);
    }

    @Override
    public String readTime() {
        return readField(TIME);
    }

    @Override
    public StateReader readAggregate(final String id) {
        throw new UnexpectedCallException();
    }

    @Override
    public List<StateReader> readCollection(final String id) {
        throw new UnexpectedCallException();
    }

}
