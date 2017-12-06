package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.error;
import static org.junit.Assert.*;

public class ClassesTest extends TypeCheckingTestCase {
  @Test
  public void dynamicStaticCallError() {
    typeCheckModule(
        "\\class A \\where {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicStaticCallError2() {
    typeCheckModule(
        "\\class A \\where {\n" +
        "  \\function f => 0\n" +
        "}\n" +
        "\\function g (a : A) => A.f\n" +
        "\\function h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicInnerFunctionCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "  \\function g => B.f\n" +
        "}");
  }

  @Test
  public void staticInnerFunctionCall() {
    typeCheckModule(
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
    typeCheckModule(
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
        "| h : f = 0\n", "", 1);
  }

  @Test
  public void dynamicFromDynamicCall() {
    typeCheckClass(
        "\\function f => 0\n" +
        "\\function h (_ : f = 0) => 0", "");
  }

  @Test
  public void dynamicConstructorFromDynamicCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\data D | con\n" +
        "  \\function x (_ : con = con) => 0\n" +
        "}\n" +
        "\\function test (a : A) => a.x\n");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall111() {
    typeCheckModule(
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
    resolveNamesModule(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall101() {
    typeCheckModule(
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
    resolveNamesModule(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall110() {
    typeCheckModule(
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
    resolveNamesModule(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall100() {
    typeCheckModule(
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
    resolveNamesModule(
        "\\class A {\n" +
        "  \\function g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void recordTest() {
    typeCheckModule(
        "\\class B {\n" +
        "  | f : Nat -> \\Type0\n" +
        "  | g : f 0\n" +
        "}\n" +
        "\\function h (b : B) : b.f 0 => b.g");
  }

  @Test
  public void innerRecordTest() {
    typeCheckModule(
        "\\class B {\n" +
        "  | f : Nat -> \\Type0\n" +
        "  \\class A {\n" +
        "    | g : f 0\n" +
        "  }\n" +
        "}\n" +
        "\\function h (b : B) (a : b.A) : b.f 0 => a.g");
  }

  @Test
  public void constructorTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => a.con1 (path (\\lam _ => a.x))\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => a.con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\function f (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\function f' (a : A) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\function g (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))\n" +
        "\\function g' (a : A) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorThisTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
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
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
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
        "| + : Nat -> Nat -> Nat\n" +
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat -> Nat) => \\elim n\n" +
        "    | zero => con1 (f x x = f x x)\n" +
        "    | suc n => con2 (D n f) (f n x = f n x)\n" +
        "  \\function f (n : Nat) : D n `+ => \\elim n\n" +
        "    | zero => con1 (path (\\lam _ => x + x))\n" +
        "    | suc n => con2 (f n) (path (\\lam _ => n + x))\n" +
        "}\n" +
        "\\function f (a : A) (n : Nat) : a.D n `+ => a.f n\n" +
        "\\function f' (a : A) (n : Nat) => a.f\n" +
        "\\function g (a : A) (n : Nat) : a.D n `+ => \\elim n\n" +
        "  | zero => a.con1 (path (\\lam _ => a.x + a.x))\n" +
        "  | suc n => a.con2 (g a n) (path (\\lam _ => n + a.x))", "");
  }

  @Test
  public void fieldCallTest() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class A {\n" +
        "  | x : \\Type0\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : a.x\n" +
        "}");
    ClassField xField = (ClassField) result.getDefinition("A.x");
    ClassField aField = (ClassField) result.getDefinition("B.a");
    ClassField yField = (ClassField) result.getDefinition("B.y");
    assertEquals(FieldCall(xField, FieldCall(aField, Ref(yField.getThisParameter()))), yField.getBaseType(Sort.SET0));
  }

  @Test
  public void funCallsTest() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\function + (x y : Nat) => x\n" +
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
    assertTrue(aClass.getFields().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) result.getDefinition("A.p");
    assertEquals(Nat(), pFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), Zero()), pFun.getBody());
    FunctionDefinition qFun = (FunctionDefinition) result.getDefinition("A.q");
    List<DependentLink> qParams = new ArrayList<>();
    Expression qType = qFun.getTypeWithParams(qParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(qType, qParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(aClass)), FunCall(pFun, Sort.SET0)), qFun.getBody());

    ClassDefinition bClass = (ClassDefinition) result.getDefinition("A.B");
    assertTrue(bClass.getFields().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) result.getDefinition("A.B.f");
    assertEquals(Nat(), fFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), FunCall(pFun, Sort.SET0)), fFun.getBody());
    FunctionDefinition gFun = (FunctionDefinition) result.getDefinition("A.B.g");
    List<DependentLink> gParams = new ArrayList<>();
    Expression gType = gFun.getTypeWithParams(gParams, Sort.SET0);
    assertEquals(Pi(ClassCall(bClass), Nat()), fromPiParameters(gType, gParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(bClass)), FunCall(plus, Sort.SET0, FunCall(fFun, Sort.SET0), FunCall(pFun, Sort.SET0))), gFun.getBody());

    ClassDefinition cClass = (ClassDefinition) result.getDefinition("A.C");
    assertEquals(1, cClass.getFields().size());
    ClassField cParent = cClass.getEnclosingThisField();
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) result.getDefinition("A.C.h");
    List<DependentLink> hParams = new ArrayList<>();
    Expression hType = hFun.getTypeWithParams(hParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(hType, hParams));
    DependentLink hFunParam = param("\\this", ClassCall(aClass));
    assertEquals(new LeafElimTree(hFunParam, FunCall(plus, Sort.SET0, FunCall(pFun, Sort.SET0), FunCall(qFun, Sort.SET0, Ref(hFunParam)))), hFun.getBody());
    FunctionDefinition kFun = (FunctionDefinition) result.getDefinition("A.C.k");
    List<DependentLink> kParams = new ArrayList<>();
    Expression kType = kFun.getTypeWithParams(kParams, Sort.SET0);
    assertEquals(Pi(ClassCall(cClass), Nat()), fromPiParameters(kType, kParams));
    DependentLink kFunParam = param("\\this", ClassCall(cClass));
    Expression aRef = FieldCall(cParent, Ref(kFunParam));
    assertEquals(new LeafElimTree(kFunParam, FunCall(plus, Sort.SET0, FunCall(hFun, Sort.SET0, aRef), FunCall(plus, Sort.SET0, FunCall(pFun, Sort.SET0), FunCall(qFun, Sort.SET0, aRef)))), kFun.getBody());
  }

  @Test
  public void fieldCallInClass() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : a.x = a.x\n" +
        "}");
  }

  @Test
  public void fieldCallInClass2() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : a.x = a.x\n" +
        "  | z : y = y\n" +
        "}");
  }

  @Test
  public void fieldCallInClass3() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : path (\\lam _ => a.x) = path (\\lam _ => a.x)\n" +
        "}");
  }

  @Test
  public void fieldCallWithArg0() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\function y (b : B) => b.a.x");
  }

  @Test
  public void fieldCallWithArg1() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\function y (b : Nat -> B) => (b 0).a.x");
  }

  @Test
  public void fieldCallWithArg2() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : Nat -> A\n" +
        "}\n" +
        "\\function y (b : B) => (b.a 1).x");
  }

  @Test
  public void fieldCallWithArg3() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : Nat -> A\n" +
        "}\n" +
        "\\function y (b : Nat -> B) => ((b 0).a 1).x");
  }

  @Test
  public void staticDynamicCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\function f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function y (a : A) => a.B.f");
  }

  @Test
  public void staticDynamicCall2() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\function y (a : A) => a.B.C.f");
  }

  @Test
  public void staticDynamicCall3() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\function y (a : A) : \\Set0 => a.B.C");
  }

  @Test
  public void staticDynamicCall31() {
    typeCheckModule(
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
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\function f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\function y (a : A) : \\Set0 => a.B");
  }

  @Test
  public void staticDynamicCall5() {
    typeCheckModule(
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
    typeCheckModule(
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
    typeCheckModule(
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
    typeCheckModule(
        "\\class A {\n" +
        "   | X : \\0-Type \\lp\n" +
        "   \\function f (x : \\0-Type \\lp) => x\n" +
        "   \\data D (x : \\0-Type \\lp)\n" +
        "   \\class B {\n" +
        "       | Y : X -> \\0-Type \\lp\n" +
        "       \\function g : \\0-Type \\lp => X\n" +
        "   }\n" +
        "}");
  }

  @Test
  public void recursiveExtendsError() {
    typeCheckModule("\\class A \\extends A", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void recursiveFieldError() {
    typeCheckModule("\\class A { | a : A }", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void mutualRecursiveExtendsError() {
    typeCheckModule(
      "\\class A \\extends B\n" +
      "\\class B \\extends A", 1);
    assertThatErrorsAre(error());
  }
}
