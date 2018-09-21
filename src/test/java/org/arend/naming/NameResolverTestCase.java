package org.arend.naming;

import org.arend.core.context.binding.Binding;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.*;
import org.arend.prelude.PreludeLibrary;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TestLocalErrorReporter;

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

    expression = expression.accept(new ExpressionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, parentScope, context, new TestLocalErrorReporter(errorReporter)), null);
    assertThat(errorList, containsErrors(errors));
    return expression;
  }

  protected Concrete.Expression resolveNamesExpr(Scope parentScope, String text, int errors) {
    return resolveNamesExpr(parentScope, new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(String text, @SuppressWarnings("SameParameterValue") int errors) {
    return resolveNamesExpr(PreludeLibrary.getPreludeScope(), new ArrayList<>(), text, errors);
  }

  protected Concrete.Expression resolveNamesExpr(Scope parentScope, @SuppressWarnings("SameParameterValue") String text) {
    return resolveNamesExpr(parentScope, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(Map<Referable, Binding> context, String text) {
    List<Referable> names = new ArrayList<>(context.keySet());
    return resolveNamesExpr(PreludeLibrary.getPreludeScope(), names, text, 0);
  }

  protected Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(new HashMap<>(), text);
  }


  ChildGroup resolveNamesDefGroup(String text, int errors) {
    ChildGroup group = parseDef(text);
    Scope parentScope = new MergeScope(new SingletonScope(group.getReferable()), PreludeLibrary.getPreludeScope());
    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, errorReporter).resolveGroupWithTypes(group, null, CachingScope.make(LexicalScope.insideOf(group, parentScope)));
    assertThat(errorList, containsErrors(errors));
    return group;
  }

  protected ChildGroup resolveNamesDefGroup(String text) {
    return resolveNamesDefGroup(text, 0);
  }

  protected ConcreteLocatedReferable resolveNamesDef(String text, int errors) {
    return (ConcreteLocatedReferable) resolveNamesDefGroup(text, errors).getReferable();
  }

  protected ConcreteLocatedReferable resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesModule(ChildGroup group, int errors) {
    Scope scope = CachingScope.make(ScopeFactory.forGroup(group, libraryManager.getModuleScopeProvider()));
    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, errorReporter).resolveGroupWithTypes(group, null, scope);
    libraryManager.getInstanceProviderSet().collectInstances(group, CachingScope.make(ScopeFactory.parentScopeForGroup(group, libraryManager.getModuleScopeProvider(), true)), ConcreteReferableProvider.INSTANCE, null);
    assertThat(errorList, containsErrors(errors));
  }

  protected ChildGroup resolveNamesModule(String text, int errors) {
    ChildGroup group = parseModule(text);
    resolveNamesModule(group, errors);
    return group;
  }

  protected ChildGroup resolveNamesModule(String text) {
    return resolveNamesModule(text, 0);
  }
}
