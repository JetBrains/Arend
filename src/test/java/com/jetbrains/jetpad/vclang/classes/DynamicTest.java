package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class DynamicTest extends TypeCheckingTestCase {
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
  public void classConstructorsTest() {
    ChildGroup result = typeCheckModule(
      "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data Foo | foo (x = 0)\n" +
        "  \\func y : foo = foo => path (\\lam _ => foo)\n" +
        "}\n" +
        "\\func test (p : A) => p.y");
    FunctionDefinition testFun = (FunctionDefinition) getDefinition(result, "test");
    Expression function = testFun.getResultType().normalize(NormalizeVisitor.Mode.WHNF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    Constructor foo = ((DataDefinition) getDefinition(result, "A.Foo")).getConstructor("foo");

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
    assertEquals(FieldCall((ClassField) getDefinition(result, "A.x"), Ref(testFun.getParameters())), domArguments.get(1));
    assertEquals(Prelude.ZERO, domArguments.get(2).cast(ConCallExpression.class).getDefinition());
  }

  @Test
  public void classConstructorsParametersTest() {
    ChildGroup result = typeCheckModule(
      "\\class A {\n" +
        "  | x : Nat\n" +
        "  \\data Foo (p : x = x) | foo (p = p)\n" +
        "  \\func y (_ : foo (path (\\lam _ => path (\\lam _ => x))) = foo (path (\\lam _ => path (\\lam _ => x)))) => 0\n" +
        "}\n" +
        "\\func test (q : A) => q.y");
    FunctionDefinition testFun = (FunctionDefinition) getDefinition(result, "test");
    Expression xCall = FieldCall((ClassField) getDefinition(result, "A.x"), Ref(testFun.getParameters()));
    Expression function = testFun.getResultType().cast(PiExpression.class).getParameters().getTypeExpr().normalize(NormalizeVisitor.Mode.NF);
    assertEquals(Prelude.PATH, function.cast(DataCallExpression.class).getDefinition());
    List<? extends Expression> arguments = function.cast(DataCallExpression.class).getDefCallArguments();
    assertEquals(3, arguments.size());

    DataDefinition Foo = (DataDefinition) getDefinition(result, "A.Foo");
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
