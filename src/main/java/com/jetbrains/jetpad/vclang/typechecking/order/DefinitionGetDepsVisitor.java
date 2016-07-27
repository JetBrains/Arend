package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;

import java.util.*;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Boolean, Set<Referable>> {
  private final Queue<Abstract.Definition> myOthers;
  private final Map<Abstract.Definition, List<Abstract.Definition>> myClassToNonStatics;

  public DefinitionGetDepsVisitor(Queue<Abstract.Definition> myOthers, Map<Abstract.Definition, List<Abstract.Definition>> classToNonStatics) {
    this.myOthers = myOthers;
    myClassToNonStatics = classToNonStatics;
  }

  @Override
  public Set<Referable> visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    if (isStatic) {
      return visitStatements(def, def.getStatements(), true);
    }

    Set<Referable> result = new HashSet<>();

    visitStatements(def, def.getStatements(), false);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        result.addAll(((Abstract.TypeArgument) arg).getType().accept(new CollectDefCallsVisitor(), null));
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      result.addAll(resultType.accept(new CollectDefCallsVisitor(), null));
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      result.addAll(term.accept(new CollectDefCallsVisitor(), null));
    }

    return result;
  }

  @Override
  public Set<Referable> visitAbstract(Abstract.AbstractDefinition def, Boolean params) {
    throw new IllegalStateException();
  }

  private Set<Referable> visitAbstract(Abstract.AbstractDefinition def) {
    Set<Referable> result = new HashSet<>();

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        result.addAll(((Abstract.TypeArgument) arg).getType().accept(new CollectDefCallsVisitor(), null));
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      result.addAll(resultType.accept(new CollectDefCallsVisitor(), null));
    }

    return result;
  }

  @Override
  public Set<Referable> visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (isStatic)
      return Collections.emptySet();

    Set<Referable> result = new HashSet<>();

    for (Abstract.TypeArgument param : def.getParameters()) {
      result.addAll(param.getType().accept(new CollectDefCallsVisitor(), null));
    }

    for (Abstract.Constructor constructor : def.getConstructors()) {
      result.addAll(visitConstructor(constructor));
    }

    return result;
  }

  @Override
  public Set<Referable> visitConstructor(Abstract.Constructor def, Boolean isStatic) {
    throw new IllegalStateException();
  }

  private Set<Referable> visitConstructor(Abstract.Constructor def) {
    Set<Referable> result = new HashSet<>();
    for (Abstract.TypeArgument arg : def.getArguments()) {
      result.addAll(arg.getType().accept(new CollectDefCallsVisitor(), null));
    }
    if (def.getDataType().getConditions() != null) {
      for (Abstract.Condition cond : def.getDataType().getConditions()) {
        if (cond.getConstructorName().equals(def.getName())) {
          result.addAll(cond.getTerm().accept(new CollectDefCallsVisitor(), null));
        }
      }
    }

    return result;
  }

  public Set<Referable> visitStatements(Abstract.Definition parent, Collection<? extends Abstract.Statement> statements, boolean isStatic) {
    Set<Referable> result = new HashSet<>();
    Set<Abstract.Definition> nonStatic = !isStatic && parent instanceof Abstract.ClassDefinition ? new HashSet<Abstract.Definition>() : null;
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.DefineStatement defineStatement = (Abstract.DefineStatement) statement;
        if (isStatic) {
          if (defineStatement.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC || parent instanceof Abstract.FunctionDefinition) {
            result.add(defineStatement.getDefinition());
            result.addAll(defineStatement.getDefinition().accept(
                new DefinitionGetDepsVisitor(myOthers, null), true
            ));
          }
        } else {
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.AbstractDefinition) {
            result.addAll(visitAbstract((Abstract.AbstractDefinition) ((Abstract.DefineStatement) statement).getDefinition()));
          } else
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.ImplementDefinition) {
            result.addAll(visitImplement((Abstract.ImplementDefinition) ((Abstract.DefineStatement) statement).getDefinition()));
          } else {
            myOthers.add(defineStatement.getDefinition());
            if (parent instanceof Abstract.ClassDefinition && ((Abstract.DefineStatement) statement).getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
              nonStatic.add(defineStatement.getDefinition());
              for (Referable def : ((Abstract.DefineStatement) statement).getDefinition().accept(new DefinitionGetDepsVisitor(myOthers, null), true)) {
                nonStatic.add((Abstract.Definition) def);
              }
            }
          }
        }
      }
    }
    if (nonStatic != null) {
      myClassToNonStatics.put(parent, new ArrayList<>(nonStatic));
    }
    return result;
  }

  @Override
  public Set<Referable> visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    Set<Referable> result = new HashSet<>();
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      result.addAll(superClass.getSuperClass().accept(new CollectDefCallsVisitor(), null));
    }
    result.addAll(visitStatements(def, def.getStatements(), isStatic));
    return result;
  }

  @Override
  public Set<Referable> visitImplement(Abstract.ImplementDefinition def, Boolean isStatic) {
    throw new IllegalStateException();
  }

  private Set<Referable> visitImplement(Abstract.ImplementDefinition def) {
    return def.getExpression().accept(new CollectDefCallsVisitor(), null);
  }
}
