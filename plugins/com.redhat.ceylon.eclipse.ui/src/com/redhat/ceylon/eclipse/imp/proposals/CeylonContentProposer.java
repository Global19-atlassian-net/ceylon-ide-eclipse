package com.redhat.ceylon.eclipse.imp.proposals;

import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.LIDENTIFIER;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.MEMBER_OP;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.RBRACE;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.SEMICOLON;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.UIDENTIFIER;
import static com.redhat.ceylon.eclipse.imp.core.CeylonReferenceResolver.getCompilationUnit;
import static com.redhat.ceylon.eclipse.imp.core.CeylonReferenceResolver.getReferencedNode;
import static com.redhat.ceylon.eclipse.imp.editor.CeylonAutoEditStrategy.getDefaultIndent;
import static com.redhat.ceylon.eclipse.imp.hover.CeylonDocumentationProvider.getDocumentation;
import static com.redhat.ceylon.eclipse.imp.outline.CeylonLabelProvider.ATTRIBUTE;
import static com.redhat.ceylon.eclipse.imp.outline.CeylonLabelProvider.PACKAGE;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.findNode;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.getOccurrenceLocation;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.getTokenIndexAtCharacter;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.EXPRESSION;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.EXTENDS;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.IMPORT;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.PARAMETER_LIST;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.SATISFIES;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.TYPE_ARGUMENT_LIST;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.TYPE_PARAMETER_LIST;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation.UPPER_BOUND;
import static com.redhat.ceylon.eclipse.imp.parser.CeylonTokenColorer.keywords;
import static com.redhat.ceylon.eclipse.imp.quickfix.CeylonQuickFixAssistant.getIndent;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.eclipse.imp.editor.SourceProposal;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IContentProposer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.redhat.ceylon.compiler.typechecker.model.Class;
import com.redhat.ceylon.compiler.typechecker.model.ClassOrInterface;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.DeclarationWithProximity;
import com.redhat.ceylon.compiler.typechecker.model.Functional;
import com.redhat.ceylon.compiler.typechecker.model.Generic;
import com.redhat.ceylon.compiler.typechecker.model.ImportList;
import com.redhat.ceylon.compiler.typechecker.model.Interface;
import com.redhat.ceylon.compiler.typechecker.model.Method;
import com.redhat.ceylon.compiler.typechecker.model.MethodOrValue;
import com.redhat.ceylon.compiler.typechecker.model.Module;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.Parameter;
import com.redhat.ceylon.compiler.typechecker.model.ParameterList;
import com.redhat.ceylon.compiler.typechecker.model.ProducedType;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.TypeParameter;
import com.redhat.ceylon.compiler.typechecker.model.TypedDeclaration;
import com.redhat.ceylon.compiler.typechecker.model.Value;
import com.redhat.ceylon.compiler.typechecker.model.ValueParameter;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.QualifiedMemberOrTypeExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.SimpleType;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.Type;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.eclipse.imp.outline.CeylonLabelProvider;
import com.redhat.ceylon.eclipse.imp.parser.CeylonParseController;
import com.redhat.ceylon.eclipse.imp.parser.CeylonSourcePositionLocator.OccurrenceLocation;
import com.redhat.ceylon.eclipse.ui.CeylonPlugin;
import com.redhat.ceylon.eclipse.ui.ICeylonResources;

public class CeylonContentProposer implements IContentProposer {
    
