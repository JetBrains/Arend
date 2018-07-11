package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.reference.TCReferable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.listener.TypecheckingOrderingListener;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

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

  CheckTypeVisitor.Result typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(typecheckerState, context, localErrorReporter, null);
    visitor.getFreeBindings().addAll(context.values());
    CheckTypeVisitor.Result result = visitor.finalCheckExpr(expression, expectedType, false);
    assertThat(errorList, containsErrors(errors));
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }
    return result;
  }

  CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new HashMap<>(), expression, expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(context, expression, expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(new HashMap<>(), expression, expectedType, 0);
  }


  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    Map<Referable, Binding> mapContext = new HashMap<>();
    for (Binding binding : context) {
      mapContext.put(ConcreteExpressionFactory.ref(binding.getName()), binding);
    }
    return typeCheckExpr(mapContext, resolveNamesExpr(mapContext, text), expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, text, expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }


  private Definition typeCheckDef(ConcreteLocatedReferable reference, int errors) {
    new TypecheckingOrderingListener(libraryManager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, errorReporter).typecheckDefinitions(Collections.singletonList((Concrete.Definition) reference.getDefinition()));
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
    assertTrue(new TypecheckingOrderingListener(libraryManager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, localErrorReporter).typecheckModules(Collections.singletonList(group)));
    assertThat(errorList, containsErrors(errors));
  }


  public Definition getDefinition(ChildGroup group, String path) {
    TCReferable ref = get(group.getGroupScope(), path);
    return ref != null ? typecheckerState.getTypechecked(ref) : null;
  }

  public Definition getDefinition(String path) {
    TCReferable ref = get(lastGroup.getGroupScope(), path);
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
