package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefCall {
    private void test(Expression expected, NamespaceMember member) {
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getDefinition("test")).getElimTree()).getExpression());
  }

  private void testFI(Expression expected, NamespaceMember member) {
    assertEquals(expected, ((LeafElimTreeNode) ((FunctionDefinition) member.namespace.getChild("Test").getDefinition("test")).getElimTree()).getExpression());
  }

  private void testType(Expression expected, NamespaceMember member) {
    assertEquals(expected, ((FunctionDefinition) member.namespace.getDefinition("test")).getResultType());
  }

  private DependentLink getThis(NamespaceMember member) {
    FunctionDefinition function = (FunctionDefinition) member.namespace.getDefinition("test");
    return function.getParameters();
  }

  private Expression getThisFI(NamespaceMember member) {
    FunctionDefinition function = (FunctionDefinition) member.namespace.getChild("Test").getDefinition("test");
    return Apps(FieldCall(((ClassDefinition) member.namespace.getDefinition("Test")).getParentField()), Reference(function.getParameters()));
  }

  @Test
  public void funStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\function f => 0\n" +
        "\\static \\function test => f");
    test(FunCall((FunctionDefinition) member.namespace.getDefinition("f")), member);
  }

  @Test
  public void funDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\function f => 0\n" +
        "\\function test => f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getDefinition("f")), Reference(getThis(member))), member);
  }

  @Test
  public void funDynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\function f => 0\n" +
        "\\class Test {\n" +
        "  \\function test => f\n" +
        "}");
    testFI(Apps(FunCall((FunctionDefinition) member.namespace.getDefinition("f")), getThisFI(member)), member);
  }

  @Test
  public void funDynamicError() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\static \\function test => f", 1);
  }

  @Test
  public void funStaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.f");
    test(FunCall((FunctionDefinition) member.namespace.getChild("A").getChild("B").getDefinition("f")), member);
  }

  @Test
  public void funDynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getChild("A").getChild("B").getDefinition("f")), Reference(getThis(member))), member);
  }

  @Test
  public void funFieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getChild("E").getDefinition("f")), Reference(getThis(member))), member);
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
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function test (e : E) => e.f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getChild("E").getDefinition("f")), Reference(getThis(member).getNext())), member);
  }

  @Test
  public void funFieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("f")), Reference(getThis(member))), member);
  }

  @Test
  public void funFieldInside2() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) (b : e.A.B) => b.f");
    test(Apps(FunCall((FunctionDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("f")), Reference(getThis(member).getNext())), member);
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
    NamespaceMember member = typeCheckClass(
        "\\static \\data D | c\n" +
        "\\static \\function test => c");
    test(ConCall((Constructor) member.namespace.getDefinition("c")), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void dataStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D | c\n" +
        "\\static \\function test => D.c");
    test(ConCall((Constructor) member.namespace.getDefinition("c")), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data0Static() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => (D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getDefinition("c"), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data1Static() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => (D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getDefinition("c"), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data2Static() {
    NamespaceMember member = typeCheckClass(
        "\\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\static \\function test => D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getDefinition("c")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void conDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => c");
    test(ConCall((Constructor) member.namespace.getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void conDynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => c\n" +
        "}");
    testFI(ConCall((Constructor) member.namespace.getDefinition("c"), getThisFI(member)), member);
  }

  @Test
  public void dataDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\data D | c\n" +
        "\\function test => D.c");
    test(ConCall((Constructor) member.namespace.getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void dataDynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c\n" +
        "}");
    testFI(ConCall((Constructor) member.namespace.getDefinition("c"), getThisFI(member)), member);
  }

  @Test
  public void data0Dynamic() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getDefinition("c"), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data0DynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0 (\\lam _ => 1)).c\n" +
        "}");
    testFI(ConCall((Constructor) member.namespace.getDefinition("c"), getThisFI(member), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
  }

  @Test
  public void data1Dynamic() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => (D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getDefinition("c"), Reference(getThis(member)), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data1DynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => (D 0).c {\\lam _ => 1}\n" +
        "}");
    testFI(Apps(ConCall((Constructor) member.namespace.getDefinition("c"), getThisFI(member), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
  }

  @Test
  public void data2Dynamic() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\function test => D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getDefinition("c"), Reference(getThis(member))), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getDefinition("c"), member.namespace.getChild("D").getDefinition("c"));
  }

  @Test
  public void data2DynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\function test => D.c {0} {\\lam _ => 1}\n" +
        "}");
    testFI(Apps(ConCall((Constructor) member.namespace.getDefinition("c"), getThisFI(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
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
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c")), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void dataStaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.D.c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c")), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data0StaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => (A.B.D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data1StaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => (A.B.D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data2StaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void conDynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void dataDynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data0DynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data1DynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => (A.B.D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member)), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data2DynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void conFieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void dataFieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.D.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data0FieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data1FieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member)), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data2FieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member))), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
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
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member).getNext())), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void dataFieldDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member).getNext())), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data0FieldDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data1FieldDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => (e.D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member).getNext()), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void data2FieldDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\function test (e : E) => e.D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getDefinition("c"), Reference(getThis(member).getNext())), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getDefinition("D")), Reference(getThis(member).getNext()), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getDefinition("c"), member.namespace.getChild("E").getChild("D").getDefinition("c"));
  }

  @Test
  public void conFieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("E").getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void dataFieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c");
    test(ConCall((Constructor) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), member);
    assertEquals(member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("E").getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data0FieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.A.B.D 0 (\\lam _ => 1)).c");
    test(ConCall((Constructor) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("E").getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data1FieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => (e.A.B.D 0).c {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member)), Zero()), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("E").getChild("A").getChild("B").getChild("D").getDefinition("c"));
  }

  @Test
  public void data2FieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.D.c {0} {\\lam _ => 1}");
    test(Apps(ConCall((Constructor) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), Reference(getThis(member))), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    testType(Apps(DataCall((DataDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("D")), Reference(getThis(member)), Zero(), Lam(param(Nat()), Suc(Zero()))), member);
    assertEquals(member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("c"), member.namespace.getChild("E").getChild("A").getChild("B").getChild("D").getDefinition("c"));
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
    NamespaceMember member = typeCheckClass(
        "\\static \\class C {}\n" +
        "\\static \\function test => C");
    test(ClassCall((ClassDefinition) member.namespace.getDefinition("C")), member);
  }

  @Test
  public void classDynamic() {
    NamespaceMember member = typeCheckClass(
        "\\class C {}\n" +
        "\\function test => C");
    test(ClassCall((ClassDefinition) member.namespace.getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(Reference(getThis(member))), member);
  }

  @Test
  public void classDynamicFromInside() {
    NamespaceMember member = typeCheckClass(
        "\\class C {}\n" +
        "\\class Test {\n" +
        "  \\function test => C\n" +
        "}");
    testFI(ClassCall((ClassDefinition) member.namespace.getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(getThisFI(member)), member);
  }

  @Test
  public void classDynamicError() {
    typeCheckClass(
        "\\class C {}\n" +
        "\\static \\function test => C", 1);
  }

  @Test
  public void classStaticInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {}\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test => A.B.C");
    test(ClassCall((ClassDefinition) member.namespace.getChild("A").getChild("B").getDefinition("C")), member);
  }

  @Test
  public void classDynamicInside() {
    NamespaceMember member = typeCheckClass(
        "\\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {}\n" +
        "  }\n" +
        "}\n" +
        "\\function test => A.B.C");
    test(ClassCall((ClassDefinition) member.namespace.getChild("A").getChild("B").getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(Reference(getThis(member))), member);
  }

  @Test
  public void classFieldStatic() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class C {}\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.C");
    test(ClassCall((ClassDefinition) member.namespace.getChild("E").getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(Reference(getThis(member))), member);
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
    NamespaceMember member = typeCheckClass(
        "\\class E {\n" +
        "  \\class C {}\n" +
        "}\n" +
        "\\function test (e : E) => e.C");
    test(ClassCall((ClassDefinition) member.namespace.getChild("E").getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(Reference(getThis(member).getNext())), member);
  }

  @Test
  public void classFieldInside() {
    NamespaceMember member = typeCheckClass(
        "\\static \\class E {\n" +
        "  \\class A {\n" +
        "    \\static \\class B {\n" +
        "      \\static \\class C {}\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function test (e : E) => e.A.B.C");
    test(ClassCall((ClassDefinition) member.namespace.getChild("E").getChild("A").getChild("B").getDefinition("C"), new HashMap<ClassField, ClassCallExpression.ImplementStatement>()).applyThis(Reference(getThis(member))), member);
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

  @Test
  public void resolvedConstructorTest() {
      Concrete.ClassDefinition cd = resolveNamesClass("test",
          "\\static \\function isequiv {A B : \\Type0} (f : A -> B) => 0\n" +
          "\\static \\function inP-isequiv (P : \\Prop) => isequiv (TrP P).inP");
      Concrete.DefineStatement lastDef = (Concrete.DefineStatement) cd.getStatements().get(((ArrayList) cd.getStatements()).size() - 1);
      ((Concrete.DefCallExpression) ((Concrete.AppExpression) ((Concrete.FunctionDefinition) lastDef.getDefinition()).getTerm()).getArgument().getExpression()).setResolvedDefinition(Prelude.PROP_TRUNC.getConstructor("inP"));
      typeCheckClass(cd, 0);
  }
}
