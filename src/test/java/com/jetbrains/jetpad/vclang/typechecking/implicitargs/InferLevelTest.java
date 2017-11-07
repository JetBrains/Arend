package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class InferLevelTest extends TypeCheckingTestCase {
  @Test
  public void noEquations() {
    // no equations
    // error: cannot infer ?l
    typeCheckModule(
        "\\function A => \\Type\n" +
        "\\function f => A");
  }

  @Test
  public void metaVarEquation() {
    // ?l <= ?l'
    // error: cannot infer ?l, ?l'
    typeCheckModule(
        "\\function A => \\Type\n" +
        "\\function f (A : \\Type) => A\n" +
        "\\function g => f A");
  }

  @Test
  public void belowTen() {
    // ?l <= 10
    // error: cannot infer ?l
    typeCheckModule(
        "\\function A => \\oo-Type\n" +
        "\\function f : \\oo-Type10 => A");
  }

  @Test
  public void belowParam() {
    // ?l <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\function A => \\Type\n" +
        "\\function f : \\Type (\\suc \\lp) (\\suc \\lh) => A");
  }

  @Test
  public void belowParam2() {
    typeCheckModule(
        "\\function A => \\Type\n" +
        "\\function f : \\oo-Type (\\suc \\lp) => A");
  }

  @Test
  public void belowParamError() {
    // ?l + 1 <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\function A => \\oo-Type\n" +
        "\\function f : \\oo-Type => A", 1);
  }

  @Test
  public void btwZeroAndParam() {
    // 0 <= ?l, 0 <= c
    // ok: ?l = 0
    typeCheckModule(
        "\\function f (A : \\oo-Type) => A\n" +
        "\\function g : \\oo-Type => f Nat");
  }

  @Test
  public void btwOneAndParam() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\function f (A : \\Type) => A\n" +
        "\\function g : \\Type \\lp (\\suc \\lh) => f \\Type0", 1);
  }

  @Test
  public void btwOneAndParamWithH() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\function f (A : \\Type) => A\n" +
        "\\function g : \\Type => f \\Type0", 2);
  }

  @Test
  public void btwZeroAndTen() {
    // 0 <= ?l <= 10
    // ok: ?l = 0
    typeCheckModule(
        "\\function f (A : \\oo-Type) => A\n" +
        "\\function g : \\oo-Type10 => f Nat");
  }

  @Test
  public void btwOneAndTen() {
    // 1 <= ?l <= 10
    // ok: ?l = 1
    typeCheckModule(
        "\\function f (A : \\oo-Type) => A\n" +
        "\\function g : \\oo-Type10 => f \\oo-Type0");
  }

  @Test
  public void greaterThanZero() {
    // 0 <= ?l
    // ok: ?l = 0
    typeCheckModule(
        "\\function f (A : \\Type) => A\n" +
        "\\function g => f Nat");
  }

  @Test
  public void greaterThanOne() {
    // 1 <= ?l
    // ok: ?l = 1
    typeCheckModule(
        "\\function f (A : \\Type) => A\n" +
        "\\function g => f \\Type0");
  }

  @Test
  public void propImpredicative() {
    typeCheckModule("\\function g (X : \\Set10) (P : X -> \\Prop) : \\Prop => \\Pi (a : X) -> P a");
  }

  @Test
  public void propImpredicative2() {
    typeCheckModule(
      "\\function f (X : \\Set10) (P : X -> \\Type) => \\Pi (a : X) -> P a\n" +
      "\\function g (X : \\Set10) (P : X -> \\Prop) : \\Prop => f X P", 1);
  }

  @Test
  public void levelOfPath() {
    typeCheckModule("\\function f (X : \\Set10) (x : X) : \\Prop => x = x");
  }

  @Test
  public void levelOfPath2() {
    typeCheckModule("\\function f (X : \\Set10) (x : X) : \\1-Type1 => x = x -> \\Set0", 1);
  }

  @Test
  public void levelOfPath3() {
    typeCheckModule("\\function f (X : \\Set10) (x : X) : \\1-Type1 => (x = x : \\Prop) -> \\Set0");
  }

  @Test
  public void constantUpperBound() {
    typeCheckModule(
      "\\function f (A : \\Type) => A\n" +
      "\\function g (B : \\Type) : \\Set => f B", 1);
  }

  @Test
  public void expectedType() {
    typeCheckModule(
      "\\function X => \\Type\n" +
      "\\function f : X => \\Type"
    );
  }

  @Test
  public void parameters() {
    typeCheckModule(
      "\\function X => \\Type\n" +
      "\\function f (A : X) => 0\n" +
      "\\function g => f \\Set0"
    );
  }

  @Test
  public void lhLessThanInf() {
    typeCheckModule(
      "\\function f (A : \\Type) (a a' : A) (p : a = a') => p\n" +
      "\\function X : \\oo-Type => Nat\n" +
      "\\function g : X = X => f \\Type X X (path (\\lam _ => X))");
  }

  @Test
  public void pLevelTest() {
    typeCheckModule(
      "\\function idp  {A : \\Type} {a : A} =>\n" +
      "  path (\\lam _ => a)\n" +
      "\\function squeeze1 (i j : I) : I =>\n" +
      "  coe (\\lam x => left = x) (path (\\lam _ => left)) j @ i\n" +
      "\\function squeeze (i j : I) =>\n" +
      "  coe (\\lam i => Path (\\lam j => left = squeeze1 i j) (path (\\lam _ => left)) (path (\\lam j => squeeze1 i j))) (path (\\lam _ => path (\\lam _ => left))) right @ i @ j\n" +
      "\\function psqueeze {A : \\Type} {a a' : A} (p : a = a') (i : I) : a = p @ i =>\n" +
      "  path (\\lam j => p @ squeeze i j)\n" +
      "\\function Jl {A : \\Type} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type) (b : B a idp) {a' : A} (p : a = a') : B a' p =>\n" +
      "  coe (\\lam i => B (p @ i) (psqueeze p i)) b right\n" +
      "\\function foo (A : \\Type) (a0 a1 : A) (p : a0 = a1) =>\n" +
      "  Jl (\\lam _ q => (idp {A} {a0} = idp {A} {a0}) = (q = q)) idp p");
  }

  @Test
  public void classLevelTest() {
    typeCheckModule(
      "\\class A {\n" +
      "  | X : \\oo-Type\n" +
      "}\n" +
      "\\function f : A (\\levels 0 _) => \\new A { X => \\oo-Type0 }", 1);
  }

  @Test
  public void setIsNotProp() {
    typeCheckDef(
      "\\function isSur {A B : \\Set} (f : A -> B) : \\Prop =>\n" +
      "  \\Pi (b : B) -> \\Sigma (a : A) (b = f a)", 1);
  }
}
