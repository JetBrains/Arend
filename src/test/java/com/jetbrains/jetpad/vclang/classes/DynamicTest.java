package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.Matchers.notInScope;
import static org.junit.Assert.*;

public class DynamicTest extends TypeCheckingTestCase {
  @Test
  public void dynamicIsNotVisible() {
    resolveNamesModule(
      "\\class A {\n" +
      "  \\func f => 0\n" +
      "}\n" +
      "\\func h => f", 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void dynamicDefinition() {
    typeCheckModule(
      "\\class A {\n" +
      "  \\func f => 0\n" +
      "}\n" +
      "\\func h (a : A) : Nat => A.f {a}");
  }

  @Test
  public void dynamicStaticCallError() {
    resolveNamesModule(
      "\\class A \\where {\n" +
      "  \\func f => 0\n" +
      "}\n" +
      "\\func h (a : A) => a.f", 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void dynamicCallError() {
    resolveNamesModule(
      "\\class A {\n" +
      "  \\func f => 0\n" +
      "}\n" +
      "\\func h (a : A) => a.f", 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void dynamicInheritance() {
    resolveNamesModule(
      "\\class X {\n" +
      "  \\class A\n" +
      "}\n" +
      "\\func x : X => \\new X\n" +
      "\\class B \\extends x.A", 1);
    assertThatErrorsAre(notInScope("A"));
  }

  @Test
  public void dynamicCallFromField() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : 0 = f\n" +
      "  \\func f => 0\n" +
      "}", 1);
  }

  @Test
  public void fieldCallFromDynamic() {
    typeCheckModule(
      "\\func h (x : Nat) => x\n" +
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  \\func f => h x\n" +
      "}\n" +
      "\\func g (a : A { | x => 0 }) : a.x = A.f {a} => path (\\lam _ => 0)");
  }

  @Test
  public void inheritanceFieldAccess() {
    typeCheckModule(
      "\\class X {\n" +
      "  \\class A {\n" +
      "    | n : Nat\n" +
      "  }\n" +
      "}\n" +
      "\\class B \\extends X.A {\n" +
      "  \\func my : Nat => n\n" +
      "}");
  }

  @Test
  public void dynamicInheritanceFieldAccessQualified() {
    typeCheckModule(
      "\\class X {\n" +
      "  \\class A {\n" +
      "    | n : Nat\n" +
      "  }\n" +
      "}\n" +
      "\\func x => \\new X\n" +
      "\\class B \\extends X.A {\n" +
      "  \\func my : Nat => X.A.n\n" +
      "}");
  }

  @Test
  public void dynamicInnerFunctionCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "  \\func g => B.f\n" +
        "}");
  }

  @Test
  public void staticInnerFunctionCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\func g => B.f\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void staticFromDynamicCall() {
    typeCheckClass(
        "\\func h : Nat => f",
        "\\func f => 0");
  }

  @Test
  public void staticFromDynamicCallInside() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B {\n" +
        "    \\func h : Nat => f\n" +
        "  } \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicFromAbstractCall() {
    typeCheckClass(
        "\\func f => 0\n" +
        "| h : f = 0\n", "", 1);
  }

  @Test
  public void dynamicFromDynamicCall() {
    typeCheckClass(
        "\\func f => 0\n" +
        "\\func h (_ : f = 0) => 0", "");
  }

  @Test
  public void dynamicConstructorFromDynamicCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\data D | con\n" +
        "  \\func x (_ : con = con) => 0\n" +
        "}\n" +
        "\\func test (a : A) => A.x {a}\n");
  }

