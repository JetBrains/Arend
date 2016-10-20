package com.jetbrains.jetpad.vclang.naming.oneshot.visitor;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolver;
import com.jetbrains.jetpad.vclang.naming.error.NotInScopeError;
import com.jetbrains.jetpad.vclang.naming.error.WrongDefinition;
import com.jetbrains.jetpad.vclang.naming.namespace.DynamicNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.namespace.StaticNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.oneshot.ResolveListener;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.Utils;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;

import java.util.ArrayList;
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
      visitArguments(def.getArguments(), exprVisitor);

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

  private void visitArguments(List<? extends Abstract.Argument> arguments, ExpressionResolveNameVisitor visitor) {
    for (Abstract.Argument argument : arguments) {
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
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Boolean isStatic) {
    if (myResolveListener == null) {
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitArguments(def.getArguments(), exprVisitor);

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
    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(exprVisitor, null);
    }

    try {
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && Abstract.DefineStatement.StaticMod.STATIC.equals(((Abstract.DefineStatement) statement).getStaticMod()) && ((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.ClassView) {
          visitClassView((Abstract.ClassView) ((Abstract.DefineStatement) statement).getDefinition(), true);
        }
      }

      Namespace staticNamespace = myStaticNsProvider.forDefinition(def);
      Scope staticScope = new StaticClassScope(myParentScope, staticNamespace);
      StatementResolveNameVisitor stVisitor = new StatementResolveNameVisitor(myStaticNsProvider, myDynamicNsProvider, myNameResolver, myErrorReporter, staticScope, myContext, myResolveListener);
      for (Abstract.Statement statement : def.getStatements()) {
        if (statement instanceof Abstract.DefineStatement && (!Abstract.DefineStatement.StaticMod.STATIC.equals(((Abstract.DefineStatement) statement).getStaticMod()) || ((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.ClassView))
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

    if (parentDef instanceof Abstract.ClassDefinition) {
      Abstract.Definition referable = myNameResolver.resolveClassField((Abstract.ClassDefinition) parentDef, def.getName(), myErrorReporter, def);
      if (referable != null) {
        myResolveListener.implementResolved(def, referable);
      }
    } else {
      // TODO: Is this possible? If it is, then this error message is incorrect.
      myErrorReporter.report(new NotInScopeError(def, def.getName()));
    }

    def.getExpression().accept(new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Boolean params) {
    if (myResolveListener == null) {
      return null;
    }

    Namespace staticNamespace = myStaticNsProvider.forDefinition(def.getParentStatement().getParentDefinition());
    def.getUnderlyingClassDefCall().accept(new ExpressionResolveNameVisitor(staticNamespace /* TODO: Why not myParentScope? */, myContext, myNameResolver, myErrorReporter, myResolveListener), null);
    Abstract.Definition resolvedUnderlyingClass = def.getUnderlyingClassDefCall().getReferent();
    if (!(resolvedUnderlyingClass instanceof Abstract.ClassDefinition)) {
      myErrorReporter.report(resolvedUnderlyingClass != null ? new WrongDefinition("Expected a class", def) : new NotInScopeError(def, def.getUnderlyingClassDefCall().getName()));
      return null;
    }

    Namespace dynamicNamespace = myDynamicNsProvider.forClass((Abstract.ClassDefinition) resolvedUnderlyingClass);
    Abstract.Definition resolvedClassifyingField = dynamicNamespace.resolveName(def.getClassifyingFieldName());
    if (!(resolvedClassifyingField instanceof Abstract.ClassField)) {
      myErrorReporter.report(resolvedClassifyingField != null ? new WrongDefinition("Expected a class field", def) : new NotInScopeError(def, def.getClassifyingFieldName()));
      return null;
    }

    myResolveListener.classViewResolved(def, (Abstract.ClassField) resolvedClassifyingField);

    for (Abstract.ClassViewField viewField : def.getFields()) {
      Abstract.ClassField classField = myNameResolver.resolveClassField((Abstract.ClassDefinition) resolvedUnderlyingClass, viewField.getUnderlyingFieldName(), myErrorReporter, viewField);
      if (classField != null) {
        myResolveListener.classViewFieldResolved(viewField, classField);
      }
    }
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Boolean params) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Boolean params) {
    if (myResolveListener == null) {
      return null;
    }

    ExpressionResolveNameVisitor exprVisitor = new ExpressionResolveNameVisitor(myParentScope, myContext, myNameResolver, myErrorReporter, myResolveListener);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      visitArguments(def.getArguments(), exprVisitor);

      Abstract.Expression term = def.getTerm();
      if (term != null) {
        term.accept(exprVisitor, null);
      }
    }

    return null;
  }

  public enum Flag { MUST_BE_STATIC, MUST_BE_DYNAMIC }
}
