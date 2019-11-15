package org.arend.typechecking;

import org.junit.Test;

public class PreludeTest extends TypeCheckingTestCase {
  @Test
  public void testCoe2Sigma() {
    typeCheckModule("\\func foo (A : \\Type) (i j : I) (a : A) : coe2 (\\lam _ => A) i a j = a => idp");
  }

  @Test
  public void testCoe2Left() {
    typeCheckModule("\\func foo (A : I -> \\Type) (j : I) (a : A left) : coe2 A left a j = coe A a j => idp");
  }

  @Test
  public void testCoe2Right() {
    typeCheckModule("\\func foo (A : I -> \\Type) (i : I) (a : A i) : coe2 A i a right = coe2 A i a right => path (\\lam _ => coe (\\lam k => A (I.squeezeR i k)) a right)");
  }

  @Test
  public void testCoe2RightRight() {
    typeCheckModule("\\func foo (A : I -> \\Type) (a : A right) : coe2 A right a right = a => idp");
  }
}
