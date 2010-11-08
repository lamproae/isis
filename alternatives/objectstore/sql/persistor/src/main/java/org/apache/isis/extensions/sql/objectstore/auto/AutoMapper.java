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


package org.apache.isis.extensions.sql.objectstore.auto;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.isis.core.commons.debug.DebugInfo;
import org.apache.isis.core.commons.debug.DebugString;
import org.apache.isis.metamodel.adapter.ObjectAdapter;
import org.apache.isis.metamodel.adapter.ResolveState;
import org.apache.isis.metamodel.adapter.oid.Oid;
import org.apache.isis.metamodel.adapter.version.SerialNumberVersion;
import org.apache.isis.metamodel.spec.ObjectSpecification;
import org.apache.isis.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.extensions.sql.objectstore.CollectionMapper;
import org.apache.isis.extensions.sql.objectstore.DatabaseConnector;
import org.apache.isis.extensions.sql.objectstore.FieldMappingLookup;
import org.apache.isis.extensions.sql.objectstore.IdMapping;
import org.apache.isis.extensions.sql.objectstore.ObjectMapping;
import org.apache.isis.extensions.sql.objectstore.ObjectMappingLookup;
import org.apache.isis.extensions.sql.objectstore.Results;
import org.apache.isis.extensions.sql.objectstore.SqlObjectStoreException;
import org.apache.isis.extensions.sql.objectstore.TitleMapping;
import org.apache.isis.extensions.sql.objectstore.VersionMapping;
import org.apache.isis.extensions.sql.objectstore.mapping.FieldMapping;
import org.apache.isis.runtime.persistence.ConcurrencyException;
import org.apache.isis.runtime.persistence.ObjectNotFoundException;
import org.apache.isis.runtime.persistence.PersistorUtil;
import org.apache.isis.runtime.persistence.query.PersistenceQueryFindByPattern;


public class AutoMapper extends AbstractAutoMapper implements ObjectMapping, DebugInfo {
    private static final Logger LOG = Logger.getLogger(AutoMapper.class);
    private static final int MAX_INSTANCES = 100;
    private final IdMapping idMapping;
    private final VersionMapping versionMapping;
    private final TitleMapping titleMapping;
    
    public AutoMapper(final String className, final String parameterBase, FieldMappingLookup lookup, ObjectMappingLookup objectMapperLookup) {
        super(className, parameterBase, lookup, objectMapperLookup);
        idMapping = lookup.createIdMapping();
        versionMapping = lookup.createVersionMapping();
        titleMapping = lookup.createTitleMapping();
    }

    public void createObject(final DatabaseConnector connector, final ObjectAdapter object) {
        int versionSequence = 1;
        SerialNumberVersion version = createVersion(versionSequence);

        StringBuffer sql = new StringBuffer();
        sql.append("insert into " + table + " (");
        idMapping.appendColumnNames(sql);
        sql.append(", ");
        sql.append(columnList());
        sql.append(", ");
        titleMapping.appendColumnNames(sql);
        sql.append(", ");
        sql.append(versionMapping.insertColumns());
        sql.append(") values (" );
        idMapping.appendInsertValues(sql, object);
        sql.append(", ");
        sql.append(values(object));
        titleMapping.appendInsertValues(sql, object);
        sql.append(", ");
        sql.append(versionMapping.insertValues(version));
        sql.append(") " );
        
        connector.insert(sql.toString());
        object.setOptimisticLock(version);

        for (int i = 0; i < collectionMappers.length; i++) {
            collectionMappers[i].saveInternalCollection(connector, object);
        }
    }

    public void createTables(final DatabaseConnector connection) {
        if (!connection.hasTable(table)) {
            StringBuffer sql = new StringBuffer();
            sql.append("create table ");
            sql.append(table);
            sql.append(" (");
            idMapping.appendCreateColumnDefinitions(sql);
            sql.append(", ");
            for (FieldMapping mapping : fieldMappings) {
                mapping.appendColumnDefinitions(sql);
                sql.append(",");
            }
            titleMapping.appendColumnDefinitions(sql);
            sql.append(", ");
            sql.append(versionMapping.appendColumnDefinitions());
            sql.append(")");
            connection.update(sql.toString());
        }
        for (int i = 0; collectionMappers != null && i < collectionMappers.length; i++) {
            if (collectionMappers[i].needsTables(connection)) {
                collectionMappers[i].createTables(connection);
            }
        }
    }

