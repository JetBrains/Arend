package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.resolving.visitor.ExpressionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.naming.scope.*;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TestLocalErrorReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class NameResolverTestCase extends ParserTestCase {
  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<Referable> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression.accept(new ExpressionResolveNameVisitor(parentScope, context, new TestLocalErrorReporter(errorReporter)), null);
    assertThat(errorList, containsErrors(errors));
    return expression;
  }

  protected Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(String text, @SuppressWarnings("SameParameterValue") int errors) {
    return resolveNamesExpr(CachingScope.make(ScopeFactory.forGroup(null, moduleScopeProvider)), new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(Scope parentScope, @SuppressWarnings("SameParameterValue") String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(Map<Referable, Binding> context, String text) {
    List<Referable> names = new ArrayList<>(context.keySet());
    return resolveNamesExpr(CachingScope.make(ScopeFactory.forGroup(null, moduleScopeProvider)), names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new HashMap<>(), text);
  }


  ChildGroup resolveNamesDefGroup(String text, int errors) {
    ChildGroup group = parseDef(text);
    Scope preludeScope = moduleScopeProvider.forModule(PreludeStorage.PRELUDE_MODULE_PATH);
    Scope parentScope = new SingletonScope(group.getReferable());
    if (preludeScope != null) {
      parentScope = new MergeScope(parentScope, preludeScope);
    }
    new DefinitionResolveNameVisitor(errorReporter).resolveGroup(group, CachingScope.make(LexicalScope.insideOf(group, parentScope)), ConcreteReferableProvider.INSTANCE);
    assertThat(errorList, containsErrors(errors));
    return group;
  }

  protected ChildGroup resolveNamesDefGroup(String text) {
    return resolveNamesDefGroup(text, 0);
  }

  ConcreteGlobalReferable resolveNamesDef(String text, int errors) {
    return (ConcreteGlobalReferable) resolveNamesDefGroup(text, errors).getReferable();
  }

  protected ConcreteGlobalReferable resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesModule(ChildGroup group, int errors) {
    new DefinitionResolveNameVisitor(errorReporter).resolveGroup(group, CachingScope.make(ScopeFactory.forGroup(group, moduleScopeProvider)), ConcreteReferableProvider.INSTANCE);
    assertThat(errorList, containsErrors(errors));
  }

  // FIXME[tests] should be package-private
  protected ChildGroup resolveNamesModule(String text, int errors) {
    ChildGroup group = parseModule(text);
    resolveNamesModule(group, errors);
    return group;
  }

  protected ChildGroup resolveNamesModule(String text) {
    return resolveNamesModule(text, 0);
  }
}
