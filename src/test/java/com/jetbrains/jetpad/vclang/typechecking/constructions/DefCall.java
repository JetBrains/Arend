package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
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
  private void test(Expression expected, ChildGroup result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) getDefinition(result, "test")).getBody()).getExpression());
  }

  private void testFI(Expression expected, ChildGroup result) {
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) getDefinition(result, "Test.test")).getBody()).getExpression());
  }

  private void testType(Expression expected, ChildGroup result) {
    assertEquals(expected, ((FunctionDefinition) getDefinition(result, "test")).getResultType());
    assertEquals(expected, ((LeafElimTree) ((FunctionDefinition) getDefinition(result, "test")).getBody()).getExpression().getType());
  }

  private DependentLink getThis(ChildGroup result) {
    FunctionDefinition function = (FunctionDefinition) getDefinition(result, "test");
    return function.getParameters();
  }

  private Expression getThisFI(ChildGroup result) {
    FunctionDefinition function = (FunctionDefinition) getDefinition(result, "Test.test");
    return FieldCall((ClassField) getDefinition(result, "Test.parent"), Sort.PROP, Ref(function.getParameters()));
  }

  private ClassCallExpression makeClassCall(Definition definition, Expression impl) {
    ClassDefinition classDef = (ClassDefinition) definition;
    for (ClassField field : classDef.getFields()) {
      if ("parent".equals(field.getReferable().textRepresentation())) {
        return new ClassCallExpression(classDef, Sort.SET0, Collections.singletonMap(field, impl), classDef.getSort());
      }
    }
    return null;
  }

  @Test
  public void funStatic() {
    ChildGroup result = typeCheckModule(
        "\\func f => 0\n" +
        "\\func test => f");
    test(FunCall((FunctionDefinition) getDefinition(result, "f"), Sort.SET0), result);
  }

  @Test
  public void funDynamic() {
    ChildGroup result = typeCheckClass(
        "\\func f => 0\n" +
        "\\func test => f", "");
    test(FunCall((FunctionDefinition) getDefinition(result, "f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funDynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\func f => 0\n" +
        "\\class Test {\n" +
        "  \\func test => f\n" +
        "}", "");
    testFI(FunCall((FunctionDefinition) getDefinition(result, "f"), Sort.SET0, getThisFI(result)), result);
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
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.f");
    test(FunCall((FunctionDefinition) getDefinition(result, "A.B.f"), Sort.SET0), result);
  }

  @Test
  public void funDynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.f", "");
    test(FunCall((FunctionDefinition) getDefinition(result, "A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func test (e : E) => e.f");
    test(FunCall((FunctionDefinition) getDefinition(result, "E.f"), Sort.SET0, Ref(getThis(result))), result);
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
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func test (e : E) => e.f", "");
    test(FunCall((FunctionDefinition) getDefinition(result, "E.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
  }

  @Test
  public void funFieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.f");
    test(FunCall((FunctionDefinition) getDefinition(result, "E.A.B.f"), Sort.SET0, Ref(getThis(result))), result);
  }

  @Test
  public void funFieldInside2() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) (b : e.A.B) => b.f");
    test(FunCall((FunctionDefinition) getDefinition(result, "E.A.B.f"), Sort.SET0, Ref(getThis(result).getNext())), result);
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
    ChildGroup result = typeCheckModule(
        "\\data D | c\n" +
        "\\func test => c");
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void dataStatic() {
    ChildGroup result = typeCheckModule(
        "\\data D | c\n" +
        "\\func test => D.c");
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data0Static() {
    ChildGroup result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data1Static() {
    ChildGroup result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data2Static() {
    ChildGroup result = typeCheckModule(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void conDynamic() {
    ChildGroup result = typeCheckClass(
        "\\data D | c\n" +
        "\\func test => c", "");
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void conDynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => c\n" +
        "}", "");
    testFI(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void dataDynamic() {
    ChildGroup result = typeCheckClass(
        "\\data D | c\n" +
        "\\func test => D.c", "");
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void dataDynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\data D | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c\n" +
        "}", "");
    testFI(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, Collections.singletonList(getThisFI(result))), result);
  }

  @Test
  public void data0Dynamic() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data0DynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => (D 0 (\\lam _ => 1)).c\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data1Dynamic() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => (D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data1DynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => (D 0).c {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
  }

  @Test
  public void data2Dynamic() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\func test => D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "c"), getDefinition(result, "D.c"));
  }

  @Test
  public void data2DynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "\\class Test {\n" +
        "  \\func test => D.c {0} {\\lam _ => 1}\n" +
        "}", "");
    List<Expression> dataTypeArgs = Arrays.asList(getThisFI(result), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    testFI(ConCall((Constructor) getDefinition(result, "c"), Sort.SET0, dataTypeArgs), result);
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
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.c");
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void dataStaticInside() {
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c");
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, Collections.emptyList()), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data0StaticInside() {
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data1StaticInside() {
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data2StaticInside() {
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void conDynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.c", "");
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void dataDynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c", "");
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data0DynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data1DynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => (A.B.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void data2DynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "A.B.c"), getDefinition(result, "A.B.D.c"));
  }

  @Test
  public void conFieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.c");
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void dataFieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c");
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data0FieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data1FieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data2FieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
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
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.c", "");
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void dataFieldDynamic() {
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c", "");
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result).getNext()))), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data0FieldDynamic() {
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0 (\\lam _ => 1)).c", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data1FieldDynamic() {
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => (e.D 0).c {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void data2FieldDynamic() {
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "}\n" +
        "\\func test (e : E) => e.D.c {0} {\\lam _ => 1}", "");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result).getNext()), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.c"), getDefinition(result, "E.D.c"));
  }

  @Test
  public void conFieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.c");
    test(ConCall((Constructor) getDefinition(result, "E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "E.A.B.c"), getDefinition(result, "E.A.B.D.c"));
  }

  @Test
  public void dataFieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c");
    test(ConCall((Constructor) getDefinition(result, "E.A.B.c"), Sort.SET0, Collections.singletonList(Ref(getThis(result)))), result);
    assertEquals(getDefinition(result, "E.A.B.c"), getDefinition(result, "E.A.B.D.c"));
  }

  @Test
  public void data0FieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => (e.A.B.D 0 (\\lam _ => 1)).c");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.A.B.c"), getDefinition(result, "E.A.B.D.c"));
  }

  @Test
  public void data1FieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => (e.A.B.D 0).c {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.A.B.c"), getDefinition(result, "E.A.B.D.c"));
  }

  @Test
  public void data2FieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\data D (x : Nat) (y : Nat -> Nat) | c\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.D.c {0} {\\lam _ => 1}");
    List<Expression> dataTypeArgs = Arrays.asList(Ref(getThis(result)), Zero(), Lam(singleParam(null, Nat()), Suc(Zero())));
    test(ConCall((Constructor) getDefinition(result, "E.A.B.c"), Sort.SET0, dataTypeArgs), result);
    testType(DataCall((DataDefinition) getDefinition(result, "E.A.B.D"), Sort.SET0, dataTypeArgs), result);
    assertEquals(getDefinition(result, "E.A.B.c"), getDefinition(result, "E.A.B.D.c"));
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
    ChildGroup result = typeCheckModule(
        "\\class C\n" +
        "\\func test => C");
    test(new ClassCallExpression((ClassDefinition) getDefinition(result, "C"), Sort.SET0), result);
  }

  @Test
  public void classDynamic() {
    ChildGroup result = typeCheckClass(
        "\\class C\n" +
        "\\func test => C", "");
    test(makeClassCall(getDefinition(result, "C"), Ref(getThis(result))), result);
  }

  @Test
  public void classDynamicFromInside() {
    ChildGroup result = typeCheckClass(
        "\\class C\n" +
        "\\class Test {\n" +
        "  \\func test => C\n" +
        "}", "");
    testFI(makeClassCall(getDefinition(result, "C"), getThisFI(result)), result);
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
    ChildGroup result = typeCheckModule(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.C");
    test(new ClassCallExpression((ClassDefinition) getDefinition(result, "A.B.C"), Sort.SET0), result);
  }

  @Test
  public void classDynamicInside() {
    ChildGroup result = typeCheckClass(
        "\\class A \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}\n" +
        "\\func test => A.B.C", "");
    test(makeClassCall(getDefinition(result, "A.B.C"), Ref(getThis(result))), result);
  }

  @Test
  public void classFieldStatic() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\func test (e : E) => e.C");
    test(makeClassCall(getDefinition(result, "E.C"), Ref(getThis(result))), result);
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
    ChildGroup result = typeCheckClass(
        "\\class E {\n" +
        "  \\class C\n" +
        "}\n" +
        "\\func test (e : E) => e.C", "");
    test(makeClassCall(getDefinition(result, "E.C"), Ref(getThis(result).getNext())), result);
  }

  @Test
  public void classFieldInside() {
    ChildGroup result = typeCheckModule(
        "\\class E {\n" +
        "  \\class A \\where {\n" +
        "    \\class B \\where {\n" +
        "      \\class C\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func test (e : E) => e.A.B.C");
    test(makeClassCall(getDefinition(result, "E.A.B.C"), Ref(getThis(result))), result);
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
    Concrete.FunctionDefinition lastDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getReferable()).getDefinition();
    ((Concrete.ReferenceExpression) ((Concrete.AppExpression) ((Concrete.TermFunctionBody) lastDef.getBody()).getTerm()).getArgument().getExpression()).setReferent(Prelude.PROP_TRUNC.getConstructor("inP").getReferable());
    typeCheckModule(cd);
  }
}
