package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<StatementResolveNameVisitor.Flag, Object> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final Scope myParentScope;
  private final List<String> myContext;
  private ResolveListener myResolveListener;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, StaticNamespaceProvider staticNsProvider, Scope parentScope, List<String> context) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myStaticNsProvider = staticNsProvider;
    myParentScope = parentScope;
    myContext = context;
  }

  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }

  public void setResolveListener(ResolveListener resolveListener) {
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Flag flag) {
    if (stat.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_STATIC) {
      myErrorReporter.report(new TypeCheckingError("Non-static definition in a static context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
      myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
      return null;
    }
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myErrorReporter, myNameResolver, myStaticNsProvider, myParentScope, myContext);
    visitor.setResolveListener(myResolveListener);
    stat.getDefinition().accept(visitor, stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Flag flag) {
    /*
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

    NamespaceMember member;
    if (stat.getResolvedClass() == null) {
      if (stat.getModulePath() != null) {
        member = myModuleResolver.locateModule(new ModulePath(stat.getModulePath()));
        if (member == null) {
          myErrorReporter.report(new NotInScopeError(stat, new ModulePath(stat.getModulePath()).toString()));
          return null;
        }
      } else {
        member = null;
      }

      List<String> path = stat.getPath();
      for (String aPath : path) {
        //NamespaceMember member1 = member == null ? NameResolver.Helper.locateName(myNameResolver, aPath, false) : myNameResolver.getMember(member.namespace, aPath);
        if (member1 == null) {
          if (member != null) {
            myErrorReporter.report(new NameDefinedError(false, stat, aPath, member.namespace.getResolvedName()));
          } else {
            myErrorReporter.report(new NotInScopeError(stat, aPath));
          }
          return null;
        }
        member = member1;
      }
      if (member == null) return null;

      myResolveListener.nsCmdResolved(stat, member.getResolvedDefinition());
    } else {
      member = toNamespaceMember(stat.getResolvedClass());
    }

    List<String> names = stat.getNames();
    if (names != null) {
      for (String name1 : names) {
        NamespaceMember member1 = myNameResolver.getMember(member.namespace, name1);
        if (member1 == null) {
          myErrorReporter.report(new NameDefinedError(false, stat, name1, member.namespace.getResolvedName()));
        } else {
          if (member1.abstractDefinition != null) {
            Abstract.DefineStatement parentStatement = member1.abstractDefinition.getParentStatement();
            if (parentStatement != null && parentStatement.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC) {
              processNamespaceCommand(member1, export, remove, stat);
            } else {
              myErrorReporter.report(new TypeCheckingError("Definition '" + name1 + "' is not static", stat));
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
          if (parentStatement != null && parentStatement.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC) {
            processNamespaceCommand(member1, export, remove, stat);
          }
        } else if (member1.definition != null && member1.definition.getThisClass() == null) {
          processNamespaceCommand(member1, export, remove, stat);
        }
      }
    }
    return null;
    */
    return null;//
  }

  @Override
  public Object visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, Flag params) {
    return null;
  }

  private void processNamespaceCommand(NamespaceMember member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
    /*
    boolean ok;
    if (export) {
      ok = myNamespace.addMember(member) == null;
    } else
    if (remove) {
      ok = myPrivateNameResolver.locateName(member.namespace.getName()) != null;
      myPrivateNameResolver.remove(member.namespace.getName());
    } else {
      ok = myPrivateNameResolver.locateName(member.namespace.getName()) == null;
      myPrivateNameResolver.add(member);
    }

    if (!ok) {
      GeneralError error = new NameDefinedError(!remove, sourceNode, member.namespace.getName(), null);
      error.setLevel(GeneralError.Level.WARNING);
      myErrorReporter.report(error);
    }
    */
  }
}
