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

import static org.eclipse.ceylon.ide.eclipse.code.refactor.MoveUtil.createEditorChange;
import static org.eclipse.ceylon.ide.eclipse.code.refactor.MoveUtil.getImportText;
import static org.eclipse.ceylon.ide.eclipse.code.refactor.MoveUtil.getImports;
import static org.eclipse.ceylon.ide.eclipse.java2ceylon.Java2CeylonProxies.utilJ2C;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonToken;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.compiler.typechecker.tree.Visitor;
import org.eclipse.ceylon.ide.eclipse.code.correct.correctJ2C;
import org.eclipse.ceylon.ide.common.platform.CommonDocument;
import org.eclipse.ceylon.ide.common.platform.TextEdit;
import org.eclipse.ceylon.model.typechecker.model.ClassOrInterface;
import org.eclipse.ceylon.model.typechecker.model.Declaration;
import org.eclipse.ceylon.model.typechecker.model.Package;
import org.eclipse.ceylon.model.typechecker.model.Parameter;
import org.eclipse.ceylon.model.typechecker.model.Scope;
import org.eclipse.ceylon.model.typechecker.model.Type;
import org.eclipse.ceylon.model.typechecker.model.TypeDeclaration;
import org.eclipse.ceylon.model.typechecker.model.TypeParameter;
import org.eclipse.ceylon.model.typechecker.model.TypedDeclaration;

public class ExtractInterfaceRefactoring extends AbstractRefactoring {

    String newInterfaceName;
    Tree.TypedDeclaration[] extractedMembers;
    Tree.TypedDeclaration[] extractableMembers;

    private Tree.Declaration container;
    private Tree.ClassOrInterface containerAsClassOrInter;
    private Tree.ObjectDefinition containerAsObjectDef;
    private String packageName;

    private LinkedHashSet<TypeParameter> extractedTypeParameters = 
            new LinkedHashSet<TypeParameter>();
    private HashMap<Declaration, String> extractedImports = 
            new HashMap<Declaration, String>();
    private HashSet<String> extractedImportsPackages = 
            new HashSet<String>();

    public ExtractInterfaceRefactoring(IEditorPart editor) {
        super(editor);

        ClassOrInterface clazz = null;
        if (node != null) {
            InternalFindContainerVisitor findContainerVisitor = 
                    new InternalFindContainerVisitor(node);
            findContainerVisitor.visit(rootNode);
            container = findContainerVisitor.container;

            if (container instanceof Tree.ClassOrInterface) {
                containerAsClassOrInter = (Tree.ClassOrInterface) container;
                clazz = containerAsClassOrInter.getDeclarationModel();
            }
            if (container instanceof Tree.ObjectDefinition) {
                containerAsObjectDef = (Tree.ObjectDefinition) container;
                clazz = containerAsObjectDef.getAnonymousClass();
            }
        }
        if (clazz != null) {
            extractableMembers = findExtractableMembers(clazz);
            packageName = findPackageName(clazz);
        }
    }

    @Override
    public boolean getEnabled() {
        return extractableMembers != null && 
                extractableMembers.length > 0;
    }

    @Override
    public String getName() {
        return "Extract Interface";
    }
    
