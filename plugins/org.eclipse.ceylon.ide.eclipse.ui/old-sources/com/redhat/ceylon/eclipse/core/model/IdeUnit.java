/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.model;

import org.eclipse.ceylon.compiler.typechecker.context.TypecheckerUnit;

public abstract class IdeUnit extends TypecheckerUnit implements IUnit {
    public JDTModule getModule() {
        return (JDTModule) getPackage().getModule();
    }

    abstract public String getSourceFileName();
    abstract public String getSourceRelativePath();
    abstract public String getSourceFullPath();
}
