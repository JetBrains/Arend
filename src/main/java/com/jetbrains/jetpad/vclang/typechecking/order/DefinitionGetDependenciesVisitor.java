package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.concrete.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Collection;

public class DefinitionGetDependenciesVisitor implements ConcreteDefinitionVisitor<Boolean, Void> {
  private final CollectDefCallsVisitor myVisitor;

  DefinitionGetDependenciesVisitor(Collection<GlobalReferable> dependencies) {
    myVisitor = new CollectDefCallsVisitor(dependencies);
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Boolean isHeader) {
    for (Concrete.Parameter param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(myVisitor, null);
      }
    }

    if (isHeader) {
      Concrete.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(myVisitor, null);
      }
    } else {
      Concrete.FunctionBody body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody) body).getTerm().accept(myVisitor, null);
      }
      if (body instanceof Concrete.ElimFunctionBody) {
        for (Concrete.FunctionClause clause : ((Concrete.ElimFunctionBody) body).getClauses()) {
          for (Concrete.Pattern pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
          if (clause.getExpression() != null) {
            clause.getExpression().accept(myVisitor, null);
          }
        }
      }
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Boolean isHeader) {
    if (isHeader) {
      for (Concrete.TypeParameter param : def.getParameters()) {
        param.getType().accept(myVisitor, null);
      }

      Concrete.Expression universe = def.getUniverse();
      if (universe != null) {
        universe.accept(myVisitor, null);
      }
    } else {
      for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
        if (clause.getPatterns() != null) {
          for (Concrete.Pattern pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
        }
        for (Concrete.Constructor constructor : clause.getConstructors()) {
          visitConstructor(constructor);
        }
      }
    }

    return null;
  }

  private void visitPattern(Concrete.Pattern pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern conPattern = (Concrete.ConstructorPattern) pattern;
      if (conPattern.getConstructor() instanceof GlobalReferable) {
        myVisitor.getDependencies().add((GlobalReferable) conPattern.getConstructor());
      }
      for (Concrete.Pattern patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  private void visitConstructor(Concrete.Constructor def) {
    for (Concrete.TypeParameter param : def.getParameters()) {
      param.getType().accept(myVisitor, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      for (Concrete.FunctionClause clause : def.getClauses()) {
        for (Concrete.Pattern pattern : clause.getPatterns()) {
          visitPattern(pattern);
        }
        if (clause.getExpression() != null) {
          clause.getExpression().accept(myVisitor, null);
        }
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Boolean params) {
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      myVisitor.visitReference(superClass, null);
    }

    for (Concrete.Parameter param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(myVisitor, null);
      }
    }

    for (Concrete.ClassField field : def.getFields()) {
      field.getResultType().accept(myVisitor, null);
    }

    for (Concrete.ClassFieldImpl impl : def.getImplementations()) {
      impl.getImplementation().accept(myVisitor, null);
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView def, Boolean params) {
    myVisitor.visitReference(def.getUnderlyingClass(), null);
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance def, Boolean params) {
    for (Concrete.Parameter param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(myVisitor, null);
      }
    }

    def.getClassReference().accept(myVisitor, null);
    for (Concrete.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      if (classFieldImpl.getImplementedField() instanceof GlobalReferable) {
        myVisitor.getDependencies().add((GlobalReferable) classFieldImpl.getImplementedField());
      }
      classFieldImpl.getImplementation().accept(myVisitor, null);
    }

    return null;
  }
}