    public static Image DEFAULT_REFINEMENT = CeylonPlugin.getInstance()
            .getImageRegistry().get(ICeylonResources.CEYLON_DEFAULT_REFINEMENT);
    public static Image FORMAL_REFINEMENT = CeylonPlugin.getInstance()
            .getImageRegistry().get(ICeylonResources.CEYLON_FORMAL_REFINEMENT);
    
    
    /**
     * Returns an array of content proposals applicable relative to the AST of the given
     * parse controller at the given position.
     * 
     * (The provided ITextViewer is not used in the default implementation provided here
     * but but is stipulated by the IContentProposer interface for purposes such as accessing
     * the IDocument for which content proposals are sought.)
     * 
     * @param controller  A parse controller from which the AST of the document being edited
     *             can be obtained
     * @param int      The offset for which content proposals are sought
     * @param viewer    The viewer in which the document represented by the AST in the given
     *             parse controller is being displayed (may be null for some implementations)
     * @return        An array of completion proposals applicable relative to the AST of the given
     *             parse controller at the given position
     */
    public ICompletionProposal[] getContentProposals(IParseController controller,
            final int offset, ITextViewer viewer) {
        CeylonParseController cpc = (CeylonParseController) controller;
        
        //BEGIN HUGE BUG WORKAROUND
        //What is going on here is that when I have a list of proposals open
        //and then I type a character, IMP sends us the old syntax tree and
        //doesn't bother to even send us the character I just typed, except
        //in the ITextViewer. So we need to do some guessing to figure out
        //that there is a missing character in the token stream and take
        //corrective action. This should be fixed in IMP!
        CommonToken token;
        CommonToken previousToken;
        int index = getTokenIndexAtCharacter(cpc.getTokenStream(), offset-1);
        if (index<0) {
            index = -index;
            previousToken = (CommonToken) cpc.getTokenStream().get(index);
            token = index==cpc.getTokenStream().size()-1 ? 
                    null : (CommonToken) cpc.getTokenStream().get(index+1);
        }
        else {
            previousToken = index==0 ? 
                    null : (CommonToken) cpc.getTokenStream().get(index-1);
            token = (CommonToken) cpc.getTokenStream().get(index);
        }
        char charAtOffset = viewer.getDocument().get().charAt(offset-1);
        Character charInTokenAtOffset = token==null ? 
                null : token.getText().charAt(offset-token.getStartIndex()-1);
        String prefix = "";
        int start = offset;
        int end = offset;
        if (charInTokenAtOffset!=null && 
                charAtOffset==charInTokenAtOffset) {
            if (isIdentifier(token)) {
                prefix = token.getText().substring(0, offset-token.getStartIndex());
                start = token.getStartIndex();
                end = token.getStopIndex();
            }
        } 
        else {
            boolean isIdentifierChar = isJavaIdentifierPart(charAtOffset);
            if (previousToken!=null) {
                if (isIdentifierChar) {
                    if (previousToken.getType()==MEMBER_OP) {
                        prefix = Character.toString(charAtOffset);
                        start = previousToken.getStartIndex();
                        end = previousToken.getStopIndex();
                    }
                    else if (isIdentifier(previousToken)) {
                        prefix = previousToken.getText()+charAtOffset;
                        start = previousToken.getStartIndex();
                        end = previousToken.getStopIndex();
                    }
                    else {
                        prefix = Character.toString(charAtOffset);
                    }
                }
            }
            else if (isIdentifierChar) {
                prefix = Character.toString(charAtOffset);
            }
        }
        //END BUG WORKAROUND
        
        //adjust the token to account for unclosed blocks
        //we search for the first non-whitespace/non-comment
        //token to the left of the caret
        int tokenIndex = getTokenIndexAtCharacter(cpc.getTokenStream(), start);
        if (tokenIndex<0) tokenIndex = -tokenIndex;
        int adjustedStart = start;
        int adjustedEnd = end;
        Token adjustedToken = cpc.getTokenStream().get(tokenIndex); 
        int tokenType = adjustedToken.getType();
        while (--tokenIndex>=0 && 
                (adjustedToken.getChannel()==CommonToken.HIDDEN_CHANNEL //ignore whitespace and comments
                || ((CommonToken) adjustedToken).getStartIndex()==offset)) { //don't consider the token to the right of the caret
            adjustedToken = cpc.getTokenStream().get(tokenIndex);
            if (adjustedToken.getChannel()!=CommonToken.HIDDEN_CHANNEL) { //don't adjust to a ws/comment token
                adjustedStart = ((CommonToken) adjustedToken).getStartIndex();
                adjustedEnd = ((CommonToken) adjustedToken).getStopIndex()+1;
                tokenType = adjustedToken.getType();
                break;
            }
        }
                
        if (cpc.getRootNode() != null) {
            Node node = findNode(cpc.getRootNode(), adjustedStart, adjustedEnd);
            if (tokenType==RBRACE || tokenType==SEMICOLON) {
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
                BodyVisitor mv = new BodyVisitor(node, cpc.getRootNode());
                mv.visit(cpc.getRootNode());
                node = mv.result;
            }
            if (node==null) node = cpc.getRootNode(); //we're in whitespace at the start of the file
            return constructCompletions(offset, prefix, tokenType,
                        sortProposals(prefix, getProposals(node, prefix, 
                                cpc.getRootNode())),
                        cpc, node, viewer.getDocument());
        } 
        return null;
        
    }
    
