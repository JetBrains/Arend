package com.jetbrains.jetpad.vclang.typechecking.constructions;


import com.jetbrains.jetpad.vclang.core.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TypeOmega extends TypeCheckingTestCase {
  @Test
  public void dataExpansion() {
    typeCheckClass(
      "\\data D (A : \\Type) (a : A) | d (B : A -> \\Type2)\n" +
      "\\function f : \\Pi {A : \\Type \\lp} {a : A} -> (A -> \\Type1) -> D A a => d\n" +
      "\\function test => f {_} {\\Set0} {\\Prop} (\\lam _ => \\Type0)");
  }

  @Test
  public void allowedInArgs() {
    typeCheckClass("\\function f (A : \\Type -> \\Type) => 0");
  }

  @Test
  public void allowedInResultType() {
    typeCheckClass("\\function g : \\Type -> \\Type => \\lam X => X");
  }

  @Test
  public void allowedAsExpression() {
    typeCheckClass("\\function f => \\Type");
  }

  @Test
  public void equalityOfTypes() {
    typeCheckClass("\\function f (A B : \\Type) => A = B");
  }

  @Test
  public void callPolyFromOmega() {
     typeCheckClass(
         "\\function f (A : \\Type \\lp) => A\n" +
         "\\function g (A : \\Type) => f A");
  }

  @Test
  public void typeOmegaResult() {
    typeCheckClass("\\function f (A : \\Type \\lp) : \\Type => A");
  }

  @Test
  public void callNonPolyFromOmega() {
    typeCheckClass(
        "\\function f (A : \\Type0) => 0\n" +
        "\\function g (A : \\Type) => f A", 1);
  }

  @Test
  public void levelP() {
    typeCheckExpr("\\Type \\lh", null, 1);
  }

  @Test
  public void levelH() {
    typeCheckExpr("\\Type1 \\lp", null, 1);
  }

  @Test
  public void truncatedLevel() {
    assertEquals(new UniverseExpression(new Sort(7, 2)), typeCheckExpr("\\2-Type 7", null).expression);
  }
}
