package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.MultiNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.SingleNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
  private final ErrorReporter myErrorReporter;
  private final Namespace myNamespace;
  private final CompositeNameResolver myNameResolver;
  private List<String> myContext;
  private ResolveListener myResolveListener;

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, Namespace namespace, NameResolver nameResolver) {
    this(errorReporter, namespace, new CompositeNameResolver(), new ArrayList<String>());
    if (nameResolver != null) {
      myNameResolver.pushNameResolver(nameResolver);
    }
  }

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, Namespace namespace, CompositeNameResolver nameResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myNamespace = namespace;
    myNameResolver = nameResolver;
    myContext = context;
  }

  public void setResolveListener(ResolveListener resolveListener) {
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    Collection<? extends Abstract.Statement> statements = def.getStatements();
    if (statements.isEmpty()) {
      visitFunction(def);
      return null;
    } else {
      try (StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, myNamespace.getChild(def.getName().name), myNameResolver, myContext)) {
        visitor.setResolveListener(myResolveListener);
        for (Abstract.Statement statement : statements) {
          statement.accept(visitor, isStatic ? StatementResolveNameVisitor.Flag.MUST_BE_STATIC : null);
        }
        visitFunction(def);
      }
      return null;
    }
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myResolveListener);
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

      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(visitor, null);
      }
    }
    return null;
  }

  private void visitFunction(Abstract.FunctionDefinition def) {
    if (myResolveListener == null) {
      return;
    }

    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myResolveListener);
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

      Abstract.Expression resultType = def.getResultType();
      if (resultType != null) {
        resultType.accept(visitor, null);
      }

      Abstract.Expression term = def.getTerm();
      if (term != null) {
        Name name = def.getName();
        myNameResolver.pushNameResolver(new SingleNameResolver(name.name, new NamespaceMember(myNamespace.getChild(name.name), def, null)));
        term.accept(visitor, null);
        myNameResolver.popNameResolver();
      }
    }
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myResolveListener);
    try (Utils.CompleteContextSaver<String> saver = new Utils.CompleteContextSaver<>(myContext)) {
      for (Abstract.TypeArgument parameter : def.getParameters()) {
        parameter.getType().accept(visitor, null);
        if (parameter instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) parameter).getNames());
        }
      }

      Name name = def.getName();

      MultiNameResolver conditionsResolver = new MultiNameResolver();
      conditionsResolver.add(new NamespaceMember(myNamespace.getChild(name.name), def, null));
      myNameResolver.pushNameResolver(new SingleNameResolver(name.name, new NamespaceMember(myNamespace.getChild(name.name), def, null)));

      for (Abstract.Constructor constructor : def.getConstructors()) {
        conditionsResolver.add(new NamespaceMember(myNamespace.getChild(name.name).getChild(constructor.getName().name), constructor, null));
        if (constructor.getPatterns() == null) {
          visitConstructor(constructor, null);
        } else {
          myContext = saver.getOldContext();
          visitConstructor(constructor, null);
          myContext = saver.getCurrentContext();
        }
      }

      myNameResolver.pushNameResolver(conditionsResolver);
      if (def.getConditions() != null) {
        for (Abstract.Condition cond : def.getConditions()) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            for (Abstract.PatternArgument patternArgument : cond.getPatterns()) {
              if (visitor.visitPattern(patternArgument.getPattern())) {
                myResolveListener.replaceWithConstructor(patternArgument);
              }
            }
            cond.getTerm().accept(visitor, null);
          }
        }
      }
      myNameResolver.popNameResolver();
      myNameResolver.popNameResolver();
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ResolveNameVisitor visitor = new ResolveNameVisitor(myErrorReporter, myNameResolver, myContext, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      if (def.getPatterns() != null) {
        for (Abstract.PatternArgument patternArg : def.getPatterns()) {
          if (visitor.visitPattern(patternArg.getPattern())) {
            myResolveListener.replaceWithConstructor(patternArg);
          }
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
  public Void visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    try (StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, myNamespace.getChild(def.getName().name), myNameResolver, myContext)) {
      visitor.setResolveListener(myResolveListener);
      for (Abstract.Statement statement : def.getStatements()) {
        statement.accept(visitor, null);
      }
    }
    return null;
  }
}
