package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.type.TypeMax;
import com.jetbrains.jetpad.vclang.core.sort.LevelArguments;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ClassesTest extends TypeCheckingTestCase {
  @Test
  public void dynamicStaticCallError() {
    typeCheckClass(
        "\\class A \\where {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicStaticCallError2() {
    typeCheckClass(
        "\\class A \\where {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function g (a : A) => A.f\n" +
        "\\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicInnerFunctionCall() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "  \\function g => B.f\n" +
        "}");
  }

  @Test
  public void staticInnerFunctionCall() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\function g => B.f\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void staticFromDynamicCall() {
    typeCheckClass(
        "\\function h : Nat => f",
        "\\function f => 0");
  }

  @Test
  public void staticFromDynamicCallInside() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B {\n" +
        "    \\function h : Nat => f\n" +
        "  } \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicFromAbstractCall() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\field h : f = 0\n", "", 1);
  }

  @Test
  public void dynamicFromDynamicCall() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\function h (_ : f = 0) => 0", "");
  }

  @Test
  public void dynamicConstructorFromDynamicCall() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\data D | con\n" +
        "  \\function x (_ : con = con) => 0\n" +
        "}\n" +
        "\\function test (a : A) => a.x\n");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall111() {
    typeCheckClass(
        "\\class A {\n" +
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
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall101() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B \\where {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall001() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall110() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall010() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void dynamicDoubleInnerFunctionCall100() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall000() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void recordTest() {
    typeCheckClass(
        "\\class B {\n" +
        "  \\field f : Nat -> \\Type0\n" +
        "  \\field g : f 0\n" +
        "}\n" +
        "\\function f (b : B) : b.f 0 => b.g");
  }

  @Test
  public void innerRecordTest() {
    typeCheckClass(
        "\\class B {\n" +
        "  \\field f : Nat -> \\Type0\n" +
        "  \\class A {\n" +
        "    \\field g : f 0\n" +
        "  }\n" +
        "}\n" +
        "\\function f (b : B) (a : b.A) : b.f 0 => a.g");
  }

  @Test
  public void constructorTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => a.con1 (path (\\lam _ => a.x))\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => a.con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\function f' (a : A) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))\n" +
        "\\function g' (a : A) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorThisTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\function f : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
        "  \\function g : D x (\\lam y => y) => con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\function f' (a : A) => a.f\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\function g' (a : A) => a.g");
  }

  @Test
  public void constructorWithParamsThisTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\function f : D x (\\lam y => y) => (D x (\\lam y => y)).con1 (path (\\lam _ => x))\n" +
        "  \\function g => (D x (\\lam y => y)).con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\function f' (a : A) => a.f\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\function g' (a : A) => a.g");
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass(
        "\\field (+) : Nat -> Nat -> Nat\n" +
        "\\class A {\n" +
        "  \\field x : Nat\n" +
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
        "  | suc n => a.con2 (g a n) (path (\\lam _ => n + a.x))", "");
  }

  @Test
  public void fieldCallTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field x : \\Type0\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "  \\field y : a.x\n" +
        "}");
    ClassField xField = (ClassField) result.getDefinition("A.x");
    ClassField aField = (ClassField) result.getDefinition("B.a");
    ClassField yField = (ClassField) result.getDefinition("B.y");
    assertEquals(FieldCall(xField, FieldCall(aField, Reference(yField.getThisParameter()))), yField.getBaseType(LevelArguments.ZERO));
  }

  @Test
  public void funCallsTest() {
    TypeCheckClassResult result = typeCheckClass(
        "\\function (+) (x y : Nat) => x\n" +
        "\\class A {\n" +
        "  \\function q => p\n" +
        "  \\class C {\n" +
        "    \\function k => h + (p + q)" +
        "  } \\where {\n" +
        "    \\function h => p + q" +
        "  }\n" +
        "} \\where {\n" +
        "  \\function p => 0\n" +
        "  \\class B {\n" +
        "    \\function g => f + p\n" +
        "  } \\where {\n" +
        "    \\function f : Nat => p\n" +
        "  }\n" +
        "}");
    FunctionDefinition plus = (FunctionDefinition) result.getDefinition("+");

    ClassDefinition aClass = (ClassDefinition) result.getDefinition("A");
    assertTrue(aClass.getFieldSet().getFields().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) result.getDefinition("A.p");
    assertEquals(Nat(), pFun.getTypeWithParams(new ArrayList<DependentLink>(), LevelArguments.ZERO));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, Zero()), pFun.getElimTree());
    FunctionDefinition qFun = (FunctionDefinition) result.getDefinition("A.q");
    List<DependentLink> qParams = new ArrayList<>();
    TypeMax qType = qFun.getTypeWithParams(qParams, LevelArguments.ZERO);
    assertEquals(Pi(ClassCall(aClass), Nat()), qType.fromPiParameters(qParams));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(pFun, LevelArguments.ZERO)), qFun.getElimTree());

    ClassDefinition bClass = (ClassDefinition) result.getDefinition("A.B");
    assertTrue(bClass.getFieldSet().getFields().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) result.getDefinition("A.B.f");
    assertEquals(Nat(), fFun.getTypeWithParams(new ArrayList<DependentLink>(), LevelArguments.ZERO));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(pFun, LevelArguments.ZERO)), fFun.getElimTree());
    FunctionDefinition gFun = (FunctionDefinition) result.getDefinition("A.B.g");
    List<DependentLink> gParams = new ArrayList<>();
    TypeMax gType = gFun.getTypeWithParams(gParams, LevelArguments.ZERO);
    assertEquals(Pi(ClassCall(bClass), Nat()), gType.fromPiParameters(gParams));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(plus, LevelArguments.ZERO, FunCall(fFun, LevelArguments.ZERO), FunCall(pFun, LevelArguments.ZERO))), gFun.getElimTree());

    ClassDefinition cClass = (ClassDefinition) result.getDefinition("A.C");
    assertEquals(1, cClass.getFieldSet().getFields().size());
    ClassField cParent = cClass.getEnclosingThisField();
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) result.getDefinition("A.C.h");
    List<DependentLink> hParams = new ArrayList<>();
    TypeMax hType = hFun.getTypeWithParams(hParams, LevelArguments.ZERO);
    assertEquals(Pi(ClassCall(aClass), Nat()), hType.fromPiParameters(hParams));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(plus, LevelArguments.ZERO, FunCall(pFun, LevelArguments.ZERO), FunCall(qFun, LevelArguments.ZERO, Reference(hFun.getParameters())))), hFun.getElimTree());
    FunctionDefinition kFun = (FunctionDefinition) result.getDefinition("A.C.k");
    List<DependentLink> kParams = new ArrayList<>();
    TypeMax kType = kFun.getTypeWithParams(kParams, LevelArguments.ZERO);
    assertEquals(Pi(ClassCall(cClass), Nat()), kType.fromPiParameters(kParams));
    Expression aRef = FieldCall(cParent, Reference(kFun.getParameters()));
    assertEquals(leaf(Abstract.Definition.Arrow.RIGHT, FunCall(plus, LevelArguments.ZERO, FunCall(hFun, LevelArguments.ZERO, aRef), FunCall(plus, LevelArguments.ZERO, FunCall(pFun, LevelArguments.ZERO), FunCall(qFun, LevelArguments.ZERO, aRef)))), kFun.getElimTree());
  }

  @Test
  public void fieldCallInClass() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "  \\field y : a.x = a.x\n" +
        "}");
  }

  @Test
  public void fieldCallInClass2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "  \\field y : a.x = a.x\n" +
        "  \\field z : y = y\n" +
        "}");
  }

  @Test
  public void fieldCallInClass3() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "  \\field y : path (\\lam _ => a.x) = path (\\lam _ => a.x)\n" +
        "}");
  }

  @Test
  public void fieldCallWithArg0() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\function y (b : B) => b.a.x");
  }

  @Test
  public void fieldCallWithArg1() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\function y (b : Nat -> B) => (b 0).a.x");
  }

  @Test
  public void fieldCallWithArg2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : Nat -> A\n" +
        "}\n" +
        "\\function y (b : B) => (b.a 1).x");
  }

  @Test
  public void fieldCallWithArg3() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  \\field a : Nat -> A\n" +
        "}\n" +
        "\\function y (b : Nat -> B) => ((b 0).a 1).x");
  }

  @Test
  public void staticDynamicCall() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) => a.B.f");
  }

  @Test
  public void staticDynamicCall2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\function y (a : A) => a.B.C.f");
  }

  @Test
  public void staticDynamicCall3() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\function y (a : A) : \\Set0 => a.B.C");
  }

  @Test
  public void staticDynamicCall31() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) : \\Prop => a.B.C");
  }

  @Test
  public void staticDynamicCall4() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\function y (a : A) : \\Set0 => a.B");
  }

  @Test
  public void staticDynamicCall5() {
    typeCheckClass(
        "\\class D {\n" +
        "  \\class E \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function d : D => \\new D\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void staticDynamicCall6() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function d => e\n" +
        "        \\where \\function e => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) => a.B.C.d.e");
  }

  @Test
  public void staticDynamicCall7() {
    typeCheckClass(
        "\\class D {\n" +
        "  \\class E \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function d : D => \\new D\n" +
        "        \\where\n" +
        "          \\function E => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void classPolyParams() {
    typeCheckClass(
        "\\class A {\n" +
        "   \\field X : \\0-Type \\lp\n" +
        "   \\function f (x : \\0-Type \\lp) => x\n" +
        "   \\data D (x : \\0-Type \\lp)\n" +
        "   \\class B {\n" +
        "       \\field Y : X -> \\0-Type \\lp\n" +
        "       \\function g : \\0-Type \\lp => X\n" +
        "   }\n" +
        "}");
  }
}
