package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.ResolvedName;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.BaseDefinition;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;

import java.util.*;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Boolean, Set<BaseDefinition>> {
  private final Namespace myNamespace;
  private final Queue<Abstract.Definition> myOthers;
  private final Map<Abstract.Definition, List<Abstract.Definition>> myClassToNonStatics;

  public DefinitionGetDepsVisitor(Namespace namespace, Queue<Abstract.Definition> myOthers, Map<Abstract.Definition, List<Abstract.Definition>> classToNonStatics) {
    myNamespace = namespace;
    this.myOthers = myOthers;
    myClassToNonStatics = classToNonStatics;
  }

  @Override
  public Set<BaseDefinition> visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    if (isStatic) {
      return visitStatements(def, def.getStatements(), true);
    }

    Set<BaseDefinition> result = new HashSet<>();

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
  public Set<BaseDefinition> visitAbstract(Abstract.AbstractDefinition def, Boolean params) {
    throw new IllegalStateException();
  }

  private Set<BaseDefinition> visitAbstract(Abstract.AbstractDefinition def) {
    Set<BaseDefinition> result = new HashSet<>();

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
  public Set<BaseDefinition> visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (isStatic)
      return Collections.emptySet();

    Set<BaseDefinition> result = new HashSet<>();

    for (Abstract.TypeArgument param : def.getParameters()) {
      result.addAll(param.getType().accept(new CollectDefCallsVisitor(), null));
    }

    for (Abstract.Constructor constructor : def.getConstructors()) {
      result.addAll(visitConstructor(constructor));
    }

    return result;
  }

  @Override
  public Set<BaseDefinition> visitConstructor(Abstract.Constructor def, Boolean isStatic) {
    throw new IllegalStateException();
  }

  private Set<BaseDefinition> visitConstructor(Abstract.Constructor def) {
    Set<BaseDefinition> result = new HashSet<>();
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

  public Set<BaseDefinition> visitStatements(Abstract.Definition parent, Collection<? extends Abstract.Statement> statements, boolean isStatic) {
    Set<BaseDefinition> result = new HashSet<>();
    Set<Abstract.Definition> nonStatic = !isStatic && parent instanceof Abstract.ClassDefinition ? new HashSet<Abstract.Definition>() : null;
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.DefineStatement defineStatement = (Abstract.DefineStatement) statement;
        if (isStatic) {
          if (defineStatement.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC || parent instanceof Abstract.FunctionDefinition) {
            result.add(defineStatement.getDefinition());
            result.addAll(defineStatement.getDefinition().accept(
                new DefinitionGetDepsVisitor(myNamespace.getChild(defineStatement.getDefinition().getName()), myOthers, null), true
            ));
          }
        } else {
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.AbstractDefinition) {
            result.addAll(visitAbstract((Abstract.AbstractDefinition) ((Abstract.DefineStatement) statement).getDefinition()));
          } else {
            myOthers.add(defineStatement.getDefinition());
            if (parent instanceof Abstract.ClassDefinition && ((Abstract.DefineStatement) statement).getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
              nonStatic.add(defineStatement.getDefinition());
              for (BaseDefinition def : ((Abstract.DefineStatement) statement).getDefinition().accept(new DefinitionGetDepsVisitor(myNamespace.getChild(defineStatement.getDefinition().getName()), myOthers, null), true)) {
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
  public Set<BaseDefinition> visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    return visitStatements(def, def.getStatements(), isStatic);
  }
}
