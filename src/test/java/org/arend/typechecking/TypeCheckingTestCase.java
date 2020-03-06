package org.arend.typechecking;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.type.Type;
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
import org.arend.typechecking.doubleChecker.CoreDefinitionChecker;
import org.arend.typechecking.doubleChecker.CoreException;
import org.arend.typechecking.doubleChecker.CoreExpressionChecker;
import org.arend.typechecking.doubleChecker.CoreModuleChecker;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.arend.typechecking.order.listener.TypecheckingOrderingListener;
import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Arend;
import org.arend.util.ArendExpr;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class TypeCheckingTestCase extends NameResolverTestCase {
  protected final LocalErrorReporter localErrorReporter = new TestLocalErrorReporter(errorReporter);
  protected ChildGroup lastGroup;

  TypecheckingResult typeCheckExpr(Map<Referable, Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor visitor = new CheckTypeVisitor(typecheckerState, context, localErrorReporter, null);
    TypecheckingResult result = visitor.finalCheckExpr(expression, expectedType, false);
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }

    boolean ok = true;
    if (result != null && errors == 0) {
      CoreExpressionChecker checker = new CoreExpressionChecker(new HashSet<>(context.values()), DummyEquations.getInstance(), expression);
      try {
        result.type.accept(checker, Type.OMEGA);
        checker.check(expectedType, result.expression.accept(checker, result.type), result.expression);
      } catch (CoreException e) {
        localErrorReporter.report(e.error);
        ok = false;
      }
    }

    assertThat(errorList, containsErrors(errors));
    assertTrue(ok);
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

  protected TypecheckingResult typeCheckExpr(List<Binding> context, @ArendExpr String text, Expression expectedType) {
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
    Definition definition = typecheckerState.getTypechecked(reference);
    boolean ok = errors != 0 || new CoreDefinitionChecker(errorReporter).check(definition);
    assertThat(errorList, containsErrors(errors));
    assertTrue(ok);
    return definition;
  }

  protected Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  protected Definition typeCheckDef(@Arend  String text) {
    return typeCheckDef(text, 0);
  }


  private void typeCheckModule(Group group, int errors) {
    assertTrue(new TypecheckingOrderingListener(libraryManager.getInstanceProviderSet(), typecheckerState, ConcreteReferableProvider.INSTANCE, IdReferableConverter.INSTANCE, localErrorReporter, PositionComparator.INSTANCE).typecheckModules(Collections.singletonList(group), null));
    boolean ok = errors != 0 || new CoreModuleChecker(errorReporter, typecheckerState).checkGroup(group);
    assertThat(errorList, containsErrors(errors));
    assertTrue(ok);
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

  protected ChildGroup typeCheckModule(@Arend String text) {
    return typeCheckModule(text, 0);
  }

  protected ChildGroup typeCheckClass(String instance, String global, int errors) {
    lastGroup = resolveNamesDefGroup("\\class Test {\n" + instance + (global.isEmpty() ? "" : "\n} \\where {\n" + global) + "\n}");
    typeCheckModule(lastGroup, errors);
    return lastGroup;
  }

  protected ChildGroup typeCheckClass(@Arend String instance, String global) {
    return typeCheckClass(instance, global, 0);
  }
}
