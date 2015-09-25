package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.*;

import java.util.ArrayList;
import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Void, Namespace> {
  private final ErrorReporter myErrorReporter;
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;
  private final CompositeNameResolver myNameResolver;
  private List<String> myContext;

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, Namespace namespace, NameResolver nameResolver) {
    this(errorReporter, namespace, null, new CompositeNameResolver(), new ArrayList<String>());
    myNameResolver.pushNameResolver(nameResolver);
  }

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, Namespace staticNamespace, Namespace dynamicNamespace, CompositeNameResolver nameResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
    myNameResolver = nameResolver;
    myContext = context;

    assert myStaticNamespace != null || myDynamicNamespace != null;
  }

  @Override
  public Namespace visitFunction(Abstract.FunctionDefinition def, Void params) {
    if (def.getStatements().isEmpty()) {
      visitFunction(def);
      return null;
    } else {
      Namespace localNamespace = myDynamicNamespace == null ? null : myDynamicNamespace.getChild(def.getName());
      try (StatementResolveNameVisitor statementVisitor = new StatementResolveNameVisitor(myErrorReporter, myStaticNamespace == null ? null : myStaticNamespace.getChild(def.getName()), localNamespace, myNameResolver, myContext)) {
        for (Abstract.Statement statement : def.getStatements()) {
          statement.accept(statementVisitor, null);
        }
        visitFunction(def);
      }
      return localNamespace;
    }
  }

  private void visitFunction(Abstract.FunctionDefinition def) {
    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myDynamicNamespace == null);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(visitor, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else
        if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      if (def.getResultType() != null) {
        def.getResultType().accept(visitor, null);
      }

      if (def.getTerm() != null) {
        Namespace parentNamespace = myDynamicNamespace != null ? myDynamicNamespace : myStaticNamespace;
        myNameResolver.pushNameResolver(new SingleNameResolver(def.getName().name, new DefinitionPair(parentNamespace.getChild(def.getName()), def, null)));
        def.getTerm().accept(visitor, null);
        myNameResolver.popNameResolver();
      }
    }
  }

  @Override
  public Namespace visitData(Abstract.DataDefinition def, Void params) {
    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myDynamicNamespace == null);

    try (Utils.CompleteContextSaver<String> saver = new Utils.CompleteContextSaver<>(myContext)) {
      for (Abstract.TypeArgument parameter : def.getParameters()) {
        parameter.getType().accept(visitor, null);
        if (parameter instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) parameter).getNames());
        }
      }

      Namespace parentNamespace = myDynamicNamespace != null ? myDynamicNamespace : myStaticNamespace;
      myNameResolver.pushNameResolver(new SingleNameResolver(def.getName().name, new DefinitionPair(parentNamespace.getChild(def.getName()), def, null)));
      for (Abstract.Constructor constructor : def.getConstructors()) {
        if (constructor.getPatterns() == null) {
          visitConstructor(constructor, null);
        } else {
          myContext = saver.getOldContext();
          visitConstructor(constructor, null);
          myContext = saver.getCurrentContext();
        }
      }
      myNameResolver.popNameResolver();
    }

    return null;
  }

  @Override
  public Namespace visitConstructor(Abstract.Constructor def, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myDynamicNamespace == null);
      if (def.getPatterns() != null) {
        for (int i = 0; i < def.getPatterns().size(); ++i) {
          visitor.visitPattern(def, i);
        }
      }

      for (Abstract.TypeArgument argument : def.getArguments()) {
        argument.getType().accept(visitor, null);
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        }
      }
    }

    return null;
  }

  @Override
  public Namespace visitClass(Abstract.ClassDefinition def, Void params) {
    Namespace parentNamespace = myDynamicNamespace == null ? myStaticNamespace : myDynamicNamespace;
    Namespace localNamespace = new Namespace(def.getName());
    if (myDynamicNamespace != null) {
      myNameResolver.pushNameResolver(new NamespaceNameResolver(myDynamicNamespace, null));
    }
    CompositeNameResolver nameResolver = myNameResolver;
    if (myDynamicNamespace == null) {
      nameResolver = new CompositeNameResolver();
      nameResolver.pushNameResolver(new StaticNameResolver(myNameResolver));
    }
    try (StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, parentNamespace.getChild(def.getName()), localNamespace, nameResolver, myContext)) {
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(visitor, null);
      }
    }
    if (myDynamicNamespace != null) {
      myNameResolver.popNameResolver();
    }
    return localNamespace;
  }
}
