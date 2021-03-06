/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.refactor;

import static org.eclipse.ceylon.ide.eclipse.code.correct.LinkedModeCompletionProposal.getSupertypeProposals;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.CeylonPreferenceInitializer.LINKED_MODE_EXTRACT;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;

import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.ide.eclipse.code.correct.LinkedModeCompletionProposal;
import org.eclipse.ceylon.ide.eclipse.code.correct.LinkedModeImporter;
import org.eclipse.ceylon.ide.eclipse.code.editor.CeylonEditor;
import org.eclipse.ceylon.ide.eclipse.core.builder.CeylonNature;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin;
import org.eclipse.ceylon.ide.eclipse.util.LinkedMode;
import org.eclipse.ceylon.ide.common.util.escaping_;
import org.eclipse.ceylon.model.typechecker.model.Type;
import org.eclipse.ceylon.model.typechecker.model.Unit;

public abstract class ExtractLinkedMode extends RefactorLinkedMode {

    protected LinkedPosition namePosition;
    protected LinkedPositionGroup linkedPositionGroup;

    public ExtractLinkedMode(CeylonEditor editor) {
        super(editor);
    }
    
    @Override
    public void done() {
        if (isEnabled()) {
            IProject project = 
                    editor.getParseController()
                        .getProject();
            if (CeylonNature.isEnabled(project)) {
                setName(getNewNameFromNamePosition());
                if (isShowPreview() || forceWizardMode()) {
                    try {
                        revertChanges();
                        openPreview();
                    } 
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            super.done();
        }
        else {
            super.cancel();
        }
    }
    
    protected abstract boolean forceWizardMode();

    public boolean isEnabled() {
        String newName = getNewNameFromNamePosition();
        return newName.matches("^\\w(\\w|\\d)*$") &&
                !escaping_.get_().isKeyword(newName);
    }
    
    protected abstract int getNameOffset();
    protected abstract int getTypeOffset();
    
    protected void addNamePosition(IDocument document, 
            int offset2, int length) {
        addNamePosition(document, offset2, length, 
                Collections.<IRegion>emptyList());
    }
    
    protected void addNamePosition(IDocument document, 
            int offset, int length, List<IRegion> regions) {
    	linkedPositionGroup = new LinkedPositionGroup();
    	namePosition =
    			new ProposalPosition(document, 
    			        getNameOffset(), 
                        getInitialName().length(), 0,
                        LinkedModeCompletionProposal
                            .getNameProposals(
                                getTypeOffset(), 1, 
                        		getNameProposals()));
        try {
            linkedPositionGroup.addPosition(namePosition);
            linkedPositionGroup.addPosition(
                    new LinkedPosition(document, 
                            offset, length, 2));
            int i = 3;
            for (IRegion region: regions) {
                linkedPositionGroup.addPosition(
                        new LinkedPosition(document, 
                            region.getOffset(), 
                            region.getLength(), 
                            i++));
            }
            linkedModeModel.addGroup(linkedPositionGroup);
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    protected abstract String[] getNameProposals();

	protected void addTypePosition(IDocument document,
            Type type, int offset, int length) {
        Tree.CompilationUnit rootNode = 
                editor.getParseController()
                    .getLastCompilationUnit();
        Unit unit = rootNode.getUnit();
        
        LinkedModeImporter importer = 
                new LinkedModeImporter(document, editor) {
            @Override
            public void selected(Type type) {
                super.selected(type);
                setReturnType(type);
            }
        };
        linkedModeModel.addLinkingListener(importer);
        
        ProposalPosition linkedPosition = 
                new ProposalPosition(document, 
                        offset, length, 1, 
                        getSupertypeProposals(offset, 
                                unit, type,
                                canBeInferred(), 
                                getKind(), 
                                importer));
        try {
            LinkedMode.addLinkedPosition(linkedModeModel, 
                    linkedPosition);
        } 
        catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    protected abstract void setReturnType(Type type);

    protected abstract void addLinkedPositions(
            IDocument document, 
            Tree.CompilationUnit rootNode, 
            int adjust);

    @Override
    protected String getNewNameFromNamePosition() {
        try {
            return namePosition.getContent();
        }
        catch (BadLocationException e) {
            return getInitialName();
        }
    }

    @Override
    protected void setupLinkedPositions(
            IDocument document, 
            int adjust) 
                    throws BadLocationException {
        Tree.CompilationUnit rootNode = 
                editor.getParseController()
                    .getLastCompilationUnit();
        addLinkedPositions(document, rootNode, adjust);
    }

    public static boolean useLinkedMode() {
        return CeylonPlugin.getPreferences()
                .getBoolean(LINKED_MODE_EXTRACT);
    }
    
    @Override
    public final String getHintTemplate() {
        return "Enter name for extracted " + getKind() +
                " declaration {0}";
    }

    @Override
    protected final void updatePopupLocation() {
        LinkedPosition currentLinkedPosition = 
                getCurrentLinkedPosition();
        if (currentLinkedPosition==null) {
            getInfoPopup()
                .setHintTemplate(getHintTemplate());
        }
        else if (currentLinkedPosition.getSequenceNumber()==1) {
            getInfoPopup()
                .setHintTemplate("Enter type for extracted " + 
                        getKind() + " declaration {0}");
        }
        else {
            getInfoPopup()
                .setHintTemplate("Enter name for extracted " + 
                        getKind() + " declaration {0}");
        }
    }

    public boolean canBeInferred() {
        return false;
    }

    protected abstract String getKind();
    
}
