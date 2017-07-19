package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.error.Error;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.resolving.OpenCommand;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateInstanceError;
import com.jetbrains.jetpad.vclang.naming.namespace.Namespace;
import com.jetbrains.jetpad.vclang.naming.scope.DataScope;
import com.jetbrains.jetpad.vclang.naming.scope.FunctionScope;
import com.jetbrains.jetpad.vclang.naming.scope.StaticClassScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.FilteredScope;
import com.jetbrains.jetpad.vclang.naming.scope.primitive.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;

import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefinitionResolveInstanceVisitor implements AbstractDefinitionVisitor<Scope, Void> {
  private final InstanceScopeProvider myScopeProvider;
  private final SimpleClassViewInstanceProvider myInstanceProvider;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveInstanceVisitor(InstanceScopeProvider scopeProvider, SimpleClassViewInstanceProvider instanceProvider, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ErrorReporter errorReporter) {
    myScopeProvider = scopeProvider;
    myInstanceProvider = instanceProvider;
    myOpens = opens;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Scope parentScope) {
    Iterable<Scope> extraScopes = getExtraScopes(def, parentScope);
    FunctionScope scope = new FunctionScope(parentScope, myScopeProvider.forDefinition(def), extraScopes);
    scope.findIntroducedDuplicateInstances(this::warnDuplicate);

    for (Abstract.Definition definition : def.getGlobalDefinitions()) {
      definition.accept(this, scope);
    }

    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(scope, myInstanceProvider);
    exprVisitor.visitParameters(def.getParameters());

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(exprVisitor, null);
    }

    Abstract.FunctionBody body = def.getBody();
    if (body instanceof Abstract.TermFunctionBody) {
      ((Abstract.TermFunctionBody) body).getTerm().accept(exprVisitor, null);
    }
    if (body instanceof Abstract.ElimFunctionBody) {
      for (Abstract.ReferenceExpression ref : ((Abstract.ElimFunctionBody) body).getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
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
    Scope scope = new DataScope(parentScope, myScopeProvider.forDefinition(def));
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(scope, myInstanceProvider);
    exprVisitor.visitParameters(def.getParameters());

    if (def.getEliminatedReferences() != null) {
      for (Abstract.ReferenceExpression ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
    }
    for (Abstract.ConstructorClause clause : def.getConstructorClauses()) {
      for (Abstract.Constructor constructor : clause.getConstructors()) {
        visitConstructor(constructor, scope);
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Scope parentScope) {
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider);
    exprVisitor.visitParameters(def.getParameters());
    if (def.getEliminatedReferences() != null) {
      for (Abstract.ReferenceExpression ref : def.getEliminatedReferences()) {
        exprVisitor.visitReference(ref, null);
      }
      for (Abstract.FunctionClause clause : def.getClauses()) {
        if (clause.getExpression() != null) {
          clause.getExpression().accept(exprVisitor, null);
        }
      }
    }
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Scope parentScope) {
    ExpressionResolveInstanceVisitor exprVisitor = new ExpressionResolveInstanceVisitor(parentScope, myInstanceProvider);
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(exprVisitor, null);
    }

    try {
      Iterable<Scope> extraScopes = getExtraScopes(def, parentScope);
      StaticClassScope staticScope = new StaticClassScope(parentScope, myScopeProvider.forDefinition(def), extraScopes);
      staticScope.findIntroducedDuplicateInstances(this::warnDuplicate);

      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        definition.accept(this, staticScope);
      }

      exprVisitor.visitParameters(def.getPolyParameters());

      for (Abstract.ClassField field : def.getFields()) {
        field.accept(this, staticScope);
      }
      for (Abstract.Implementation implementation : def.getImplementations()) {
        implementation.accept(this, staticScope);
      }
      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        definition.accept(this, staticScope);
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
    exprVisitor.visitParameters(def.getParameters());
    exprVisitor.visitReference(def.getClassView(), null);
    for (Abstract.ClassFieldImpl impl : def.getClassFieldImpls()) {
      impl.getImplementation().accept(exprVisitor, null);
    }
    return null;
  }

  private Iterable<Scope> getExtraScopes(Abstract.Definition def, Scope currentScope) {
    return StreamSupport.stream(myOpens.apply(def).spliterator(), false)
        .flatMap(this::processOpenCommand)
        .collect(Collectors.toList());
  }

  private Stream<Scope> processOpenCommand(OpenCommand cmd) {
    Scope scope = myScopeProvider.forDefinition(cmd.getResolvedClass());
    if (cmd.getNames() != null) {
      scope = new FilteredScope(scope, new HashSet<>(cmd.getNames()), !cmd.isHiding());
    }
    return Stream.of(scope);
  }

  private void warnDuplicate(Abstract.ClassViewInstance instance1, Abstract.ClassViewInstance instance2) {
    myErrorReporter.report(new DuplicateInstanceError(Error.Level.WARNING, instance1, instance2));
  }
}