    private static void addPackageCompletions(CeylonParseController cpc, 
            int offset, String prefix, Tree.ImportPath path, Node node, 
            List<ICompletionProposal> result) {
        StringBuilder fullPath = new StringBuilder();
        if (path!=null) {
            int ids = path.getIdentifiers().size();
            if (!prefix.isEmpty()) ids--; //when the path does not end in a .
            for (int i=0; i<ids; i++) {
                fullPath.append(path.getIdentifiers().get(i).getText()).append('.');
            }
            fullPath.setLength(offset-path.getStartIndex()-prefix.length());
        }
        int len = fullPath.length();
        fullPath.append(prefix);
        //TODO: someday it would be nice to propose from all packages 
        //      and auto-add the module dependency!
        /*TypeChecker tc = CeylonBuilder.getProjectTypeChecker(cpc.getProject().getRawProject());
      if (tc!=null) {
        for (Module m: tc.getContext().getModules().getListOfModules()) {*/
        //Set<Package> packages = new HashSet<Package>();
        for (Package p: node.getUnit().getPackage().getModule().getAllPackages()) {
            //if (!packages.contains(p)) {
                //packages.add(p);
                String pkg = p.getQualifiedNameString();
                if (!pkg.isEmpty() && pkg.startsWith(fullPath.toString())) {
                    boolean already = false;
                    for (ImportList il: node.getUnit().getImportLists()) {
                        if (il.getImportedPackage()==p) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) {
                        result.add(sourceProposal(offset, prefix, PACKAGE, 
                                "package " + pkg, pkg, 
                                pkg.substring(len), false));
                    }
                }
            //}
        }
    }
    
    private static boolean isIdentifier(Token token) {
        return token.getType()==LIDENTIFIER || 
                token.getType()==UIDENTIFIER;
    }
    
    private static ICompletionProposal[] constructCompletions(int offset, String prefix, int tokenType,
            Set<DeclarationWithProximity> set, CeylonParseController cpc, Node node, IDocument doc) {
        System.out.println("proposals for a " + node.getNodeType());
        List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();
        if (node instanceof Tree.Import) {
            addPackageCompletions(cpc, offset, prefix, null, node, result);
        }
        else if (node instanceof Tree.ImportPath) {
            addPackageCompletions(cpc, offset, prefix, (Tree.ImportPath) node, node, result);
        }
        else if (node instanceof Tree.TypedDeclaration && 
                !(((Tree.TypedDeclaration)node).getType() instanceof Tree.SyntheticVariable)) {
            addMemberNameProposal(offset, prefix, node, result);
        }
        else if (node instanceof Tree.TypeConstraint) {
            for (DeclarationWithProximity dwp: set) {
                Declaration dec = dwp.getDeclaration();
                if (isTypeParameterOfCurrentDeclaration(node, dec)) {
                    addBasicProposal(offset, prefix, cpc, result, dwp, dec, null);
                }
            }
        }
        else if (node instanceof Tree.UnionType || 
                node instanceof Tree.IntersectionType) {
            for (DeclarationWithProximity dwp: set) {
                Declaration dec = dwp.getDeclaration();
                if (isProposable(dec, null) && dec instanceof TypeDeclaration) {
                    addBasicProposal(offset, prefix, cpc, result, dwp, dec, null);
                }
            }
        }
        else if (node instanceof QualifiedMemberOrTypeExpression) {
            for (DeclarationWithProximity dwp: set) {
                Declaration dec = dwp.getDeclaration();
                addBasicProposal(offset, prefix, cpc, result, dwp, dec, EXPRESSION);
                if (isInvocationProposable(dec, EXPRESSION)) {
                    addInvocationProposals(offset, prefix, cpc, result, dwp, dec, EXPRESSION);
                }
            }
        }
        else if (node instanceof Tree.ClassOrInterface || 
                node instanceof Tree.VoidModifier ||
                node instanceof Tree.ValueModifier ||
                node instanceof Tree.FunctionModifier) {
            //no proposals 
        }
        else {
            OccurrenceLocation ol = getOccurrenceLocation(cpc.getRootNode(), node);
            if (isKeywordProposable(ol)) {
                addKeywordProposals(offset, prefix, result);
            }
            for (DeclarationWithProximity dwp: set) {
                Declaration dec = dwp.getDeclaration();
                if (isProposable(dec, ol)) {
                    addBasicProposal(offset, prefix, cpc, result, dwp, dec, ol);
                }
                if (isInvocationProposable(dec, ol)) {
                    addInvocationProposals(offset, prefix, cpc, result, dwp, dec, ol);
                }
                if (isRefinementProposable(dec, ol)) {
                    addRefinementProposal(offset, prefix, cpc, node, result, dec, doc);
                }
                if (isAttributeProposable(dec, ol)) {
                    addAttributeProposal(offset, prefix, cpc, result, dec);
                }
            }
        }
        return result.toArray(new ICompletionProposal[result.size()]);
    }

