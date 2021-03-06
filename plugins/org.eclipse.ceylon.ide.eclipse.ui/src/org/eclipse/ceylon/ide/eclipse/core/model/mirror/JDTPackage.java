/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.model.mirror;

import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;

import org.eclipse.ceylon.model.loader.mirror.PackageMirror;

public class JDTPackage implements PackageMirror {

    private String name;

    public JDTPackage(PackageBinding pkg) {
        name = pkg == null ? "java.lang" : new String(pkg.readableName());
    }

    @Override
    public String getQualifiedName() {
        return name;
    }

}
