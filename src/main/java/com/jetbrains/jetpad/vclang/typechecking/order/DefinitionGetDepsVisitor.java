package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;

import java.util.Set;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Void, Void> {
  private final Set<Abstract.Definition> myDependencies;

  public DefinitionGetDepsVisitor(Set<Abstract.Definition> dependencies) {
    myDependencies = dependencies;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myDependencies);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(visitor, null);
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(visitor, null);
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      term.accept(visitor, null);
    }

    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Void params) {
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(new CollectDefCallsVisitor(myDependencies), null);
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myDependencies);

    for (Abstract.TypeArgument param : def.getParameters()) {
      param.getType().accept(visitor, null);
    }

    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitConstructor(constructor, null);
    }

    if (def.getConditions() != null) {
      for (Abstract.Condition cond : def.getConditions()) {
        cond.getTerm().accept(visitor, null);
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myDependencies);

    for (Abstract.TypeArgument arg : def.getArguments()) {
      arg.getType().accept(visitor, null);
    }

    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myDependencies);

    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(visitor, null);
    }

    for (Abstract.ClassField field : def.getFields()) {
      visitClassField(field, null);
    }

    for (Abstract.Implementation implementation : def.getImplementations()) {
      visitImplement(implementation, null);
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Void params) {
    def.getImplementation().accept(new CollectDefCallsVisitor(myDependencies), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Void params) {
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Void params) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Void params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myDependencies);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(visitor, null);
      }
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      term.accept(visitor, null);
    }

    return null;
  }
}
