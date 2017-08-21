package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefCall extends TypeCheckingTestCase {
  private void test(Expression expected, TypeCheckClassResult result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("test")).getBody()).getExpression());
  }

  private void testFI(Expression expected, TypeCheckClassResult result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("Test.test")).getBody()).getExpression());
  }

  private void testType(Expression expected, TypeCheckClassResult result) {
    assertEquals(expected, ((FunctionDefinition) result.getDefinition("test")).getResultType());
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("test")).getBody()).getExpression().getType());
  }

  private DependentLink getThis(TypeCheckClassResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("test");
    return function.getParameters();
  }

  private Expression getThisFI(TypeCheckClassResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("Test.test");
    return FieldCall(((ClassDefinition) result.getDefinition("Test")).getEnclosingThisField(), Ref(function.getParameters()));
  }

  @Test
  public void funStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function f => 0\n" +
        "\\function test => f");
    test(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0), result);
  }

  @Test
  public void funDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function f => 0\n" +
        "\\function test => f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function f => 0\n" +
        "\\class Test {\n" +
        "  \\function test => f\n" +
        "}", "");
    testFI(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0, getThisFI(result)), result);
  }

  @Test
  public void funDynamicError() {
    resolveNamesClass(
        "\\class Test {\n" +
        "  \\function f => 0\n" +
        "} \\where {\n" +
        "  \\function test => f\n" +
        "}", 1);
  }

  @Test
  public void funStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.f");
    test(FunCall((FunctionDefinition) result.getDefinition("A.B.f"), Sort.SET0), result);
  }

  @Test
  public void funDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function test (e : E) => e.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldError() {
    typeCheckClass(
        "\\class E \\where {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function test (e : E) => e.f", 1);
  }

  @Test
  public void funFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function test (e : E) => e.f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("E.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldInside2() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) (b : e.A.B) => b.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.A.B.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInsideError() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void funFieldInsideError2() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void conStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => c");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => D.c");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => c", "");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => c\n" +
        "}", "");
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void dataDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => D.c", "");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c\n" +
        "}", "");
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void data0Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0 (\\lam _ => 1)).c\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data1Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0).c {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data2Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c {0} {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void conDynamicError() {
    resolveNamesClass(
        "\\class Test {\n" +
        "  \\data D | c\n" +
        "} \\where {\n" +
        "  \\function test => c\n" +
        "}", 1);
  }

  @Test
  public void dataDynamicError() {
    resolveNamesClass(
        "\\class Test {\n" +
        "  \\data D | c\n" +
        "} \\where {" +
        "  \\function test => D.c\n" +
        "}", 1);
  }

  @Test
  public void conStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.c");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.c", "");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c", "");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.c");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldError() {
    typeCheckClass(
        "\\class E \\where {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.c", 1);
  }

  @Test
  public void dataFieldError() {
    typeCheckClass(
        "\\class E \\where {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c", 1);
  }

  @Test
  public void conFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.c", "");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c", "");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.c");
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void dataFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.D.c");
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data0FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => (e.A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data1FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => (e.A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data2FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void conFieldInsideError() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void conFieldInsideError2() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError2() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void classStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class C\n" +
        "\\function test => C");
    test(new ClassCallExpression((ClassDefinition) result.getDefinition("C"), Sort.SET0), result);
  }

  @Test
  public void classDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class C\n" +
        "\\function test => C", "");
    test(result.getDefinition("C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class C\n" +
        "\\class Test {\n" +
        "  \\function test => C\n" +
        "}", "");
    testFI(result.getDefinition("C").getDefCall(Sort.SET0, getThisFI(result), Collections.emptyList()), result);
  }

  @Test
  public void classDynamicError() {
    resolveNamesClass(
        "\\class Test {\n" +
        "  \\class C\n" +
        "} \\where {\n" +
        "  \\function test => C\n" +
        "}", 1);
  }

  @Test
  public void classStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.C");
    test(new ClassCallExpression((ClassDefinition) result.getDefinition("A.B.C"), Sort.SET0), result);
  }

  @Test
  public void classDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.C", "");
    test(result.getDefinition("A.B.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\function test (e : E) => e.C");
    test(result.getDefinition("E.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldError() {
    typeCheckClass(
        "\\class E \\where {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\function test (e : E) => e.C", 1);
  }

  @Test
  public void classFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\function test (e : E) => e.C", "");
    test(result.getDefinition("E.C").getDefCall(Sort.SET0, Ref(getThis(result).getNext()), Collections.emptyList()), result);
  }

  @Test
  public void classFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.C");
    test(result.getDefinition("E.A.B.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldInsideError() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.C", 1);
  }

  @Test
  public void classFieldInsideError2() {
    typeCheckClass(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function test (e : E) => e.A.B.C", 1);
  }

  @Test
  public void local() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("x", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "x", null);
    assertNotNull(result);
    assertEquals(Ref(context.get(0)), result.expression);
  }

  @Test
  public void nonStaticTestError() {
    resolveNamesClass("\\class A { \\function x => 0 } \\function y => A.x", 1);
  }

  @Test
  public void staticTestError() {
    typeCheckClass("\\class A \\where { \\function x => 0 } \\function y (a : A) => a.x", 1);
  }

  @Test
  public void innerNonStaticTestError() {
    typeCheckClass("\\class A { \\class B { \\function x => 0 } } \\function y (a : A) => a.B.x", 1);
  }

  @Test
  public void innerNonStaticTestAcc() {
    typeCheckClass("\\class A { \\class B { \\function x => 0 } } \\function y (a : A) (b : a.B) => b.x");
  }

  @Test
  public void innerNonStaticTest() {
    typeCheckClass("\\class A { \\class B \\where { \\function x => 0 } } \\function y (a : A) => a.B.x");
  }

  @Test
  public void staticTest() {
    typeCheckClass("\\class A \\where { \\function x => 0 } \\function y : Nat => A.x");
  }

  @Test
  public void resolvedConstructorTest() {
    Concrete.ClassDefinition<Position> cd = resolveNamesClass(
        "\\function isequiv {A B : \\Type0} (f : A -> B) => 0\n" +
        "\\function inP-isequiv (P : \\Prop) => isequiv (TrP P).inP");
    Concrete.FunctionDefinition lastDef = (Concrete.FunctionDefinition) ((Concrete.DefineStatement) cd.getGlobalStatements().get(1)).getDefinition();
    ((Concrete.ReferenceExpression) ((Concrete.AppExpression) ((Abstract.TermFunctionBody) lastDef.getBody()).getTerm()).getArgument().getExpression()).setReferent(Prelude.PROP_TRUNC.getConstructor("inP").getConcreteDefinition());
    typeCheckClass(cd);
  }
}
