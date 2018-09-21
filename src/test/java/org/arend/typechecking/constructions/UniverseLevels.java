package org.arend.typechecking.constructions;


import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UniverseLevels extends TypeCheckingTestCase {
  @Test
  public void dataExpansion() {
    typeCheckModule(
      "\\data D (A : \\Type) (a : A) | d (B : A -> \\Type2)\n" +
      "\\func f : \\Pi {A : \\Type} {a : A} -> (A -> \\Type1) -> D A a => \\lam B => d B\n" +
      "\\func test => f {\\Set0} {\\Prop} (\\lam _ => \\Type0)");
  }

  @Test
  public void allowedInArgs() {
    typeCheckModule("\\func f (A : \\Type -> \\Type) => 0");
  }

  @Test
  public void allowedInResultType() {
    typeCheckModule("\\func g : \\Type -> \\Type => \\lam X => X");
  }

  @Test
  public void allowedAsExpression() {
    typeCheckModule("\\func f => \\Type");
  }

  @Test
  public void equalityOfTypes() {
    typeCheckModule("\\func f (A B : \\Type) => A = B");
  }

  @Test
  public void callPolyFromOmega() {
     typeCheckModule(
         "\\func f (A : \\Type) => A\n" +
         "\\func g (A : \\Type) => f A");
  }

  @Test
  public void typeOmegaResult() {
    typeCheckModule("\\func f (A : \\Type) : \\Type => A");
  }

  @Test
  public void callNonPolyFromOmega() {
    typeCheckModule(
        "\\func f (A : \\Type0) => 0\n" +
        "\\func g (A : \\Type) => f A", 1);
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

  @Test
  public void func() {
    typeCheckModule(
      "\\data Foo (A : \\Type) : \\Type | foo A\n" +
      "\\func bar (A : \\Type \\lp (\\max \\lh 1)) : \\Type \\lp (\\max \\lh 1) => Foo A");
  }

  @Test
  public void dataMaxTest() {
    typeCheckModule(
      "\\data Foo (A : \\Type) : \\Type | foo A\n" +
      "\\data Bar (A : \\Type \\lp (\\max \\lh 1)) : \\Type \\lp (\\max \\lh 1) | bar (Foo A)");
  }
}
