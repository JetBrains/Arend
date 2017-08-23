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
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.ConcreteDefinitionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.InstanceProviderSet;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.provider.SimpleInstanceProvider;
import com.jetbrains.jetpad.vclang.typechecking.typeclass.scope.InstanceNamespaceProvider;

import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefinitionResolveInstanceVisitor<T> implements ConcreteDefinitionVisitor<T, SimpleInstanceProvider, Void> {
  private final InstanceProviderSet<T> myInstanceProviderSet;
  private final InstanceNamespaceProvider<T> myScopeProvider;
  private final Function<Concrete.Definition, Iterable<OpenCommand>> myOpens;
  private final ErrorReporter<T> myErrorReporter;

  public DefinitionResolveInstanceVisitor(InstanceProviderSet<T> instanceProviderSet, InstanceNamespaceProvider<T> scopeProvider, Function<Concrete.Definition, Iterable<OpenCommand>> opens, ErrorReporter<T> errorReporter) {
    myInstanceProviderSet = instanceProviderSet;
    myScopeProvider = scopeProvider;
    myOpens = opens;
    myErrorReporter = errorReporter;
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition<T> def, SimpleInstanceProvider parentInstanceProvider) {
    Iterable<Scope> extraScopes = getExtraScopes(def, parentInstanceProvider.getScope());
    FunctionScope scope = new FunctionScope(parentInstanceProvider.getScope(), myScopeProvider.forDefinition(def), extraScopes);
    scope.findIntroducedDuplicateInstances(this::warnDuplicate);
    SimpleInstanceProvider instanceProvider = new SimpleInstanceProvider(scope);
    myInstanceProviderSet.setProvider(def, instanceProvider);

    for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
      definition.accept(this, instanceProvider);
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition<T> def, SimpleInstanceProvider parentInstanceScope) {
    myInstanceProviderSet.setProvider(def, new SimpleInstanceProvider(new DataScope(parentInstanceScope.getScope(), myScopeProvider.forDefinition(def))));
    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition<T> def, SimpleInstanceProvider parentInstanceScope) {
    try {
      Iterable<Scope> extraScopes = getExtraScopes(def, parentInstanceScope.getScope());
      StaticClassScope staticScope = new StaticClassScope(parentInstanceScope.getScope(), myScopeProvider.forDefinition(def), extraScopes);
      staticScope.findIntroducedDuplicateInstances(this::warnDuplicate);
      SimpleInstanceProvider instanceProvider = new SimpleInstanceProvider(staticScope);
      myInstanceProviderSet.setProvider(def, instanceProvider);

      for (Concrete.Definition<T> definition : def.getGlobalDefinitions()) {
        definition.accept(this, instanceProvider);
      }

      for (Concrete.Definition<T> definition : def.getInstanceDefinitions()) {
        definition.accept(this, instanceProvider);
      }
    } catch (Namespace.InvalidNamespaceException e) {
      myErrorReporter.report(e.toError());
    }

    return null;
  }

  @Override
  public Void visitClassView(Concrete.ClassView def, SimpleInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClassViewField(Concrete.ClassViewField def, SimpleInstanceProvider parentInstanceScope) {
    return null;
  }

  @Override
  public Void visitClassViewInstance(Concrete.ClassViewInstance def, SimpleInstanceProvider parentInstanceScope) {
    myInstanceProviderSet.setProvider(def, parentInstanceScope);
    return null;
  }

  private Iterable<Scope> getExtraScopes(Concrete.Definition def, Scope currentScope) {
    return StreamSupport.stream(myOpens.apply(def).spliterator(), false)
        .flatMap(this::processOpenCommand)
        .collect(Collectors.toList());
  }

  private Stream<Scope> processOpenCommand(OpenCommand cmd) {
    Scope scope = myScopeProvider.forDefinition((Concrete.Definition) cmd.getResolvedClass());
    if (cmd.getNames() != null) {
      scope = new FilteredScope(scope, new HashSet<>(cmd.getNames()), !cmd.isHiding());
    }
    return Stream.of(scope);
  }

  private void warnDuplicate(Concrete.ClassViewInstance instance1, Concrete.ClassViewInstance instance2) {
    myErrorReporter.report(new DuplicateInstanceError(Error.Level.WARNING, instance1, instance2 /* TODO[abstract] */));
  }
}
