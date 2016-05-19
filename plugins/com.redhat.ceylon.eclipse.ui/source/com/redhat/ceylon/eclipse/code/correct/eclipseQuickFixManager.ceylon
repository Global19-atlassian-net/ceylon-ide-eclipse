import com.redhat.ceylon.compiler.typechecker.tree {
    Tree
}
import com.redhat.ceylon.ide.common.correct {
    IdeQuickFixManager,
    QuickFixData,
    convertForToWhileQuickFix,
    joinIfStatementsQuickFix,
    splitIfStatementQuickFix,
    verboseRefinementQuickFix,
    addParameterQuickFix,
    miscQuickFixes,
    changeToIfQuickFix,
    splitDeclarationQuickFix,
    expandTypeQuickFix,
    convertStringQuickFix,
    assignToFieldQuickFix,
    joinDeclarationQuickFix,
    operatorQuickFix,
    convertToDefaultConstructorQuickFix,
    convertSwitchToIfQuickFix,
    invertIfElseQuickFix,
    convertIfElseToThenElseQuickFix,
    convertThenElseToIfElse,
    assertExistsDeclarationQuickFix,
    addAnnotationQuickFix,
    addThrowsAnnotationQuickFix,
    convertToClassQuickFix,
    refineFormalMembersQuickFix,
    refineEqualsHashQuickFix
}

import java.util {
    Collection
}

import org.eclipse.core.resources {
    IFile
}
import org.eclipse.jface.text {
    IDocument
}
import org.eclipse.jface.text.contentassist {
    ICompletionProposal
}
import org.eclipse.jface.text.link {
    LinkedModeModel
}

object eclipseQuickFixManager
        extends IdeQuickFixManager<IDocument,ICompletionProposal,LinkedModeModel,EclipseQuickFixData>() {
    
    declareLocalQuickFix => eclipseDeclareLocalQuickFix;
    assignToLocalQuickFix => eclipseAssignToLocalQuickFix;
    
    shared actual void addImportProposals(Collection<ICompletionProposal> proposals, QuickFixData data) {
        if (is EclipseQuickFixData data) {
            data.proposals.addAll(proposals);
        }
    }
    
    specifyTypeQuickFix => eclipseSpecifyTypeQuickFix;
    
    shared actual void addCreateTypeParameterProposal<Data>(Data data,
        Tree.BaseType bt, String brokenName)
            given Data satisfies QuickFixData {
        
        assert (is EclipseQuickFixData data);
        
        CreateTypeParameterProposal.addCreateTypeParameterProposal(
            data.proposals, data.rootNode, bt, brokenName);
    }
    
    shared void addQuickAssists(EclipseQuickFixData data, IFile file,
        IDocument doc, Tree.Statement? statement,
        Tree.Declaration? declaration, Tree.NamedArgument? namedArgument,
        Tree.ImportMemberOrType? imp, Tree.OperatorExpression? oe,
        Integer currentOffset) {
        
        assignToLocalQuickFix.addProposal(data, currentOffset);
        
        if (is Tree.BinaryOperatorExpression oe) {
            operatorQuickFix.addReverseOperatorProposal(data,  oe);
            operatorQuickFix.addInvertOperatorProposal(data, oe);
            operatorQuickFix.addSwapBinaryOperandsProposal(data, oe);
        }
        operatorQuickFix.addParenthesesProposals(data, oe);
        
        verboseRefinementQuickFix.addVerboseRefinementProposal(data, statement);
        verboseRefinementQuickFix.addShortcutRefinementProposal(data, statement);
        
        addAnnotationQuickFix.addContextualAnnotationProposals(data, declaration, currentOffset);
        specifyTypeQuickFix.addTypingProposals(data, declaration);
        
        miscQuickFixes.addAnonymousFunctionProposals(data);
        
        miscQuickFixes.addDeclarationProposals(data, declaration, currentOffset);
        
        assignToFieldQuickFix.addAssignToFieldProposal(data, statement, declaration);
        
        changeToIfQuickFix.addChangeToIfProposal(data, statement);
        
        convertToDefaultConstructorQuickFix.addConvertToDefaultConstructorProposal(data, statement);

        convertToClassQuickFix.addConvertToClassProposal(data, declaration);
        assertExistsDeclarationQuickFix.addAssertExistsDeclarationProposals(data, declaration);
        splitDeclarationQuickFix.addSplitDeclarationProposals(data, declaration, statement);
        joinDeclarationQuickFix.addJoinDeclarationProposal(data, statement);
        addParameterQuickFix.addParameterProposals(data);

        miscQuickFixes.addArgumentProposals(data, namedArgument);

        convertThenElseToIfElse.addConvertToIfElseProposal(data, statement);
        convertIfElseToThenElseQuickFix.addConvertToThenElseProposal(data, statement);
        invertIfElseQuickFix.addInvertIfElseProposal(data, statement);
        
        convertSwitchToIfQuickFix.addConvertSwitchToIfProposal(data, statement);
        convertSwitchToIfQuickFix.addConvertIfToSwitchProposal(data, statement);
        
        splitIfStatementQuickFix.addSplitIfStatementProposal(data, statement);
        joinIfStatementsQuickFix.addJoinIfStatementsProposal(data, statement);
        
        convertForToWhileQuickFix.addConvertForToWhileProposal(data, statement);
        
        addThrowsAnnotationQuickFix.addThrowsAnnotationProposal(data, statement);
        
        refineFormalMembersQuickFix.addRefineFormalMembersProposal(data, false);
        refineEqualsHashQuickFix.addRefineEqualsHashProposal(data, currentOffset);
        
        convertStringQuickFix.addConvertToVerbatimProposal(data);
        convertStringQuickFix.addConvertFromVerbatimProposal(data);
        convertStringQuickFix.addConvertToConcatenationProposal(data);
        convertStringQuickFix.addConvertToInterpolationProposal(data);
        
        expandTypeQuickFix.addExpandTypeProposal {
            data = data;
            node = statement;
            selectionStart 
                    = data.editor.selection.offset;
            selectionStop 
                    = data.editor.selection.offset 
                    + data.editor.selection.length;
        };
    }
}
