package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.NameDefinedError;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.MultiNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<StatementResolveNameVisitor.Flag, Object>, AutoCloseable {
  private final ErrorReporter myErrorReporter;
  private final Namespace myNamespace;
  private final MultiNameResolver myPrivateNameResolver;
  private final CompositeNameResolver myNameResolver;
  private final List<String> myContext;
  private ResolveListener myResolveListener;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, Namespace namespace, CompositeNameResolver nameResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myNamespace = namespace;
    myContext = context;

    myPrivateNameResolver = new MultiNameResolver();
    myNameResolver = nameResolver;
    myNameResolver.pushNameResolver(new NamespaceNameResolver(namespace));
    myNameResolver.pushNameResolver(myPrivateNameResolver);
  }

  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }

  public void setResolveListener(ResolveListener resolveListener) {
    myResolveListener = resolveListener;
  }

  @Override
  public NamespaceMember visitDefine(Abstract.DefineStatement stat, Flag flag) {
    if (!stat.isStatic() && flag == Flag.MUST_BE_STATIC) {
      myErrorReporter.report(new TypeCheckingError("Non-static definition in a static context", stat));
      return null;
    } else
    if (stat.isStatic() && flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
      return null;
    } else
    if (stat.isStatic() && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
      myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
      return null;
    } else {
      DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myErrorReporter, myNamespace, myNameResolver, myContext);
      visitor.setResolveListener(myResolveListener);
      stat.getDefinition().accept(visitor, stat.isStatic());
      NamespaceMember namespaceMember = myNamespace.addAbstractDefinition(stat.getDefinition());
      if (namespaceMember == null) {
        myErrorReporter.report(new NameDefinedError(true, stat, stat.getDefinition().getName(), myNamespace.getResolvedName()));
        return null;
      }
      if (stat.getDefinition() instanceof Abstract.DataDefinition) {
        Abstract.DataDefinition dataDefinition = (Abstract.DataDefinition) stat.getDefinition();
        for (Abstract.Constructor constructor : dataDefinition.getConstructors()) {
          namespaceMember.namespace.addAbstractDefinition(constructor);
          myNamespace.addMember(namespaceMember.namespace.getMember(constructor.getName().name));
        }
      }

      return namespaceMember;
    }
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Flag flag) {
    if (flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Namespace commands are not allowed in this context", stat));
      return null;
    }

    boolean export = false, remove = false;
    switch (stat.getKind()) {
      case OPEN:
        break;
      case CLOSE:
        remove = true;
        break;
      case EXPORT:
        export = true;
        break;
      default:
        throw new IllegalStateException();
    }

    List<? extends Abstract.Identifier> path = stat.getPath();
    Abstract.Identifier identifier = path.get(0);
    NamespaceMember member = null;
    for (Abstract.Identifier aPath : path) {
      Name name = identifier.getName();
      Name aPathName = aPath.getName();
      NamespaceMember member1 = member == null ? NameResolver.Helper.locateName(myNameResolver, name.name, false) : myNameResolver.getMember(member.namespace, aPathName.name);
      if (member1 == null) {
        if (member != null) {
          myErrorReporter.report(new NameDefinedError(false, stat, aPathName, member.namespace.getResolvedName()));
        } else {
          myErrorReporter.report(new NotInScopeError(aPath, name));
        }
        return null;
      }
      member = member1;
    }
    if (member == null) return null;

    stat.setResolvedPath(member.getResolvedName());

    List<? extends Abstract.Identifier> names = stat.getNames();
    if (names != null) {
      for (Abstract.Identifier name : names) {
        NamespaceMember member1 = myNameResolver.getMember(member.namespace, name.getName().name);
        if (member1 == null) {
          myErrorReporter.report(new NameDefinedError(false, stat, name.getName(), member.namespace.getResolvedName()));
        } else {
          if (member1.abstractDefinition != null) {
            Abstract.DefineStatement parentStatement = member1.abstractDefinition.getParentStatement();
            if (parentStatement != null && parentStatement.isStatic()) {
              processNamespaceCommand(member1, export, remove, stat);
            } else {
              myErrorReporter.report(new TypeCheckingError("Definition '" + name.getName() + "' is not static", stat));
            }
          } else if (member1.definition != null && member1.definition.getThisClass() == null) {
            processNamespaceCommand(member1, export, remove, stat);
          }
        }
      }
    } else {
      for (NamespaceMember member1 : member.namespace.getMembers()) {
        if (member1.abstractDefinition != null) {
          Abstract.DefineStatement parentStatement = member1.abstractDefinition.getParentStatement();
          if (parentStatement != null && parentStatement.isStatic()) {
            processNamespaceCommand(member1, export, remove, stat);
          }
        } else if (member1.definition != null && member1.definition.getThisClass() == null) {
          processNamespaceCommand(member1, export, remove, stat);
        }
      }
    }
    return null;
  }

  private void processNamespaceCommand(NamespaceMember member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
    boolean ok;
    if (export) {
      ok = myNamespace.addMember(member) == null;
    } else
    if (remove) {
      ok = myPrivateNameResolver.locateName(member.namespace.getName().name) != null;
      myPrivateNameResolver.remove(member.namespace.getName().name);
    } else {
      ok = myPrivateNameResolver.locateName(member.namespace.getName().name) == null;
      myPrivateNameResolver.add(member);
    }

    if (!ok) {
      GeneralError error = new NameDefinedError(!remove, sourceNode, member.namespace.getName(), null);
      error.setLevel(GeneralError.Level.WARNING);
      myErrorReporter.report(error);
    }
  }

  @Override
  public void close() {
    myNameResolver.popNameResolver();
    myNameResolver.popNameResolver();
  }
}
