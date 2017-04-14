package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CollectDefCallsVisitor;

import java.util.Set;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
  private final ClassViewInstanceProvider myInstanceProvider;
  private final Set<Abstract.Definition> myDependencies;

  public DefinitionGetDepsVisitor(ClassViewInstanceProvider instanceProvider, Set<Abstract.Definition> dependencies) {
    myInstanceProvider = instanceProvider;
    myDependencies = dependencies;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Boolean isHeader) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myDependencies);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(visitor, null);
      }
    }

    if (isHeader) {
      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(visitor, null);
      }
    } else {
      Abstract.Expression term = def.getTerm();
      if (term != null) {
        term.accept(visitor, null);
      }
    }

    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Boolean params) {
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(new CollectDefCallsVisitor(myInstanceProvider, myDependencies), null);
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Boolean isHeader) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myDependencies);

    if (isHeader) {
      for (Abstract.TypeArgument param : def.getParameters()) {
        param.getType().accept(visitor, null);
      }

      Abstract.Expression universe = def.getUniverse();
      if (universe != null) {
        universe.accept(visitor, null);
      }
    } else {
      for (Abstract.Constructor constructor : def.getConstructors()) {
        visitConstructor(constructor, null);
      }

      if (def.getConditions() != null) {
        for (Abstract.Condition cond : def.getConditions()) {
          cond.getTerm().accept(visitor, null);
        }
      }
    }

    return null;
  }

  private void visitPatternArgument(Abstract.PatternArgument patternArgument) {
    if (patternArgument.getPattern() instanceof Abstract.ConstructorPattern) {
      Abstract.ConstructorPattern conPattern = (Abstract.ConstructorPattern) patternArgument.getPattern();
      if (conPattern.getConstructor() != null) {
        myDependencies.add(conPattern.getConstructor());
      }
      for (Abstract.PatternArgument argument : conPattern.getArguments()) {
        visitPatternArgument(argument);
      }
    }
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myDependencies);

    if (def.getPatterns() != null) {
      for (Abstract.PatternArgument patternArgument : def.getPatterns()) {
        visitPatternArgument(patternArgument);
      }
    }

    for (Abstract.TypeArgument arg : def.getArguments()) {
      arg.getType().accept(visitor, null);
    }

    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myDependencies);

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
  public Void visitImplement(Abstract.Implementation def, Boolean params) {
    def.getImplementation().accept(new CollectDefCallsVisitor(myInstanceProvider, myDependencies), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Boolean params) {
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Boolean params) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Boolean params) {
    CollectDefCallsVisitor visitor = new CollectDefCallsVisitor(myInstanceProvider, myDependencies);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(visitor, null);
      }
    }

    def.getClassView().accept(visitor, null);
    for (Abstract.ClassFieldImpl classFieldImpl : def.getClassFieldImpls()) {
      classFieldImpl.getImplementation().accept(visitor, null);
    }

    return null;
  }
}
