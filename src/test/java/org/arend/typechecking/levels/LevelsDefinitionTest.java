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
  public void notInScopeError() {
    resolveNamesModule(
      "\\hlevels p1 <= p2\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A", 2);
    assertThatErrorsAre(Matchers.notInScope("p1"), Matchers.notInScope("p2"));
  }

  @Test
  public void differentVarsError() {
    typeCheckModule(
      "\\plevels p1\n" +
      "\\plevels p2\n" +
      "\\func test (A : \\Type p1) (B : \\Type p2) => A", 1);
  }

  @Test
  public void alreadyWithVarsTest() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test \\hlevels h (A : \\Type p1 h) : \\Type p2 => A");
  }

  @Test
  public void alreadyWithVarsError() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test \\plevels p3 (A : \\Type p1) : \\Type p2 => A", 1);
  }

  @Test
  public void openTest() {
    typeCheckModule(
      "\\module M \\where {\n" +
      "  \\plevels p1 <= p2\n" +
      "}\n" +
      "\\open M\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A");
  }

  @Test
  public void openTest2() {
    typeCheckModule(
      "\\module M \\where {\n" +
      "  \\plevels p1 <= p2\n" +
      "}\n" +
      "\\open M(p1,p2)\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A");
  }
}
