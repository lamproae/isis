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


package org.apache.isis.extensions.dnd.view.action;

import org.apache.isis.core.commons.ensure.Assert;
import org.apache.isis.metamodel.adapter.ObjectAdapter;
import org.apache.isis.metamodel.consent.Consent;
import org.apache.isis.metamodel.spec.feature.ObjectAction;
import org.apache.isis.extensions.dnd.drawing.Location;
import org.apache.isis.extensions.dnd.view.BackgroundTask;
import org.apache.isis.extensions.dnd.view.Placement;
import org.apache.isis.extensions.dnd.view.View;
import org.apache.isis.extensions.dnd.view.Workspace;
import org.apache.isis.runtime.context.IsisContext;


/**
 * Options for an underlying object determined dynamically by looking for methods starting with action, veto
 * and option for specifying the action, vetoing the option and giving the option an name respectively.
 */
public class ImmediateObjectOption extends AbstractObjectOption {

    public static ImmediateObjectOption createOption(final ObjectAction action, final ObjectAdapter object) {
        Assert.assertTrue("Only suitable for 0 param methods", action.getParameterCount() == 0);
        if (!action.isVisible(IsisContext.getAuthenticationSession(), object).isAllowed()) {
            return null;
        }
        final ImmediateObjectOption option = new ImmediateObjectOption(action, object);
        return option;
    }

    public static ImmediateObjectOption createServiceOption(final ObjectAction action, final ObjectAdapter object) {
        Assert.assertTrue("Only suitable for 1 param methods", action.getParameterCount() == 1);
        if (!action.isVisible(IsisContext.getAuthenticationSession(), object).isAllowed()) {
            return null;
        }
        final ImmediateObjectOption option = new ImmediateObjectOption(action, object);

        return option;
    }

    private ImmediateObjectOption(final ObjectAction action, final ObjectAdapter target) {
        super(action, target, action.getName());
    }

    @Override
    protected Consent checkValid() {
        return action.isProposedArgumentSetValid(target, null);
    }

    // TODO this method is very similar to ActionDialogSpecification.execute()
    @Override
    public void execute(final Workspace workspace, final View view, final Location at) {
        BackgroundWork.runTaskInBackground(view, new BackgroundTask() {
            public void execute() {
                ObjectAdapter result;
                result = action.execute(target, null);
                view.objectActionResult(result, new Placement(view));
                view.getViewManager().disposeUnneededViews();
                view.getFeedbackManager().showMessagesAndWarnings();
            }

            public String getDescription() {
                return "Running action " + getName() + " on  " + view.getContent().getAdapter();
            }

            public String getName() {
                return "ObjectAction " + action.getName();
            }
        });
    }
}
