package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.ChildGroup;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TypeCheckingTestCase extends NameResolverTestCase {
  @SuppressWarnings("StaticNonFinalField")
  private static SimpleTypecheckerState PRELUDE_TYPECHECKER_STATE = null;

  private TypecheckerState state = new SimpleTypecheckerState();

  protected final LocalErrorReporter localErrorReporter = new TestLocalErrorReporter(errorReporter);

  public TypeCheckingTestCase() {
    typeCheckPrelude();
  }

  private void typeCheckPrelude() {
    loadPrelude();

    if (PRELUDE_TYPECHECKER_STATE == null) {
      PRELUDE_TYPECHECKER_STATE = new SimpleTypecheckerState();
      new Prelude.PreludeTypechecking(PRELUDE_TYPECHECKER_STATE).typecheckModules(Collections.singletonList(prelude));
    }

    state = new SimpleTypecheckerState(PRELUDE_TYPECHECKER_STATE);
  }


  CheckTypeVisitor.Result typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(state, context, localErrorReporter, null);
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


  private Definition typeCheckDef(ConcreteGlobalReferable reference, int errors) {
    new Typechecking(state, ConcreteReferableProvider.INSTANCE, errorReporter).typecheckDefinitions(Collections.singletonList((Concrete.Definition) reference.getDefinition()));
    assertThat(errorList, containsErrors(errors));
    return state.getTypechecked(reference);
  }

  protected Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  protected Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }


  private TypecheckerState typeCheckModule(Group group, int errors) {
    new Typechecking(state, ConcreteReferableProvider.INSTANCE, localErrorReporter).typecheckModules(Collections.singletonList(group));
    assertThat(errorList, containsErrors(errors));
    return state;
  }


  protected class TypeCheckModuleResult {
    public final TypecheckerState typecheckerState;
    public final ChildGroup group;

    public TypeCheckModuleResult(TypecheckerState typecheckerState, ChildGroup group) {
      this.typecheckerState = typecheckerState;
      this.group = group;
    }

    public Definition getDefinition(String path) {
      GlobalReferable ref = get(group.getGroupScope(), path);
      return ref != null ? typecheckerState.getTypechecked(ref) : null;
    }

    public Definition getDefinition() {
      return typecheckerState.getTypechecked(group.getReferable());
    }
  }

  protected TypeCheckModuleResult typeCheckModule(ChildGroup group) {
    TypecheckerState state = typeCheckModule(group, 0);
    return new TypeCheckModuleResult(state, group);
  }

  protected TypeCheckModuleResult typeCheckModule(String text, int errors) {
    ChildGroup module = resolveNamesModule(text);
    TypecheckerState state = typeCheckModule(module, errors);
    return new TypeCheckModuleResult(state, module);
  }

  protected TypeCheckModuleResult typeCheckModule(String text) {
    return typeCheckModule(text, 0);
  }

  protected TypeCheckModuleResult typeCheckClass(String instance, String global, int errors) {
    ChildGroup group = resolveNamesDefGroup("\\class Test {\n" + instance + (global.isEmpty() ? "" : "\n} \\where {\n" + global) + "\n}");
    return new TypeCheckModuleResult(typeCheckModule(group, errors), group);
  }

  protected TypeCheckModuleResult typeCheckClass(String instance, String global) {
    return typeCheckClass(instance, global, 0);
  }
}
