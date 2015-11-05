package com.jetbrains.jetpad.vclang.record;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class ClassesTest {
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
    typeCheckClass(
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
    typeCheckClass(
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
    typeCheckClass(
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
    typeCheckClass(
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
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n) \n" +
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => a.con1 (path (\\lam _ => a.x))\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => a.con2 (path (\\lam _ => a.x))");
  }

  @Test
  public void constructorThisTest() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat) | con1 (f n = n) | con2 (f x = n) \n" +
        "  \\function f : D x (\\lam y => y) => con1 (path (\\lam _ => x))\n" +
        "  \\function g : D x (\\lam y => y) => con2 (path (\\lam _ => x))\n"+
        "}\n" +
        "\\static \\function f (a : A) : a.D (a.x) (\\lam y => y) => a.f\n" +
        "\\static \\function g (a : A) : a.D (a.x) (\\lam y => y) => a.g");
  }

  @Test
  public void constructorIndicesThisTest() {
    typeCheckClass(
        "\\abstract (+) (x y : Nat) : Nat\n" +
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "  \\data D (n : Nat) (f : Nat -> Nat -> Nat)" +
        "    | D zero f => con1 (f x x = f x x)\n" +
        "    | D (suc n) f => con2 (D n f) (f n x = f n x)\n" +
        "  \\function f (n : Nat) : D n (+) <= \\elim n\n" +
        "    | zero => con1 (path (\\lam _ => x + x))\n" +
        "    | suc n => con2 (f n) (path (\\lam _ => n + x))\n" +
        "}\n" +
        "\\function f (a : A) (n : Nat) : a.D n (+) => a.f\n" +
        "\\function g (a : A) (n : Nat) : a.D n (+) <= \\elim n\n" +
        "  | zero => con1 (path (\\lam _ => x + x))\n" +
        "  | suc n => con2 (g a n) (path (\\lam _ => n + a.x))");
  }
}