    public void destroyObject(final DatabaseConnector connector, final ObjectAdapter object) {
        StringBuffer sql = new StringBuffer();
        sql.append("delete from " + table + " where ");
        idMapping.appendWhereClause(sql, object.getOid());
        sql.append(" and ");
        sql.append(versionMapping.whereClause((SerialNumberVersion) object.getVersion()));
        int updateCount = connector.update(sql.toString());
        if (updateCount == 0) {
            LOG.info("concurrency conflict object " + this + "; no deletion performed");
            throw new ConcurrencyException("", object.getOid());
        }
     }

    public ObjectAdapter[] getInstances(final DatabaseConnector connector, final ObjectSpecification spec) {
        StringBuffer sql = createSelectStatement();
        return loadInstances(connector, spec, completeSelectStatement(sql));
    }


    public ObjectAdapter[] getInstances(final DatabaseConnector connector, final ObjectSpecification spec, final PersistenceQueryFindByPattern query) {
        StringBuffer sql = createSelectStatement();
        sql.append(" where ");
        
        int initialLength = sql.length();
        ObjectAdapter pattern = query.getPattern();
        for (ObjectAssociation assoc : specification.getAssociations()) {
            ObjectAdapter field = assoc.get(pattern);
            if (field != null) {
                FieldMapping fieldMapping = fieldMappingFor(assoc);
                if (fieldMapping != null) {
                    if (sql.length() > initialLength) {
                        sql.append(" and ");
                    }
                    fieldMapping.appendWhereClause(sql, pattern);
                }
            }
        }
        return loadInstances(connector, spec, completeSelectStatement(sql));
    }

    public ObjectAdapter[] getInstances(final DatabaseConnector connector, final ObjectSpecification spec, final String title) {
        StringBuffer sql = createSelectStatement();
        sql.append(" where ");
        titleMapping.appendWhereClause(sql, title);
        return loadInstances(connector, spec, completeSelectStatement(sql));
    }

    public ObjectAdapter getObject(final DatabaseConnector connector, final Oid oid, final ObjectSpecification hint) {
        StringBuffer sql = createSelectStatement();
        sql.append(" where ");
        idMapping.appendWhereClause(sql, oid);     
        Results rs = connector.select(completeSelectStatement(sql));
        if(rs.next()) {
            return loadObject(connector, hint, rs);
        } else {
            throw new ObjectNotFoundException("No object with with " + oid + " in table " + table);
        }
    }

    public boolean hasInstances(final DatabaseConnector connector, final ObjectSpecification cls) {
        String statement = "select count(*) from " + table;
        int instances = connector.count(statement);
        return instances > 0;
    }

    private StringBuffer createSelectStatement() {
        StringBuffer sql = new StringBuffer();
        sql.append("select ");
        idMapping.appendColumnNames(sql);
        sql.append(", ");
        sql.append(columnList());
        sql.append(", ");
        sql.append(versionMapping.insertColumns());
        sql.append(" from " + table);
        return sql;
    } /*
        if (whereClause != null) {
            sql.append(" where ");
            sql.append(whereClause);
        } else if (whereClause != null) {
            sql.append(" where ");
            idMapping.appendWhereClause(sql, oid);            
        }
        */
    
    private String completeSelectStatement(final StringBuffer sql) {
        sql.append(" order by ");
        idMapping.appendColumnNames(sql);
        return sql.toString();
    }

    protected void loadFields(final ObjectAdapter object, final Results rs) {
        PersistorUtil.start(object, ResolveState.RESOLVING);
        for (FieldMapping mapping  : fieldMappings) {
            mapping.initializeField(object, rs);
        }
/*
        for (int i = 0; i < oneToManyProperties.length; i++) {
            /*
             * Need to set up collection to be a ghost before we access as below
             */
            // CollectionAdapter collection = (CollectionAdapter)
   /*         oneToManyProperties[i].get(object);
        }
*/
        object.setOptimisticLock(versionMapping.getLock(rs));
        PersistorUtil.end(object);
    }
    // KAM
    private void loadCollections(DatabaseConnector connector,
			ObjectAdapter instance) {
    	
    	for (CollectionMapper mapper : collectionMappers){
    		mapper.loadInternalCollection(connector, instance);
    	}
	}

