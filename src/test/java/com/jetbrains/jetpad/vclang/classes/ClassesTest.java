package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.PiExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
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
        "  \\func f => 0\n" +
        "}\n" +
        "\\func h (a : A) => a.f", 1);
  }

  @Test
  public void dynamicStaticCallError2() {
    typeCheckModule(
        "\\class A \\where {\n" +
        "  \\func f => 0\n" +
        "}\n" +
        "\\func g (a : A) => A.f\n" +
        "\\func h (a : A) => a.f", 1);
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
        "\\func test (a : A) => a.x\n");
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
        "\\func h (b : B) (a : b.A) : b.f 0 => a.g");
  }

  @Test
  public void constructorTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\func f (a : A) : a.D (a.x) (\\lam y => y) => a.con1 (path (\\lam _ => a.x))\n" +
        "\\func g (a : A) : a.D (a.x) (\\lam y => y) => a.con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorWithParamsTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "}\n" +
        "\\func f (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\func f' (a : A) => (a.D (a.x) (\\lam y => y)).con1 (path (\\lam _ => a.x))\n" +
        "\\func g (a : A) : a.D (a.x) (\\lam y => y) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))\n" +
        "\\func g' (a : A) => (a.D (a.x) (\\lam y => y)).con2 (path (\\lam _ => a.x))");
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
        "\\func f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\func f' (a : A) => a.f\n" +
        "\\func g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\func g' (a : A) => a.g");
  }

  @Test
  public void constructorWithParamsThisTest() {
    typeCheckModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n)\n" +
        "  \\func f : D x (\\lam y => y) => (D x (\\lam y => y)).con1 (path (\\lam _ => x))\n" +
        "  \\func g => (D x (\\lam y => y)).con2 (path (\\lam _ => x))\n" +
        "}\n" +
        "\\func f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\func f' (a : A) => a.f\n" +
        "\\func g (a : A) : a.D (a.x) (\\lam y => y) => a.g\n" +
        "\\func g' (a : A) => a.g");
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass(
        "| + : Nat -> Nat -> Nat\n" +
        "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat -> Nat) \\elim n\n" +
        "    | zero => con1 (f x x = f x x)\n" +
        "    | suc n => con2 (D n f) (f n x = f n x)\n" +
        "  \\func f (n : Nat) : D n (+)\n" +
        "    | zero => con1 (path (\\lam _ => x + x))\n" +
        "    | suc n => con2 (f n) (path (\\lam _ => n + x))\n" +
        "}\n" +
        "\\func f (a : A) (n : Nat) : a.D n `+ => a.f n\n" +
        "\\func f' (a : A) (n : Nat) => a.f\n" +
        "\\func g (a : A) (n : Nat) : a.D n (+) \\elim n\n" +
        "  | zero => a.con1 (path (\\lam _ => a.x + a.x))\n" +
        "  | suc n => a.con2 (g a n) (path (\\lam _ => n + a.x))", "");
  }

  @Test
  public void fieldCallTest() {
    ChildGroup result = typeCheckModule(
        "\\class A {\n" +
        "  | x : \\Type0\n" +
        "}\n" +
        "\\class B {\n" +
        "  | a : A\n" +
        "  | y : a.x\n" +
        "}");
    ClassField xField = (ClassField) getDefinition(result, "A.x");
    ClassField aField = (ClassField) getDefinition(result, "B.a");
    ClassField yField = (ClassField) getDefinition(result, "B.y");
    PiExpression piType = yField.getType(Sort.SET0);
    assertEquals(FieldCall(xField, FieldCall(aField, Ref(piType.getParameters()))), piType.getCodomain());
  }

  @Test
  public void funCallsTest() {
    ChildGroup result = typeCheckModule(
        "\\func + (x y : Nat) => x\n" +
        "\\class A {\n" +
        "  \\func q => p\n" +
        "  \\class C {\n" +
        "    \\func k => h + (p + q)" +
        "  } \\where {\n" +
        "    \\func h => p + q" +
        "  }\n" +
        "} \\where {\n" +
        "  \\func p => 0\n" +
        "  \\class B {\n" +
        "    \\func g => f + p\n" +
        "  } \\where {\n" +
        "    \\func f : Nat => p\n" +
        "  }\n" +
        "}");
    FunctionDefinition plus = (FunctionDefinition) getDefinition(result, "+");

    ClassDefinition aClass = (ClassDefinition) getDefinition(result, "A");
    assertTrue(aClass.getFields().isEmpty());
    FunctionDefinition pFun = (FunctionDefinition) getDefinition(result, "A.p");
    assertEquals(Nat(), pFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), Zero()), pFun.getBody());
    FunctionDefinition qFun = (FunctionDefinition) getDefinition(result, "A.q");
    List<DependentLink> qParams = new ArrayList<>();
    Expression qType = qFun.getTypeWithParams(qParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(qType, qParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(aClass)), FunCall(pFun, Sort.SET0)), qFun.getBody());

    ClassDefinition bClass = (ClassDefinition) getDefinition(result, "A.B");
    assertTrue(bClass.getFields().isEmpty());
    FunctionDefinition fFun = (FunctionDefinition) getDefinition(result, "A.B.f");
    assertEquals(Nat(), fFun.getTypeWithParams(new ArrayList<>(), Sort.SET0));
    assertEquals(new LeafElimTree(EmptyDependentLink.getInstance(), FunCall(pFun, Sort.SET0)), fFun.getBody());
    FunctionDefinition gFun = (FunctionDefinition) getDefinition(result, "A.B.g");
    List<DependentLink> gParams = new ArrayList<>();
    Expression gType = gFun.getTypeWithParams(gParams, Sort.SET0);
    assertEquals(Pi(ClassCall(bClass), Nat()), fromPiParameters(gType, gParams));
    assertEquals(new LeafElimTree(param("\\this", ClassCall(bClass)), FunCall(plus, Sort.SET0, FunCall(fFun, Sort.SET0), FunCall(pFun, Sort.SET0))), gFun.getBody());

    ClassDefinition cClass = (ClassDefinition) getDefinition(result, "A.C");
    assertEquals(1, cClass.getFields().size());
    ClassField cParent = (ClassField) getDefinition(result, "A.C.parent");
    assertNotNull(cParent);
    FunctionDefinition hFun = (FunctionDefinition) getDefinition(result, "A.C.h");
    List<DependentLink> hParams = new ArrayList<>();
    Expression hType = hFun.getTypeWithParams(hParams, Sort.SET0);
    assertEquals(Pi(ClassCall(aClass), Nat()), fromPiParameters(hType, hParams));
    DependentLink hFunParam = param("\\this", ClassCall(aClass));
    assertEquals(new LeafElimTree(hFunParam, FunCall(plus, Sort.SET0, FunCall(pFun, Sort.SET0), FunCall(qFun, Sort.SET0, Ref(hFunParam)))), hFun.getBody());
    FunctionDefinition kFun = (FunctionDefinition) getDefinition(result, "A.C.k");
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
        "\\func y (b : B) => b.a.x");
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
        "\\func y (b : Nat -> B) => (b 0).a.x");
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
        "\\func y (b : B) => (b.a 1).x");
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
        "\\func y (b : Nat -> B) => ((b 0).a 1).x");
  }

  @Test
  public void staticDynamicCall() {
    typeCheckModule(
        "\\class A {\n" +
        "  \\class B \\where {\n" +
        "    \\func f => 0\n" +
        "  }\n" +
        "}\n" +
        "\\func y (a : A) => a.B.f");
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
        "\\func y (a : A) => a.B.C.f");
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
        "\\func y (a : A) : \\Set0 => a.B.C");
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
        "\\func y (a : A) : \\Prop => a.B.C");
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
        "\\func y (a : A) : \\Set0 => a.B");
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
        "\\func y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
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
        "\\func y (a : A) => a.B.C.d.e");
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
        "\\func y (a : A) : a.B.C.d.E.f = 0 => path (\\lam _ => 0)");
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
