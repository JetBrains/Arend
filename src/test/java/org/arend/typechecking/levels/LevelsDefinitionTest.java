package org.arend.typechecking.levels;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class LevelsDefinitionTest extends TypeCheckingTestCase {
  @Test
  public void defTest() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A");
  }

  @Test
  public void errorTest() {
    typeCheckModule(
      "\\hlevels p1 <= p2\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A", 2);
    assertThatErrorsAre(Matchers.notInScope("p1"), Matchers.notInScope("p2"));
  }
}
