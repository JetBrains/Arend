package org.arend.typechecking;

import org.junit.Test;

public class StrictPropTest extends TypeCheckingTestCase {
  @Test
  public void parametersTest() {
    typeCheckDef("\\func f {A : \\Prop} (x y : A) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void setError() {
    typeCheckDef("\\func f {A : \\Set0} (x y : A) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void setPathTest() {
    typeCheckDef("\\func f {A : \\Set} (x y : A) (p q : x = y) : p = q => path (\\lam _ => p)");
  }
}
