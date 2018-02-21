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
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import java.util.*;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefCall extends TypeCheckingTestCase {
  private void test(Expression expected, TypeCheckModuleResult result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("test")).getBody()).getExpression());
  }

  private void testFI(Expression expected, TypeCheckModuleResult result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("Test.test")).getBody()).getExpression());
  }

  private void testType(Expression expected, TypeCheckModuleResult result) {
    assertEquals(expected, ((FunctionDefinition) result.getDefinition("test")).getResultType());
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) result.getDefinition("test")).getBody()).getExpression().getType());
  }

  private DependentLink getThis(TypeCheckModuleResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("test");
    return function.getParameters();
  }

  private Expression getThisFI(TypeCheckModuleResult result) {
    FunctionDefinition function = (FunctionDefinition) result.getDefinition("Test.test");
    return FieldCall(((ClassDefinition) result.getDefinition("Test")).getEnclosingThisField(), Ref(function.getParameters()));
  }

  @Test
  public void funStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\func f => 0\n" +
        "\\func test => f");
    test(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0), result);
  }

  @Test
  public void funDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\func f => 0\n" +
        "\\func test => f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funDynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\func f => 0\n" +
        "\\class Test {\n" +
        "  \\func test => f\n" +
        "}", "");
    testFI(FunCall((FunctionDefinition) result.getDefinition("f"), Sort.SET0, getThisFI(result)), result);
  }

  @Test
  public void funDynamicError() {
    resolveNamesModule(
        "\\class Test {\n" +
        "  \\func f => 0\n" +
        "} \\where {\n" +
        "  \\func test => f\n" +
        "}", 1);
  }

  @Test
  public void funStaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.f");
    test(FunCall((FunctionDefinition) result.getDefinition("A.B.f"), Sort.SET0), result);
  }

  @Test
  public void funDynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func test (e : E) => e.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldError() {
    typeCheckModule(
        "\\class E \\where {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func test (e : E) => e.f", 1);
  }

  @Test
  public void funFieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func test (e : E) => e.f", "");
    test(FunCall((FunctionDefinition) result.getDefinition("E.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldInside2() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) (b : e.A.B) => b.f");
    test(FunCall((FunctionDefinition) result.getDefinition("E.A.B.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInsideError() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void funFieldInsideError2() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.f", 1);
  }

  @Test
  public void conStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\data D | c\n" +
        "\\func test => c");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\data D | c\n" +
        "\\func test => D.c");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0Static() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1Static() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2Static() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\func test => c", "");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void conDynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => c\n" +
        "}", "");
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void dataDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\func test => D.c", "");
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void dataDynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c\n" +
        "}", "");
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void data0Dynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data0DynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => (D 0 (\\lam _ => 1)).c\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data1Dynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data1DynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => (D 0).c {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data2Dynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("c"), result.getDefinition("D.c"));
  }

  @Test
  public void data2DynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c {0} {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) result.getDefinition("c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void conDynamicError() {
    resolveNamesModule(
        "\\class Test {\n" +
        "  \\data D | c\n" +
        "} \\where {\n" +
        "  \\func test => c\n" +
        "}", 1);
  }

  @Test
  public void dataDynamicError() {
    resolveNamesModule(
        "\\class Test {\n" +
        "  \\data D | c\n" +
        "} \\where {" +
        "  \\func test => D.c\n" +
        "}", 1);
  }

  @Test
  public void conStaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.c");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataStaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0StaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1StaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2StaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conDynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.c", "");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void dataDynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c", "");
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data0DynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data1DynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void data2DynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("A.B.c"), result.getDefinition("A.B.D.c"));
  }

  @Test
  public void conFieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.c");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldError() {
    typeCheckModule(
        "\\class E \\where {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.c", 1);
  }

  @Test
  public void dataFieldError() {
    typeCheckModule(
        "\\class E \\where {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c", 1);
  }

  @Test
  public void conFieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.c", "");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void dataFieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c", "");
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data0FieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data1FieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void data2FieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.c"), result.getDefinition("E.D.c"));
  }

  @Test
  public void conFieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.c");
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void dataFieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c");
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data0FieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => (e.A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data1FieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => (e.A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void data2FieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) result.getDefinition("E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) result.getDefinition("E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(result.getDefinition("E.A.B.c"), result.getDefinition("E.A.B.D.c"));
  }

  @Test
  public void conFieldInsideError() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void conFieldInsideError2() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.c", 1);
  }

  @Test
  public void dataFieldInsideError2() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c", 1);
  }

  @Test
  public void classStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class C\n" +
        "\\func test => C");
    test(new ClassCallExpression((ClassDefinition) result.getDefinition("C"), Sort.SET0), result);
  }

  @Test
  public void classDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class C\n" +
        "\\func test => C", "");
    test(result.getDefinition("C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classDynamicFromInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class C\n" +
        "\\class Test {\n" +
        "  \\func test => C\n" +
        "}", "");
    testFI(result.getDefinition("C").getDefCall(Sort.SET0, getThisFI(result), Collections.emptyList()), result);
  }

  @Test
  public void classDynamicError() {
    resolveNamesModule(
        "\\class Test {\n" +
        "  \\class C\n" +
        "} \\where {\n" +
        "  \\func test => C\n" +
        "}", 1);
  }

  @Test
  public void classStaticInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.C");
    test(new ClassCallExpression((ClassDefinition) result.getDefinition("A.B.C"), Sort.SET0), result);
  }

  @Test
  public void classDynamicInside() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.C", "");
    test(result.getDefinition("A.B.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldStatic() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\func test (e : E) => e.C");
    test(result.getDefinition("E.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldError() {
    typeCheckModule(
        "\\class E \\where {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\func test (e : E) => e.C", 1);
  }

  @Test
  public void classFieldDynamic() {
    TypeCheckModuleResult result = typeCheckClass(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\func test (e : E) => e.C", "");
    test(result.getDefinition("E.C").getDefCall(Sort.SET0, Ref(getThis(result).getNext()), Collections.emptyList()), result);
  }

  @Test
  public void classFieldInside() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.C");
    test(result.getDefinition("E.A.B.C").getDefCall(Sort.SET0, Ref(getThis(result)), Collections.emptyList()), result);
  }

  @Test
  public void classFieldInsideError() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.C", 1);
  }

  @Test
  public void classFieldInsideError2() {
    typeCheckModule(
        "\\class E {\n" +
        "  \\class A {\n" +
        "    \\class B \\where {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.C", 1);
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
    resolveNamesModule("\\class A { \\func x => 0 } \\func y => A.x", 1);
  }

  @Test
  public void staticTestError() {
    typeCheckModule("\\class A \\where { \\func x => 0 } \\func y (a : A) => a.x", 1);
  }

  @Test
  public void innerNonStaticTestError() {
    typeCheckModule("\\class A { \\class B { \\func x => 0 } } \\func y (a : A) => a.B.x", 1);
  }

  @Test
  public void innerNonStaticTestAcc() {
    typeCheckModule("\\class A { \\class B { \\func x => 0 } } \\func y (a : A) (b : a.B) => b.x");
  }

  @Test
  public void innerNonStaticTest() {
    typeCheckModule("\\class A { \\class B \\where { \\func x => 0 } } \\func y (a : A) => a.B.x");
  }

  @Test
  public void staticTest() {
    typeCheckModule("\\class A \\where { \\func x => 0 } \\func y : Nat => A.x");
  }

  @Test
  public void resolvedConstructorTest() {
    ChildGroup cd = resolveNamesModule(
        "\\func isequiv {A B : \\Type0} (f : A -> B) => 0\n" +
        "\\func inP-isequiv (P : \\Prop) => isequiv (TrP P).inP");
    Iterator<? extends Group> it = cd.getSubgroups().iterator();
    it.next();
    Concrete.FunctionDefinition lastDef = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    ((Concrete.ReferenceExpression) ((Concrete.AppExpression) ((Concrete.TermFunctionBody) lastDef.getBody()).getTerm()).getArgument().getExpression()).setReferent(Prelude.PROP_TRUNC.getConstructor("inP").getReferable());
    typeCheckModule(cd);
  }
}
