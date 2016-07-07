package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.AppExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.TypeCheckClassResult;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.*;

public class ClassesTest {
  @Test
  public void dynamicStaticCallError() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\function f => 0\n" +
        "}\n" +
        "\\static \\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicStaticCallError2() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\function f => 0\n" +
        "}\n" +
        "\\static \\function g (a : A) => A.f\n" +
        "\\static \\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicInnerFunctionCall() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "  \\function g => B.f\n" +
        "}");
  }

  @Test
  public void staticInnerFunctionCall() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "  \\function g => B.f\n" +
        "}");
  }

  @Test
  public void staticFromDynamicCall() {
    typeCheckClass(
        "\\static \\function f => 0\n" +
        "\\function h : Nat => f");
  }

  @Test
  public void staticFromDynamicCallInside() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "    \\function h : Nat => f\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicFromAbstractCall() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\abstract h : f = 0", 1);
  }

  @Test
  public void dynamicFromDynamicCall() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\function h (_ : f = 0) => 0");
  }

  @Test
  public void dynamicConstructorFromDynamicCall() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\data D | con\n" +
        "  \\function x (_ : con = con) => 0\n" +
        "}\n" +
        "\\static \\function test (a : A) => a.x\n");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall111() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall011() {
    resolveNamesClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\static \\class B {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall101() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall001() {
    resolveNamesClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall110() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\static \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall010() {
    resolveNamesClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\static \\class B {\n" +
        "    \\class C {\n" +
        "      \\static \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall100() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall000() {
    resolveNamesClass(
        "\\static \\class A {\n" +
        "  \\function g => 0\n" +
        "  \\static \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void recordTest() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract f : Nat -> \\Type0\n" +
        "  \\abstract g : f 0\n" +
        "}\n" +
        "\\static \\function f (b : B) : b.f 0 => b.g");
  }

  @Test
  public void innerRecordTest() {
    typeCheckClass(
        "\\static \\class B {\n" +
        "  \\abstract f : Nat -> \\Type0\n" +
        "  \\class A {\n" +
        "    \\abstract g : f 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function f (b : B) (a : b.A) : b.f 0 => a.g");
  }

  @Test
  public void constructorTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => a.con1 (path (\\lam _ => a.x))\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => a.con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\static \\function f' (a : A) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))\n" +
        "\\static \\function g' (a : A) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorThisTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\function f : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
        "  \\function g : D x (\\lam y => y) => con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\static \\function f' (a : A) => a.f\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\static \\function g' (a : A) => a.g");
  }

  @Test
  public void constructorWithParamsThisTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\function f : D x (\\lam y => y) => (D x (\\lam y => y)).con1 (path (\\lam _ => x))\n" +
        "  \\function g => (D x (\\lam y => y)).con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\static \\function f' (a : A) => a.f\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\static \\function g' (a : A) => a.g");
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass(
        "\\abstract (+) (x y : Nat) : Nat\n" +
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat -> Nat)\n" +
        "    | D zero f => con1 (f x x = f x x)\n" +
        "    | D (suc n) f => con2 (D n f) (f n x = f n x)\n" +
        "  \\function f (n : Nat) : D n (+) <= \\elim n\n" +
        "    | zero => con1 (path (\\lam _ => x + x))\n" +
        "    | suc n => con2 (f n) (path (\\lam _ => n + x))\n" +
        "}\n" +
        "\\function f (a : A) (n : Nat) : a.D n (+) => a.f n\n" +
        "\\function f' (a : A) (n : Nat) => a.f\n" +
        "\\function g (a : A) (n : Nat) : a.D n (+) <= \\elim n\n" +
        "  | zero => a.con1 (path (\\lam _ => a.x + a.x))\n" +
        "  | suc n => a.con2 (g a n) (path (\\lam _ => n + a.x))");
  }

  @Test
  public void fieldCallTest() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : \\Type0\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "  \\abstract y : a.x\n" +
        "}");
    ClassDefinition aClass = (ClassDefinition) result.getDefinition("A");
    ClassDefinition bClass = (ClassDefinition) result.getDefinition("B");
    ClassField xField = aClass.getField("x");
    ClassField aField = bClass.getField("a");
    ClassField yField = bClass.getField("y");
    Expression type = yField.getBaseType();
    AppExpression appType = type.toApp();
    assertNotNull(appType);
    assertEquals(FieldCall(xField), appType.getFunction());
    assertEquals(1, appType.getArguments().size());
    AppExpression appArg = appType.getArguments().get(0).toApp();
    assertNotNull(appArg);
    assertEquals(FieldCall(aField), appArg.getFunction());
    assertEquals(1, appArg.getArguments().size());
    assertEquals(Reference(yField.getThisParameter()), appArg.getArguments().get(0));
  }

  @Test
  public void funCallsTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\static \\function (+) (x y : Nat) => x\n" +
        "\\static \\class A {\n" +
        "  \\static \\function p => 0\n" +
        "  \\function q => p\n" +
        "  \\static \\class B {\n" +
        "    \\static \\function f : Nat => p\n" +
        "    \\function g => f + p\n" +
        "  }\n" +
        "  \\class C {\n" +
        "    \\static \\function h => p + q" +
        "    \\function k => h + (p + q)" +
        "  }\n" +
        "}");
    FunctionDefinition plus = (FunctionDefinition) result.getDefinition("+");

    ClassDefinition aClass = (ClassDefinition) result.getDefinition("A");
    assertTrue(aClass.getFields().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) result.getDefinition("A.p");
    assertEquals(Nat(), pFun.getType());
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, Zero()), pFun.getElimTree());
    FunctionDefinition qFun = (FunctionDefinition) result.getDefinition("A.q");
    assertEquals(Pi(ClassCall(aClass), Nat()), qFun.getType());
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(pFun)), qFun.getElimTree());

    ClassDefinition bClass = (ClassDefinition) result.getDefinition("A.B");
    assertTrue(bClass.getFields().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) result.getDefinition("A.B.f");
    assertEquals(Nat(), fFun.getType());
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(pFun)), fFun.getElimTree());
    FunctionDefinition gFun = (FunctionDefinition) result.getDefinition("A.B.g");
    assertEquals(Pi(ClassCall(bClass), Nat()), gFun.getType());
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, Apps(FunCall(plus), FunCall(fFun), FunCall(pFun))), gFun.getElimTree());

    ClassDefinition cClass = (ClassDefinition) result.getDefinition("A.C");
    assertEquals(1, cClass.getFields().size());
    ClassField cParent = cClass.getParentField();
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) result.getDefinition("A.C.h");
    assertEquals(Pi(ClassCall(aClass), Nat()), hFun.getType());
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, Apps(FunCall(plus), FunCall(pFun), Apps(FunCall(qFun), Reference(hFun.getParameters())))), hFun.getElimTree());
    FunctionDefinition kFun = (FunctionDefinition) result.getDefinition("A.C.k");
    assertEquals(Pi(ClassCall(cClass), Nat()), kFun.getType());
    Expression aRef = Apps(FieldCall(cParent), Reference(kFun.getParameters()));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, Apps(FunCall(plus), Apps(FunCall(hFun), aRef), Apps(FunCall(plus), FunCall(pFun), Apps(FunCall(qFun), aRef)))), kFun.getElimTree());
  }

  @Test
  public void fieldCallInClass() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "  \\abstract y : a.x = a.x\n" +
        "}");
  }

  @Test
  public void fieldCallInClass2() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "  \\abstract y : a.x = a.x\n" +
        "  \\abstract z : y = y\n" +
        "}");
  }

  @Test
  public void fieldCallInClass3() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "  \\abstract y : path (\\lam _ => a.x) = path (\\lam _ => a.x)\n" +
        "}");
  }

  @Test
  public void fieldCallWithArg0() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\function y (b : B) => b.a.x");
  }

  @Test
  public void fieldCallWithArg1() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\function y (b : Nat -> B) => (b 0).a.x");
  }

  @Test
  public void fieldCallWithArg2() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : Nat -> A\n" +
        "}\n" +
        "\\static \\function y (b : B) => (b.a 1).x");
  }

  @Test
  public void fieldCallWithArg3() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\class B {\n" +
        "  \\abstract a : Nat -> A\n" +
        "}\n" +
        "\\static \\function y (b : Nat -> B) => ((b 0).a 1).x");
  }

  @Test
  public void staticDynamicCall() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function y (a : A) => a.B.f");
  }

  @Test
  public void staticDynamicCall2() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\function y (a : A) => a.B.C.f");
  }

  @Test
  public void staticDynamicCall3() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\function y (a : A) : \\Set0 => a.B.C");
  }

  @Test
  public void staticDynamicCall4() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\static \\function y (a : A) : \\Set0 => a.B");
  }

  @Test
  public void staticDynamicCall5() {
    typeCheckClass(
        "\\static \\class D {\n" +
        "  \\class E {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function d : D => \\new D\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void staticDynamicCall6() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function d => e\n" +
        "        \\where \\static \\function e => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function y (a : A) => a.B.C.d.e");
  }

  @Test
  public void staticDynamicCall7() {
    typeCheckClass(
        "\\static \\class D {\n" +
        "  \\class E {\n" +
        "    \\static \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\class A {\n" +
        "  \\class B {\n" +
        "    \\static \\class C {\n" +
        "      \\static \\function d : D => \\new D\n" +
        "        \\where\n" +
        "          \\static \\function E => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
  }
}
