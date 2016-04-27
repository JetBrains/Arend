package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.listener.ResolveListener;

import java.util.ArrayList;
import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final Scope myParentScope;
  private List<String> myContext;
  private ResolveListener myResolveListener;

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, StaticNamespaceProvider staticNsProvider) {
    this(errorReporter, nameResolver, staticNsProvider, new EmptyScope(), new ArrayList<String>());
  }

  public DefinitionResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, StaticNamespaceProvider staticNsProvider, Scope parentScope, List<String> context) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myStaticNsProvider = staticNsProvider;
    myParentScope = parentScope;
    myContext = context;
  }

  public void setResolveListener(ResolveListener resolveListener) {
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }
    final FunctionScope scope = new FunctionScope(myParentScope, myStaticNsProvider.forDefinition(def));

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myErrorReporter, myNameResolver, scope, myContext, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(exprVisitor, null);
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
        resultType.accept(exprVisitor, null);
      }

      Abstract.Expression term = def.getTerm();
      if (term != null) {
        term.accept(exprVisitor, null);
      }
    }

    StatementResolveNameVisitor statementVisitor = new StatementResolveNameVisitor(myErrorReporter, myNameResolver, myStaticNsProvider, scope, myContext);
    statementVisitor.setResolveListener(myResolveListener);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(statementVisitor, null);
    }

    return null;
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myErrorReporter, myNameResolver, myParentScope, myContext, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(exprVisitor, null);
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
        resultType.accept(exprVisitor, null);
      }
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    Scope scope = new DataScope(myParentScope, myStaticNsProvider.forDefinition(def));

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myErrorReporter, myNameResolver, scope, myContext, myResolveListener);
    try (Utils.CompleteContextSaver<String> saver = new Utils.CompleteContextSaver<>(myContext)) {
      for (Abstract.TypeArgument parameter : def.getParameters()) {
        parameter.getType().accept(exprVisitor, null);
        if (parameter instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) parameter).getNames());
        }
      }

      for (Abstract.Constructor constructor : def.getConstructors()) {
        if (constructor.getPatterns() == null) {
          visitConstructor(constructor, null);
        } else {
          myContext = saver.getOldContext();
          visitConstructor(constructor, null);
          myContext = saver.getCurrentContext();
        }
      }

      if (def.getConditions() != null) {
        for (Abstract.Condition cond : def.getConditions()) {
          try (Utils.ContextSaver ignore = new Utils.ContextSaver(myContext)) {
            for (Abstract.PatternArgument patternArgument : cond.getPatterns()) {
              if (exprVisitor.visitPattern(patternArgument.getPattern())) {
                myResolveListener.replaceWithConstructor(patternArgument);
              }
            }
            cond.getTerm().accept(exprVisitor, null);
          }
        }
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myErrorReporter, myNameResolver, myParentScope, myContext, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      if (def.getPatterns() != null) {
        for (Abstract.PatternArgument patternArg : def.getPatterns()) {
          if (exprVisitor.visitPattern(patternArg.getPattern())) {
            myResolveListener.replaceWithConstructor(patternArg);
          }
        }
      }

      for (Abstract.TypeArgument argument : def.getArguments()) {
        argument.getType().accept(exprVisitor, null);
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        }
      }
    }

    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    Scope scope = new StaticClassScope(myParentScope, myStaticNsProvider.forDefinition(def));
    StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, myNameResolver, myStaticNsProvider, scope, myContext);
    visitor.setResolveListener(myResolveListener);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(visitor, null);
    }
    return null;
  }
}
