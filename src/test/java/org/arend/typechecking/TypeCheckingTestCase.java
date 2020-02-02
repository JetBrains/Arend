package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.frontend.ConcreteReferableProvider;
import org.arend.frontend.PositionComparator;
import org.arend.frontend.reference.ConcreteLocatedReferable;
import org.arend.naming.NameResolverTestCase;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.reference.converter.IdReferableConverter;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.ConcreteExpressionFactory;
import org.arend.term.group.ChildGroup;
import org.arend.term.group.Group;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TypeCheckingTestCase extends NameResolverTestCase {
  protected final LocalErrorReporter localErrorReporter = new TestLocalErrorReporter(errorReporter);
  protected ChildGroup lastGroup;

  TypecheckingResult typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(typecheckerState, context, localErrorReporter, null);
    TypecheckingResult result = visitor.finalCheckExpr(expression, expectedType, false);
    assertThat(errorList, containsErrors(errors));
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }
    return result;
  }

  TypecheckingResult typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new HashMap<>(), expression, expectedType, errors);
  }

  protected TypecheckingResult typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(context, expression, expectedType, 0);
  }

  protected TypecheckingResult typeCheckExpr(Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(new HashMap<>(), expression, expectedType, 0);
  }


  protected TypecheckingResult typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    Map<Referable, Binding> mapContext = new HashMap<>();
    for (Binding binding : context) {
      mapContext.put(ConcreteExpressionFactory.ref(binding.getName()), binding);
    }
    return typeCheckExpr(mapContext, resolveNamesExpr(mapContext, text), expectedType, errors);
  }

  protected TypecheckingResult typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  protected TypecheckingResult typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, text, expectedType, 0);
  }

  protected TypecheckingResult typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }

  protected Definition typeCheckDef(ConcreteLocatedReferable reference) {
    return typeCheckDef(reference, 0);
  }

  private Definition typeCheckDef(ConcreteLocatedReferable reference, int errors) {
    new TypecheckingOrderingListener(libraryManager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, errorReporter, PositionComparator.INSTANCE).typecheckDefinitions(Collections.singletonList((Concrete.Definition) reference.getDefinition()), null);
    assertThat(errorList, containsErrors(errors));
    return typecheckerState.getTypechecked(reference);
  }

  protected Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  protected Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }


  private void typeCheckModule(Group group, int errors) {
    assertTrue(new TypecheckingOrderingListener(libraryManager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, localErrorReporter, PositionComparator.INSTANCE).typecheckModules(Collections.singletonList(group), null));
    assertThat(errorList, containsErrors(errors));
  }


  public TCReferable get(String path) {
    return get(lastGroup.getGroupScope(), path);
  }

  public Definition getDefinition(ChildGroup group, String path) {
    TCReferable ref = get(group.getGroupScope(), path);
    return ref != null ? typecheckerState.getTypechecked(ref) : null;
  }

  public Definition getDefinition(String path) {
    TCReferable ref = get(path);
    return ref != null ? typecheckerState.getTypechecked(ref) : null;
  }

  protected void typeCheckModule(ChildGroup group) {
    lastGroup = group;
    typeCheckModule(group, 0);
  }

  protected ChildGroup typeCheckModule(String text, int errors) {
    lastGroup = resolveNamesModule(text);
    typeCheckModule(lastGroup, errors);
    return lastGroup;
  }

  protected ChildGroup typeCheckModule(String text) {
    return typeCheckModule(text, 0);
  }

  protected ChildGroup typeCheckClass(String instance, String global, int errors) {
    lastGroup = resolveNamesDefGroup("\\class Test {\n" + instance + (global.isEmpty() ? "" : "\n} \\where {\n" + global) + "\n}");
    typeCheckModule(lastGroup, errors);
    return lastGroup;
  }

  protected ChildGroup typeCheckClass(String instance, String global) {
    return typeCheckClass(instance, global, 0);
  }
}
