package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.typeMismatchError;

public class AsPatternsTest extends TypeCheckingTestCase {
  @Test
  public void asPattern() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc _ \\as x => x\n" +
      "\\func g : f 2 = 2 => idp");
  }

  @Test
  public void numberPatter() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 \\as x => x\n" +
      "  | suc n => suc n\n" +
      "\\func g : f 0 = 0 => idp");
  }

  @Test
  public void typedAsPattern() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc _ \\as x : Nat => x\n" +
      "\\func g : f 1 = 1 => idp");
  }

  @Test
  public void incorrectlyTypedAsPattern() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc _ \\as x : Int => x", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void tuplePattern() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : Nat\n" +
      "  | (a,b) \\as p => a Nat.+ R.x {p} Nat.+ b Nat.+ R.y {p}\n" +
      "\\func g : f (\\new R 3 4) = 14 => idp");
  }

  @Test
  public void typedTuplePattern() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : Nat\n" +
      "  | (a,b) \\as p : R => a Nat.+ p.x Nat.+ b Nat.+ p.y\n" +
      "\\func g : f (\\new R 3 4) = 14 => idp");
  }

  @Test
  public void sigmaPattern() {
    typeCheckModule(
      "\\data D | con (\\Sigma Nat Nat) Nat\n" +
      "\\func foo (d : D) : \\Sigma Nat Nat\n" +
      "  | con ((x,y) \\as p) m => p");
  }
}
