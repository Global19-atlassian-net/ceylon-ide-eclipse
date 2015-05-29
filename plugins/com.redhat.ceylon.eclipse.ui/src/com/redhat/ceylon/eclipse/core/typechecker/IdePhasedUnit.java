package com.redhat.ceylon.eclipse.core.typechecker;

import java.lang.ref.WeakReference;
import java.util.List;

import org.antlr.runtime.CommonToken;

import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.model.typechecker.util.ModuleManager;
import com.redhat.ceylon.compiler.typechecker.analyzer.ModuleSourceMapper;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.io.VirtualFile;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.Package;
import com.redhat.ceylon.model.typechecker.model.Unit;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.CompilationUnit;
import com.redhat.ceylon.eclipse.core.model.CeylonBinaryUnit;
import com.redhat.ceylon.eclipse.core.model.CeylonUnit;
import com.redhat.ceylon.eclipse.core.model.ProjectSourceFile;
import com.redhat.ceylon.eclipse.util.SingleSourceUnitPackage;

public abstract class IdePhasedUnit extends PhasedUnit {

    protected WeakReference<TypeChecker> typeCheckerRef = null;

    public IdePhasedUnit(VirtualFile unitFile, VirtualFile srcDir,
            CompilationUnit cu, Package p, ModuleManager moduleManager,
            ModuleSourceMapper moduleSourceMapper,
            TypeChecker typeChecker, List<CommonToken> tokenStream) {
        super(unitFile, srcDir, cu, p, moduleManager, moduleSourceMapper, typeChecker.getContext(), tokenStream);
        typeCheckerRef = new WeakReference<TypeChecker>(typeChecker);
    }
    
    public IdePhasedUnit(PhasedUnit other) {
        super(other);
        if (other instanceof IdePhasedUnit) {
            typeCheckerRef = new WeakReference<TypeChecker>(((IdePhasedUnit) other).getTypeChecker());
        }
    }

    public TypeChecker getTypeChecker() {
        return typeCheckerRef.get();
    }
    
    protected TypecheckerUnit createUnit() {
        TypecheckerUnit oldUnit = getUnit();
        TypecheckerUnit newUnit = newUnit();
        if (oldUnit != null) {
            newUnit.setFilename(oldUnit.getFilename());
            newUnit.setFullPath(oldUnit.getFullPath());
            newUnit.setRelativePath(oldUnit.getRelativePath());
            newUnit.setPackage(oldUnit.getPackage());
            newUnit.getDependentsOf().addAll(oldUnit.getDependentsOf());
        }
        return newUnit;
    }
    

    protected abstract TypecheckerUnit newUnit();

    public static boolean isCentralModelUnit(Unit unit) {
        return ! (unit instanceof CeylonUnit) ||
                    unit instanceof ProjectSourceFile ||
                    !(unit.getPackage() instanceof SingleSourceUnitPackage);
    }

    public static boolean isCentralModelDeclaration(Declaration declaration) {
        return declaration == null ||
                isCentralModelUnit(declaration.getUnit());
    }

    public static void addCentralModelOverloads(Unit unit) {
        if (isCentralModelUnit(unit)) {
            return;
        }
        SingleSourceUnitPackage pkg = (SingleSourceUnitPackage) unit.getPackage();
        Package centralModelPackage = pkg.getModelPackage();
        for (Declaration decl : unit.getDeclarations()) {
            if (!decl.isNative()) {
                continue;
            }
            List<Declaration> overloads = decl.getOverloads();
            if (overloads == null) {
                continue;
            }
            
            Declaration centralModelDecl = null;
            for (Declaration d : centralModelPackage.getMembers()) {
                if (d.isNative() && d.equals(decl)) {
                    centralModelDecl = d;
                    break;
                }
            }
            if (centralModelDecl == null) {
                continue;
            }
            if (centralModelDecl.getUnit() instanceof CeylonBinaryUnit) {
                // Do nothing for the moment.
                // we will not find any overloads, because it's loaded from a binary module
                // => we need to use the mapping between natives
            } else {
                List<Declaration> centralModelOverloads = centralModelDecl.getOverloads();
                if (centralModelOverloads == null) {
                    continue;
                }
                for (Declaration centralModelOverload : centralModelOverloads) {
                    if (overloads.contains(centralModelOverload)) {
                        continue;
                    }
                    overloads.add(centralModelOverload);
                }
            }
        }
    }

}
