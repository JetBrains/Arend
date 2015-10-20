package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.visitor.GetDepsVisitor;

import java.util.*;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Void, Set<ResolvedName>> {
  private final Namespace myNamespace;

  public DefinitionGetDepsVisitor(Namespace namespace) {
    myNamespace = namespace;
  }

  public static Set<ResolvedName> visitNamespace(Namespace ns) {
    Set<ResolvedName> result = new HashSet<>();
    for (NamespaceMember member : ns.getMembers()) {
      if (member.abstractDefinition != null) {
        if (!(member.abstractDefinition instanceof Abstract.Constructor)) {
          result.add(member.getResolvedName());
          result.addAll(member.abstractDefinition.accept(new DefinitionGetDepsVisitor(member.namespace), null));
        }
      } else {
        result.addAll(visitNamespace(member.namespace));
      }
    }
    return result;
  }

  @Override
  public Set<ResolvedName> visitFunction(Abstract.FunctionDefinition def, Void params) {
    Set<ResolvedName> result = new HashSet<>();
    result.addAll(visitStatements(def.getStatements()));

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        result.addAll(((Abstract.TypeArgument) arg).getType().accept(new GetDepsVisitor(), null));
      }
    }

    if (def.getResultType() != null) {
      result.addAll(def.getResultType().accept(new GetDepsVisitor(), null));
    }

    if (def.getTerm() != null) {
      result.addAll(def.getTerm().accept(new GetDepsVisitor(), null));
    }

    return result;
  }

  @Override
  public Set<ResolvedName> visitData(Abstract.DataDefinition def, Void isSiblings) {
    Set<ResolvedName> result = new HashSet<>();
    for (Abstract.TypeArgument param : def.getParameters()) {
      result.addAll(param.getType().accept(new GetDepsVisitor(), null));
    }
    for (Abstract.Constructor constructor : def.getConstructors()) {
      result.add(new ResolvedName(myNamespace, constructor.getName()));
    }
    return result;
  }

  @Override
  public Set<ResolvedName> visitConstructor(Abstract.Constructor def, Void isSiblings) {
    Set<ResolvedName> result = new HashSet<>();
    for (Abstract.TypeArgument arg : def.getArguments()) {
      result.addAll(arg.getType().accept(new GetDepsVisitor(), null));
    }
    result.remove(new ResolvedName(myNamespace.getParent().getParent(), def.getDataType().getName().name));
    return result;
  }

  public Set<ResolvedName> visitStatements(Collection<? extends Abstract.Statement> statements) {
    Set<ResolvedName> result = new HashSet<>();
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.DefineStatement defineStatement = (Abstract.DefineStatement) statement;
        result.add(new ResolvedName(myNamespace, defineStatement.getDefinition().getName()));
        result.addAll(defineStatement.getDefinition().accept(new DefinitionGetDepsVisitor(
            myNamespace.getChild(defineStatement.getDefinition().getName())
        ), null));
      } else if (statement instanceof Abstract.NamespaceCommandStatement) {
        Abstract.NamespaceCommandStatement nsStatement = (Abstract.NamespaceCommandStatement) statement;
        if (nsStatement.getKind() == Abstract.NamespaceCommandStatement.Kind.EXPORT) {
          result.addAll(nsStatement.getExported());
        }
      }
    }
    return result;
  }

  @Override
  public Set<ResolvedName> visitClass(Abstract.ClassDefinition def, Void isSiblings) {
    return visitStatements(def.getStatements());
  }
}
