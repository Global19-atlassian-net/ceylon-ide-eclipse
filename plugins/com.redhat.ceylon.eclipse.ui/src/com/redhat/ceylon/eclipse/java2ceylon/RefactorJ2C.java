package com.redhat.ceylon.eclipse.java2ceylon;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.eclipse.code.refactor.EclipseExtractFunctionRefactoring;
import com.redhat.ceylon.ide.common.refactoring.DeprecatedExtractFunctionRefactoring;
import com.redhat.ceylon.ide.common.refactoring.ExtractParameterRefactoring;
import com.redhat.ceylon.ide.common.refactoring.ExtractValueRefactoring;
import com.redhat.ceylon.ide.common.refactoring.InlineRefactoring;

public interface RefactorJ2C {

    ExtractValueRefactoring<IFile, ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange, IRegion> newExtractValueRefactoring(
            IEditorPart editorPart);

    ExtractParameterRefactoring<IFile, ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange, IRegion> newExtractParameterRefactoring(
            IEditorPart editorPart);

    EclipseExtractFunctionRefactoring newExtractFunctionRefactoring(
            IEditorPart editorPart);

    EclipseExtractFunctionRefactoring newExtractFunctionRefactoring(
            IEditorPart editorPart, Tree.Declaration target);

    InlineRefactoring<ICompletionProposal, IDocument, InsertEdit, TextEdit, TextChange, CompositeChange> newInlineRefactoring(
            IEditorPart editorPart);


}