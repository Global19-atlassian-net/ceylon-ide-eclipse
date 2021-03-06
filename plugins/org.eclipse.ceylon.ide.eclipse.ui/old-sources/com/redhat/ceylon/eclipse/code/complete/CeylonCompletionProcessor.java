/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.complete;

import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.AIDENTIFIER;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.ASTRING_LITERAL;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.AVERBATIM_STRING;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.CASE_TYPES;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.CHAR_LITERAL;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.COMMA;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.EOF;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.FLOAT_LITERAL;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.IS_OP;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.LARGER_OP;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.LBRACE;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.LIDENTIFIER;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.LINE_COMMENT;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.LPAREN;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.MEMBER_OP;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.MULTI_COMMENT;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.NATURAL_LITERAL;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.PIDENTIFIER;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.RBRACE;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.SAFE_MEMBER_OP;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.SEMICOLON;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.SPREAD_OP;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.STRING_END;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.STRING_LITERAL;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.STRING_MID;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.STRING_START;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.UIDENTIFIER;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.VERBATIM_STRING;
import static org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer.WS;
import static org.eclipse.ceylon.ide.eclipse.code.complete.AnonFunctionProposal.addAnonFunctionProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.BasicCompletionProposal.addDocLinkProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.BasicCompletionProposal.addImportProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.getLine;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.isEmptyModuleDescriptor;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.isEmptyPackageDescriptor;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.isModuleDescriptor;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.isPackageDescriptor;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.nextTokenType;
import static org.eclipse.ceylon.ide.eclipse.code.complete.CompletionUtil.overloads;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addAssertExistsProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addAssertNonemptyProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addForProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addIfExistsProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addIfNonemptyProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addSwitchProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ControlStructureCompletionProposal.addTryProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.FunctionCompletionProposal.addFunctionProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.addFakeShowParametersCompletion;
import static org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.addInvocationProposals;
import static org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.addProgramElementReferenceProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.addReferenceProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.addSecondLevelProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.KeywordCompletionProposal.addKeywordProposals;
import static org.eclipse.ceylon.ide.eclipse.code.complete.MemberNameCompletions.addMemberNameProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.MemberNameCompletions.addMemberNameProposals;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ModuleCompletions.addModuleCompletions;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ModuleCompletions.addModuleDescriptorCompletion;
import static org.eclipse.ceylon.ide.eclipse.code.complete.PackageCompletions.addCurrentPackageNameCompletion;
import static org.eclipse.ceylon.ide.eclipse.code.complete.PackageCompletions.addPackageCompletions;
import static org.eclipse.ceylon.ide.eclipse.code.complete.PackageCompletions.addPackageDescriptorCompletion;
import static org.eclipse.ceylon.ide.eclipse.code.complete.ParametersCompletionProposal.addParametersProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.RefinementCompletionProposal.addInlineFunctionProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.RefinementCompletionProposal.addNamedArgumentProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.RefinementCompletionProposal.addRefinementProposal;
import static org.eclipse.ceylon.ide.eclipse.code.complete.RefinementCompletionProposal.getRefinedProducedReference;
import static org.eclipse.ceylon.ide.eclipse.code.complete.TypeArgumentListCompletions.addTypeArgumentListProposal;
import static org.eclipse.ceylon.ide.eclipse.code.outline.CeylonLabelProvider.getDecoratedImage;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.CeylonPreferenceInitializer.AUTO_ACTIVATION_CHARS;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.CeylonPreferenceInitializer.COMPLETION_FILTERS;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.CeylonPreferenceInitializer.ENABLE_COMPLETION_FILTERS;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.CeylonPreferenceInitializer.FILTERS;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.findNode;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.getIdentifyingNode;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.getOccurrenceLocation;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.getReferencedNodeInUnit;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.getTokenIndexAtCharacter;
import static org.eclipse.ceylon.ide.eclipse.util.Types.getRequiredType;
import static org.eclipse.ceylon.ide.eclipse.util.Types.getResultType;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.ALIAS_REF;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.CASE;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.CATCH;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.CLASS_ALIAS;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.DOCLINK;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.EXISTS;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.EXPRESSION;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.EXTENDS;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.FUNCTION_REF;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.IMPORT;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.IS;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.META;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.NONEMPTY;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.OF;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.PARAMETER_LIST;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.SATISFIES;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.TYPE_ALIAS;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.TYPE_ARGUMENT_LIST;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.TYPE_PARAMETER_LIST;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.TYPE_PARAMETER_REF;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.UPPER_BOUND;
import static org.eclipse.ceylon.ide.common.util.OccurrenceLocation.VALUE_REF;
import static org.eclipse.ceylon.model.typechecker.model.ModelUtil.isAbstraction;
import static org.eclipse.ceylon.model.typechecker.model.ModelUtil.isConstructor;
import static org.eclipse.ceylon.model.typechecker.model.ModelUtil.isTypeUnknown;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static java.util.Collections.emptyMap;
import static org.antlr.runtime.Token.HIDDEN_CHANNEL;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import org.eclipse.ceylon.compiler.typechecker.context.PhasedUnit;
import org.eclipse.ceylon.compiler.typechecker.parser.CeylonLexer;
import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.compiler.typechecker.tree.Visitor;
import org.eclipse.ceylon.ide.eclipse.code.complete.InvocationCompletionProposal.ParameterContextInformation;
import org.eclipse.ceylon.ide.eclipse.code.editor.CeylonEditor;
import org.eclipse.ceylon.ide.eclipse.code.parse.CeylonParseController;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonResources;
import org.eclipse.ceylon.ide.eclipse.util.Types;
import org.eclipse.ceylon.ide.common.completion.FindScopeVisitor;
import org.eclipse.ceylon.ide.common.util.OccurrenceLocation;
import org.eclipse.ceylon.ide.common.util.escaping_;
import org.eclipse.ceylon.model.typechecker.model.Cancellable;
import org.eclipse.ceylon.model.typechecker.model.Class;
import org.eclipse.ceylon.model.typechecker.model.ClassOrInterface;
import org.eclipse.ceylon.model.typechecker.model.Constructor;
import org.eclipse.ceylon.model.typechecker.model.Declaration;
import org.eclipse.ceylon.model.typechecker.model.DeclarationWithProximity;
import org.eclipse.ceylon.model.typechecker.model.Function;
import org.eclipse.ceylon.model.typechecker.model.FunctionOrValue;
import org.eclipse.ceylon.model.typechecker.model.Functional;
import org.eclipse.ceylon.model.typechecker.model.ImportList;
import org.eclipse.ceylon.model.typechecker.model.Interface;
import org.eclipse.ceylon.model.typechecker.model.Package;
import org.eclipse.ceylon.model.typechecker.model.Parameter;
import org.eclipse.ceylon.model.typechecker.model.ParameterList;
import org.eclipse.ceylon.model.typechecker.model.Reference;
import org.eclipse.ceylon.model.typechecker.model.Scope;
import org.eclipse.ceylon.model.typechecker.model.Type;
import org.eclipse.ceylon.model.typechecker.model.TypeAlias;
import org.eclipse.ceylon.model.typechecker.model.TypeDeclaration;
import org.eclipse.ceylon.model.typechecker.model.TypeParameter;
import org.eclipse.ceylon.model.typechecker.model.TypedDeclaration;
import org.eclipse.ceylon.model.typechecker.model.Unit;
import org.eclipse.ceylon.model.typechecker.model.Value;

public class CeylonCompletionProcessor implements IContentAssistProcessor {
    
    static final Image LARGE_CORRECTION_IMAGE = 
            getDecoratedImage(CeylonResources.CEYLON_CORRECTION, 0, false);

    private static final char[] CONTEXT_INFO_ACTIVATION_CHARS = 
            ",(;{".toCharArray();
    
    private static final IContextInformation[] NO_CONTEXTS = new IContextInformation[0];
    static ICompletionProposal[] NO_COMPLETIONS = new ICompletionProposal[0];
    
