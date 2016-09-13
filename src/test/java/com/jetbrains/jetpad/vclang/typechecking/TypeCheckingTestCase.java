package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NamespaceUtil;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.order.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.*;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseClass;
import static com.jetbrains.jetpad.vclang.util.TestUtil.assertErrorListSize;
import static junit.framework.TestCase.assertTrue;

public class TypeCheckingTestCase extends PreludeTest {
  public static TypecheckerState state;
  public List<GeneralError> errorList = new ArrayList<>();
  public LocalErrorReporter errorReporter = new ProxyErrorReporter(null, new ListErrorReporter(errorList));

  @BeforeClass
  public static void typeCheckPrelude() {
    state = new TypecheckerState();
    ListErrorReporter errorReporter = new ListErrorReporter();
    assertTrue(TypecheckingOrdering.typecheck(state, Collections.singletonList(PRELUDE_DEFINITION), errorReporter, true));
  }

  public CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType) {
    return new CheckTypeVisitor.Builder(state, context, errorReporter).build().checkType(expression, expectedType);
  }

  public CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType);
  }

  public CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor.Result result = typeCheckExpr(context, expression, expectedType);
    assertErrorListSize(errorList, errors);
    return result;
  }

  public CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errors);
  }

  public CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, errors);
  }

  public CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  public CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, 0);
  }

  public CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }

  public Definition typeCheckDef(Concrete.Definition definition, int errors) {
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(state, errorReporter);
    Definition result = definition.accept(visitor, null);
    assertErrorListSize(errorList, errors);
    return result;
  }

  public Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  public Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }

  public TypecheckerState typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    TypecheckingOrdering.typecheck(state, classDefinition, errorReporter);
    assertErrorListSize(errorList, errors);
    return state;
  }

  public static class TypeCheckClassResult {
    public final TypecheckerState typecheckerState;
    public final Concrete.ClassDefinition classDefinition;

    public TypeCheckClassResult(TypecheckerState typecheckerState, Concrete.ClassDefinition classDefinition) {
      this.typecheckerState = typecheckerState;
      this.classDefinition = classDefinition;
    }

    public Definition getDefinition(String path) {
      Abstract.Definition ref = NamespaceUtil.get(classDefinition, path);
      return ref != null ? typecheckerState.getTypechecked(ref) : null;
    }
  }

  public TypeCheckClassResult typeCheckClass(String text, int nameErrors, int tcErrors) {
    Concrete.ClassDefinition classDefinition = parseClass("test", text);
    resolveNamesClass(classDefinition, nameErrors);
    TypecheckerState state = typeCheckClass(classDefinition, tcErrors);
    return new TypeCheckClassResult(state, classDefinition);
  }

  public TypeCheckClassResult typeCheckClass(String text, int tcErrors) {
    return typeCheckClass(text, 0, tcErrors);
  }

  public TypeCheckClassResult typeCheckClass(String text) {
    return typeCheckClass(text, 0, 0);
  }
}