    private static boolean isKeywordProposable(OccurrenceLocation ol) {
        return ol==null || ol==EXPRESSION;
    }
    
    private static boolean isRefinementProposable(Declaration dec, OccurrenceLocation ol) {
        return ol==null && (dec instanceof MethodOrValue || dec instanceof Class);
    }

    private static boolean isAttributeProposable(Declaration dec, OccurrenceLocation ol) {
        return ol==null && dec instanceof ValueParameter && dec.isClassMember();
    }

    private static boolean isInvocationProposable(Declaration dec, OccurrenceLocation ol) {
        return dec instanceof Functional && !((Functional) dec).getParameterLists().isEmpty() &&
                (ol==null || ol==EXPRESSION || ol==EXTENDS && dec instanceof Class);
    }

    private static boolean isProposable(Declaration dec, OccurrenceLocation ol) {
        return (dec instanceof Class || ol!=EXTENDS) && 
                (dec instanceof Interface || ol!=SATISFIES) &&
                (dec instanceof TypeDeclaration || (ol!=TYPE_ARGUMENT_LIST && ol!=UPPER_BOUND)) &&
                (dec instanceof TypeDeclaration || dec instanceof Method && dec.isToplevel() || ol!=PARAMETER_LIST) &&
                ol!=TYPE_PARAMETER_LIST;
    }

    private static boolean isTypeParameterOfCurrentDeclaration(Node node, Declaration d) {
        //TODO: this is a total mess and totally error-prone - figure out something better!
        return d instanceof TypeParameter && (((TypeParameter) d).getContainer()==node.getScope() ||
                        ((Tree.TypeConstraint) node).getDeclarationModel()!=null &&
                        ((TypeParameter) d).getContainer()==((Tree.TypeConstraint) node).getDeclarationModel().getContainer());
    }

    private static void addAttributeProposal(int offset, String prefix, CeylonParseController cpc,
            List<ICompletionProposal> result, Declaration d) {
        Declaration member = d.getContainer().getMember(d.getName());
        if (member==null || member==d) {
            result.add(sourceProposal(offset, prefix, ATTRIBUTE, 
                            getDocumentationFor(cpc, d), 
                            getAttributeDescriptionFor(d), 
                            getAttributeTextFor(d), false));
        }
        //TODO: typechecker does not currently accept "super.x = x"
        //      as legal syntax
        /*else if (member instanceof TypedDeclaration && 
                member.isFormal() && d.getContainer().isInherited(member)
                && ((TypedDeclaration) member).getType()
                        .isSupertypeOf(((ValueParameter)d).getType())) {
            result.add(sourceProposal(offset, prefix, 
                    CeylonLabelProvider.ATTRIBUTE, 
                            getDocumentationFor(cpc, member), 
                            getAttributeRefinementDescriptionFor(d), 
                            getAttributeRefinementTextFor(d), false));
        }*/
    }

    private static void addRefinementProposal(int offset, String prefix, CeylonParseController cpc,
            Node node, List<ICompletionProposal> result, Declaration d, IDocument doc) {
        if ((d.isDefault() || d.isFormal()) &&
                node.getScope() instanceof ClassOrInterface &&
                ((ClassOrInterface) node.getScope()).isInheritedFromSupertype(d)) {
            //TODO: substitute type arguments of subtype
            //TODO: if it is equals() or hash, fill in the implementation
            result.add(sourceProposal(offset, prefix, 
                    d.isFormal() ? FORMAL_REFINEMENT : DEFAULT_REFINEMENT, 
                            getDocumentationFor(cpc, d), 
                            getRefinementDescriptionFor(d), 
                            getRefinementTextFor(d, "\n" + getIndent(node, doc)), false));
        }
    }

