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


package org.apache.isis.runtime.authentication.standard.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.isis.core.commons.ensure.Assert;
import org.apache.isis.core.commons.exceptions.IsisException;
import org.apache.isis.core.commons.io.IoUtils;
import org.apache.isis.core.commons.resource.ResourceStreamSource;
import org.apache.isis.metamodel.config.IsisConfiguration;
import org.apache.isis.runtime.authentication.AuthenticationRequest;
import org.apache.isis.runtime.authentication.AuthenticationRequestPassword;
import org.apache.isis.runtime.authentication.standard.PasswordRequestAuthenticatorAbstract;

import com.google.inject.Inject;

public class FileAuthenticator extends PasswordRequestAuthenticatorAbstract {
	
	private final ResourceStreamSource resourceStreamSource;

	@Inject
    public FileAuthenticator(final IsisConfiguration configuration) {
    	super(configuration);
        this.resourceStreamSource = configuration.getResourceStreamSource();
    }

    public final boolean isValid(final AuthenticationRequest request) {
        final AuthenticationRequestPassword passwordRequest = (AuthenticationRequestPassword) request;
        final String username = passwordRequest.getName();
        if (username == null || username.equals("")) {
            return false;
        }
        final String password = passwordRequest.getPassword();
        Assert.assertNotNull(password);

        BufferedReader reader = null;
        try {
            InputStream readStream = resourceStreamSource.readResource(FileAuthenticationConstants.PASSWORDS_FILE);
            if (readStream == null) {
                throw new IsisException("Failed to open password file: " + FileAuthenticationConstants.PASSWORDS_FILE + " from " + resourceStreamSource.getName());
            }
            reader = new BufferedReader(new InputStreamReader(readStream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (commentedOutOrEmpty(line)) {
                    continue;
                }
                if (line.indexOf(':') == -1) {
                    throw new IsisException("Invalid entry in password file - no colon (:) found on line: " + line);
                }
                final String name = line.substring(0, line.indexOf(':'));
                if (!name.equals(username)) {
                	continue;
                }
                
                return isPasswordValidForUser(request, password, line);
            }
        } catch (final IOException e) {
            throw new IsisException("Failed to read password file: " + FileAuthenticationConstants.PASSWORDS_FILE + " from " + resourceStreamSource.getName());
        } finally {
        	IoUtils.closeSafely(reader);
        }

        return false;
    }

	private boolean commentedOutOrEmpty(String line) {
		return line.startsWith("#") || line.trim().length() == 0;
	}

	private boolean isPasswordValidForUser(
			final AuthenticationRequest request,
			final String password, String line) {
		int posFirstColon = line.indexOf(':');
		int posPasswordStart = posFirstColon + 1;
		
		int posSecondColonIfAny = line.indexOf(':', posPasswordStart);
		int posPasswordEnd = posSecondColonIfAny == -1 ? line.length() : posSecondColonIfAny;
		
		String parsedPassword = line.substring(posPasswordStart, posPasswordEnd);
		if (parsedPassword.equals(password)) {
		    if (posSecondColonIfAny != -1) {
		        setRoles(request, line.substring(posSecondColonIfAny + 1));
		    }
		    return true;
		} else {
		    return false;
		}
	}

    private final void setRoles(final AuthenticationRequest request, final String line) {
        final StringTokenizer tokens = new StringTokenizer(line, "|", false);
        String[] roles = new String[tokens.countTokens()];
        for (int i = 0; tokens.hasMoreTokens(); i++) {
            roles[i] = tokens.nextToken();
        }
        request.setRoles(Arrays.asList(roles));
    }

}

