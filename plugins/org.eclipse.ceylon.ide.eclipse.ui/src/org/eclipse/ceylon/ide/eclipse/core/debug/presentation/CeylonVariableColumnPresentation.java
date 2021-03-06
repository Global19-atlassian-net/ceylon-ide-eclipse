/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.debug.presentation;

import org.eclipse.jdt.internal.debug.ui.variables.JavaVariableColumnPresentation;

import org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin;

public class CeylonVariableColumnPresentation extends
        JavaVariableColumnPresentation {

    /**
     * Constant identifier for the Column variable column presentation.
     */
    public static final String CEYLON_VARIABLE_COLUMN_PRESENTATION = CeylonPlugin.PLUGIN_ID + ".VARIALBE_COLUMN_PRESENTATION";  //$NON-NLS-1$;

    /**
     * Reified type
     */
    public final static String COLUMN_REIFIED_TYPE = CEYLON_VARIABLE_COLUMN_PRESENTATION + ".COL_REIFIED_TYPE"; //$NON-NLS-1$
    
    /**
     * Column ids
     */
    private static String[] allColumns = null;
    
    @Override
    public String[] getAvailableColumns() {
        if (allColumns == null) {
            String[] basic = super.getAvailableColumns();
            allColumns = new String[basic.length + 1];
            System.arraycopy(basic, 0, allColumns, 0, basic.length);
            allColumns[basic.length] = COLUMN_REIFIED_TYPE;
        }
        return allColumns;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.internal.ui.elements.adapters.VariableColumnPresentation#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String id) {
        if (COLUMN_REIFIED_TYPE.equals(id)) {
            return "Reified type";
        }
        String existingHeader = super.getHeader(id);
        return existingHeader;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.debug.internal.ui.elements.adapters.VariableColumnPresentation#getId()
     */
    @Override
    public String getId() {
        return CEYLON_VARIABLE_COLUMN_PRESENTATION;
    }
}
