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
    typeCheckModule(
      "\\function f (X : \\Set10) (P : X -> \\Type) => \\Pi (a : X) -> P a\n" +
      "\\function g (X : \\Set10) (P : X -> \\Prop) : \\Prop => f X P"
    );
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
    typeCheckModule("\\function f (X : \\Set10) (x : X) : \\1-Type1 => (x = x : Prop) -> \\Set0");
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
}
