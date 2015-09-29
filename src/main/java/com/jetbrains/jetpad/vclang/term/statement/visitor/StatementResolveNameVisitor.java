package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.NameDefinedError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;

import java.io.Closeable;
import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<Void, Object>, Closeable {
  private final ErrorReporter myErrorReporter;
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;
  private final Namespace myPrivateNamespace;
  private final CompositeNameResolver myNameResolver;
  private final List<String> myContext;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, Namespace staticNamespace, Namespace dynamicNamespace, CompositeNameResolver nameResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
    myPrivateNamespace = staticNamespace == null ? null : new Namespace(staticNamespace.getName());
    myNameResolver = nameResolver;
    myContext = context;

    myNameResolver.pushNameResolver(new NamespaceNameResolver(staticNamespace, dynamicNamespace));
    if (myPrivateNamespace != null) {
      myNameResolver.pushNameResolver(new NamespaceNameResolver(myPrivateNamespace, null));
    }

    assert myStaticNamespace != null || myDynamicNamespace != null;
  }

  @Override
  public DefinitionPair visitDefine(Abstract.DefineStatement stat, Void params) {
    if (!stat.isStatic() && myDynamicNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myStaticNamespace, "Non-static definition in a static context", stat, myContext));
      return null;
    } else
    if (stat.isStatic() && myStaticNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(null, "Static definitions are not allowed in this context", stat, myContext));
      return null;
    } else
    if (stat.isStatic() && stat.getDefinition() instanceof Abstract.FunctionDefinition && ((Abstract.FunctionDefinition) stat.getDefinition()).getArrow() == null) {
      myErrorReporter.report(new TypeCheckingError(null, "Abstract definitions cannot be static", stat, myContext));
      return null;
    } else {
      Namespace localNamespace = stat.getDefinition().accept(new DefinitionResolveNameVisitor(myErrorReporter, myStaticNamespace, stat.isStatic() ? null : myDynamicNamespace, myNameResolver, myContext), null);
      Namespace parentNamespace = stat.isStatic() ? myStaticNamespace : myDynamicNamespace;
      DefinitionPair definitionPair = parentNamespace.addAbstractDefinition(stat.getDefinition());
      if (definitionPair == null) {
        myErrorReporter.report(new NameDefinedError(true, stat, stat.getDefinition().getName(), parentNamespace));
        return null;
      }
      if (stat.getDefinition() instanceof Abstract.DataDefinition) {
        for (Abstract.Constructor constructor : ((Abstract.DataDefinition) stat.getDefinition()).getConstructors()) {
          definitionPair.namespace.addAbstractDefinition(constructor);
          parentNamespace.addAbstractDefinition(constructor);
        }
      }

      definitionPair.setLocalNamespace(localNamespace);
      return definitionPair;
    }
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    if (myStaticNamespace == null || myPrivateNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myStaticNamespace, "Namespace commands are not allowed in this context", stat, myContext));
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
    DefinitionPair member = null;
    for (Abstract.Identifier aPath : path) {
      DefinitionPair member1 = member == null ? NameResolver.Helper.locateName(myNameResolver, identifier.getName().name, aPath, false, myErrorReporter) : myNameResolver.getMember(member.namespace, aPath.getName().name);
      if (member1 == null) {
        if (member != null) {
          myErrorReporter.report(new NameDefinedError(false, stat, aPath.getName(), member.namespace));
        }
        return null;
      }
      member = member1;
    }
    if (member == null) return null;

    List<? extends Abstract.Identifier> names = stat.getNames();
    if (names != null) {
      for (Abstract.Identifier name : names) {
        DefinitionPair member1 = myNameResolver.getMember(member.namespace, name.getName().name);
        if (member1 == null) {
          myErrorReporter.report(new NameDefinedError(false, stat, name.getName(), member.namespace));
        } else {
          processNamespaceCommand(member1, export, remove, stat);
        }
      }
    } else {
      for (DefinitionPair member1 : member.namespace.getMembers()) {
        processNamespaceCommand(member1, export, remove, stat);
      }
    }
    return null;
  }

  private void processNamespaceCommand(DefinitionPair member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
    boolean ok;
    if (export) {
      ok = myStaticNamespace.addMember(member) == null;
    } else
    if (remove) {
      ok = myPrivateNamespace.removeMember(member) != null;
    } else {
      ok = myPrivateNamespace.addMember(member) == null;
    }

    if (!ok) {
      GeneralError error = new NameDefinedError(!remove, sourceNode, member.namespace.getName(), null);
      error.setLevel(GeneralError.Level.WARNING);
      myErrorReporter.report(error);
    }
  }

  @Override
  public void close() {
    if (myPrivateNamespace != null) {
      myNameResolver.popNameResolver();
    }
    myNameResolver.popNameResolver();
  }
}
