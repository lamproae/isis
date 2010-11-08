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


package org.apache.isis.webapp.view.debug;

import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.webapp.AbstractElementProcessor;
import org.apache.isis.webapp.context.RequestContext;
import org.apache.isis.webapp.processor.Request;


public class ThrowException extends AbstractElementProcessor {

    public void process(Request request) {
        if (request.getContext().getDebug() == RequestContext.Debug.ON) {
            String message = request.getOptionalProperty("message", "Exception throw for testing purposes");
            throw new IsisException(message);
        }
    }

    public String getName() {
        return "debug-exception";
    }

}

