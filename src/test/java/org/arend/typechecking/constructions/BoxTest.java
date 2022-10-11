package org.arend.typechecking.constructions;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class BoxTest extends TypeCheckingTestCase {
  @Test
  public void compareTest() {
    typeCheckDef("\\func test {A : \\Prop} (a a' : A) : (\\box a) = \\box a' => idp");
  }

  @Test
  public void notPropTest() {
    typeCheckDef("\\func test {A : \\Set} (a : A) => \\box a", 1);
  }
}
