package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor.Mode.WHNF;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefCall {
  private void test(Expression expected, TypeCheckingTestCase.TypeCheckClassResult result) {
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("test")).getElimTree()).getExpression());
  }

  private void testFI(Expression expected, TypeCheckingTestCase.TypeCheckClassResult result) {
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("Test.test")).getElimTree()).getExpression());
  }

  private void testType(Expression expected, TypeCheckingTestCase.TypeCheckClassResult result) {
    assertEquals(expected, ((FunctionDefinition) result.getDefinition("test")).getResultType());
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) result.getDefinition("test")).getElimTree()).getExpression().getType());
  }

  private DependentLink getThis(TypeCheckingTestCase.TypeCheckClassResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("test");
    return function.getParameters();
  }

  private Expression getThisFI(TypeCheckingTestCase.TypeCheckClassResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("Test.test");
    return Apps(((ClassDefinition) result.getDefinition("Test")).getParentField().getDefCall(), Reference(function.getParameters()));
  }

  @Test
  public void funStatic() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\static \\function f => 0\n" +
        "\\static \\function test => f");
    test(result.getDefinition("f").getDefCall(), result);
  }

  @Test
  public void funDynamic() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\function f => 0\n" +
        "\\function test => f");
    test(Apps(result.getDefinition("f").getDefCall(), Reference(getThis(result))), result);
  }

  @Test
  public void funDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function f => 0\n" +
        "\\class Test {\n" +
        "  \\function test => f\n" +
        "}");
    testFI(Apps(result.getDefinition("f").getDefCall(), getThisFI(result)), result);
  }

  @Test
  public void funDynamicError() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\static \\function test => f", 1);
  }

  @Test
  public void funStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.f");
    test(result.getDefinition("A.B.f").getDefCall(), result);
  }

  @Test
  public void funDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.f");
    test(Apps(result.getDefinition("A.B.f").getDefCall(), Reference(getThis(result))), result);
  }

  @Test
  public void funFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.f");
    test(Apps(result.getDefinition("E.f").getDefCall(), Reference(getThis(result))), result);
  }

  @Test
  public void funFieldError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\static \\function f => 0\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.f", 1);
  }

  @Test
  public void funFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function test (e : E) => e.f");
    test(Apps(result.getDefinition("E.f").getDefCall(), Reference(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.f");
    test(Apps(result.getDefinition("E.A.B.f").getDefCall(), Reference(getThis(result))), result);
  }

  @Test
  public void funFieldInside2() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) (b : e.A.B) => b.f");
    test(Apps(result.getDefinition("E.A.B.f").getDefCall(), Reference(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInsideError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void funFieldInsideError2() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\class B {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void conStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D | c\n" +
        "\\static \\function test => c");
    test(result.getDefinition("c").getDefCall(), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D | c\n" +
        "\\static \\function test => D.c");
    test(result.getDefinition("c").getDefCall(), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => (D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("c").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => (D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("c").getDefCall(), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2Static() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => D.c {0} {\\lam _ => 1}");
    test(Apps(result.getDefinition("c").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => c");
    test(Apps(result.getDefinition("c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => c\n" +
        "}");
    testFI(Apps(result.getDefinition("c").getDefCall(), getThisFI(result)).normalize(WHNF), result);
  }

  @Test
  public void dataDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => D.c");
    test(Apps(result.getDefinition("c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c\n" +
        "}");
    testFI(Apps(result.getDefinition("c").getDefCall(), getThisFI(result)).normalize(WHNF), result);
  }

  @Test
  public void data0Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("c").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0 (\\lam _ => 1)).c\n" +
        "}");
    testFI(Apps(result.getDefinition("c").getDefCall(), getThisFI(result), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
  }

  @Test
  public void data1Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("c").getDefCall(), Reference(getThis(result)), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0).c {\\lam _ => 1}\n" +
        "}");
    testFI(Apps(Apps(result.getDefinition("c").getDefCall(), getThisFI(result), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
  }

  @Test
  public void data2Dynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => D.c {0} {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("c").getDefCall(), Reference(getThis(result))).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2DynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c {0} {\\lam _ => 1}\n" +
        "}");
    testFI(Apps(Apps(result.getDefinition("c").getDefCall(), getThisFI(result)).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
  }

  @Test
  public void conDynamicError() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\static \\function test => c", 1);
  }

  @Test
  public void dataDynamicError() {
    typeCheckClass(
        "\\data D | c\n" +
        "\\static \\function test => D.c", 1);
  }

  @Test
  public void conStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.c");
    test(result.getDefinition("A.B.c").getDefCall(), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.D.c");
    test(result.getDefinition("A.B.c").getDefCall(), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => (A.B.D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("A.B.c").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => (A.B.D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("A.B.c").getDefCall(), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2StaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(result.getDefinition("A.B.c").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.c");
    test(Apps(result.getDefinition("A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c");
    test(Apps(result.getDefinition("A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("A.B.c").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("A.B.c").getDefCall(), Reference(getThis(result)), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2DynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.D.c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result)), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.D.c {0} {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\static \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.c", 1);
  }

  @Test
  public void dataFieldError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\static \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.D.c", 1);
  }

  @Test
  public void conFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result).getNext())).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result).getNext())).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result).getNext()), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c {0} {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.c").getDefCall(), Reference(getThis(result).getNext())).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.D").getDefCall(), Reference(getThis(result).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.c");
    test(Apps(result.getDefinition("E.A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void dataFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c");
    test(Apps(result.getDefinition("E.A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data0FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.A.B.D 0 (\\lam _ => 1)).c");
    test(Apps(result.getDefinition("E.A.B.c").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))).normalize(WHNF), result);
    testType(Apps(result.getDefinition("E.A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data1FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.A.B.D 0).c {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.A.B.c").getDefCall(), Reference(getThis(result)), Zero()).normalize(WHNF), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data2FieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(Apps(result.getDefinition("E.A.B.c").getDefCall(), Reference(getThis(result))).normalize(WHNF), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    testType(Apps(result.getDefinition("E.A.B.D").getDefCall(), Reference(getThis(result)), Zero(), Lam(param(Nat()), Suc(Zero()))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void conFieldInsideError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void conFieldInsideError2() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError2() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void classStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class C {}\n" +
        "\\static \\function test => C");
    test(result.getDefinition("C").getDefCall(), result);
  }

  @Test
  public void classDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class C {}\n" +
        "\\function test => C");
    test(result.getDefinition("C").getDefCall().applyThis(Reference(getThis(result))), result);
  }

  @Test
  public void classDynamicFromInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class C {}\n" +
        "\\class Test {\n" +
        "  \\function test => C\n" +
        "}");
    testFI(result.getDefinition("C").getDefCall().applyThis(getThisFI(result)), result);
  }

  @Test
  public void classDynamicError() {
    typeCheckClass(
        "\\class C {}\n" +
        "\\static \\function test => C", 1);
  }

  @Test
  public void classStaticInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {}\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.C");
    test(result.getDefinition("A.B.C").getDefCall(), result);
  }

  @Test
  public void classDynamicInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {}\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.C");
    test(result.getDefinition("A.B.C").getDefCall().applyThis(Reference(getThis(result))), result);
  }

  @Test
  public void classFieldStatic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class C {}\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.C");
    test(result.getDefinition("E.C").getDefCall().applyThis(Reference(getThis(result))), result);
  }

  @Test
  public void classFieldError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\static \\class C {}\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.C", 1);
  }

  @Test
  public void classFieldDynamic() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class C {}\n" +
        "}\n" +
        "\\function test (e : E) => e.C");
    test(result.getDefinition("E.C").getDefCall().applyThis(Reference(getThis(result).getNext())), result);
  }

  @Test
  public void classFieldInside() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\class C {}\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.C");
    test(result.getDefinition("E.A.B.C").getDefCall().applyThis(Reference(getThis(result))), result);
  }

  @Test
  public void classFieldInsideError() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\class C {}\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.C", 1);
  }

  @Test
  public void classFieldInsideError2() {
    typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\class B {\n" +
        "      \\static \\class C {}\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.C", 1);
  }

  @Test
  public void local() {
    List<Binding> context = new ArrayList<>(1);
    context.add(new TypedBinding("x", Nat()));
    CheckTypeVisitor.Result result = typeCheckExpr(context, "x", null);
    assertNotNull(result);
    assertEquals(Reference(context.get(0)), result.expression);
  }

  @Test
  public void nonStaticTestError() {
    typeCheckClass("\\static \\class A { \\function x => 0 } \\static \\function y => A.x", 1);
  }

  @Test
  public void staticTestError() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\static \\function y (a : A) => a.x", 1);
  }

  @Test
  public void innerNonStaticTestError() {
    typeCheckClass("\\static \\class A { \\class B { \\function x => 0 } } \\static \\function y (a : A) => a.B.x", 1);
  }

  @Test
  public void innerNonStaticTestAcc() {
    typeCheckClass("\\static \\class A { \\class B { \\function x => 0 } } \\static \\function y (a : A) (b : a.B) => b.x");
  }

  @Test
  public void innerNonStaticTest() {
    typeCheckClass("\\static \\class A { \\class B { \\static \\function x => 0 } } \\static \\function y (a : A) => a.B.x");
  }

  @Test
  public void staticTest() {
    typeCheckClass("\\static \\class A { \\static \\function x => 0 } \\static \\function y : Nat => A.x");
  }
}
