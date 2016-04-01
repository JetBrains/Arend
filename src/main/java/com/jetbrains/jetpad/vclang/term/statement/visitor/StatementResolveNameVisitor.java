package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Abstract;
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
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.module.ModuleResolver;

import java.util.List;

import static com.jetbrains.jetpad.vclang.naming.NamespaceMember.toNamespaceMember;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<StatementResolveNameVisitor.Flag, Object>, AutoCloseable {
  private final ErrorReporter myErrorReporter;
  private final Namespace myNamespace;
  private final MultiNameResolver myPrivateNameResolver;
  private final CompositeNameResolver myNameResolver;
  private final ModuleResolver myModuleResolver;
  private final List<String> myContext;
  private ResolveListener myResolveListener;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, Namespace namespace, CompositeNameResolver nameResolver, ModuleResolver moduleResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myNamespace = namespace;
    myModuleResolver = moduleResolver;
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
    if (stat.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_STATIC) {
      myErrorReporter.report(new TypeCheckingError("Non-static definition in a static context", stat));
      return null;
    } else
    if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
      return null;
    } else
    if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
      myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
      return null;
    } else {
      DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myErrorReporter, myNamespace, myNameResolver, myModuleResolver, myContext);
      visitor.setResolveListener(myResolveListener);
      stat.getDefinition().accept(visitor, stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC);
      NamespaceMember namespaceMember = myNamespace.addAbstractDefinition(stat.getDefinition());
      if (namespaceMember == null) {
        myErrorReporter.report(new NameDefinedError(true, stat, stat.getDefinition().getName(), myNamespace.getResolvedName()));
        return null;
      }
      if (stat.getDefinition() instanceof Abstract.DataDefinition) {
        Abstract.DataDefinition dataDefinition = (Abstract.DataDefinition) stat.getDefinition();
        for (Abstract.Constructor constructor : dataDefinition.getConstructors()) {
          namespaceMember.namespace.addAbstractDefinition(constructor);
          myNamespace.addMember(namespaceMember.namespace.getMember(constructor.getName()));
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
        NamespaceMember member1 = member == null ? NameResolver.Helper.locateName(myNameResolver, aPath, false) : myNameResolver.getMember(member.namespace, aPath);
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
  }

  @Override
  public Object visitDefaultStaticCommand(Abstract.DefaultStaticStatement stat, Flag params) {
    return null;
  }

  private void processNamespaceCommand(NamespaceMember member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
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
  }

  @Override
  public void close() {
    myNameResolver.popNameResolver();
    myNameResolver.popNameResolver();
  }
}
