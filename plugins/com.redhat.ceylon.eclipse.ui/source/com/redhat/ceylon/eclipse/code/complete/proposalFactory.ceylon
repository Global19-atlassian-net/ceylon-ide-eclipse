import com.redhat.ceylon.eclipse.code.hover {
    DocumentationHover
}
import com.redhat.ceylon.eclipse.code.outline {
    CeylonLabelProvider
}
import com.redhat.ceylon.eclipse.ui {
    CeylonPlugin
}
import com.redhat.ceylon.eclipse.util {
    eclipseIcons,
    Highlights
}
import com.redhat.ceylon.ide.common.completion {
    ProposalKind,
    keyword
}
import com.redhat.ceylon.ide.common.doc {
    Icons
}
import com.redhat.ceylon.ide.common.platform {
    TextChange
}
import com.redhat.ceylon.model.typechecker.model {
    Declaration
}

import java.lang {
    JString=String
}

import org.eclipse.core.runtime {
    IProgressMonitor
}
import org.eclipse.jface.text {
    IDocument
}
import org.eclipse.jface.text.contentassist {
    ICompletionProposal
}
import org.eclipse.jface.viewers {
    StyledString
}
import org.eclipse.swt.graphics {
    Image,
    Point
}
shared object proposalFactory {
    
    alias Builder => ICompletionProposal(EclipseCompletionContext, Integer, 
        String, Image?, String, String,
        ProposalKind, TextChange?, Declaration?, Point?);
    
    shared ICompletionProposal create(EclipseCompletionContext ctx, Integer offset, 
        String prefix, Icons|Declaration icon, String description, String text,
        ProposalKind kind, TextChange? additionalChange, Point? region) {
        
        Builder builder = switch (kind)
        case (keyword) keywordProposal
        else basicProposal;
        
        value [image, decl] = if (is Icons icon)
        then [eclipseIcons.fromIcons(icon), null]
        else [CeylonLabelProvider.getImageForDeclaration(icon), icon];

        return builder(ctx, offset, prefix, image, description, text, kind, 
            additionalChange, decl, region);
    }
    
    ICompletionProposal basicProposal(EclipseCompletionContext ctx, Integer _offset, 
        String _prefix, Image? icon, String description, String _text,
        ProposalKind kind, TextChange? additionalChange, Declaration? decl,
        Point? selection) {
        
        return object extends CompletionProposal(_offset, _prefix, icon, description, _text) {
            shared actual String? additionalProposalInfo {
                if (exists res = getAdditionalProposalInfo(null)) {
                    return res.string;
                }
                return null;
            }
            
            // We need to return a JString because the formal type is Object,
            // otherwise the compiler will generate an incorrect c.l.String
            shared actual JString? getAdditionalProposalInfo(IProgressMonitor? pm) {
                return if (exists decl)
                then JString(DocumentationHover.getDocumentationFor(ctx.cpc, decl, pm))
                else null;
            }
            
            shared actual Point? getSelection(IDocument document)
                    => selection else super.getSelection(document);
        };
    }
    
    ICompletionProposal keywordProposal(EclipseCompletionContext ctx, Integer _offset, 
        String _prefix, Image? icon, String description, String _text,
        ProposalKind kind, TextChange? additionalChange, Declaration? decl,
        Point? selection) {
        
        return object extends CompletionProposal(_offset, _prefix, icon, description, _text) {
            
            styledDisplayString =>
                StyledString(displayString, Highlights.FontStyler(
                    CeylonPlugin.completionFont, Highlights.\iKW_STYLER));

            shared actual Point? getSelection(IDocument document)
                    => selection;

            length(IDocument? iDocument) => prefix.size;
        };
    }
}