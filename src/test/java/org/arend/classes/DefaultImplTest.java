package org.arend.classes;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class DefaultImplTest extends TypeCheckingTestCase {
  @Test
  public void defaultTest() {
    typeCheckModule(
      "\\record C\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "\\record D \\extends C {\n" +
      "  \\default x => 0\n" +
      "}\n" +
      "\\func f => \\new D { | y => idp }\n" +
      "\\func g : D \\cowith\n" +
      "  | y => idp");
  }

  @Test
  public void redefineTest() {
    typeCheckModule(
      "\\record C\n" +
      "  | x : Nat\n" +
      "  | y : x = 1\n" +
      "\\record D \\extends C {\n" +
      "  \\default x => 0\n" +
      "}\n" +
      "\\func f => \\new D { | x => 1 | y => idp }\n" +
      "\\func g : D \\cowith\n" +
      "  | x => 1\n" +
      "  | y => idp\n" +
      "\\record E \\extends D\n" +
      "  | x => 1\n" +
      "  | y => idp");
  }

  @Test
  public void redefineDefaultTest() {
    typeCheckModule(
      "\\record C\n" +
      "  | x : Nat\n" +
      "  | y : x = 1\n" +
      "\\record D \\extends C {\n" +
      "  \\default x => 0\n" +
      "}\n" +
      "\\record E \\extends D {\n" +
      "  \\default x => 1\n" +
      "}\n" +
      "\\func f => \\new E { | y => idp }\n" +
      "\\func g : E \\cowith\n" +
      "  | y => idp");
  }

  @Test
  public void defaultAssumptionError() {
    typeCheckModule(
      "\\record C\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "\\record D \\extends C {\n" +
      "  \\default x => 0\n" +
      "  \\default y => idp\n" +
      "}", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void defaultFunction() {
    typeCheckModule(
      "\\record C (k : Nat)\n" +
      "  | f (n : Nat) : n = k -> Nat\n" +
      "\\record D \\extends C {\n" +
      "  \\default f (n : Nat) (p : n = k) : Nat \\elim n {\n" +
      "    | 0 => 0\n" +
      "    | suc n => n\n" +
      "  }\n" +
      "}\n" +
      "\\func d : D 3 \\cowith\n" +
      "\\func test : d.f 3 idp = 2 => idp");
  }

  @Test
  public void renameDefault() {
    typeCheckModule(
      "\\record C {\n" +
      "  | f : Nat\n" +
      "  \\default f \\as f' => 0\n" +
      "}\n" +
      "\\func c : C \\cowith\n" +
      "\\func test (c' : C) : c.f = C.f' {c'} => idp");
  }

  @Test
  public void renameDefaultError() {
    resolveNamesModule(
      "\\record C {\n" +
      "  | f : Nat -> Nat\n" +
      "  \\default f n \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc n => n\n" +
      "  }\n" +
      "}", 1);
  }

  @Test
  public void sameName() {
    typeCheckModule(
      "\\record C {\n" +
      "  | f : Nat\n" +
      "  \\default f => 0\n" +
      "}\n" +
      "\\func g : C \\cowith");
  }
}
