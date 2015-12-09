package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.visitor.GetDepsVisitor;

import java.util.*;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Boolean, Set<ResolvedName>> {
  private final Namespace myNamespace;
  private final Queue<ResolvedName> myOthers;
  private final Map<ResolvedName, List<ResolvedName>> myClassToNonStatics;

  public DefinitionGetDepsVisitor(Namespace namespace, Queue<ResolvedName> myOthers, Map<ResolvedName, List<ResolvedName>> classToNonStatics) {
    myNamespace = namespace;
    this.myOthers = myOthers;
    myClassToNonStatics = classToNonStatics;
  }

  @Override
  public Set<ResolvedName> visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    if (isStatic) {
      return visitStatements(def.getStatements(), true, true);
    }

    Set<ResolvedName> result = new HashSet<>();

    visitStatements(def.getStatements(), false, true);

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        result.addAll(((Abstract.TypeArgument) arg).getType().accept(new GetDepsVisitor(), null));
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      result.addAll(resultType.accept(new GetDepsVisitor(), null));
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      result.addAll(term.accept(new GetDepsVisitor(), null));
    }

    return result;
  }

  @Override
  public Set<ResolvedName> visitAbstract(Abstract.AbstractDefinition def, Boolean params) {
    throw new IllegalStateException();
  }

  private Set<ResolvedName> visitAbstract(Abstract.AbstractDefinition def) {
    Set<ResolvedName> result = new HashSet<>();

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        result.addAll(((Abstract.TypeArgument) arg).getType().accept(new GetDepsVisitor(), null));
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      result.addAll(resultType.accept(new GetDepsVisitor(), null));
    }

    return result;
  }

  @Override
  public Set<ResolvedName> visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (isStatic)
      return Collections.emptySet();

    Set<ResolvedName> result = new HashSet<>();

    for (Abstract.TypeArgument param : def.getParameters()) {
      result.addAll(param.getType().accept(new GetDepsVisitor(), null));
    }

    for (Abstract.Constructor constructor : def.getConstructors()) {
      result.addAll(visitConstructor(constructor));
    }

    return result;
  }

  @Override
  public Set<ResolvedName> visitConstructor(Abstract.Constructor def, Boolean params) {
    throw new IllegalStateException();
  }

  private Set<ResolvedName> visitConstructor(Abstract.Constructor def) {
    Set<ResolvedName> result = new HashSet<>();
    for (Abstract.TypeArgument arg : def.getArguments()) {
      result.addAll(arg.getType().accept(new GetDepsVisitor(), null));
    }
    if (def.getDataType().getConditions() != null) {
      for (Abstract.Condition cond : def.getDataType().getConditions()) {
        if (cond.getConstructorName().equals(def.getName())) {
          result.addAll(cond.getTerm().accept(new GetDepsVisitor(), null));
        }
      }
    }

    return result;
  }

  public Set<ResolvedName> visitStatements(Collection<? extends Abstract.Statement> statements, boolean isStatic, boolean isFunction) {
    Set<ResolvedName> result = new HashSet<>();
    Set<ResolvedName> nonStatic = !isStatic && !isFunction ? new HashSet<ResolvedName>() : null;
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.DefineStatement defineStatement = (Abstract.DefineStatement) statement;
        if (isStatic) {
          if (defineStatement.isStatic() || isFunction) {
            result.add(new ResolvedName(myNamespace, defineStatement.getDefinition().getName()));
            result.addAll(defineStatement.getDefinition().accept(
                new DefinitionGetDepsVisitor(myNamespace.getChild(defineStatement.getDefinition().getName()), myOthers, null), true
            ));
          }
        } else {
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.AbstractDefinition) {
            result.addAll(visitAbstract((Abstract.AbstractDefinition) ((Abstract.DefineStatement) statement).getDefinition()));
          } else {
            myOthers.add(new ResolvedName(myNamespace, defineStatement.getDefinition().getName()));
            if (!isFunction && !((Abstract.DefineStatement) statement).isStatic()) {
              nonStatic.add(new ResolvedName(myNamespace, defineStatement.getDefinition().getName()));
              nonStatic.addAll(((Abstract.DefineStatement) statement).getDefinition().accept(
                  new DefinitionGetDepsVisitor(myNamespace.getChild(defineStatement.getDefinition().getName()), myOthers, null), true
              ));
            }
          }
        }
      }
    }
    if (nonStatic != null) {
      myClassToNonStatics.put(myNamespace.getResolvedName(), new ArrayList<>(nonStatic));
    }
    return result;
  }

  @Override
  public Set<ResolvedName> visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    return visitStatements(def.getStatements(), isStatic, false);
  }
}
