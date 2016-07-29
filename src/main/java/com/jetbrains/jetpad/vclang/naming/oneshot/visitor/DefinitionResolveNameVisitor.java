package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.Referable;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
  private final Scope myParentScope;
  private List<String> myContext;
  private final NameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;
  private final StaticNamespaceProvider myStaticNsProvider;
  private final DynamicNamespaceProvider myDynamicNsProvider;
  private final ResolveListener myResolveListener;

  public DefinitionResolveNameVisitor(StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider,
                                      Scope parentScope,
                                      NameResolver nameResolver, ErrorReporter errorReporter, ResolveListener resolveListener) {
    this(staticNsProvider, dynamicNsProvider, parentScope, new ArrayList<String>(), nameResolver, errorReporter, resolveListener);
  }

  public DefinitionResolveNameVisitor(StaticNamespaceProvider staticNsProvider, DynamicNamespaceProvider dynamicNsProvider,
                                      Scope parentScope, List<String> context,
                                      NameResolver nameResolver, ErrorReporter errorReporter, ResolveListener resolveListener) {
    myParentScope = parentScope;
    myContext = context;
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
    myStaticNsProvider = staticNsProvider;
    myDynamicNsProvider = dynamicNsProvider;
    myResolveListener = resolveListener;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }
    final FunctionScope scope = new FunctionScope(myParentScope, myStaticNsProvider.forDefinition(def));

    StatementResolveNameVisitor statementVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, myNameResolver, myErrorReporter, scope, myContext, myResolveListener);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(statementVisitor, null);
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(statementVisitor.getCurrentScope(), myContext, myNameResolver, myErrorReporter, myResolveListener);
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

    return null;
  }

  @Override
  public Void visitAbstract(Abstract.AbstractDefinition def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
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

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(scope, myContext, myNameResolver, myErrorReporter, myResolveListener);
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

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
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
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      Referable sup = myNameResolver.resolveDefinition(myParentScope, Collections.singletonList(superClass.getName()));
      if (sup != null) {
        if (sup instanceof Abstract.ClassDefinition) {
          myResolveListener.superClassResolved(superClass, sup);
          Namespace supNamespace = myDynamicNsProvider.forClass((Abstract.ClassDefinition) sup);

          if (superClass.getRenamings() != null) {
            for (Abstract.IdPair idPair : superClass.getRenamings()) {
              Referable pair1 = supNamespace.resolveName(idPair.getFirstName());
              if (pair1 != null) {
                myResolveListener.idPairFirstResolved(idPair, pair1);
              } else {
                myErrorReporter.report(new NotInScopeError(def, idPair, idPair.getFirstName()));
              }
            }
          }

          if (superClass.getHidings() != null) {
            for (Abstract.Identifier identifier : superClass.getHidings()) {
              Referable ref = supNamespace.resolveName(identifier.getName());
              if (ref != null) {
                myResolveListener.identifierResolved(identifier, ref);
              } else {
                myErrorReporter.report(new NotInScopeError(def, identifier, identifier.getName()));
              }
            }
          }
        } else {
          myErrorReporter.report(new TypeCheckingError("Expected a class", sup));
        }
      }
    }

    try {
      Namespace staticNamespace = myStaticNsProvider.forDefinition(def);

      Scope staticScope = new StaticClassScope(myParentScope, staticNamespace);
      StatementResolveNameVisitor stVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, myNameResolver, myErrorReporter, staticScope, myContext, myResolveListener);
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && !Abstract.DefineStatement.StaticMod.STATIC.equals(((Abstract.DefineStatement) statement).getStaticMod()))
          continue;  // FIXME[where]
        statement.accept(stVisitor, null);
      }

      Scope dynamicScope = new DynamicClassScope(myParentScope, staticNamespace, myDynamicNsProvider.forClass(def));
      StatementResolveNameVisitor dyVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, myNameResolver, myErrorReporter, dynamicScope, myContext, myResolveListener);
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && !Abstract.DefineStatement.StaticMod.STATIC.equals(((Abstract.DefineStatement) statement).getStaticMod()))
          statement.accept(dyVisitor, null);
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.ImplementDefinition def, Boolean params) {
    if (myResolveListener == null) {
      return null;
    }

    Abstract.Definition parentDef = def.getParentStatement().getParentDefinition();
    Referable referable = null;
    if (parentDef instanceof Abstract.ClassDefinition && ((Abstract.ClassDefinition) parentDef).getSuperClasses() != null) {
      for (Abstract.SuperClass superClass : ((Abstract.ClassDefinition) parentDef).getSuperClasses()) {
        if (superClass.getReferent() instanceof Abstract.ClassDefinition) {
          Namespace supNamespace = myDynamicNsProvider.forClass((Abstract.ClassDefinition) superClass.getReferent());
          referable = supNamespace.resolveName(def.getName());
          if (referable != null) {
            break;
          }
        }
      }
    }

    if (referable == null) {
      myErrorReporter.report(new NotInScopeError(parentDef, def, def.getName()));
    } else {
      myResolveListener.implementResolved(def, referable);
    }

    def.getExpression().accept(new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener), null);
    return null;
  }

  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }
}