    private ParameterContextValidator validator;
    private CeylonEditor editor;
    
    private boolean secondLevel;
    private boolean returnedParamInfo;
    private int lastOffsetAcrossSessions=-1;
    private int lastOffset=-1;
    
    public void sessionStarted() {
        secondLevel = false;
        lastOffset=-1;
    }

    public CeylonCompletionProcessor(CeylonEditor editor) {
        this.editor=editor;
    }
    
    public ICompletionProposal[] computeCompletionProposals(
            final ITextViewer viewer, final int offset) {
        if (offset!=lastOffsetAcrossSessions) {
            returnedParamInfo = false;
            secondLevel = false;
        }
        try {
            if (lastOffset>=0 && offset>0 && 
                    offset!=lastOffset &&
                    !isIdentifierCharacter(viewer, offset)) {
                //user typed a whitespace char with an open
                //completions window, so close the window
                return NO_COMPLETIONS;
            }
        } 
        catch (BadLocationException ble) {
            ble.printStackTrace();
            return NO_COMPLETIONS;
        }
        if (offset==lastOffset) {
            secondLevel = !secondLevel;
        }
        lastOffset = offset;
        lastOffsetAcrossSessions = offset;
        
        class Runnable implements IRunnableWithProgress {
            ICompletionProposal[] contentProposals = NO_COMPLETIONS;
            @Override
            public void run(IProgressMonitor monitor) 
                    throws InvocationTargetException, 
                           InterruptedException {
                monitor.beginTask("Preparing completions...", 
                        IProgressMonitor.UNKNOWN);
                CeylonParseController controller = 
                        editor.getParseController();
                contentProposals = 
                        getContentProposals(controller, 
                                offset, viewer, 
                                secondLevel, 
                                returnedParamInfo,
                                monitor);
                if (contentProposals!=null && 
                    contentProposals.length==1 && 
                    contentProposals[0] instanceof 
                            InvocationCompletionProposal.ParameterInfo) {
                    returnedParamInfo = true;
                }
                monitor.done();
            }
        }
        Runnable runnable = new Runnable();
        try {
            if (secondLevel) {
                runnable.run(new NullProgressMonitor());
            }
            else {
                getWorkbench()
                    .getActiveWorkbenchWindow()
                    .run(true, true, runnable);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return runnable.contentProposals;
    }

    private boolean isIdentifierCharacter(
            ITextViewer viewer, int offset)
                    throws BadLocationException {
        IDocument doc = viewer.getDocument();
        char ch = doc.get(offset-1, 1).charAt(0);
        return isLetter(ch) || isDigit(ch) || ch=='_' || ch=='.';
    }

    public IContextInformation[] computeContextInformation(
            final ITextViewer viewer, final int offset) {
        CeylonParseController controller = 
                editor.getParseController();
        PhasedUnit phasedUnit =
                controller.parseAndTypecheck(
                    viewer.getDocument(),
                    10,
                    new NullProgressMonitor(), 
                    null);
        if (phasedUnit!=null) {
            return computeParameterContextInformation(offset,
                    phasedUnit.getCompilationUnit(), viewer)
                    .toArray(NO_CONTEXTS);
        } else {
            return NO_CONTEXTS;
        }
    }

    public char[] getCompletionProposalAutoActivationCharacters() {
        return CeylonPlugin.getPreferences()
                .getString(AUTO_ACTIVATION_CHARS)
                .toCharArray();
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return CONTEXT_INFO_ACTIVATION_CHARS;
    }

    public IContextInformationValidator getContextInformationValidator() {
        if (validator==null) {
            validator = new ParameterContextValidator(editor);
        }
        return validator;
    }

    public String getErrorMessage() {
        return "No completions available";
    }
    
    public ICompletionProposal[] getContentProposals(
            CeylonParseController controller,
            int offset, ITextViewer viewer, 
            boolean secondLevel, boolean returnedParamInfo, 
            final IProgressMonitor monitor) {
        Cancellable cancellable = new Cancellable() {
            @Override
            public boolean isCancelled() {
                return monitor.isCanceled();
            }
        };
        if (controller==null || viewer==null) {
            return null;
        }
        
        PhasedUnit typecheckedPhasedUnit =
                controller.parseAndTypecheck(
                        viewer.getDocument(),
                        10, monitor, null);
        if (typecheckedPhasedUnit == null) {
            return null;
        }
        editor.getAnnotationCreator().updateAnnotations();
        List<CommonToken> tokens = controller.getTokens(); 
        Tree.CompilationUnit typecheckedRootNode = 
                typecheckedPhasedUnit.getCompilationUnit();
        
        //adjust the token to account for unclosed blocks
        //we search for the first non-whitespace/non-comment
        //token to the left of the caret
        int tokenIndex = 
                getTokenIndexAtCharacter(tokens, offset);
        if (tokenIndex<0) tokenIndex = -tokenIndex;
        CommonToken adjustedToken = 
                adjust(tokenIndex, offset, tokens);
        int tt = adjustedToken.getType();
        
        if (offset<=adjustedToken.getStopIndex() && 
            offset>adjustedToken.getStartIndex()) {
            if (isCommentOrCodeStringLiteral(adjustedToken)) {
                return null;
            }
        }
        if (isLineComment(adjustedToken) &&
            offset>adjustedToken.getStartIndex() && 
            adjustedToken.getLine()==getLine(offset,viewer)+1) {
            return null;
        }
        
        //find the node at the token
        Node node = getTokenNode(
                adjustedToken.getStartIndex(), 
                adjustedToken.getStopIndex()+1, 
                tt, typecheckedRootNode, offset);
        
        //it's useful to know the type of the preceding 
        //token, if any
        int index = adjustedToken.getTokenIndex();
        if (offset<=adjustedToken.getStopIndex()+1 && 
            offset>adjustedToken.getStartIndex()) {
            index--;
        }
        int tokenType = adjustedToken.getType();
        int previousTokenType = index>=0 ? 
                adjust(index, offset, tokens).getType() : 
                -1;
                
        //find the type that is expected in the current
        //location so we can prioritize proposals of that
        //type
        //TODO: this breaks as soon as the user starts typing
        //      an expression, since RequiredTypeVisitor
        //      doesn't know how to search up the tree for
        //      the containing InvocationExpression
        Types.Required required =
                getRequiredType(typecheckedRootNode, node,
                        adjustedToken);
        
        String prefix = "";
        String fullPrefix = "";
        if (isIdentifierOrKeyword(adjustedToken)) {
            String text = adjustedToken.getText();
            //work from the end of the token to
            //compute the offset, in order to
            //account for quoted identifiers, where
            //the \i or \I is not in the token text 
            int offsetInToken = 
                    offset-adjustedToken.getStopIndex()-1+text.length();
            int realOffsetInToken = 
                    offset-adjustedToken.getStartIndex();
            if (offsetInToken<=text.length()) {
                prefix = text.substring(0, offsetInToken);
                fullPrefix = getRealText(adjustedToken)
                        .substring(0, realOffsetInToken);
            }
        }
        boolean isMemberOp = isMemberOperator(adjustedToken);
        String qualified = null;
        
        // special handling for doc links
        boolean inDoc = 
                isAnnotationStringLiteral(adjustedToken) &&
                offset>adjustedToken.getStartIndex() &&
                offset<=adjustedToken.getStopIndex();
        if (inDoc) {
            if (node instanceof Tree.DocLink) {
                Tree.DocLink docLink = (Tree.DocLink) node;
                int offsetInLink = 
                        offset - docLink.getStartIndex();
                String text = docLink.getToken().getText();
                int bar = text.indexOf('|')+1;
                if (offsetInLink<bar) {
                    return null;
                }
                qualified = text.substring(bar, offsetInLink);
                int dcolon = qualified.indexOf("::");
                String pkg = null;
                if (dcolon>=0) {
                    pkg = qualified.substring(0, dcolon+2);
                    qualified = qualified.substring(dcolon+2);
                }
                int dot = qualified.indexOf('.')+1;
                isMemberOp = dot>0;
                prefix = qualified.substring(dot);
                if (dcolon>=0) {
                    qualified = pkg + qualified;
                }
                fullPrefix = prefix;
            }
            else {
                return null;
            }
        }
        
        FindScopeVisitor fsv = new FindScopeVisitor(node);
        fsv.visit(typecheckedRootNode);
        Scope scope = fsv.getScope();
        
        //construct completions when outside ordinary code
        ICompletionProposal[] completions = 
                constructCompletions(offset, fullPrefix, 
                        controller, node, adjustedToken, 
                        scope, returnedParamInfo, isMemberOp, 
                        viewer.getDocument(), tokenType,
                        monitor);
        if (completions==null) {
            //finally, construct and sort proposals
            Map<String, DeclarationWithProximity> proposals =
                    getProposals(node, scope, prefix,
                            isMemberOp, typecheckedRootNode, cancellable);
            Map<String, DeclarationWithProximity> functionProposals =
                    getFunctionProposals(node, scope, prefix, 
                            isMemberOp, cancellable);
            filterProposals(proposals);
            filterProposals(functionProposals);
            Set<DeclarationWithProximity> sortedProposals = 
                    sortProposals(prefix, required,
                            proposals);
            Set<DeclarationWithProximity> sortedFunctionProposals =
                    sortProposals(prefix, required,
                            functionProposals);
            completions =
                    constructCompletions(offset, 
                            inDoc ? qualified : fullPrefix, 
                            sortedProposals, 
                            sortedFunctionProposals,
                            controller, scope, node, 
                            adjustedToken, isMemberOp, 
                            viewer.getDocument(), 
                            secondLevel, inDoc,
                            required.getType(),
                            previousTokenType,
                            tokenType);
        }
        return completions;
        
    }
    
    private void filterProposals(
            Map<String, DeclarationWithProximity> proposals) {
        List<Pattern> filters = getProposalFilters();
        if (!filters.isEmpty()) {
            Iterator<DeclarationWithProximity> iterator = 
                    proposals.values()
                        .iterator();
            while (iterator.hasNext()) {
                DeclarationWithProximity dwp = 
                        iterator.next();
                String name = 
                        dwp.getDeclaration()
                            .getQualifiedNameString();
                for (Pattern filter: filters) {
                    if (filter.matcher(name).matches()) {
                        iterator.remove();
                        continue;
                    }
                }
            }
        }
    }

    private List<Pattern> getProposalFilters() {
        List<Pattern> filters = new ArrayList<Pattern>();
        IPreferenceStore preferences = CeylonPlugin.getPreferences();
        parseFilters(filters, 
                preferences.getString(FILTERS));
        if (preferences.getBoolean(ENABLE_COMPLETION_FILTERS)) {
            parseFilters(filters,
                    preferences.getString(COMPLETION_FILTERS));
        }
        return filters;
    }

    private void parseFilters(List<Pattern> filters, 
            String filtersString) {
        if (!filtersString.trim().isEmpty()) {
            String[] regexes = 
                    filtersString
                        .replaceAll("\\(\\w+\\)", "")
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .split(",");
            for (String regex: regexes) {
                regex = regex.trim();
                if (!regex.isEmpty()) {
                    filters.add(Pattern.compile(regex));
                }
            }
        }
    }
    
    private String getRealText(CommonToken token) {
        String text = token.getText();
        int type = token.getType();
        int len = token.getStopIndex()-token.getStartIndex()+1;
        if (text.length()<len) {
            String quote;
            if (type==LIDENTIFIER) {
                quote = "\\i";
            }
            else if (type==UIDENTIFIER) {
                quote = "\\I";
            }
            else {
                quote = "";
            }
            return quote + text;
        }
        else {
            return text;
        }
    }

    private boolean isLineComment(CommonToken adjustedToken) {
        return adjustedToken.getType()==LINE_COMMENT;
    }

    private boolean isCommentOrCodeStringLiteral(
            CommonToken adjustedToken) {
        int tt = adjustedToken.getType();
        return tt==MULTI_COMMENT ||
            tt==LINE_COMMENT ||
            tt==STRING_LITERAL ||
            tt==STRING_END ||
            tt==STRING_MID ||
            tt==STRING_START ||
            tt==VERBATIM_STRING ||
            tt==CHAR_LITERAL ||
            tt==FLOAT_LITERAL ||
            tt==NATURAL_LITERAL;
    }
    
    private static boolean isAnnotationStringLiteral(
            CommonToken token) {
        int type = token.getType();
        return type == ASTRING_LITERAL || 
                type == AVERBATIM_STRING;
    }
    
    private static CommonToken adjust(
            int tokenIndex, int offset, 
            List<CommonToken> tokens) {
        CommonToken adjustedToken = 
                tokens.get(tokenIndex); 
        while (--tokenIndex>=0 && 
                (adjustedToken.getType()==WS //ignore whitespace
                || adjustedToken.getType()==EOF
                || adjustedToken.getStartIndex()==offset)) { //don't consider the token to the right of the caret
            adjustedToken = tokens.get(tokenIndex);
            if (adjustedToken.getType()!=WS &&
                    adjustedToken.getType()!=EOF &&
                    adjustedToken.getChannel()!=HIDDEN_CHANNEL) { //don't adjust to a ws token
                break;
            }
        }
        return adjustedToken;
    }
    
    private static Boolean isDirectlyInsideBlock(Node node,
            CeylonParseController cpc, Scope scope,
            CommonToken token) {
        if (scope instanceof Interface || 
                scope instanceof Package) {
            return false;
        }
        else {
            //TODO: check that it is not the opening/closing 
            //      brace of a named argument list!
            return !(node instanceof Tree.SequenceEnumeration) && 
                    occursAfterBraceOrSemicolon(token, 
                            cpc.getTokens());
        }
    }

    private static Boolean occursAfterBraceOrSemicolon(
            CommonToken token, List<CommonToken> tokens) {
        if (token.getTokenIndex()==0) {
            return false;
        }
        else {
            int tokenType = token.getType();
            if (tokenType==LBRACE || 
                tokenType==RBRACE || 
                tokenType==SEMICOLON) {
                return true;
            }
            int previousTokenType = 
                    adjust(token.getTokenIndex()-1, 
                            token.getStartIndex(), 
                            tokens)
                        .getType();
            return previousTokenType==LBRACE || 
                    previousTokenType==RBRACE || 
                    previousTokenType==SEMICOLON;
        }
    }

    private static Node getTokenNode(
            int adjustedStart, int adjustedEnd,
            int tokenType, Tree.CompilationUnit rootNode, 
            int offset) {
        Node node = findNode(rootNode, null, 
                adjustedStart, adjustedEnd);
        if (node instanceof Tree.StringLiteral) {
            Tree.StringLiteral sl = 
                    (Tree.StringLiteral) node;
            if (!sl.getDocLinks().isEmpty()) {
                node = findNode(node, null, offset, offset);
            }
        }
        if (tokenType==RBRACE && 
                !(node instanceof Tree.IterableType) || 
            tokenType==SEMICOLON) {
            //We are to the right of a } or ;
            //so the returned node is the previous
            //statement/declaration. Look for the
            //containing body.
            class BodyVisitor extends Visitor {
                Node node, currentBody, result;
                BodyVisitor(Node node, Node root) {
                    this.node = node;
                    currentBody = root;
                }
                @Override
                public void visitAny(Node that) {
                    if (that==node) {
                        result = currentBody;
                    }
                    else {
                        Node cb = currentBody;
                        if (that instanceof Tree.Body) {
                            currentBody = that;
                        }
                        if (that instanceof Tree.NamedArgumentList) {
                            currentBody = that;
                        }
                        super.visitAny(that);
                        currentBody = cb;
                    }
                }
            }
            BodyVisitor mv = new BodyVisitor(node, rootNode);
            mv.visit(rootNode);
            node = mv.result;
        }
        
        if (node==null) node = rootNode; //we're in whitespace at the start of the file
        return node;
    }
    
    private static boolean isIdentifierOrKeyword(Token token) {
        int type = token.getType();
        return type==LIDENTIFIER || 
                type==UIDENTIFIER ||
                type==AIDENTIFIER ||
                type==PIDENTIFIER ||
                escaping_.get_().isKeyword(token.getText());
    }
    
    private static ICompletionProposal[] 
    constructCompletions(int offset, String prefix, 
            CeylonParseController cpc, Node node, 
            CommonToken token, Scope scope, 
            boolean returnedParamInfo, boolean memberOp, 
            final IDocument document, int tokenType, 
            IProgressMonitor monitor) {
        
        final List<ICompletionProposal> result = 
                new ArrayList<ICompletionProposal>();
        
        if (!returnedParamInfo && 
                atStartOfPositionalArgument(node, token)) {
            addFakeShowParametersCompletion(node, cpc, result);
            if (result.isEmpty()) {
                return null;
            }
        }
        else if (node instanceof Tree.PackageLiteral) {
            addPackageCompletions(cpc, offset, prefix, 
                    null, node, result, false, monitor);
        }
        else if (node instanceof Tree.ModuleLiteral) {
            addModuleCompletions(cpc, offset, prefix, 
                    null, node, result, false, monitor);
        }
        else if (isDescriptorPackageNameMissing(node)) {
            addCurrentPackageNameCompletion(cpc, offset, 
                    prefix, result);
        }
        else if (node instanceof Tree.Import && 
                offset>token.getStopIndex()+1) {
            addPackageCompletions(cpc, offset, prefix, 
                    null, node, result, 
                    nextTokenType(cpc, token)!=LBRACE, 
                    monitor);
        }
        else if (node instanceof Tree.ImportModule && 
                offset>token.getStopIndex()+1) {
            addModuleCompletions(cpc, offset, prefix, 
                    null, node, result, 
                    nextTokenType(cpc, token)!=STRING_LITERAL, 
                    monitor);
        }
        else if (node instanceof Tree.ImportPath) {
            Tree.CompilationUnit upToDateAndTypechecked =
                    cpc.getTypecheckedRootNode();
            if (upToDateAndTypechecked != null) {
                new ImportVisitor(prefix, token, offset,
                        node, cpc, result, monitor)
                            .visit(upToDateAndTypechecked);
            }
        }
        else if (isEmptyModuleDescriptor(cpc)) {
            addModuleDescriptorCompletion(cpc, offset, 
                    prefix, result);
            addKeywordProposals(cpc, offset, 
                    prefix, result, node, null, false, 
                    tokenType);
        }
        else if (isEmptyPackageDescriptor(cpc)) {
            addPackageDescriptorCompletion(cpc, offset, 
                    prefix, result);
            addKeywordProposals(cpc, offset, 
                    prefix, result, node, null, false, 
                    tokenType);
        }
        else if (node instanceof Tree.TypeArgumentList && 
                token.getType()==LARGER_OP) {
            if (offset==token.getStopIndex()+1) {
                addTypeArgumentListProposal(offset, cpc, 
                        node, scope, document, result);
            }
            else if (isMemberNameProposable(offset, node, memberOp)) {
                addMemberNameProposals(offset, cpc, node, result);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
        return result.toArray(NO_COMPLETIONS);
    }

    private static boolean atStartOfPositionalArgument(
            Node node, CommonToken token) {
        if (node instanceof Tree.PositionalArgumentList) {
            int type = token.getType();
            return type==LPAREN || type==COMMA;
        }
        else if (node instanceof Tree.NamedArgumentList) {
            int type = token.getType();
            return type==LBRACE || type==SEMICOLON;
        }
        else {
            return false;
        }
    }
    
    private static boolean isDescriptorPackageNameMissing(Node node) {
        Tree.ImportPath path;
        if (node instanceof Tree.ModuleDescriptor) {
            Tree.ModuleDescriptor md = 
                    (Tree.ModuleDescriptor) node;
            path = md.getImportPath();
        }
        else if (node instanceof Tree.PackageDescriptor) {
            Tree.PackageDescriptor pd = 
                    (Tree.PackageDescriptor) node;
            path = pd.getImportPath();
        }
        else {
            return false;
        }
        return path==null || 
                path.getIdentifiers().isEmpty();
    }

    private static boolean isMemberNameProposable(int offset, 
            Node node, boolean memberOp) {
        CommonToken token = (CommonToken) node.getEndToken();
        return !memberOp && token!=null && 
                token.getStopIndex()>=offset-2;
    }
    
    private ICompletionProposal[] 
    constructCompletions(int offset, String prefix, 
            Set<DeclarationWithProximity> sortedProposals, 
            Set<DeclarationWithProximity> sortedFunctionProposals, 
            CeylonParseController controller, Scope scope,
            final Node node, CommonToken token, 
            boolean memberOp, IDocument doc, 
            boolean secondLevel, boolean inDoc,
            Type requiredType, int previousTokenType, 
            int tokenType) {
        
        List<ICompletionProposal> result = 
                new ArrayList<ICompletionProposal>();
        Tree.CompilationUnit rootNode =
                controller.getTypecheckedRootNode();
        if (rootNode == null) {
            return result.toArray(NO_COMPLETIONS);
        }
        OccurrenceLocation ol =
                getOccurrenceLocation(rootNode, node, offset);
        Unit unit = node.getUnit();
        if (node instanceof Tree.Term) {
            addParametersProposal(offset, node, result);
        }
        else if (node instanceof Tree.ArgumentList) {
            class FindInvocationVisitor extends Visitor {
                Tree.InvocationExpression result;
                @Override
                public void visit(Tree.InvocationExpression that) {
                    if (that.getNamedArgumentList()==node ||
                        that.getPositionalArgumentList()==node) {
                        result = that;
                    }
                    super.visit(that);
                }
            }
            FindInvocationVisitor fiv = new FindInvocationVisitor();
            fiv.visit(rootNode);
            Tree.InvocationExpression ie = fiv.result;
            if (ie!=null) {
                addParametersProposal(offset, ie, result);
            }
        }
        
        if (node instanceof Tree.TypeConstraint) {
            for (DeclarationWithProximity dwp: sortedProposals) {
                Declaration dec = dwp.getDeclaration();
                if (isTypeParameterOfCurrentDeclaration(node, dec)) {
                    addReferenceProposal(offset, prefix, controller,
                            result, dwp, scope, false, null, ol);
                }
            }
        }
        else if (prefix.isEmpty() && ol!=IS &&
                isMemberNameProposable(offset, node, memberOp) &&
                (node instanceof Tree.Type || 
                node instanceof Tree.BaseTypeExpression ||
                node instanceof Tree.QualifiedTypeExpression)) {
            
            //member names we can refine
            Type t=null;
            if (node instanceof Tree.Type) {
                Tree.Type type = (Tree.Type) node;
                t = type.getTypeModel();
            }
            else if (node instanceof Tree.BaseTypeExpression) {
                Tree.BaseTypeExpression bte = 
                        (Tree.BaseTypeExpression) node;
                Reference target = bte.getTarget();
                if (target!=null) {
                    t = target.getType();
                }
            }
            else if (node instanceof Tree.QualifiedTypeExpression) {
                Tree.QualifiedTypeExpression qte = 
                        (Tree.QualifiedTypeExpression) node;
                Reference target = qte.getTarget();
                if (target!=null) {
                    t = target.getType();
                }
            }
            if (t!=null) {
                addRefinementProposals(offset, 
                        sortedProposals, controller, scope, node, doc,
                        secondLevel, result, ol, t, false);
            }
            //otherwise guess something from the type
            addMemberNameProposal(offset, prefix, node, result, rootNode);
        }
        else if (node instanceof Tree.TypedDeclaration && 
                !(node instanceof Tree.Variable && 
                        ((Tree.Variable) node).getType() 
                                instanceof Tree.SyntheticVariable) &&
                !(node instanceof Tree.InitializerParameter) &&
                isMemberNameProposable(offset, node, memberOp)) {
            //member names we can refine
            Tree.TypedDeclaration td = 
                    (Tree.TypedDeclaration) node;
            Tree.Type dnt = td.getType();
            if (dnt!=null && dnt.getTypeModel()!=null) {
                Type t = dnt.getTypeModel();
                addRefinementProposals(offset, sortedProposals, 
                        controller, scope, node, doc, secondLevel,
                        result, ol, t, true);
            }
            //otherwise guess something from the type
            addMemberNameProposal(offset, prefix, node, result, rootNode);
        }
        else {
            boolean isMember = 
                    node instanceof Tree.QualifiedMemberOrTypeExpression ||
                    node instanceof Tree.QualifiedType ||
                    node instanceof Tree.MemberLiteral && 
                            ((Tree.MemberLiteral) node).getType()!=null;
            
            if (!secondLevel && !inDoc && !memberOp) {
                addKeywordProposals(controller, offset, prefix,
                        result, node, ol, isMember, tokenType);
                //addTemplateProposal(offset, prefix, result);
            }
            
            if (!secondLevel && !inDoc && !isMember) {
                if (prefix.isEmpty() && 
                        !isTypeUnknown(requiredType) &&
                        unit.isCallableType(requiredType)) {
                    addAnonFunctionProposal(offset, requiredType, 
                            result, unit);
                }
            }
            
            boolean isPackageOrModuleDescriptor = 
                    isModuleDescriptor(controller) ||
                    isPackageDescriptor(controller);
            for (DeclarationWithProximity dwp: sortedProposals) {
                Declaration dec = dwp.getDeclaration();
            try {
                if (!dec.isToplevel() && 
                    !dec.isClassOrInterfaceMember() &&
                    dec.getUnit().equals(unit)) {
                    Node decNode = 
                            getReferencedNodeInUnit(dec, 
                                    rootNode);
                    if (decNode!=null && 
                            offset<getIdentifyingNode(decNode)
                                        .getStartIndex()) {
                        continue;
                    }
                }
                
                if (isPackageOrModuleDescriptor && !inDoc && 
                        ol!=META && (ol==null || !ol.reference) &&
                    (!dec.isAnnotation() || 
                            !(dec instanceof Function))) {
                    continue;
                }
                
                if (!secondLevel && 
                        isParameterOfNamedArgInvocation(scope, dwp) &&
                        isDirectlyInsideNamedArgumentList(controller, node, token)) {
                    addNamedArgumentProposal(offset, prefix, controller,
                            result, dec, scope);
                    addInlineFunctionProposal(offset, dec, scope, 
                            node, prefix, controller, doc, result);
                }

                CommonToken nextToken = getNextToken(controller, token);
                boolean noParamsFollow = noParametersFollow(nextToken);
                
                if (!secondLevel && !inDoc && noParamsFollow &&
                        isInvocationProposable(dwp, ol, previousTokenType) && 
                        (!isQualifiedType(node) ||
                                isConstructor(dec) || 
                                dec.isStaticallyImportable()) &&
                        (!(scope instanceof Constructor) || 
                                ol!=EXTENDS || 
                                isDelegatableConstructor(scope, dec))) {
                    for (Declaration d: overloads(dec)) {
                        Reference pr = isMember ? 
                                getQualifiedProducedReference(node, d) :
                                getRefinedProducedReference(scope, d);
                        addInvocationProposals(
                                offset, prefix, controller, result,
                                dwp, d, pr, scope, ol, null, isMember);
                    }
                }
                
                if (isProposable(dwp, ol, scope, unit, 
                            requiredType, previousTokenType) && 
                        isProposable(node, ol, dec) &&
                        (definitelyRequiresType(ol) || 
                                noParamsFollow || 
                                dec instanceof Functional) &&
                        (!(scope instanceof Constructor) || 
                                ol!=EXTENDS || 
                                isDelegatableConstructor(scope, dec))) {
                    if (ol==DOCLINK) {
                        addDocLinkProposal(offset, prefix, 
                                controller, result, dec, scope);
                    }
                    else if (ol==IMPORT) {
                        addImportProposal(
                                offset, prefix, 
                                controller, result, dec, scope);
                    }
                    else if (ol!=null && ol.reference) {
                        if (isReferenceProposable(ol, dec)) {
                            addProgramElementReferenceProposal(
                                    offset, prefix, 
                                    controller, result, dec, scope,
                                    isMember);
                        }
                    }
                    else {
                        Reference pr = isMember ? 
                                getQualifiedProducedReference(node, dec) :
                                getRefinedProducedReference(scope, dec);
                        if (secondLevel) {
                            addSecondLevelProposal(
                                    offset, prefix, 
                                    controller, result, dec, scope,
                                    false, pr, requiredType, ol);
                        }
                        else {
                            if (!(dec instanceof Function) || 
                                    !isAbstraction(dec) || 
                                    !noParamsFollow) {
                                addReferenceProposal(
                                        offset, prefix, 
                                        controller, result, dwp, scope,
                                        isMember, pr, ol);
                            }
                        }
                    }
                }

                if (!memberOp && !secondLevel &&
                        isProposable(dwp, ol, scope, unit, 
                                requiredType, previousTokenType) && 
                        ol!=IMPORT && ol!=CASE && ol!=CATCH &&
                        isDirectlyInsideBlock(node, 
                                controller, scope, token)) {
                    addForProposal(offset, prefix, 
                            controller, result, dwp, dec);
                    addIfExistsProposal(offset, prefix, 
                            controller, result, dwp, dec);
                    addAssertExistsProposal(offset, prefix,
                            controller, result, dwp, dec);
                    addIfNonemptyProposal(offset, prefix, 
                            controller, result, dwp, dec);
                    addAssertNonemptyProposal(offset, prefix,
                            controller, result, dwp, dec);
                    addTryProposal(offset, prefix, 
                            controller, result, dwp, dec);
                    addSwitchProposal(offset, prefix, 
                            controller, result, dwp, dec, node, doc);
                }

                if (!memberOp && !isMember && !secondLevel) {
                    for (Declaration d: overloads(dec)) {
                        if (isRefinementProposable(d, ol, scope)) {
                            addRefinementProposal(offset, d, 
                                    (ClassOrInterface) scope, 
                                    node, scope, prefix, 
                                    controller, doc, result, true);
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            }
            
            if (node instanceof Tree.QualifiedMemberExpression ||
                    memberOp && node instanceof Tree.QualifiedTypeExpression) {
                for (DeclarationWithProximity dwp: sortedFunctionProposals) {
                    Tree.QualifiedMemberOrTypeExpression qmte = 
                            (Tree.QualifiedMemberOrTypeExpression) 
                                node;
                    Tree.Primary primary = qmte.getPrimary();
                    addFunctionProposal(offset, controller, primary,
                            result, dwp.getDeclaration(), doc);
                }
            }
        }
        if (previousTokenType==CeylonLexer.OBJECT_DEFINITION) {
            addKeywordProposals(controller, offset, prefix,
                        result, node, ol, false, tokenType);
        }
        return result.toArray(NO_COMPLETIONS);
    }

    private static boolean isDelegatableConstructor(
            Scope scope, Declaration dec) {
        if (isConstructor(dec)) {
            Scope container = dec.getContainer();
            Scope outerScope = scope.getContainer();
            if (container==null || outerScope==null) {
                return false;
            }
            else if (outerScope.equals(container)) {
                return !scope.equals(dec); //local constructor
            }
            else {
                TypeDeclaration id =
                        scope.getInheritingDeclaration(dec);
                return id!=null && id.equals(outerScope); //inherited constructor
            }
        }
        else if (dec instanceof Class) {
            Scope outerScope = scope.getContainer();
            if (outerScope instanceof Class) {
                Class c = (Class) outerScope;
                Type sup = c.getExtendedType();
                return sup!=null && 
                        sup.getDeclaration().equals(dec); 
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }

    private static boolean isProposable(Node node, 
            OccurrenceLocation ol, Declaration dec) {
        if (ol!=EXISTS && ol!=NONEMPTY && ol!=IS) {
            return true;
        }
        else if (dec instanceof Value) {
            Value val = (Value) dec;
            Type type = val.getType();
            if (val.isVariable() || val.isTransient() || 
                val.isDefault() || val.isFormal() ||
                isTypeUnknown(type)) {
                return false;
            }
            else {
                Unit unit = node.getUnit();
                switch (ol) {
                case EXISTS:
                    return unit.isOptionalType(type);
                case NONEMPTY:
                    return unit.isPossiblyEmptyType(type);
                case IS:
                    return true;
                default:
                    return false;
                }
            }
        }
        else {
            return false;
        }
    }

    private static boolean isReferenceProposable(OccurrenceLocation ol,
            Declaration dec) {
        return (ol==VALUE_REF || !(dec instanceof Value) || 
                    ((Value)dec).getTypeDeclaration().isAnonymous()) &&
            (ol==FUNCTION_REF || !(dec instanceof Function)) &&
            (ol==ALIAS_REF || !(dec instanceof TypeAlias)) &&
            (ol==TYPE_PARAMETER_REF || !(dec instanceof TypeParameter)) &&
            //note: classes and interfaces are almost always proposable 
            //      because they are legal qualifiers for other refs
            (ol!=TYPE_PARAMETER_REF || dec instanceof TypeParameter);
    }

    private static void addRefinementProposals(int offset,
            Set<DeclarationWithProximity> set, 
            CeylonParseController cpc, Scope scope, 
            Node node, IDocument doc, boolean filter,
            final List<ICompletionProposal> result, 
            OccurrenceLocation ol, Type t, 
            boolean preamble) {
        for (DeclarationWithProximity dwp: set) {
            Declaration dec = dwp.getDeclaration();
            if (!filter && dec instanceof FunctionOrValue) {
                FunctionOrValue m = (FunctionOrValue) dec;
                for (Declaration d: overloads(dec)) {
                    if (isRefinementProposable(d, ol, scope) &&
                            isReturnType(t, m, node)) {
                        try {
                            int start = node.getStartIndex();
                            String pfx = 
                                    doc.get(start, offset-start);
                            addRefinementProposal(offset, d, 
                                    (ClassOrInterface) scope, 
                                    node, scope, pfx, 
                                    cpc, doc, result, preamble);
                        }
                        catch (BadLocationException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static final List<Type> NO_TYPES = Collections.<Type>emptyList();

    private static boolean isReturnType(Type t, FunctionOrValue m, Node node) {
        if (t.isSubtypeOf(m.getType())) {
            return true;
        }
        if (node instanceof Tree.TypedDeclaration) {
            Tree.TypedDeclaration td = (Tree.TypedDeclaration) node;
            Scope container = td.getDeclarationModel().getContainer();
            if (container instanceof ClassOrInterface) {
                ClassOrInterface ci = (ClassOrInterface) container;
                Type type =
                        ci.getType()
                            .getTypedMember(m, NO_TYPES)
                            .getType();
                if (t.isSubtypeOf(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isQualifiedType(Node node) {
        if (node instanceof Tree.QualifiedType) {
            return true;
        }
        else if (node instanceof Tree.QualifiedMemberOrTypeExpression) {
            Tree.QualifiedMemberOrTypeExpression qmte =
                    (Tree.QualifiedMemberOrTypeExpression)
                        node;
            return qmte.getStaticMethodReference();
        }
        else {
            return false;
        }
    }

    private static boolean noParametersFollow(CommonToken nextToken) {
        return nextToken==null ||
                //should we disable this, since a statement
                //can in fact begin with an LPAREN??
                nextToken.getType()!=LPAREN
                //disabled now because a declaration can
                //begin with an LBRACE (an Iterable type)
                /*&& nextToken.getType()!=CeylonLexer.LBRACE*/;
    }
    
    private static boolean definitelyRequiresType(
            OccurrenceLocation ol) {
        return ol==SATISFIES || 
                ol==OF || 
                ol==UPPER_BOUND || 
                ol==TYPE_ALIAS;
    }

    private static CommonToken getNextToken(
            CeylonParseController cpc, CommonToken token) {
        int i = token.getTokenIndex();
        CommonToken nextToken=null;
        List<CommonToken> tokens = cpc.getTokens();
        do {
            if (++i<tokens.size()) {
                nextToken = tokens.get(i);
            }
            else {
                break;
            }
        }
        while (nextToken.getChannel()==HIDDEN_CHANNEL);
        return nextToken;
    }

    private static boolean isDirectlyInsideNamedArgumentList(
            CeylonParseController cpc, Node node, 
            CommonToken token) {
        return node instanceof Tree.NamedArgumentList ||
                (!(node instanceof Tree.SequenceEnumeration) &&
                        occursAfterBraceOrSemicolon(token, cpc.getTokens()));
    }
    
    private static boolean isMemberOperator(Token token) {
        int type = token.getType();
        return type==MEMBER_OP || 
                type==SPREAD_OP ||
                type==SAFE_MEMBER_OP;
    }

    private static boolean isRefinementProposable(
            Declaration dec, OccurrenceLocation ol, 
            Scope scope) {
        return ol==null && 
                (dec.isDefault() || dec.isFormal()) &&
                (dec instanceof FunctionOrValue || dec instanceof Class) && 
                scope instanceof ClassOrInterface &&
                ((ClassOrInterface) scope).isInheritedFromSupertype(dec);
    }
    
    private static boolean isInvocationProposable(
            DeclarationWithProximity dwp, 
            OccurrenceLocation ol, int previousTokenType) {
        Declaration dec = dwp.getDeclaration();
        return dec instanceof Functional && 
                previousTokenType!=IS_OP && (previousTokenType!=CASE_TYPES||ol==OF) &&
                (ol==null || 
                 ol==EXPRESSION && (!(dec instanceof Class) || !((Class) dec).isAbstract()) || 
                 ol==EXTENDS && dec instanceof Class && !((Class) dec).isFinal() && 
                         ((Class) dec).getTypeParameters().isEmpty() ||
                 ol==EXTENDS && isConstructor(dec) && !((Class) dec.getContainer()).isFinal() && 
                         ((Class) dec.getContainer()).getTypeParameters().isEmpty() ||
                 ol==CLASS_ALIAS && dec instanceof Class ||
                 ol==PARAMETER_LIST && dec instanceof Function && 
                         dec.isAnnotation()) &&
                dwp.getNamedArgumentList()==null &&
                (!dec.isAnnotation() || !(dec instanceof Function) || 
                        !((Function) dec).getParameterLists().isEmpty() &&
                        !((Function) dec).getParameterLists().get(0).getParameters().isEmpty());
    }

    private static boolean isProposable(
            DeclarationWithProximity dwp, 
            OccurrenceLocation ol, Scope scope, Unit unit, 
            Type requiredType, int previousTokenType) {
        Declaration dec = dwp.getDeclaration();
        return (ol!=EXTENDS || dec instanceof Class && !((Class) dec).isFinal() || 
                               isConstructor(dec) && !((Class) dec.getContainer()).isFinal()) && 
               (ol!=CLASS_ALIAS || dec instanceof Class) &&
               (ol!=SATISFIES || dec instanceof Interface) &&
               (ol!=OF || dec instanceof Class || isAnonymousClassValue(dec)) && 
               ((ol!=TYPE_ARGUMENT_LIST && ol!=UPPER_BOUND && ol!=TYPE_ALIAS && ol!=CATCH) || 
                       dec instanceof TypeDeclaration) &&
               (ol!=CATCH || isExceptionType(unit, dec)) &&
               (ol!=PARAMETER_LIST ||
                       dec instanceof TypeDeclaration || 
                       dec instanceof Function && dec.isAnnotation() || //i.e. an annotation 
                       dec instanceof Value && dec.getContainer().equals(scope)) && //a parameter ref
               (ol!=IMPORT || !dwp.isUnimported()) &&
               (ol!=CASE || isCaseOfSwitch(requiredType, dec, previousTokenType)) &&
               (previousTokenType!=IS_OP && (previousTokenType!=CASE_TYPES||ol==OF) || 
                       dec instanceof TypeDeclaration) &&
               ol!=TYPE_PARAMETER_LIST && 
               dwp.getNamedArgumentList()==null;
    }

    private static boolean isCaseOfSwitch(Type requiredType,
            Declaration dec, int previousTokenType) {
        return previousTokenType==IS_OP &&
                isTypeCaseOfSwitch(requiredType, dec) || 
                previousTokenType!=IS_OP && 
                isValueCaseOfSwitch(requiredType, dec);
    }

    private static boolean isValueCaseOfSwitch(Type requiredType,
            Declaration dec) {
        if (requiredType!=null && requiredType.isUnion()) {
            for (Type td: requiredType.getCaseTypes()) {
                if (isValueCaseOfSwitch(td, dec)) {
                    return true;
                }
            }
            return false;
        }
        else {
            if (isAnonymousClassValue(dec)) {
                if (requiredType==null) return true;
                TypedDeclaration d = (TypedDeclaration) dec;
                TypeDeclaration td = d.getTypeDeclaration();
                TypeDeclaration rtd = requiredType.getDeclaration();
                return td.inherits(rtd);
            }
            else {
                return false;
            }
        }
    }

    private static boolean isTypeCaseOfSwitch(Type requiredType,
            Declaration dec) {
        if (requiredType!=null && requiredType.isUnion()) {
            for (Type td: requiredType.getCaseTypes()) {
                if (isTypeCaseOfSwitch(td, dec)) {
                    return true;
                }
            }
            return false;
        }
        else {
            if (dec instanceof TypeDeclaration) {
                if (requiredType==null) return true;
                TypeDeclaration td = (TypeDeclaration) dec;
                TypeDeclaration rtd = requiredType.getDeclaration();
                return td.inherits(rtd);
            }
            else {
                return false;
            }
        }
    }

    private static boolean isExceptionType(Unit unit, Declaration dec) {
        if (dec instanceof TypeDeclaration) { 
            TypeDeclaration td = (TypeDeclaration) dec;
            return td.inherits(unit.getExceptionDeclaration());
        }
        else {
            return false;
        }
    }

    private static boolean isAnonymousClassValue(Declaration dec) {
        if (dec instanceof Value) { 
            Value value = (Value) dec;
            TypeDeclaration vtd = value.getTypeDeclaration();
            return vtd!=null && vtd.isAnonymous();
        }
        else {
            return false;
        }
    }

    private static boolean isTypeParameterOfCurrentDeclaration(
            Node node, Declaration d) {
        //TODO: this is a total mess and totally error-prone 
        //       - figure out something better!
        if (d instanceof TypeParameter) { 
            TypeParameter tp = (TypeParameter) d;
            Scope tpc = tp.getContainer();
            if (tpc==node.getScope()) {
                return true;
            }
            else {
                Tree.TypeConstraint constraint = 
                        (Tree.TypeConstraint) node;
                TypeParameter tcp = 
                        constraint.getDeclarationModel();
                return tcp!=null && tpc==tcp.getContainer();
            }
        }
        else {
            return false;
        }
    }
    
    private static boolean isParameterOfNamedArgInvocation(
            Scope scope, DeclarationWithProximity d) {
        return scope==d.getNamedArgumentList();
    }

    private static Reference getQualifiedProducedReference(
            Node node, Declaration d) {
        Type pt;
        if (node instanceof Tree.QualifiedMemberOrTypeExpression) {
            Tree.QualifiedMemberOrTypeExpression qmte = 
                    (Tree.QualifiedMemberOrTypeExpression) 
                        node;
            pt = qmte.getPrimary().getTypeModel();
        }
        else if (node instanceof Tree.QualifiedType) {
            Tree.QualifiedType qt = 
                    (Tree.QualifiedType) node;
            pt = qt.getOuterType().getTypeModel();
        }
        else {
            return null;
        }
        if (pt!=null && d.isClassOrInterfaceMember()) {
            TypeDeclaration container = 
                    (TypeDeclaration) 
                        d.getContainer();
            pt = pt.getSupertype(container);
        }
        return d.appliedReference(pt, 
                Collections.<Type>emptyList());
    }

    private static Set<DeclarationWithProximity> 
    sortProposals(String prefix, Types.Required required,
            Map<String, DeclarationWithProximity> proposals) {
        Set<DeclarationWithProximity> set = 
                new TreeSet<DeclarationWithProximity>
                    (new ProposalComparator(prefix, required));
        set.addAll(proposals.values());
        return set;
    }
    
    public static Map<String, DeclarationWithProximity> 
    getProposals(Node node, Scope scope, 
            Tree.CompilationUnit rootNode, Cancellable cancellable) {
       return getProposals(node, scope, "", false, rootNode, cancellable);
    }

    private static Map<String, DeclarationWithProximity>
    getFunctionProposals(Node node, Scope scope,
            String prefix, boolean memberOp, Cancellable cancellable) {
        Unit unit = node.getUnit();
        if (node instanceof Tree.QualifiedMemberOrTypeExpression) {
            Tree.QualifiedMemberOrTypeExpression qmte =
                    (Tree.QualifiedMemberOrTypeExpression)
                        node;
            Type type = getPrimaryType(qmte);
            if (!qmte.getStaticMethodReference() &&
                    !isTypeUnknown(type)) {
                return collectUnaryFunctions(type,
                        scope.getMatchingDeclarations(
                                unit, prefix, 0, cancellable));
            }
        }
        else if (memberOp && node instanceof Tree.Term) {
            Type type = null;
            if (node instanceof Tree.Term) {
                Tree.Term term = (Tree.Term) node;
                type = term.getTypeModel();
            }
            if (type!=null) {
                return collectUnaryFunctions(type,
                        scope.getMatchingDeclarations(
                                unit, prefix, 0, cancellable));
            }
            else {
                return emptyMap();
            }
        }
        return emptyMap();
    }

    public static Map<String, DeclarationWithProximity>
    collectUnaryFunctions(Type type,
            Map<String, DeclarationWithProximity> candidates) {
        Map<String,DeclarationWithProximity> matches =
                new HashMap<String, DeclarationWithProximity>();
        for (Map.Entry<String,DeclarationWithProximity> e:
                candidates.entrySet()) {
            Declaration declaration =
                    e.getValue().getDeclaration();
            if (declaration instanceof Function &&
                    !declaration.isAnnotation()) {
                Function m = (Function) declaration;
                List<ParameterList> pls =
                        m.getParameterLists();
                if (!pls.isEmpty()) {
                    ParameterList pl = pls.get(0);
                    List<Parameter> params =
                            pl.getParameters();
                    if (!params.isEmpty()) {
                        boolean unary=true;
                        for (int i=1; i<params.size(); i++) {
                            if (!params.get(i).isDefaulted()) {
                                unary = false;
                            }
                        }
                        Type t = params.get(0).getType();
                        if (unary && !isTypeUnknown(t) &&
                                type.isSubtypeOf(t)) {
                            matches.put(e.getKey(),
                                    e.getValue());
                        }
                    }
                }
            }
        }
        return matches;
    }

    private static Map<String, DeclarationWithProximity>
    getProposals(Node node, Scope scope, String prefix,
            boolean memberOp, Tree.CompilationUnit rootNode, Cancellable cancellable) {
        Unit unit = node.getUnit();
        if (node instanceof Tree.MemberLiteral) {
            Tree.MemberLiteral ml = (Tree.MemberLiteral) node;
            Tree.StaticType mlt = ml.getType();
            if (mlt!=null) {
                Type type = mlt.getTypeModel();
                if (type!=null) {
                    return type.resolveAliases()
                            .getDeclaration()
                            .getMatchingMemberDeclarations(
                                    unit, scope, prefix, 0);
                }
                else {
                    return emptyMap();
                }
            }
        }
        else if (node instanceof Tree.TypeLiteral) {
            Tree.TypeLiteral tl = (Tree.TypeLiteral) node;
            Tree.StaticType tlt = tl.getType();
            if (tlt instanceof Tree.BaseType) {
                Tree.BaseType bt = (Tree.BaseType) tlt;
                if (bt.getPackageQualified()) {
                    return unit.getPackage()
                            .getMatchingDirectDeclarations(
                                    prefix, 0);
                }
            }
            if (tlt!=null) {
                Type type = tlt.getTypeModel();
                if (type!=null) {
                    return type.resolveAliases()
                            .getDeclaration()
                            .getMatchingMemberDeclarations(
                                    unit, scope, prefix, 0);
                }
                else {
                    return emptyMap();
                }
            }
        }

        if (node instanceof Tree.QualifiedMemberOrTypeExpression) {
            Tree.QualifiedMemberOrTypeExpression qmte =
                    (Tree.QualifiedMemberOrTypeExpression)
                        node;
            Type type = getPrimaryType(qmte);
            if (qmte.getStaticMethodReference()) {
                type = unit.getCallableReturnType(type);
            }
            if (type!=null && !type.isUnknown()) {
                return type.resolveAliases()
                        .getDeclaration()
                        .getMatchingMemberDeclarations(
                                unit, scope, prefix, 0);
            }
            else {
                Tree.Primary primary = qmte.getPrimary();
                if (primary instanceof Tree.MemberOrTypeExpression) {
                    //it might be a qualified type or even a static method reference
                    Tree.MemberOrTypeExpression pmte =
                            (Tree.MemberOrTypeExpression)
                                primary;
                    Declaration d = pmte.getDeclaration();
                    if (d instanceof TypeDeclaration) {
                        TypeDeclaration td =
                                (TypeDeclaration) d;
                        type = td.getType();
                        if (type!=null) {
                            return type.resolveAliases()
                                    .getDeclaration()
                                    .getMatchingMemberDeclarations(
                                            unit, scope, prefix, 0);
                        }
                    }
                }
                else if (primary instanceof Tree.Package) {
                    return unit.getPackage()
                            .getMatchingDirectDeclarations(
                                    prefix, 0);
                }
            }
            return emptyMap();
        }
        else if (node instanceof Tree.QualifiedType) {
            Tree.QualifiedType type =
                    (Tree.QualifiedType) node;
            Type t = type.getOuterType().getTypeModel();
            if (t!=null) {
                return t.resolveAliases()
                        .getDeclaration()
                        .getMatchingMemberDeclarations(
                                unit, scope, prefix, 0);
            }
            else {
                return emptyMap();
            }
        }
        else if (node instanceof Tree.BaseType) {
            Tree.BaseType type = (Tree.BaseType) node;
            if (type.getPackageQualified()) {
                return unit.getPackage()
                        .getMatchingDirectDeclarations(
                                prefix, 0);
            }
            else if (scope!=null) {
                return scope.getMatchingDeclarations(
                        unit, prefix, 0, cancellable);
            }
            else {
                return emptyMap();
            }
        }
        else if (memberOp &&
                (node instanceof Tree.Term ||
                 node instanceof Tree.DocLink)) {
            Type type = null;
            if (node instanceof Tree.DocLink) {
                Tree.DocLink docLink = (Tree.DocLink) node;
                Declaration d = docLink.getBase();
                if (d != null) {
                    type = getResultType(d);
                    if (type == null) {
                        type = d.getReference().getFullType();
                    }
                }
            }
//            else if (node instanceof Tree.StringLiteral) {
//                type = null;
//            }
            else if (node instanceof Tree.Term) {
                Tree.Term term = (Tree.Term) node;
                type = term.getTypeModel();
            }

            if (type!=null) {
                return type.resolveAliases()
                        .getDeclaration()
                        .getMatchingMemberDeclarations(
                                unit, scope, prefix, 0);
            }
            else {
                return scope.getMatchingDeclarations(
                        unit, prefix, 0, cancellable);
            }
        }
        else {
            if (scope instanceof ImportList) {
                ImportList IL = (ImportList) scope;
                return IL.getMatchingDeclarations(
                        unit, prefix, 0, cancellable);
            }
            else {
                return scope==null ? //a null scope occurs when we have not finished parsing the file
                        getUnparsedProposals(rootNode, prefix, cancellable) :
                        scope.getMatchingDeclarations(
                                unit, prefix, 0, cancellable);
            }
        }
    }

    private static Type getPrimaryType(
            Tree.QualifiedMemberOrTypeExpression qme) {
        Type type =
                qme.getPrimary().getTypeModel();
        if (type==null) {
            return null;
        }
        else {
            Tree.MemberOperator mo = qme.getMemberOperator();
            Unit unit = qme.getUnit();
            if (mo instanceof Tree.SafeMemberOp) {
                return unit.getDefiniteType(type);
            }
            else if (mo instanceof Tree.SpreadOp) {
                return unit.getIteratedType(type);
            }
            else {
                return type;
            }
        }
    }

    private static Map<String, DeclarationWithProximity>
    getUnparsedProposals(Node node, String prefix, Cancellable cancellable) {
        if (node == null) {
            return newEmptyProposals();
        }
        Unit unit = node.getUnit();
        if (unit == null) {
            return newEmptyProposals();
        }
        Package pkg = unit.getPackage();
        if (pkg == null) {
            return newEmptyProposals();
        }
        return pkg.getModule()
                .getAvailableDeclarations(prefix, 0, cancellable);
    }

    private static TreeMap<String, DeclarationWithProximity>
    newEmptyProposals() {
        return new TreeMap<String,DeclarationWithProximity>();
    }
    
    static List<IContextInformation> computeParameterContextInformation(
            final int offset,
            final Tree.CompilationUnit rootNode, 
            final ITextViewer viewer) {
        final List<IContextInformation> infos = 
                new ArrayList<IContextInformation>();
        rootNode.visit(new Visitor() {
            @Override
            public void visit(Tree.InvocationExpression that) {
                Tree.ArgumentList al = 
                        that.getPositionalArgumentList();
                if (al==null) {
                    al = that.getNamedArgumentList();
                }
                if (al!=null) {
                    //TODO: should reuse logic for adjusting tokens
                    //      from CeylonContentProposer!!
                    Integer start = al.getStartIndex();
                    Integer stop = al.getEndIndex();
                    if (start!=null && stop!=null && offset>start) {
                        String string = "";
                        if (offset>stop) {
                            try {
                                string =
                                    viewer.getDocument()
                                        .get(stop, offset-stop);
                            } 
                            catch (BadLocationException e) {}
                        }
                        if (string.trim().isEmpty()) {
                            Unit unit = rootNode.getUnit();
                            Tree.Term primary = that.getPrimary();
                            Declaration declaration;
                            Reference target;
                            if (primary instanceof Tree.MemberOrTypeExpression) {
                                Tree.MemberOrTypeExpression mte = 
                                    (Tree.MemberOrTypeExpression) primary;
                                declaration = mte.getDeclaration();
                                target = mte.getTarget();
                            }
                            else {
                                declaration = null;
                                target = null;
                            }
                            if (declaration instanceof Functional) {
                                Functional fd =
                                        (Functional) declaration;
                                List<ParameterList> pls = 
                                        fd.getParameterLists();
                                if (!pls.isEmpty()) {
                                    //Note: This line suppresses the little menu 
                                    //      that gives me a choice of context infos.
                                    //      Delete it to get a choice of all surrounding
                                    //      argument lists.
                                    infos.clear();
                                    infos.add(new ParameterContextInformation(
                                            declaration, target, unit, 
                                            pls.get(0), start, true, 
                                            al instanceof Tree.NamedArgumentList));
                                }
                            }
                            else {
                                Type type = primary.getTypeModel();
                                if (unit.isCallableType(type)) {
                                    List<Type> argTypes = 
                                            unit.getCallableArgumentTypes(type);
                                    if (!argTypes.isEmpty()) {
                                        infos.clear();                                
                                        infos.add(new ParametersCompletionProposal.ParameterContextInformation(
                                                argTypes, start, unit));
                                    }
                                }
                            }
                        }
                    }
                }
                super.visit(that);
            }
        });
        return infos;
    }
    
}