    @Override
    protected boolean isAffectingOtherFiles() {
        return true; //TODO!!!!!
    }

    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) 
            throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) 
            throws CoreException, OperationCanceledException {
        return new RefactoringStatus();
    }

    @Override
    public Change createChange(IProgressMonitor pm) 
            throws CoreException, OperationCanceledException {
        collectExtractedTypeParametersFromMembers();
        collectExtractedTypeParametersFromTheirConstrains();
        collectExtractedImports();

        StringBuilder newUnitBuilder = new StringBuilder();
        addImports(newUnitBuilder);
        addInterfaceHeader(newUnitBuilder);
        addInterfaceTypeParameters(newUnitBuilder);
        addInterfaceTypeConstraints(newUnitBuilder);
        addInterfaceBody(newUnitBuilder);

        Change newUnitChange = createNewUnitChange(newUnitBuilder);
        Change originalUnitChange = createOriginalUnitChange();

        CompositeChange cc = new CompositeChange(getName());
        cc.add(newUnitChange);
        cc.add(originalUnitChange);

        return cc;
    }

    @Override
    void refactorInFile(TextChange textChange, 
            CompositeChange compositChange, 
            Tree.CompilationUnit rootNode,
            List<CommonToken> tokens) {
        throw new UnsupportedOperationException();
    }
    
    private Change createNewUnitChange(StringBuilder newUnitBuilder) {
        IPath path = 
                sourceFile.getParent()
                    .getProjectRelativePath()
                    .append(newInterfaceName + ".ceylon");
        IFile file = project.getFile(path);
        String desc = 
                "Create source file '" + 
                file.getProjectRelativePath() + 
                "'";
        CreateUnitChange newUnitChange = 
                new CreateUnitChange(file, true, 
                        newUnitBuilder.toString(), 
                        project, desc);
        return newUnitChange;
    }

    private Change createOriginalUnitChange() {
        TextChange originalUnitChange = 
                createEditorChange(editor, document);
        originalUnitChange.setEdit(new MultiTextEdit());
        addSatisfies(originalUnitChange);
        addSharedAndActualAnnotations(originalUnitChange);
        return originalUnitChange;
    }

    private void collectExtractedTypeParametersFromMembers() {
        for (Tree.TypedDeclaration extractedMember : extractedMembers) {

            TypedDeclaration d = 
                    extractedMember.getDeclarationModel();
            if (d != null) {
                collectExtractedTypeParameters(d.getType());
            }

            if (extractedMember instanceof Tree.AnyMethod) {
                Tree.AnyMethod method = 
                        (Tree.AnyMethod) extractedMember;
                if (method.getParameterLists() != null) {
                    for (Tree.ParameterList parameterList : 
                            method.getParameterLists()) {
                        for (Tree.Parameter parameter : 
                            parameterList.getParameters()) {
                            Parameter parameterModel = 
                                    parameter.getParameterModel();
                            if (parameterModel != null) {
                                collectExtractedTypeParameters(
                                        parameterModel.getType());
                            }
                        }
                    }
                }
                if (method.getTypeConstraintList() != null) {
                    for (Tree.TypeConstraint typeConstraint : 
                            method.getTypeConstraintList()
                                .getTypeConstraints()) {
                        TypeParameter typeConstraintModel = 
                                typeConstraint.getDeclarationModel();
                        if (typeConstraintModel != null) {
                            collectExtractedTypeParameters(
                                    typeConstraintModel.getType());
                        }
                    }
                }
            }
        }
    }

    private void collectExtractedTypeParametersFromTheirConstrains() {
        if (containerAsClassOrInter != null && 
                !extractedTypeParameters.isEmpty()) {
            Tree.TypeConstraintList typeConstraintList = 
                    typeConstraintList();
            if (typeConstraintList != null) {
                boolean repeat = true;
                while (repeat) {
                    int size = extractedTypeParameters.size();
                    for (Tree.TypeConstraint tc : 
                            typeConstraintList.getTypeConstraints()) {
                        if (extractedTypeParameters.contains(
                                tc.getDeclarationModel())) {
                            if (tc.getSatisfiedTypes() != null && 
                                    tc.getSatisfiedTypes().getTypes() != null) {
                                for (Tree.StaticType t : 
                                        tc.getSatisfiedTypes().getTypes()) {
                                    collectExtractedTypeParameters(
                                            t.getTypeModel());
                                }
                            }
                        }
                    }
                    if (size == extractedTypeParameters.size()) {
                        repeat = false;
                    }
                }
            }
        }
    }

    private void collectExtractedTypeParameters(Type pt) {
        if (pt.isTypeParameter()) {
            TypeDeclaration d = pt.getDeclaration();
            extractedTypeParameters.add((TypeParameter) d);
            for (Type st : pt.getSatisfiedTypes()) {
                collectExtractedTypeParameters(st);
            }
        }
        else if (pt.isUnion()) {
            for (Type ct : pt.getCaseTypes()) {
                collectExtractedTypeParameters(ct);
            }
        }
        else if (pt.isIntersection()) {
            for (Type st : pt.getSatisfiedTypes()) {
                collectExtractedTypeParameters(st);
            }
        }
        if (pt.getTypeArgumentList() != null) {
            for (Type ta : pt.getTypeArgumentList()) {
                collectExtractedTypeParameters(ta);
            }
        }
    }

    private void collectExtractedImports() {
        if (containerAsClassOrInter != null && 
                !extractedTypeParameters.isEmpty()) {
            Tree.TypeParameterList typeParameterList = 
                    containerAsClassOrInter.getTypeParameterList();
            if (typeParameterList != null) {
                for (Tree.TypeParameterDeclaration tp : 
                        typeParameterList.getTypeParameterDeclarations()) {
                    if (extractedTypeParameters.contains(
                            tp.getDeclarationModel())) {
                        collectExtractedImports(tp);
                    }
                }
            }
            Tree.TypeConstraintList typeConstraintList = 
                    typeConstraintList();
            if (typeConstraintList != null) {
                for (Tree.TypeConstraint tc : 
                        typeConstraintList.getTypeConstraints()) {
                    if (extractedTypeParameters.contains(
                            tc.getDeclarationModel())) {
                        collectExtractedImports(tc);
                    }
                }
            }
        }

        for (Tree.TypedDeclaration member : extractedMembers) {
            collectExtractedImports(member.getType());
            if (member instanceof Tree.AnyMethod) {
                Tree.AnyMethod method = (Tree.AnyMethod) member;
                for (Tree.ParameterList parameterList : 
                        method.getParameterLists()) {
                    collectExtractedImports(parameterList);
                }
                collectExtractedImports(method.getTypeParameterList());
                collectExtractedImports(method.getTypeConstraintList());
            }
        }

    }

    private void collectExtractedImports(Node node) {
        if (node != null) {
            Map<Declaration, String> imports = 
                    getImports(node, packageName, null, 
                            extractedImportsPackages);
            extractedImports.putAll(imports);
        }
    }

    private void addImports(StringBuilder content) {
        String delim = 
                utilJ2C().indents()
                    .getDefaultLineDelimiter(document);
        String importText = 
                getImportText(extractedImportsPackages, 
                        extractedImports, delim);
        if (!importText.isEmpty()) {
            content.append(importText);
            content.append(delim);
        }
    }

    private void addInterfaceHeader(StringBuilder content) {
        content.append("shared interface ");
        content.append(newInterfaceName);
    }

    private void addInterfaceTypeParameters(StringBuilder content) {
        if (containerAsClassOrInter != null && 
                !extractedTypeParameters.isEmpty()) {
            Tree.TypeParameterList typeParameterList = 
                    containerAsClassOrInter.getTypeParameterList();
            if (typeParameterList != null) {
                boolean first = true;
                for (Tree.TypeParameterDeclaration tp : 
                        typeParameterList.getTypeParameterDeclarations()) {
                    if (extractedTypeParameters.contains(
                            tp.getDeclarationModel())) {
                        if (first) {
                            first = false;
                            content.append("<");
                        } else {
                            content.append(", ");
                        }
                        content.append(text(tp, tokens));
                    }
                }
                if (!first) {
                    content.append(">");
                }
            }
        }
    }

    private void addInterfaceTypeConstraints(StringBuilder content) {
        if (containerAsClassOrInter != null && 
                !extractedTypeParameters.isEmpty()) {
            Tree.TypeConstraintList typeConstraintList = 
                    typeConstraintList();
            if (typeConstraintList != null) {
                for (Tree.TypeConstraint tc : 
                        typeConstraintList.getTypeConstraints()) {
                    if (extractedTypeParameters.contains(
                            tc.getDeclarationModel())) {
                        content.append(" ");
                        content.append(text(tc, tokens));
                    }
                }
            }
        }
    }

    private Tree.TypeConstraintList typeConstraintList() {
        Tree.TypeConstraintList typeConstraintList = null;
        if (containerAsClassOrInter instanceof Tree.AnyClass) {
            Tree.AnyClass cl = 
                    (Tree.AnyClass) containerAsClassOrInter;
            typeConstraintList = cl.getTypeConstraintList();
        }
        if (containerAsClassOrInter instanceof Tree.AnyInterface) {
            Tree.AnyInterface in = 
                    (Tree.AnyInterface) containerAsClassOrInter;
            typeConstraintList = in.getTypeConstraintList();
        }
        return typeConstraintList;
    }

    private void addInterfaceBody(StringBuilder content) {
        String delim = 
                utilJ2C().indents()
                    .getDefaultLineDelimiter(document);
        String indent = 
                utilJ2C().indents()
                    .getDefaultIndent();

        content.append(" {");
        content.append(delim);

        for (Tree.TypedDeclaration member : extractedMembers) {
            content.append(delim);
            content.append(indent);

            Tree.AnnotationList annotationList = 
                    member.getAnnotationList();
            Tree.AnonymousAnnotation anonymousAnnotation = 
                    annotationList.getAnonymousAnnotation();
            if (anonymousAnnotation != null) {
                content.append(text(anonymousAnnotation, tokens))
                    .append(delim).append(indent);
            }
            content.append("shared formal ");
            if(member.getDeclarationModel().isVariable()){
                content.append("variable ");
            }
            for (Tree.Annotation annotation : 
                    annotationList.getAnnotations()) {
                String annotationText = text(annotation, tokens);
                if (annotationText.equals("shared") ||
                        annotationText.equals("variable") ||
                        annotationText.equals("late") ||
                        annotationText.equals("default") ||
                        annotationText.equals("actual") ||
                        annotationText.equals("formal")) {
                    continue;
                }
                content.append(annotationText).append(" ");
            }

            content.append(text(member.getType(), tokens));
            content.append(" ");
            content.append(text(member.getIdentifier(), tokens));

            if (member instanceof Tree.AnyMethod) {
                Tree.AnyMethod method = (Tree.AnyMethod) member;

                if (method.getTypeConstraintList() != null) {
                    content.append(text(method.getTypeParameterList(), tokens));
                }
                for (Tree.ParameterList parameterList : 
                        method.getParameterLists()) {
                    content.append(text(parameterList, tokens));
                }
                if (method.getTypeConstraintList() != null) {
                    content.append(" ");
                    content.append(text(method.getTypeConstraintList(), tokens));
                }
            }

            content.append(";");
            content.append(delim);
        }

        content.append(delim);
        content.append("}");
    }

    private void addSatisfies(TextChange originalUnitChange) {
        int offset = -1;
        boolean containsSatisfies = false;
        if (containerAsClassOrInter != null) {
            if (containerAsClassOrInter instanceof Tree.AnyClass) {
                Tree.AnyClass cl = (Tree.AnyClass) containerAsClassOrInter;
                if (cl.getSatisfiedTypes() != null) {
                    offset = cl.getSatisfiedTypes().getEndIndex();
                    containsSatisfies = true;
                }
                else if (cl.getExtendedType() != null) {
                    offset = cl.getExtendedType().getEndIndex();
                }
                else if (cl.getCaseTypes() != null) {
                    offset = cl.getCaseTypes().getEndIndex();
                }
                else if (cl.getParameterList() != null) {
                    offset = cl.getParameterList().getEndIndex();
                }
                else if (cl.getTypeParameterList() != null) {
                    offset = cl.getTypeParameterList().getEndIndex();
                }
                else {
                    offset = cl.getIdentifier().getEndIndex();
                }
            }
            if (containerAsClassOrInter instanceof Tree.AnyInterface) {
                Tree.AnyInterface in = (Tree.AnyInterface) containerAsClassOrInter;
                if (in.getSatisfiedTypes() != null) {
                    offset = in.getSatisfiedTypes().getEndIndex();
                    containsSatisfies = true;
                }
                else if (in.getCaseTypes() != null) {
                    offset = in.getCaseTypes().getEndIndex();
                }
                else if (in.getTypeParameterList() != null) {
                    offset = in.getTypeParameterList().getEndIndex();
                }
                else {
                    offset = in.getIdentifier().getEndIndex();
                }
            }
        }
        if (containerAsObjectDef != null) {
            if (containerAsObjectDef.getSatisfiedTypes() != null) {
                offset = containerAsObjectDef.getSatisfiedTypes().getEndIndex();
                containsSatisfies = true;
            }
            else if (containerAsObjectDef.getExtendedType() != null) {
                offset = containerAsObjectDef.getExtendedType().getEndIndex();
            }
            else {
                offset = containerAsObjectDef.getIdentifier().getEndIndex();
            }
        }

        StringBuilder sb = new StringBuilder();
        if (containsSatisfies) {
            sb.append(" & ");
        }
        else {
            sb.append(" satisfies ");
        }
        sb.append(newInterfaceName);
        if (!extractedTypeParameters.isEmpty() && container instanceof Tree.ClassOrInterface) {
            Tree.ClassOrInterface clazz = (Tree.ClassOrInterface) container;
            if (clazz.getTypeParameterList() != null) {
                boolean first = true;
                for (Tree.TypeParameterDeclaration tp : clazz.getTypeParameterList().getTypeParameterDeclarations()) {
                    if (extractedTypeParameters.contains(tp.getDeclarationModel())) {
                        if (first) {
                            first = false;
                            sb.append("<");
                        } else {
                            sb.append(", ");
                        }
                        sb.append(text(tp.getIdentifier(), tokens));
                    }
                }
                if (!first) {
                    sb.append(">");
                }
            }
        }
        sb.append(" ");

        originalUnitChange.addEdit(new InsertEdit(offset, sb.toString()));
    }

    private void addSharedAndActualAnnotations(TextChange originalUnitChange) {
        CommonDocument platformDoc = new correctJ2C().newDocument(document);
        
        for (Tree.TypedDeclaration member : extractedMembers) {
            if (!member.getDeclarationModel().isShared()) {
                TextEdit createInsertAnnotationEdit = new correctJ2C()
                        .addAnnotationsQuickFix()
                        .createInsertAnnotationEdit("shared", member, platformDoc);
                originalUnitChange.addEdit(new InsertEdit(
                        (int) createInsertAnnotationEdit.getStart(),
                        createInsertAnnotationEdit.getText()
                ));
            }
            if (!member.getDeclarationModel().isActual()) {
                TextEdit createInsertAnnotationEdit = new correctJ2C()
                        .addAnnotationsQuickFix()
                        .createInsertAnnotationEdit("actual", member, platformDoc);
                originalUnitChange.addEdit(new InsertEdit(
                        (int) createInsertAnnotationEdit.getStart(),
                        createInsertAnnotationEdit.getText()
                ));
            }
        }
    }

    private Tree.TypedDeclaration[] findExtractableMembers(ClassOrInterface clazz) {
        InternalFindExtractableVisitor findExtractableVisitor = 
                new InternalFindExtractableVisitor(clazz);
        findExtractableVisitor.visit(container);
        return findExtractableVisitor.extractableMembers
                .toArray(new Tree.TypedDeclaration[] {});
    }

    private String findPackageName(Scope scope) {
        String packageName = null;
        while (packageName == null) {
            if (scope == null) {
                break;
            }
            if (scope instanceof Package) {
                packageName = ((Package) scope).getNameAsString();
            }
            scope = scope.getContainer();
        }
        return packageName;
    }

    private static class InternalFindContainerVisitor extends Visitor {

        private Node node;
        private Tree.Declaration current;
        private Tree.Declaration container;

        private InternalFindContainerVisitor(Node node) {
            this.node = node;
        }

        @Override
        public void visit(Tree.ObjectDefinition that) {
            Tree.Declaration d = current;
            current = that;
            super.visit(that);
            current = d;
        }

        @Override
        public void visit(Tree.ClassDefinition that) {
            Tree.Declaration d = current;
            current = that;
            super.visit(that);
            current = d;
        }

        @Override
        public void visit(Tree.InterfaceDefinition that) {
            Tree.Declaration d = current;
            current = that;
            super.visit(that);
            current = d;
        }

        @Override
        public void visitAny(Node node) {
            if (this.node == node) {
                container = current;
            }
            if (container == null) {
                super.visitAny(node);
            }
        }

    }

    private static class InternalFindExtractableVisitor extends Visitor {

        private Declaration container;
        private List<Tree.TypedDeclaration> extractableMembers = 
                new ArrayList<Tree.TypedDeclaration>();

        private InternalFindExtractableVisitor(Declaration container) {
            this.container = container;
        }

        @Override
        public void visit(Tree.AnyAttribute that) {
            if (that.getDeclarationModel().getContainer() 
                    == container) {
                extractableMembers.add(that);
            }
        }

        @Override
        public void visit(Tree.AnyMethod that) {
            if (that.getDeclarationModel().getContainer() 
                    == container) {
                extractableMembers.add(that);
            }
        }

    }

}
