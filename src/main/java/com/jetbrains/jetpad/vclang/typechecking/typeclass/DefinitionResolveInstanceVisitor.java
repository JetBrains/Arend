package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;

import java.util.Collection;
import java.util.HashSet;

public class DefinitionResolveInstanceVisitor implements AbstractDefinitionVisitor<Scope, Void>, AbstractStatementVisitor<Scope, Scope> {
  private final InstanceScopeProvider myScopeProvider;
  private final SimpleClassViewInstanceProvider myInstanceProvider;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveInstanceVisitor(InstanceScopeProvider scopeProvider, SimpleClassViewInstanceProvider instanceProvider, ErrorReporter errorReporter) {
    myScopeProvider = scopeProvider;
    myInstanceProvider = instanceProvider;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
    Scope scope = new FunctionScope(parentScope, myScopeProvider.forDefinition(def));

    for (Abstract.Statement statement : def.getGlobalStatements()) {
      if (statement instanceof Abstract.NamespaceCommandStatement) {
        scope = statement.accept(this, scope);
      }
    }
    for (Abstract.Statement statement : def.getGlobalStatements()) {
      if (!(statement instanceof Abstract.NamespaceCommandStatement)) {
        scope = statement.accept(this, scope);
      }
    }

    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(scope, myInstanceProvider);
    exprVisitor.visitArguments(def.getArguments());

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(exprVisitor, null);
    }

    Abstract.FunctionBody body = def.getBody();
    if (body instanceof Abstract.TermFunctionBody) {
      ((Abstract.TermFunctionBody) body).getTerm().accept(exprVisitor, null);
    }
    if (body instanceof Abstract.ElimFunctionBody) {
      for (Abstract.Expression expression : ((Abstract.ElimFunctionBody) body).getExpressions()) {
        expression.accept(exprVisitor, null);
      }
      for (Abstract.FunctionClause clause : ((Abstract.ElimFunctionBody) body).getClauses()) {
        if (clause.getExpression() != null) {
          clause.getExpression().accept(exprVisitor, null);
        }
      }
    }


    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, Scope parentScope) {
    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider), null);
    }
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Scope parentScope) {
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider);
    exprVisitor.visitArguments(def.getParameters());

    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitConstructor(constructor, parentScope);
    }

    if (def.getConditions() != null) {
      Scope scope = new DataScope(parentScope, myScopeProvider.forDefinition(def));
      exprVisitor = new ExpressionResolveInstanceVisitor(scope, myInstanceProvider);
      for (Abstract.Condition cond : def.getConditions()) {
        cond.getTerm().accept(exprVisitor, null);
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Scope parentScope) {
    new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider).visitArguments(def.getArguments());
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Scope parentScope) {
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider);
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(exprVisitor, null);
    }

    try {
      Scope scope = new StaticClassScope(parentScope, myScopeProvider.forDefinition(def));
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (statement instanceof Abstract.NamespaceCommandStatement) {
          scope = statement.accept(this, scope);
        }
      }
      for (Abstract.Statement statement : def.getGlobalStatements()) {
        if (!(statement instanceof Abstract.NamespaceCommandStatement)) {
          scope = statement.accept(this, scope);
        }
      }

      exprVisitor.visitArguments(def.getPolyParameters());

      for (Abstract.ClassField field : def.getFields()) {
        field.accept(this, scope);
      }
      for (Abstract.Implementation implementation : def.getImplementations()) {
        implementation.accept(this, scope);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        definition.accept(this, scope);
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Scope parentScope) {
    def.getImplementation().accept(new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider), null);
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Scope parentScope) {
    new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider).visitReference(def.getUnderlyingClassReference(), null);
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Scope parentScope) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Scope parentScope) {
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider);
    exprVisitor.visitArguments(def.getArguments());
    exprVisitor.visitReference(def.getClassView(), null);
    for (Abstract.ClassFieldImpl impl : def.getClassFieldImpls()) {
      impl.getImplementation().accept(exprVisitor, null);
    }
    return null;
  }

  @Override
  public Scope visitDefine(Abstract.DefineStatement stat, Scope parentScope) {
    stat.getDefinition().accept(this, parentScope);
    return parentScope;
  }

  @Override
  public Scope visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Scope parentScope) {
    if (stat.getKind().equals(Abstract.NamespaceCommandStatement.Kind.OPEN)) {
      Scope scope = myScopeProvider.forDefinition(stat.getResolvedClass());
      if (stat.getNames() != null) {
        scope = new FilteredScope(scope, new HashSet<>(stat.getNames()), !stat.isHiding());
      }
      mergeInstances(scope, parentScope);
      parentScope = new OverridingScope(scope, parentScope);
    }
    return parentScope;
  }

  private void mergeInstances(Scope parent, Scope child) {
    Collection<? extends Abstract.ClassViewInstance> parentInstances = parent.getInstances();
    if (!parentInstances.isEmpty()) {
      Collection<? extends Abstract.ClassViewInstance> childInstances = child.getInstances();
      if (!childInstances.isEmpty()) {
        for (Abstract.ClassViewInstance instance : parentInstances) {
          for (Abstract.ClassViewInstance childInstance : childInstances) {
            if (instance.getClassView().getReferent() == childInstance.getClassView().getReferent() && instance.getClassifyingDefinition() == childInstance.getClassifyingDefinition()) {
              myErrorReporter.report(new DuplicateInstanceError(Error.Level.WARNING, instance, childInstance));
            }
          }
        }
      }
    }
  }
}
