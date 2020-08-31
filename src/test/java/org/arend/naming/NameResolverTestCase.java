package org.arend.naming;

import org.arend.core.context.binding.Binding;
import org.arend.ext.reference.Precedence;
import org.arend.ext.typechecking.MetaDefinition;
import org.arend.ext.typechecking.MetaResolver;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.MetaReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.naming.resolving.visitor.DefinitionResolveNameVisitor;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.*;
import org.arend.prelude.PreludeLibrary;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.arend.typechecking.TestLocalErrorReporter;
import org.arend.typechecking.visitor.SyntacticDesugarVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class NameResolverTestCase extends ParserTestCase {
  protected ChildGroup lastGroup;
  private final Map<String, MetaReferable> metaDefs = new HashMap<>();
  private final Scope metaScope = new Scope() {
    @Override
    public @Nullable Referable resolveName(String name) {
      return metaDefs.get(name);
    }

    @Override
    public @NotNull Collection<? extends Referable> getElements() {
      return metaDefs.values();
    }
  };

  public TCReferable get(String path) {
    return get(lastGroup.getGroupScope(), path);
  }

  public Concrete.ReferableDefinition getConcrete(String path) {
    TCReferable ref = get(path);
    return ref instanceof ConcreteLocatedReferable ? ((ConcreteLocatedReferable) ref).getDefinition() : null;
  }

  protected void addMeta(String name, Precedence prec, MetaDefinition meta) {
    metaDefs.put(name, new MetaReferable(prec, name, MODULE_PATH, "", meta, meta instanceof MetaResolver ? (MetaResolver) meta : null, null));
  }

  private Concrete.Expression resolveNamesExpr(Scope parentScope, List<Referable> context, String text, int errors) {
    Concrete.Expression expression = parseExpr(text);
    assertThat(expression, is(notNullValue()));

    expression = expression
      .accept(new ExpressionResolveNameVisitor(IdReferableConverter.INSTANCE, parentScope, context, new TestLocalErrorReporter(errorReporter), null), null)
      .accept(new SyntacticDesugarVisitor(errorReporter), null);
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
    Scope parentScope = new MergeScope(new SingletonScope(group.getReferable()), metaScope, PreludeLibrary.getPreludeScope());
    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, null, errorReporter).resolveGroupWithTypes(group, CachingScope.make(LexicalScope.insideOf(group, parentScope)));
    assertThat(errorList, containsErrors(errors));
    return group;
  }

  protected ChildGroup resolveNamesDefGroup(String text) {
    lastGroup = resolveNamesDefGroup(text, 0);
    return lastGroup;
  }

  protected ConcreteLocatedReferable resolveNamesDef(String text, int errors) {
    return (ConcreteLocatedReferable) resolveNamesDefGroup(text, errors).getReferable();
  }

  protected ConcreteLocatedReferable resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }


  private void resolveNamesModule(ChildGroup group, int errors) {
    Scope scope = CachingScope.make(new MergeScope(ScopeFactory.forGroup(group, moduleScopeProvider), metaScope));
    new DefinitionResolveNameVisitor(ConcreteReferableProvider.INSTANCE, null, errorReporter).resolveGroupWithTypes(group, scope);
    libraryManager.getInstanceProviderSet().collectInstances(group, CachingScope.make(ScopeFactory.parentScopeForGroup(group, moduleScopeProvider, true)), IdReferableConverter.INSTANCE);
    assertThat(errorList, containsErrors(errors));
  }

  protected ChildGroup resolveNamesModule(String text, int errors) {
    ChildGroup group = parseModule(text);
    resolveNamesModule(group, errors);
    return group;
  }

  protected ChildGroup resolveNamesModule(String text) {
    lastGroup = resolveNamesModule(text, 0);
    return lastGroup;
  }
}