    private ObjectAdapter[] loadInstances(
            final DatabaseConnector connector,
            final ObjectSpecification cls,
            final String selectStatment) {
        LOG.debug("loading instances from SQL " + table);
        Vector<ObjectAdapter> instances = new Vector<ObjectAdapter>();

        Results rs = connector.select(selectStatment);
        for (int count = 0; rs.next() && count < MAX_INSTANCES; count++) {
            ObjectAdapter instance = loadObject(connector, cls, rs);
            LOG.debug("  instance  " + instance);
            instances.addElement(instance);
        }
        rs.close();

        ObjectAdapter[] array = new ObjectAdapter[instances.size()];
        instances.copyInto(array);
        return array;
    }

    private ObjectAdapter loadObject(final DatabaseConnector connector, final ObjectSpecification cls, final Results rs) {
        Oid oid = idMapping.recreateOid(rs,  specification);
        ObjectAdapter instance = getAdapter(cls, oid);

        if (instance.getResolveState().isValidToChangeTo(ResolveState.RESOLVING)) {
            loadFields(instance, rs);
            loadCollections(connector, instance); // KAM
        }
        return instance;
    }

	public void resolve(final DatabaseConnector connector, final ObjectAdapter object) {
        LOG.debug("loading data from SQL " + table + " for " + object);
        StringBuffer sql = new StringBuffer();
        sql.append("select ");
        sql.append(columnList());
        sql.append(",");
        sql.append(versionMapping.appendSelectColumns());
        sql.append(" from " + table + " where ");
        idMapping.appendWhereClause(sql, object.getOid());

        Results rs = connector.select(sql.toString());
        if (rs.next()) {
            loadFields(object, rs);
            rs.close();

            for (int i = 0; i < collectionMappers.length; i++) {
                collectionMappers[i].loadInternalCollection(connector, object);
            }
        } else {
            rs.close();
            throw new SqlObjectStoreException("Unable to load data from " + table + " with id " + idMapping.primaryKey(object.getOid()));
        }
    }

    public void resolveCollection(final DatabaseConnector connector, final ObjectAdapter object, final ObjectAssociation field) {
        if (collectionMappers.length > 0) {
            DatabaseConnector secondConnector = connector.getConnectionPool().acquire();
            for (int i = 0; i < collectionMappers.length; i++) {
                collectionMappers[i].loadInternalCollection(secondConnector, object);
            }
            connector.getConnectionPool().release(secondConnector);
        }
    }

    public void startup(final DatabaseConnector connector, final ObjectMappingLookup objectMapperLookup) {
        if (needsTables(connector)) {
            createTables(connector);
        }
    }

    public void save(final DatabaseConnector connector, final ObjectAdapter object) {
        SerialNumberVersion version =  (SerialNumberVersion) object.getVersion();
        long nextSequence = version.getSequence() + 1;
        
        StringBuffer sql = new StringBuffer();
        sql.append( "update " + table + " set ");
        for (FieldMapping mapping  : fieldMappings) {
            mapping.appendUpdateValues(sql, object);
            sql.append(", ");
        }
        sql.append(versionMapping.updateAssigment(nextSequence));
        sql.append(", ");
        titleMapping.appendUpdateAssignment(sql, object);
        sql.append( " where ");
        idMapping.appendWhereClause(sql, object.getOid());
        sql.append( " and ");
        sql.append(versionMapping.whereClause((SerialNumberVersion) object.getVersion()));
       
        int updateCount = connector.update(sql.toString());
        if (updateCount == 0) {
            LOG.info("concurrency conflict object " + this + "; no update performed");
            throw new ConcurrencyException("", object.getOid());
        } else {
            object.setOptimisticLock(createVersion(nextSequence));
        }

        // TODO update collections - change only when needed rather than reinserting from scratch
        for (int i = 0; i < collectionMappers.length; i++) {
            collectionMappers[i].saveInternalCollection(connector, object);
        }
    }

    public String toString() {
        return "AutoMapper [table=" + table + ",id=" + idMapping + ",noColumns=" + fieldMappings.size() + ",specification="
                + specification.getFullName() + "]";
    }

    public void debugData(DebugString debug) {
        debug.appendln("ID mapping", idMapping);
        debug.appendln("ID mapping", versionMapping);
        debug.appendln("ID mapping",  titleMapping);
        for (FieldMapping mapping  : fieldMappings) {
            mapping.debugData(debug);
        }
        for (int i = 0; i < collectionMappers.length; i++) {
            collectionMappers[i].debugData(debug);
        }

    }

    public String debugTitle() {
        return toString();
    }

}
