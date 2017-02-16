package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class InferLevelTest extends TypeCheckingTestCase {
  @Test
  public void noEquations() {
    // no equations
    // error: cannot infer ?l
    typeCheckClass(
        "\\function A => \\Type \\lp\n" +
        "\\function f => A");
  }

  @Test
  public void metaVarEquation() {
    // ?l <= ?l'
    // error: cannot infer ?l, ?l'
    typeCheckClass(
        "\\function A => \\Type \\lp\n" +
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g => f A");
  }

  @Test
  public void belowTen() {
    // ?l <= 10
    // error: cannot infer ?l
    typeCheckClass(
        "\\function A => \\Type \\lp\n" +
        "\\function f : \\Type10 => A");
  }

  @Test
  public void belowParam() {
    // ?l <= c
    // error: cannot infer ?l
    typeCheckClass(
        "\\function A => \\Type \\lp \n" +
        "\\function f : \\Type (suc \\lp) => A");
  }

  @Test
  public void belowParamError() {
    // ?l + 1 <= c
    // error: cannot infer ?l
    typeCheckClass(
        "\\function A => \\Type \\lp \n" +
        "\\function f : \\Type \\lp => A", 1);
  }

  @Test
  public void btwZeroAndParam() {
    // 0 <= ?l, 0 <= c
    // ok: ?l = 0
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g : \\Type \\lp => f Nat");
  }

  @Test
  public void btwOneAndParam() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g : \\Type \\lp => f \\Type0", 1);
  }

  @Test
  public void btwZeroAndTen() {
    // 0 <= ?l <= 10
    // ok: ?l = 0
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g : \\Type10 => f Nat");
  }

  @Test
  public void btwOneAndTen() {
    // 1 <= ?l <= 10
    // ok: ?l = 1
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g : \\Type10 => f \\Type0");
  }

  @Test
  public void greaterThanZero() {
    // 0 <= ?l
    // ok: ?l = 0
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g => f Nat");
  }

  @Test
  public void greaterThanOne() {
    // 1 <= ?l
    // ok: ?l = 1
    typeCheckClass(
        "\\function f (A : \\Type \\lp) => A\n" +
        "\\function g => f \\Type0");
  }

  @Test
  public void propImpredicative() {
    typeCheckClass(
      "\\function f (X : \\Set10) (P : X -> \\Type) => \\Pi (a : X) -> P a\n" +
      "\\function g (X : \\Set10) (P : X -> \\Prop) : \\Prop => f X P"
    );
  }
}
