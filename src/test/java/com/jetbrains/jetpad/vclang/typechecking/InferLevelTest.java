package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import org.junit.Test;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class InferLevelTest {
  @Test
  public void noEquations() {
    // no equations
    // error: cannot infer ?l
    typeCheckClass("\\static \\function A {l : Lvl} => \\Type (l, inf)\n" +
                    "\\static \\function f => A", 1);
  }

  @Test
  public void metavarEquation() {
    // ?l <= ?l'
    // error: cannot infer ?l, ?l'
    typeCheckClass("\\static \\function A {l : Lvl} => \\Type (l, inf)\n" +
            "\\static \\function f {l' : Lvl} (A : \\Type (l', inf)) => A\n" +
            "\\static \\function g => f A", 1);
  }

  @Test
  public void belowTen() {
    // ?l <= 10
    // error: cannot infer ?l
    typeCheckClass("\\static \\function A {l : Lvl} => \\Type (l, inf)\n" +
            "\\static \\function f : \\Type10 => A", 1);
  }

  @Test
  public void belowParam() {
    // ?l <= c
    // error: cannot infer ?l
    typeCheckClass("\\static \\function A {l : Lvl} => \\Type (l, inf)\n" +
            "\\static \\function f {c : Lvl} : \\Type (c, inf) => A", 1);
  }

  @Test
  public void btwZeroAndParam() {
    // 0 <= ?l, 0 <= c
    // ok: ?l = 0
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g {c : Lvl} : \\Type (c, inf) => f Nat");
  }

  @Test
  public void btwOneAndParam() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g {c : Lvl} : \\Type (c, inf) => f \\Type0", 1);
  }

  @Test
  public void btwZeroAndTen() {
    // 0 <= ?l <= 10
    // ok: ?l = 0
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g : \\Type10 => f Nat");
  }

  @Test
  public void btwOneAndTen() {
    // 1 <= ?l <= 10
    // ok: ?l = 1
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g : \\Type10 => f \\Type0");
  }

  @Test
  public void greaterThanZero() {
    // 0 <= ?l
    // ok: ?l = 0
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g => f Nat");
  }

  @Test
  public void greaterThanOne() {
    // 1 <= ?l
    // ok: ?l = 1
    typeCheckClass("\\static \\function f {l : Lvl} (A : \\Type (l, inf)) => A\n" +
            "\\static \\function g => f \\Type0");
  }
}