    private static void addBasicProposal(int offset, String prefix, CeylonParseController cpc,
            List<ICompletionProposal> result, DeclarationWithProximity dwp,
            Declaration d, OccurrenceLocation ol) {
        result.add(sourceProposal(offset, prefix, 
                CeylonLabelProvider.getImage(d),
                getDocumentationFor(cpc, d), 
                getDescriptionFor(dwp, ol), 
                getTextFor(dwp, ol), true));
    }

    private static void addInvocationProposals(int offset, String prefix, CeylonParseController cpc, 
            List<ICompletionProposal> result, DeclarationWithProximity dwp, 
            Declaration d, OccurrenceLocation ol) {
        boolean isAbstractClass = d instanceof Class && ((Class) d).isAbstract();
        if (!isAbstractClass || ol==EXTENDS) {
            result.add(sourceProposal(offset, prefix, 
                    CeylonLabelProvider.getImage(d),
                    getDocumentationFor(cpc, d), 
                    getPositionalInvocationDescriptionFor(dwp, ol), 
                    getPositionalInvocationTextFor(dwp, ol), true));
            List<ParameterList> pls = ((Functional) d).getParameterLists();
            if (ol!=EXTENDS && !pls.isEmpty() && pls.get(0).getParameters().size()>1) {
                //if there is more than one parameter, 
                //suggest a named argument invocation 
                result.add(sourceProposal(offset, prefix, 
                        CeylonLabelProvider.getImage(d),
                        getDocumentationFor(cpc, d), 
                        getNamedInvocationDescriptionFor(dwp), 
                        getNamedInvocationTextFor(dwp), true));
            }
        }
    }

    protected static void addMemberNameProposal(int offset, String prefix, Node node,
            List<ICompletionProposal> result) {
        Type type = ((Tree.TypedDeclaration) node).getType();
        if (type instanceof SimpleType) {
            String suggestedName = ((SimpleType) type).getIdentifier().getText();
            if (suggestedName!=null) {
                suggestedName = Character.toLowerCase(suggestedName.charAt(0)) + 
                        suggestedName.substring(1);
                if (suggestedName.startsWith(prefix) && 
                        !suggestedName.equals(prefix) && 
                        !keywords.contains(suggestedName)) {
                    result.add(sourceProposal(offset, prefix, null, 
                            "proposed name for new declaration", 
                            suggestedName, suggestedName, false));
                }
            }
        }
    }
    
    private static void addKeywordProposals(int offset, String prefix, 
            List<ICompletionProposal> result) {
        for (String keyword: keywords) {
            if (!prefix.isEmpty() && keyword.startsWith(prefix) 
                    && !keyword.equals(prefix)) {
                result.add(sourceProposal(offset, prefix, null, 
                        keyword + " keyword", keyword, keyword + " ", 
                        true));
            }
        }
    }
    
    private static String getDocumentationFor(CeylonParseController cpc, Declaration d) {
        return getDocumentation(getReferencedNode(d, getCompilationUnit(cpc, d)));
    }
    
    private static Set<DeclarationWithProximity> sortProposals(final String prefix,
            Map<String, DeclarationWithProximity> proposals) {
        Set<DeclarationWithProximity> set = new TreeSet<DeclarationWithProximity>(
                new Comparator<DeclarationWithProximity>() {
                    public int compare(DeclarationWithProximity x, DeclarationWithProximity y) {
                        String xName = x.getName();
                        String yName = y.getName();
                        if (prefix.length()!=0) {
                            int lowers =  isLowerCase(prefix.charAt(0)) ? -1 : 1;
                            if (isLowerCase(xName.charAt(0)) && 
                                    isUpperCase(yName.charAt(0))) {
                                return lowers;
                            }
                            else if (isUpperCase(xName.charAt(0)) && 
                                    isLowerCase(yName.charAt(0))) {
                                return -lowers;
                            }
                        }
                        if (x.getProximity()!=y.getProximity()) {
                            return new Integer(x.getProximity()).compareTo(y.getProximity());
                        }
                        return xName.compareTo(yName);
                    }
                });
        set.addAll(proposals.values());
        return set;
    }
    
