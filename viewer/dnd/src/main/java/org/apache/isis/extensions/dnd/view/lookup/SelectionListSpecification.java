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


package org.apache.isis.extensions.dnd.view.lookup;

import org.apache.isis.core.commons.exceptions.UnexpectedCallException;
import org.apache.isis.extensions.dnd.view.Axes;
import org.apache.isis.extensions.dnd.view.Content;
import org.apache.isis.extensions.dnd.view.View;
import org.apache.isis.extensions.dnd.view.ViewFactory;
import org.apache.isis.extensions.dnd.view.ViewRequirement;
import org.apache.isis.extensions.dnd.view.base.Layout;
import org.apache.isis.extensions.dnd.view.border.BackgroundBorder;
import org.apache.isis.extensions.dnd.view.border.LineBorder;
import org.apache.isis.extensions.dnd.view.border.ScrollBorder;
import org.apache.isis.extensions.dnd.view.composite.CompositeViewDecorator;
import org.apache.isis.extensions.dnd.view.composite.CompositeViewSpecification;
import org.apache.isis.extensions.dnd.view.composite.StackLayout;


public abstract class SelectionListSpecification extends CompositeViewSpecification {

    public SelectionListSpecification() {
        builder = new SelectionListBuilder(new ViewFactory() {
            public View createView(final Content content, final Axes axes, final int fieldNumber) {
                View elementView = createElementView(content);
                final SelectionListAxis axis = (SelectionListAxis) axes.getAxis(SelectionListAxis.class);
                axes.add(axis);
                return new SelectionItemSelector(elementView, axis);
            }

        });
        addViewDecorator(new CompositeViewDecorator() {
            public View decorate(View view, Axes axes) {
                final SelectionListAxis axis = axes.getAxis(SelectionListAxis.class);
                View list = new SelectionListFocusBorder(view, axis);
                return new DisposeOverlay(new BackgroundBorder(new LineBorder(new ScrollBorder(list))), axis);
            }
        });
    }

    protected abstract View createElementView(Content content);

    public Layout createLayout(Content content, Axes axes) {
        return new StackLayout(true);
    }

    public boolean canDisplay(ViewRequirement requirement) {
        throw new UnexpectedCallException();
    }

    public String getName() {
        return "Object Drop Down Overlay";
    }

}

