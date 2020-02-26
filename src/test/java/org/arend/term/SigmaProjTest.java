package org.arend.term;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class SigmaProjTest extends TypeCheckingTestCase {
  /**
   * See JetBrains/Arend#111
   */
  @Test
  public void zeroProjection() {
    typeCheckDef(
        "\\func f => s.0 \\where {\n" +
        "  \\func s : \\Sigma Nat Nat => (114, 514)\n" +
        "}", 1);
  }
}