    private static SourceProposal sourceProposal(final int offset, final String prefix,
            final Image image, String doc, String desc, final String text, 
            final boolean selectParams) {
        return new SourceProposal(desc, text, "", 
                new Region(offset - prefix.length(), prefix.length()), 
                offset + text.length(), doc) { 
            @Override
            public Image getImage() {
                return image;
            }
            @Override
            public Point getSelection(IDocument document) {
                if (selectParams) {
                    int locOfTypeArgs = text.indexOf('<');
                    int loc = locOfTypeArgs;
                    if (loc<0) loc = text.indexOf('(');
                    if (loc<0) loc = text.indexOf('=')+1;
                    int start;
                    int length;
                    if (loc<=0 || locOfTypeArgs<0 &&
                            (text.contains("()") || text.contains("{}"))) {
                        start = text.length();
                        length = 0;
                    }
                    else {
                        int endOfTypeArgs = text.indexOf('>'); 
                        int end = text.indexOf(',');
                        if (end<0) end = text.indexOf(';');
                        if (end<0) end = text.length()-1;
                        if (endOfTypeArgs>0) end = end < endOfTypeArgs ? end : endOfTypeArgs;
                        start = loc+1;
                        length = end-loc-1;
                    }
                    return new Point(offset-prefix.length() + start, length);
                }
                else {
                    int loc = text.indexOf("bottom;");
                    int length;
                    int start;
                    if (loc<0) {
                        start = offset + text.length()-prefix.length();
                        length = 0;
                    }
                    else {
                        start = offset + loc-prefix.length();
                        length = 6;
                    }
                    return new Point(start, length);
                }
            }
        };
    }
    
    public static Map<String, DeclarationWithProximity> getProposals(Node node, String prefix,
            Tree.CompilationUnit cu) {
        if (node instanceof Tree.QualifiedMemberOrTypeExpression) {
            ProducedType type = getPrimaryType((Tree.QualifiedMemberOrTypeExpression) node);
            if (type!=null) {
                return type.getDeclaration().getMatchingMemberDeclarations(prefix, 0);
            }
            else {
                return Collections.emptyMap();
            }
        }
        else {
            Map<String, DeclarationWithProximity> result = getLanguageModuleProposals(node, prefix);
            result.putAll(node.getScope().getMatchingDeclarations(node.getUnit(), prefix, 0));
            return result;
        }
    }

    private static ProducedType getPrimaryType(Tree.QualifiedMemberOrTypeExpression qme) {
        ProducedType type = qme.getPrimary().getTypeModel();
        if (type==null) return null;
        if (qme.getMemberOperator() instanceof Tree.SafeMemberOp) {
            return qme.getUnit().getDefiniteType(type);
        }
        else if (qme.getMemberOperator() instanceof Tree.SpreadOp) {
            return qme.getUnit().getElementType(type);
        }
        else {
            return type;
        }
    }
    
