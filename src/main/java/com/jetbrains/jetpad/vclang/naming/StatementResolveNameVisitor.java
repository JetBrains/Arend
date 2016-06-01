package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.MergeScope;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<StatementResolveNameVisitor.Flag, Object> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final List<String> myContext;
  private Scope myScope;
  private ResolveListener myResolveListener;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider, Scope parentScope, List<String> context) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myScope = parentScope;
    myContext = context;
  }

  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }

  public void setResolveListener(ResolveListener resolveListener) {
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Flag flag) {
    if (stat.getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_STATIC) {
      myErrorReporter.report(new GeneralError("Non-static definition in a static context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Static definitions are not allowed in this context", stat));
      return null;
    } else if (stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC && stat.getDefinition() instanceof Abstract.AbstractDefinition) {
      myErrorReporter.report(new TypeCheckingError("Abstract definitions cannot be static", stat));
      return null;
    }
    DefinitionResolveNameVisitor visitor = new DefinitionResolveNameVisitor(myErrorReporter, myNameResolver, myStaticNsProvider, myDynamicNsProvider, myScope, myContext);
    visitor.setResolveListener(myResolveListener);
    stat.getDefinition().accept(visitor, stat.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Flag flag) {
    if (flag == Flag.MUST_BE_DYNAMIC) {
      myErrorReporter.report(new TypeCheckingError("Namespace commands are not allowed in this context", stat));
      return null;
    }

    if (Abstract.NamespaceCommandStatement.Kind.EXPORT.equals(stat.getKind())) {
      throw new UnsupportedOperationException();
    }

    ModuleNamespace moduleNamespace = myNameResolver.resolveModuleNamespace(stat.getModulePath());
    if (moduleNamespace == null || moduleNamespace.getRegisteredClass() == null) {
      myErrorReporter.report(new NotInScopeError(null, stat.getModulePath().toString()));  // FIXME: null? really?
      return null;
    }

    Referable ref = stat.getPath().isEmpty() ? moduleNamespace.getRegisteredClass() : myNameResolver.resolveDefinition(myNameResolver.staticNamespaceFor(moduleNamespace.getRegisteredClass()), stat.getPath());
    if (ref == null) {
      myErrorReporter.report(new NotInScopeError(null, stat.getPath().toString()));  // FIXME: report proper error
      return null;
    }

    if (stat.getKind().equals(Abstract.NamespaceCommandStatement.Kind.OPEN)) {
      myScope = new MergeScope(myScope, myNameResolver.staticNamespaceFor(ref));
    }

    return null;
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
