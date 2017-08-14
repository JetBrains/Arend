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
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.ClassViewInstanceProviderProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleClassViewInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceScopeProvider;

import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefinitionResolveInstanceVisitor implements AbstractDefinitionVisitor<SimpleClassViewInstanceProvider, Void> {
  private final ClassViewInstanceProviderProvider myInstanceProviderProvider;
  private final InstanceScopeProvider myScopeProvider;
  private final Function<Abstract.Definition, Iterable<OpenCommand>> myOpens;
  private final ErrorReporter myErrorReporter;

  public DefinitionResolveInstanceVisitor(ClassViewInstanceProviderProvider instanceProviderProvider, InstanceScopeProvider scopeProvider, Function<Abstract.Definition, Iterable<OpenCommand>> opens, ErrorReporter errorReporter) {
    myInstanceProviderProvider = instanceProviderProvider;
    myScopeProvider = scopeProvider;
    myOpens = opens;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, SimpleClassViewInstanceProvider parentInstanceProvider) {
    Iterable<Scope> extraScopes = getExtraScopes(def, parentInstanceProvider.getScope());
    FunctionScope scope = new FunctionScope(parentInstanceProvider.getScope(), myScopeProvider.forDefinition(def), extraScopes);
    scope.findIntroducedDuplicateInstances(this::warnDuplicate);
    SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider(scope);
    myInstanceProviderProvider.addProvider(def, instanceProvider);

    for (Abstract.Definition definition : def.getGlobalDefinitions()) {
      definition.accept(this, instanceProvider);
    }

    return null;
  }

  @Override
  public Void visitClassField(Abstract.ClassField def, SimpleClassViewInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, SimpleClassViewInstanceProvider parentInstanceScope) {
    myInstanceProviderProvider.addProvider(def, new SimpleClassViewInstanceProvider(new DataScope(parentInstanceScope.getScope(), myScopeProvider.forDefinition(def))));
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, SimpleClassViewInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, SimpleClassViewInstanceProvider parentInstanceScope) {
    try {
      Iterable<Scope> extraScopes = getExtraScopes(def, parentInstanceScope.getScope());
      StaticClassScope staticScope = new StaticClassScope(parentInstanceScope.getScope(), myScopeProvider.forDefinition(def), extraScopes);
      staticScope.findIntroducedDuplicateInstances(this::warnDuplicate);
      SimpleClassViewInstanceProvider instanceProvider = new SimpleClassViewInstanceProvider(staticScope);
      myInstanceProviderProvider.addProvider(def, instanceProvider);

      for (Abstract.Definition definition : def.getGlobalDefinitions()) {
        definition.accept(this, instanceProvider);
      }

      for (Abstract.Definition definition : def.getInstanceDefinitions()) {
        definition.accept(this, instanceProvider);
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, SimpleClassViewInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, SimpleClassViewInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, SimpleClassViewInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, SimpleClassViewInstanceProvider parentInstanceScope) {
    myInstanceProviderProvider.addProvider(def, parentInstanceScope);
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