    //TODO: move this method to the model (perhaps make a LanguageModulePackage subclass)
    private static Map<String, DeclarationWithProximity> getLanguageModuleProposals(Node node, 
            String prefix) {
        Map<String, DeclarationWithProximity> result = new TreeMap<String, DeclarationWithProximity>();
        Module languageModule = node.getUnit().getPackage().getModule().getLanguageModule();
        if (languageModule!=null && !(node.getScope() instanceof ImportList)) {
            for (Package languageScope: languageModule.getPackages() ) {
                for (Map.Entry<String, DeclarationWithProximity> entry: 
                    languageScope.getMatchingDeclarations(null, prefix, 1000).entrySet()) {
                    if (entry.getValue().getDeclaration().isShared()) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }
    
    private static boolean forceExplicitTypeArgs(Declaration d, OccurrenceLocation ol) {
        if (ol==EXTENDS) {
            return true;
        }
        else {
            //TODO: this is a pretty limited implementation 
            //      for now, but eventually we could do 
            //      something much more sophisticated to
            //      guess is explicit type args will be
            //      necessary (variance, etc)
            if (d instanceof Functional) {
                List<ParameterList> pls = ((Functional) d).getParameterLists();
                return pls.isEmpty() || pls.get(0).getParameters().isEmpty();
            }
            else {
                return false;
            }
        }
    }
    
    private static String getTextFor(DeclarationWithProximity d, 
            OccurrenceLocation ol) {
        StringBuilder result = new StringBuilder(d.getName());
        if (ol!=IMPORT) appendTypeParameters(d.getDeclaration(), result);
        return result.toString();
    }
    
    private static String getPositionalInvocationTextFor(DeclarationWithProximity d,
            OccurrenceLocation ol) {
        StringBuilder result = new StringBuilder(d.getName());
        if (forceExplicitTypeArgs(d.getDeclaration(), ol))
            appendTypeParameters(d.getDeclaration(), result);
        appendPositionalArgs(d.getDeclaration(), result);
        return result.toString();
    }
    
    private static String getNamedInvocationTextFor(DeclarationWithProximity d) {
        StringBuilder result = new StringBuilder(d.getName());
        if (forceExplicitTypeArgs(d.getDeclaration(), null))
            appendTypeParameters(d.getDeclaration(), result);
        appendNamedArgs(d.getDeclaration(), result);
        return result.toString();
    }
    
    private static String getDescriptionFor(DeclarationWithProximity d, 
            OccurrenceLocation ol) {
        StringBuilder result = new StringBuilder(d.getName());
        if (ol!=IMPORT) appendTypeParameters(d.getDeclaration(), result);
        return result.toString();
    }
    
    private static String getPositionalInvocationDescriptionFor(DeclarationWithProximity d, 
            OccurrenceLocation ol) {
        StringBuilder result = new StringBuilder(d.getName());
        if (forceExplicitTypeArgs(d.getDeclaration(), ol))
            appendTypeParameters(d.getDeclaration(), result);
        appendPositionalArgs(d.getDeclaration(), result);
        return result/*.append(" - invoke with positional arguments")*/.toString();
    }
    
    private static String getNamedInvocationDescriptionFor(DeclarationWithProximity d) {
        StringBuilder result = new StringBuilder(d.getName());
        if (forceExplicitTypeArgs(d.getDeclaration(), null))
            appendTypeParameters(d.getDeclaration(), result);
        appendNamedArgs(d.getDeclaration(), result);
        return result/*.append(" - invoke with named arguments")*/.toString();
    }
    
    public static String getRefinementTextFor(Declaration d, String indent) {
        StringBuilder result = new StringBuilder("shared actual ");
        appendDeclarationText(d, result);
        appendTypeParameters(d, result);
        appendParameters(d, result);
        appendImpl(d, indent, result);
        return result.toString();
    }
    
    private static String getAttributeTextFor(Declaration d) {
        StringBuilder result = new StringBuilder("shared ");
        appendDeclarationText(d, result);
        result.append(" = ").append(d.getName()).append(";");
        return result.toString();
    }
    
    /*private static String getAttributeRefinementTextFor(Declaration d) {
        StringBuilder result = new StringBuilder();
        result.append("super.").append(d.getName())
            .append(" = ").append(d.getName()).append(";");
        return result.toString();
    }*/
    
    private static String getRefinementDescriptionFor(Declaration d) {
        StringBuilder result = new StringBuilder("shared actual ");
        appendDeclarationText(d, result);
        appendTypeParameters(d, result);
        appendParameters(d, result);
        /*result.append(" - refine declaration in ") 
            .append(((Declaration) d.getContainer()).getName());*/
        return result.toString();
    }
    
    private static String getAttributeDescriptionFor(Declaration d) {
        StringBuilder result = new StringBuilder("shared ");
        appendDeclarationText(d, result);
        result.append(" = ").append(d.getName()).append(";");
        return result.toString();
    }
    
    /*private static String getAttributeRefinementDescriptionFor(Declaration d) {
        StringBuilder result = new StringBuilder();
        result.append("super.").append(d.getName())
            .append(" = ").append(d.getName()).append(";");
        return result.toString();
    }*/
    
    public static String getDescriptionFor(Declaration d) {
        StringBuilder result = new StringBuilder();
        if (d!=null) {
            if (d.isFormal()) result.append("formal ");
            if (d.isDefault()) result.append("default ");
            appendDeclarationText(d, result);
            appendTypeParameters(d, result);
            appendParameters(d, result);
            /*result.append(" - refine declaration in ") 
                .append(((Declaration) d.getContainer()).getName());*/
        }
        return result.toString();
    }
    
    private static void appendPositionalArgs(Declaration d, StringBuilder result) {
        if (d instanceof Functional) {
            List<ParameterList> plists = ((Functional) d).getParameterLists();
            if (plists!=null && !plists.isEmpty()) {
                ParameterList params = plists.get(0);
                if (params.getParameters().isEmpty()) {
                    result.append("()");
                }
                else {
                    result.append("(");
                    for (Parameter p: params.getParameters()) {
                        result.append(p.getName()).append(", ");
                    }
                    result.setLength(result.length()-2);
                    result.append(")");
                }
            }
        }
    }
    
    private static void appendNamedArgs(Declaration d, StringBuilder result) {
        if (d instanceof Functional) {
            List<ParameterList> plists = ((Functional) d).getParameterLists();
            if (plists!=null && !plists.isEmpty()) {
                ParameterList params = plists.get(0);
                if (params.getParameters().isEmpty()) {
                    result.append(" {}");
                }
                else {
                    result.append(" { ");
                    for (Parameter p: params.getParameters()) {
                        if (!p.isSequenced()) {
                            result.append(p.getName()).append(" = ")
                            .append(p.getName()).append("; ");
                        }
                    }
                    result.append("}");
                }
            }
        }
    }
    
    private static void appendTypeParameters(Declaration d, StringBuilder result) {
        if (d instanceof Generic) {
            List<TypeParameter> types = ((Generic) d).getTypeParameters();
            if (!types.isEmpty()) {
                result.append("<");
                for (TypeParameter p: types) {
                    result.append(p.getName()).append(", ");
                }
                result.setLength(result.length()-2);
                result.append(">");
            }
        }
    }
    
    private static void appendDeclarationText(Declaration d, StringBuilder result) {
        if (d instanceof Class) {
            if (Character.isLowerCase(d.getName().charAt(0))) {
                result.append("object");
            }
            else {
                result.append("class");
            }
        }
        else if (d instanceof Interface) {
            result.append("interface");
        }
        else if (d instanceof TypedDeclaration) {
            TypedDeclaration td = (TypedDeclaration) d;
            if (td.getType()!=null) {
                String typeName = td.getType().getProducedTypeName();
                if (td instanceof Value &&
                        Character.isLowerCase(typeName.charAt(0))) {
                    result.append("object");
                }
                else if (d instanceof Method) {
                    if (typeName.equals("Void")) { //TODO: fix this!
                        result.append("void");
                    }
                    else {
                        result.append(typeName);
                    }
                }
                else {
                    result.append(typeName);
                }
            }
        }
        result.append(" ").append(d.getName());
    }
    
    /*private static void appendPackage(Declaration d, StringBuilder result) {
    if (d.isToplevel()) {
        result.append(" - ").append(getPackageLabel(d));
    }
    if (d.isClassOrInterfaceMember()) {
        result.append(" - ");
        ClassOrInterface td = (ClassOrInterface) d.getContainer();
        result.append( td.getName() );
        appendPackage(td, result);
    }
  }*/
    
    private static void appendImpl(Declaration d, String indent, StringBuilder result) {
        if (d instanceof Method) {
            String extraIndent = indent.contains("\n") ?  indent + getDefaultIndent() : indent;
            result.append( ((Method) d).getTypeDeclaration().getName().equals("Void") ?
                    " {}" : " {" + extraIndent + "return bottom;" + indent + "}" );
        }
        else if (d instanceof MethodOrValue) {
            result.append(" = bottom;");
        }
        else {
            result.append(" {}");
        }
    }
    
    private static void appendParameters(Declaration d, StringBuilder result) {
        if (d instanceof Functional) {
            List<ParameterList> plists = ((Functional) d).getParameterLists();
            if (plists!=null && !plists.isEmpty()) {
                ParameterList params = plists.get(0);
                if (params.getParameters().isEmpty()) {
                    result.append("()");
                }
                else {
                    result.append("(");
                    for (Parameter p: params.getParameters()) {
                        result.append(p.getType().getProducedTypeName()).append(" ")
                        .append(p.getName()).append(", ");
                    }
                    result.setLength(result.length()-2);
                    result.append(")");
                }
            }
        }
    }
    
}
