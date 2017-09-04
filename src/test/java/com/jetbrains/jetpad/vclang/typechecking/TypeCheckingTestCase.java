package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.frontend.ReferenceTypecheckableProvider;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.GlobalReference;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.DependencyListener;
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

  protected final LocalErrorReporter<Position> localErrorReporter = new TestLocalErrorReporter(errorReporter);

  public TypeCheckingTestCase() {
    typeCheckPrelude();
  }

  private void typeCheckPrelude() {
    loadPrelude();

    if (PRELUDE_TYPECHECKER_STATE == null) {
      ListErrorReporter<Position> internalErrorReporter = new ListErrorReporter<>();
      PRELUDE_TYPECHECKER_STATE = new SimpleTypecheckerState();
      new Typechecking<>(PRELUDE_TYPECHECKER_STATE, staticNsProvider, dynamicNsProvider, ReferenceTypecheckableProvider.INSTANCE, internalErrorReporter, new Prelude.UpdatePreludeReporter<>(PRELUDE_TYPECHECKER_STATE), new DependencyListener<Position>() {}).typecheckModules(Collections.singletonList(prelude));
      //assertThat(internalErrorReporter.getErrorList(), is(empty()));  // does not type-check by design
    }

    state = new SimpleTypecheckerState(PRELUDE_TYPECHECKER_STATE);
  }


  CheckTypeVisitor.Result typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression<Position> expression, Expression expectedType, int errors) {
    CheckTypeVisitor<Position> visitor = new CheckTypeVisitor<>(state, staticNsProvider, dynamicNsProvider, context, localErrorReporter, null);
    visitor.getFreeBindings().addAll(context.values());
    CheckTypeVisitor.Result result = visitor.finalCheckExpr(expression, expectedType);
    assertThat(errorList, containsErrors(errors));
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }
    return result;
  }

  CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression<Position> expression, Expression expectedType, int errors) {
    return typeCheckExpr(new HashMap<>(), expression, expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression<Position> expression, Expression expectedType) {
    return typeCheckExpr(context, expression, expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression<Position> expression, Expression expectedType) {
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


  private Definition typeCheckDef(GlobalReference reference, int errors) {
    new Typechecking<>(state, staticNsProvider, dynamicNsProvider, ReferenceTypecheckableProvider.INSTANCE, errorReporter, new TypecheckedReporter.Dummy<>(), new DependencyListener<Position>() {}).typecheckDefinitions(Collections.singletonList((Concrete.Definition<Position>) reference.getDefinition()));
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
    new Typechecking<>(state, staticNsProvider, dynamicNsProvider, ReferenceTypecheckableProvider.INSTANCE, localErrorReporter, new TypecheckedReporter.Dummy<>(), new DependencyListener<Position>() {}).typecheckModules(Collections.singletonList(group));
    assertThat(errorList, containsErrors(errors));
    return state;
  }


  protected class TypeCheckModuleResult {
    public final TypecheckerState typecheckerState;
    public final GlobalReferable referable;

    public TypeCheckModuleResult(TypecheckerState typecheckerState, GlobalReferable referable) {
      this.typecheckerState = typecheckerState;
      this.referable = referable;
    }

    public Definition getDefinition(String path) {
      GlobalReferable ref = get(referable, path);
      return ref != null ? typecheckerState.getTypechecked(ref) : null;
    }

    public Definition getDefinition() {
      return typecheckerState.getTypechecked(referable);
    }
  }

  protected TypeCheckModuleResult typeCheckModule(Group group) {
    TypecheckerState state = typeCheckModule(group, 0);
    return new TypeCheckModuleResult(state, group.getReferable());
  }

  protected TypeCheckModuleResult typeCheckModule(String text, int errors) {
    Group module = resolveNamesModule(text);
    TypecheckerState state = typeCheckModule(module, errors);
    return new TypeCheckModuleResult(state, module.getReferable());
  }

  protected TypeCheckModuleResult typeCheckModule(String text) {
    return typeCheckModule(text, 0);
  }

  protected TypeCheckModuleResult typeCheckModule(String instance, String global, int errors) {
    Group group = resolveNamesModule("\\class Test {\n" + instance + (global.isEmpty() ? "" : "\n} \\where {\n" + global) + "\n}");
    return new TypeCheckModuleResult(typeCheckModule(group, errors), group.getReferable());
  }

  protected TypeCheckModuleResult typeCheckModule(String instance, String global) {
    return typeCheckModule(instance, global, 0);
  }
}
