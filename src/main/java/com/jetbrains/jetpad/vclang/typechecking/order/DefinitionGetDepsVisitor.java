package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typecheckable.provider.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Set;

public class DefinitionGetDepsVisitor<T> implements ConcreteDefinitionVisitor<T, Boolean, Void> {
  private final InstanceProvider myInstanceProvider;
  private final TypecheckableProvider myTypecheckableProvider;
  private final Set<GlobalReferable> myDependencies;

  DefinitionGetDepsVisitor(InstanceProvider instanceProvider, TypecheckableProvider typecheckableProvider, Set<GlobalReferable> dependencies) {
    myInstanceProvider = instanceProvider;
    myTypecheckableProvider = typecheckableProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, Boolean isHeader) {
    CollectDefCallsVisitor<T> visitor = new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(visitor, null);
      }
    }

    if (isHeader) {
      Concrete.Expression<T> resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(visitor, null);
      }
    } else {
      Concrete.FunctionBody<T> body = def.getBody();
      if (body instanceof Concrete.TermFunctionBody) {
        ((Concrete.TermFunctionBody<T>) body).getTerm().accept(visitor, null);
      }
      if (body instanceof Concrete.ElimFunctionBody) {
        for (Concrete.FunctionClause<T> clause : ((Concrete.ElimFunctionBody<T>) body).getClauses()) {
          for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
            visitPattern(pattern);
          }
          if (clause.getExpression() != null) {
            clause.getExpression().accept(visitor, null);
          }
        }
      }
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, Boolean isHeader) {
    CollectDefCallsVisitor<T> visitor = new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies);

    if (isHeader) {
      for (Concrete.TypeParameter<T> param : def.getParameters()) {
        param.getType().accept(visitor, null);
      }

      Concrete.Expression<T> universe = def.getUniverse();
      if (universe != null) {
        universe.accept(visitor, null);
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
        myDependencies.add((GlobalReferable) conPattern.getConstructor());
      }
      for (Concrete.Pattern<T> patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  private void visitConstructor(Concrete.Constructor<T> def) {
    CollectDefCallsVisitor<T> visitor = new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.TypeParameter<T> param : def.getParameters()) {
      param.getType().accept(visitor, null);
    }
    if (!def.getEliminatedReferences().isEmpty()) {
      for (Concrete.FunctionClause<T> clause : def.getClauses()) {
        for (Concrete.Pattern<T> pattern : clause.getPatterns()) {
          visitPattern(pattern);
        }
        if (clause.getExpression() != null) {
          clause.getExpression().accept(visitor, null);
        }
      }
    }
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, Boolean params) {
    CollectDefCallsVisitor<T> visitor = new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.ReferenceExpression<T> superClass : def.getSuperClasses()) {
      visitor.visitReference(superClass, null);
    }

    for (Concrete.ClassField<T> field : def.getFields()) {
      field.getResultType().accept(new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies), null);
    }

    for (Concrete.ClassFieldImpl<T> impl : def.getImplementations()) {
      impl.getImplementation().accept(visitor, null);
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView def, Boolean params) {
    return null;
  }

  @Override
  public Void visitClassViewField(Concrete.ClassViewField def, Boolean params) {
    return null;
  }

  @Override
  public Void visitInstance(Concrete.Instance<T> def, Boolean params) {
    CollectDefCallsVisitor<T> visitor = new CollectDefCallsVisitor<>(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(visitor, null);
      }
    }

    def.getClassView().accept(visitor, null);
    for (Concrete.ClassFieldImpl<T> classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.getImplementation().accept(visitor, null);
    }

    return null;
  }
}
