package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckableProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Set;

public class DefinitionGetDepsVisitor<T> implements ConcreteDefinitionVisitor<T, Boolean, Void> {
  private final InstanceProvider myInstanceProvider;
  private final TypecheckableProvider myTypecheckableProvider;
  private final Set<Abstract.GlobalReferableSourceNode> myDependencies;

  DefinitionGetDepsVisitor(InstanceProvider instanceProvider, TypecheckableProvider typecheckableProvider, Set<Abstract.GlobalReferableSourceNode> dependencies) {
    myInstanceProvider = instanceProvider;
    myTypecheckableProvider = typecheckableProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, Boolean isHeader) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter<T>) param).getType().accept(visitor, null);
      }
    }

    if (isHeader) {
      Concrete.Expression resultType = def.getResultType();
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
  public Void visitClassField(Concrete.ClassField def, Boolean params) {
    def.getResultType().accept(new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies), null);
    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, Boolean isHeader) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies);

    if (isHeader) {
      for (Concrete.TypeParameter<T> param : def.getParameters()) {
        param.getType().accept(visitor, null);
      }

      Concrete.Expression universe = def.getUniverse();
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
          visitConstructor(constructor, null);
        }
      }
    }

    return null;
  }

  private void visitPattern(Concrete.Pattern<T> pattern) {
    if (pattern instanceof Concrete.ConstructorPattern) {
      Concrete.ConstructorPattern<T> conPattern = (Concrete.ConstructorPattern<T>) pattern;
      if (conPattern.getConstructor() != null) {
        myDependencies.add(conPattern.getConstructor());
      }
      for (Concrete.Pattern<T> patternArg : conPattern.getPatterns()) {
        visitPattern(patternArg);
      }
    }
  }

  @Override
  public Void visitConstructor(Concrete.Constructor<T> def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies);

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

    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.SuperClass<T> superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(visitor, null);
    }

    for (Concrete.ClassField field : def.getFields()) {
      visitClassField(field, null);
    }

    for (Concrete.Implementation implementation : def.getImplementations()) {
      visitImplement(implementation, null);
    }

    return null;
  }

  @Override
  public Void visitImplement(Concrete.Implementation def, Boolean params) {
    def.getImplementation().accept(new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies), null);
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
  public Void visitClassViewInstance(Concrete.ClassViewInstance<T> def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myTypecheckableProvider, myDependencies);

    for (Concrete.Parameter<T> param : def.getParameters()) {
      if (param instanceof Concrete.TypeParameter) {
        ((Concrete.TypeParameter) param).getType().accept(visitor, null);
      }
    }

    def.getClassView().accept(visitor, null);
    for (Concrete.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.getImplementation().accept(visitor, null);
    }

    return null;
  }
}