  @Test
  public void dynamicFunctionWithPatterns() {
    typeCheckClass("| x : Nat -> Nat \\func pred (n : Nat) : Nat | zero => x zero | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithImplicitPatterns() {
    typeCheckClass("| x : Nat -> Nat \\func f {m : Nat} (n : Nat) : Nat | {m}, zero => x m | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithElim() {
    typeCheckClass("| x : Nat -> Nat \\func pred (n : Nat) : Nat \\elim n | zero => x zero | suc n => x n", "");
  }

  @Test
  public void dynamicFunctionWithImplicitElim() {
    typeCheckClass("| x : Nat -> Nat \\func f {m : Nat} (n : Nat) : Nat \\elim n | zero => x m | suc n => x n", "");
  }

  @Test
  public void dynamicDataWithPatterns() {
    typeCheckClass("| x : Nat \\data D (n : Nat) \\with | zero => con1 (x = 0) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithImplicitPatterns() {
    typeCheckClass("| x : Nat \\data D {m : Nat} (n : Nat) \\with | {m}, zero => con1 (x = m) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithElim() {
    typeCheckClass("| x : Nat \\data D (n : Nat) \\elim n | zero => con1 (x = 0) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicDataWithImplicitElim() {
    typeCheckClass("| x : Nat \\data D {m : Nat} (n : Nat) \\elim n | zero => con1 (x = m) | suc n => con2 (x = n)", "");
  }

  @Test
  public void dynamicInnerClass() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class C\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerClass() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B {\n" +
        "    \\class C\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall111() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall011() {
    resolveNamesModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall101() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "  \\class B \\where {\n" +
        "    \\class C {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall001() {
    resolveNamesModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall110() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "  \\class B {\n" +
        "    \\class C \\where {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall010() {
    resolveNamesModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "} \\where {\n" +
        "  \\class B {\n" +
        "    \\class C \\where {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall100() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f : Nat => g\n" +
        "    }\n" +
        "  }\n" +
        "}");
  }

  @Test
  public void dynamicDoubleInnerFunctionCall000() {
    resolveNamesModule(
        "\\class A {\n" +
        "  \\func g => 0\n" +
        "} \\where {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f : Nat => g\n" +
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
        "\\func h (b : B) : b.f 0 => b.g");
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
        "\\func h (b : B) (a : B.A {b}) : b.f 0 => a.g");
  }

  @Test
  public void constructorTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con1 {a} (path (\\lam _ => a.x))\n" +
        "\\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con2 {a} (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con1 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))\n" +
        "\\func f' (a : A) => A.con1 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))\n" +
        "\\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.con2 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))\n" +
        "\\func g' (a : A) => A.con2 {a} {a.x} {\\lam y => y} (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorThisTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\func f : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
        "  \\func g : D x (\\lam y => y) => con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.f {a}\n" +
        "\\func f' (a : A) => A.f {a}\n" +
        "\\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.g {a}\n" +
        "\\func g' (a : A) => A.g {a}");
  }

  @Test
  public void constructorWithParamsThisTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\func f : D x (\\lam y => y) => con1 {x} {\\lam y => y} (path (\\lam _ => x))\n" +
        "  \\func g => con2 {x} {\\lam y => y} (path (\\lam _ => x))\n" +
        "}\n" +
        "\\func f (a : A) : A.D {a} (a.x) (\\lam y => y) => A.f {a}\n" +
        "\\func f' (a : A) => A.f {a}\n" +
        "\\func g (a : A) : A.D {a} (a.x) (\\lam y => y) => A.g {a}\n" +
        "\\func g' (a : A) => A.g {a}");
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass(
        "| \\infix 6 + : Nat -> Nat -> Nat\n" +
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat -> Nat) \\elim n\n" +
        "    | zero => con1 (f x x = f x x)\n" +
        "    | suc n => con2 (D n f) (f n x = f n x)\n" +
        "  \\func f (n : Nat) : D n (+)\n" +
        "    | zero => con1 (path (\\lam _ => x + x))\n" +
        "    | suc n => con2 (f n) (path (\\lam _ => n + x))\n" +
        "}\n" +
        "\\func f (a : A) (n : Nat) : A.D {a} n (+) => A.f {a} n\n" +
        "\\func f' (a : A) (n : Nat) => A.f {a}\n" +
        "\\func g (a : A) (n : Nat) : A.D {a} n (+) \\elim n\n" +
        "  | zero => A.con1 {a} (path (\\lam _ => a.x + a.x))\n" +
        "  | suc n => A.con2 {a} (g a n) (path (\\lam _ => n + a.x))", "");
  }

  @Test
  public void fieldCallTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : \\Type0\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : a.x\n" +
        "}");
    ClassField xField = (ClassField) getDefinition("A.x");
    ClassField aField = (ClassField) getDefinition("B.a");
    ClassField yField = (ClassField) getDefinition("B.y");
    PiExpression piType = yField.getType(Sort.SET0);
    assertEquals(FieldCall(xField, Sort.PROP, FieldCall(aField, Sort.PROP, Ref(piType.getParameters()))), piType.getCodomain());
  }

  @Test
  public void funCallsTest() {
    typeCheckModule(
        "\\func \\infix 6 + (x y : Nat) => x\n" +
        "\\class A {\n" +
        "  \\func q => p\n" +
        "  \\class C {\n" +
        "    \\func k => h + (p + q)\n" +
        "  } \\where {\n" +
        "    \\func h => p + q\n" +
        "  }\n" +
        "} \\where {\n" +
        "  \\func p => 0\n" +
        "  \\class B {\n" +
        "    \\func g => f + p\n" +
        "  } \\where {\n" +
        "    \\func f : Nat => p\n" +
        "  }\n" +
        "}");
    FunctionDefinition plus = (FunctionDefinition) getDefinition("+");

    ClassDefinition aClass = (ClassDefinition) getDefinition("A");
    assertTrue(aClass.getFields().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) getDefinition("A.p");
    assertEquals(Nat(), pFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), Zero()), pFun.getBody());
    FunctionDefinition qFun = (FunctionDefinition) getDefinition("A.q");
    List<DependentLink> qParams = new ArrayList<>();
    Expression qType = qFun.getTypeWithParams(qParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(qType, qParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(aClass)), FunCall(pFun, Sort.SET0)), qFun.getBody());

    ClassDefinition bClass = (ClassDefinition) getDefinition("A.B");
    assertTrue(bClass.getFields().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) getDefinition("A.B.f");
    assertEquals(Nat(), fFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), FunCall(pFun, Sort.SET0)), fFun.getBody());
    FunctionDefinition gFun = (FunctionDefinition) getDefinition("A.B.g");
    List<DependentLink> gParams = new ArrayList<>();
    Expression gType = gFun.getTypeWithParams(gParams, Sort.SET0);
    assertEquals(Pi(ClassCall(bClass), Nat()), fromPiParameters(gType, gParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(bClass)), FunCall(plus, Sort.SET0, FunCall(fFun, Sort.SET0), FunCall(pFun, Sort.SET0))), gFun.getBody());

    ClassDefinition cClass = (ClassDefinition) getDefinition("A.C");
    assertEquals(1, cClass.getFields().size());
    ClassField cParent = ((ClassDefinition) getDefinition("A.C")).getPersonalFields().get(0);
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) getDefinition("A.C.h");
    List<DependentLink> hParams = new ArrayList<>();
    Expression hType = hFun.getTypeWithParams(hParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(hType, hParams));
    DependentLink hFunParam = param("\\this", ClassCall(aClass));
    assertEquals(new LeafElimTree(hFunParam, FunCall(plus, Sort.SET0, FunCall(pFun, Sort.SET0), FunCall(qFun, Sort.SET0, Ref(hFunParam)))), hFun.getBody());
    FunctionDefinition kFun = (FunctionDefinition) getDefinition("A.C.k");
    List<DependentLink> kParams = new ArrayList<>();
    Expression kType = kFun.getTypeWithParams(kParams, Sort.SET0);
    assertEquals(Pi(ClassCall(cClass), Nat()), fromPiParameters(kType, kParams));
    DependentLink kFunParam = param("\\this", ClassCall(cClass));
    Expression aRef = FieldCall(cParent, Sort.PROP, Ref(kFunParam));
    assertEquals(new LeafElimTree(kFunParam, FunCall(plus, Sort.SET0, FunCall(hFun, Sort.SET0, aRef), FunCall(plus, Sort.SET0, FunCall(pFun, Sort.SET0), FunCall(qFun, Sort.SET0, aRef)))), kFun.getBody());
  }

  @Test
  public void staticDynamicCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) => A.B.f {a}");
  }

  @Test
  public void staticDynamicCall2() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\func y (a : A) => A.B.C.f {a}");
  }

  @Test
  public void staticDynamicCall3() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\func y (a : A) : \\Set0 => A.B.C {a}");
  }

  @Test
  public void staticDynamicCall31() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) : \\Prop => A.B.C {a}");
  }

  @Test
  public void staticDynamicCall4() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func f => 0\n" +
        "    }\n" +
        "  }\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\func y (a : A) : \\Set0 => A.B {a}");
  }

  @Test
  public void staticDynamicCall5() {
    typeCheckModule(
        "\\class D {\n" +
        "  \\class E \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func d : D => \\new D\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) : D.E.f {A.B.C.d {a}} = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void staticDynamicCall6() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func d => e\n" +
        "        \\where \\func e => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) => A.B.C.d.e {a}");
  }

  @Test
  public void staticDynamicCall7() {
    typeCheckModule(
        "\\class D {\n" +
        "  \\class E \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\class C \\where {\n" +
        "      \\func d : D => \\new D\n" +
        "        \\where\n" +
        "          \\func E => 0\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) : D.E.f {A.B.C.d {a}} = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void classPolyParams() {
    typeCheckModule(
        "\\class A {\n" +
        "   | X : \\0-Type \\lp\n" +
        "   \\func f (x : \\0-Type \\lp) => x\n" +
        "   \\data D (x : \\0-Type \\lp)\n" +
        "   \\class B {\n" +
        "       | Y : X -> \\0-Type \\lp\n" +
        "       \\func g : \\0-Type \\lp => X\n" +
        "   }\n" +
        "}");
  }

  @Test
  public void classConstructorsTest() {
    typeCheckModule(
      "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\func y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\func test (p : A) => A.y {p}");
    FunctionDefinition testFun = (FunctionDefinition) getDefinition("test");
    Expression function = testFun.getResultType().normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    Constructor foo = ((DataDefinition) getDefinition("A.Foo")).getConstructor("foo");

    ConCallExpression arg2 = arguments.get(2).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(1, arg2.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg2.getDataTypeArguments().get(0));
    assertEquals(foo, arg2.getDefinition());

    ConCallExpression arg1 = arguments.get(1).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(1, arg1.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg1.getDataTypeArguments().get(0));
    assertEquals(foo, arg1.getDefinition());

    Expression domFunction = arguments.get(0).cast(LamExpression.class).getBody().cast(PiExpression.class).getParameters().getTypeExpr().normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(Prelude.PATH, domFunction.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> domArguments = domFunction.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, domArguments.size());
    assertEquals(Prelude.NAT, domArguments.get(0).cast(LamExpression.class).getBody().cast(DefCallExpression.class).getDefinition());
    assertEquals(FieldCall((ClassField) getDefinition("A.x"), Sort.PROP, Ref(testFun.getParameters())), domArguments.get(1));
    assertEquals(0, domArguments.get(2).cast(SmallIntegerExpression.class).getInteger());
  }

  @Test
  public void classConstructorsParametersTest() {
    typeCheckModule(
      "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data Foo (p : x = x) | foo (p = p)\n" +
        "  \\func y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
        "}\n" +
        "\\func test (q : A) => A.y {q}");
    FunctionDefinition testFun = (FunctionDefinition) getDefinition("test");
    Expression xCall = FieldCall((ClassField) getDefinition("A.x"), Sort.PROP, Ref(testFun.getParameters()));
    Expression function = testFun.getResultType().cast(PiExpression.class).getParameters().getTypeExpr().normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    DataDefinition Foo = (DataDefinition) getDefinition("A.Foo");
    Constructor foo = Foo.getConstructor("foo");

    ConCallExpression arg2Fun = arguments.get(2).cast(ConCallExpression.class);
    assertEquals(2, arg2Fun.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg2Fun.getDataTypeArguments().get(0));
    ConCallExpression expr1 = arg2Fun.getDataTypeArguments().get(1).cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, expr1.getDefinition());
    assertEquals(xCall, expr1.getDefCallArguments().get(0).cast(LamExpression.class).getBody());

    assertEquals(foo, arg2Fun.getDefinition());
    ConCallExpression expr2 = arg2Fun.getDefCallArguments().get(0).cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, expr2.getDefinition());
    ConCallExpression expr3 = expr2.getDefCallArguments().get(0).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, expr3.getDefinition());
    assertEquals(xCall, expr3.getDefCallArguments().get(0).cast(LamExpression.class).getBody());

    ConCallExpression arg1Fun = arguments.get(1).cast(ConCallExpression.class);
    assertEquals(2, arg1Fun.getDataTypeArguments().size());
    assertEquals(Ref(testFun.getParameters()), arg1Fun.getDataTypeArguments().get(0));
    assertEquals(expr1, arg1Fun.getDataTypeArguments().get(1));
    assertEquals(foo, arg1Fun.getDefinition());
    ConCallExpression expr4 = arg1Fun.getDefCallArguments().get(0).cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, expr4.getDefinition());
    ConCallExpression expr5 = expr4.getDefCallArguments().get(0).cast(LamExpression.class).getBody().cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, expr5.getDefinition());
    assertEquals(xCall, expr5.getDefCallArguments().get(0).cast(LamExpression.class).getBody());

    LamExpression arg0 = arguments.get(0).cast(LamExpression.class);
    assertEquals(Foo, arg0.getBody().cast(DataCallExpression.class).getDefinition());
    assertEquals(Ref(testFun.getParameters()), arg0.getBody().cast(DataCallExpression.class).getDefCallArguments().get(0));
    ConCallExpression paramConCall = arg0.getBody().cast(DataCallExpression.class).getDefCallArguments().get(1).cast(ConCallExpression.class);
    assertEquals(Prelude.PATH_CON, paramConCall.getDefinition());
    assertEquals(1, paramConCall.getDefCallArguments().size());
    assertEquals(xCall, paramConCall.getDefCallArguments().get(0).cast(LamExpression.class).getBody());

    List<? extends Expression> parameters = paramConCall.getDataTypeArguments();
    assertEquals(3, parameters.size());
    assertEquals(Nat(), parameters.get(0).cast(LamExpression.class).getBody());
    assertEquals(xCall, parameters.get(1).normalize(NormalizeVisitor.Mode.WHNF));
    assertEquals(xCall, parameters.get(2).normalize(NormalizeVisitor.Mode.WHNF));
  }
}
