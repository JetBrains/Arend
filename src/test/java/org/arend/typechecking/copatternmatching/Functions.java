package org.arend.typechecking.copatternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class Functions extends TypeCheckingTestCase {
  @Test
  public void checkCowith() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : R \\cowith | x => 0 | y => 0");
  }

  @Test
  public void incompleteCowith() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : R \\cowith | x => 0", 1);
  }

  @Test
  public void withoutType() {
    resolveNamesModule(
      "\\record R (x y : Nat)\n" +
      "\\func f \\cowith | x => 0 | y => 0", 1);
  }

  @Test
  public void withIncorrectType() {
    resolveNamesDef(
      "\\data D\n" +
      "\\func f : D \\cowith");
  }

  @Test
  public void recursive() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : R \\cowith | x => 0 | y => f.x", 1);
  }

  @Test
  public void recursiveWithoutType() {
    resolveNamesModule(
      "\\record R (x y : Nat)\n" +
      "\\func f \\cowith | x => 0 | y => f.x", 1);
  }

  @Test
  public void mutuallyRecursive() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : R \\cowith | x => 0 | y => g.x\n" +
      "\\func g : R \\cowith | x => 0 | y => f.x", 2);
  }

  @Test
  public void tooManyCopatterns() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f : R \\cowith | x => 0 | y => 0 | x => 0", 1);
  }

  @Test
  public void wrongCopattern() {
    resolveNamesModule(
      "\\record R (x y : Nat)\n" +
      "\\record R' (x' y' : Nat)\n" +
      "\\func f : R \\cowith | x => 0 | y => 0 | x' => 0", 1);
  }
}
