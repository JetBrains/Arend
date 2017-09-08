package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Collection;

public class DefinitionGetDependenciesVisitor<T> implements ConcreteDefinitionVisitor<T, Boolean, Void> {
  private final CollectDefCallsVisitor<T> myVisitor;

  DefinitionGetDependenciesVisitor(Collection<GlobalReferable> dependencies) {
    myVisitor = new CollectDefCallsVisitor<>(dependencies);
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, Boolean isHeader) {
    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(myVisitor, null);
      }
    }

    if (isHeader) {
      Concrete.Expression<T> resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(myVisitor, null);
      }
    } else {
      Concrete.FunctionBody<T> body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody<T>) body).getTerm().accept(myVisitor, null);
      }
      if (body instanceof Concrete.ElimFunctionBody) {
        for (Concrete.FunctionClause<T> clause : ((Concrete.ElimFunctionBody<T>) body).getClauses()) {
          for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
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
  public Void visitData(Concrete.DataDefinition<T> def, Boolean isHeader) {
    if (isHeader) {
      for (Concrete.TypeParameter<T> param : def.getParameters()) {
        param.getType().accept(myVisitor, null);
      }

      Concrete.Expression<T> universe = def.getUniverse();
      if (universe != null) {
        universe.accept(myVisitor, null);
      }
    } else {
      for (Concrete.ConstructorClause<T> clause : def.getConstructorClauses()) {
        if (clause.getPatterns() != null) {
          for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
        }
        for (Concrete.Constructor<T> constructor : clause.getConstructors()) {
          visitConstructor(constructor);
        }
      }
    }

    return null;
  }

  private void visitPattern(Concrete.Pattern<T> pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern<T> conPattern = (Concrete.ConstructorPattern<T>) pattern;
      if (conPattern.getConstructor() instanceof GlobalReferable) {
        myVisitor.getDependencies().add((GlobalReferable) conPattern.getConstructor());
      }
      for (Concrete.Pattern<T> patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  private void visitConstructor(Concrete.Constructor<T> def) {
    for (Concrete.TypeParameter<T> param : def.getParameters()) {
      param.getType().accept(myVisitor, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      for (Concrete.FunctionClause<T> clause : def.getClauses()) {
        for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
          visitPattern(pattern);
        }
        if (clause.getExpression() != null) {
          clause.getExpression().accept(myVisitor, null);
        }
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, Boolean params) {
    for (Concrete.ReferenceExpression<T> superClass : def.getSuperClasses()) {
      myVisitor.visitReference(superClass, null);
    }

    for (Concrete.ClassField<T> field : def.getFields()) {
      field.getResultType().accept(myVisitor, null);
    }

    for (Concrete.ClassFieldImpl<T> impl : def.getImplementations()) {
      impl.getImplementation().accept(myVisitor, null);
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView<T> def, Boolean params) {
    myVisitor.visitReference(def.getUnderlyingClass(), null);
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance<T> def, Boolean params) {
    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(myVisitor, null);
      }
    }

    def.getClassView().accept(myVisitor, null);
    for (Concrete.ClassFieldImpl<T> classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.getImplementation().accept(myVisitor, null);
    }

    return null;
  }
}
