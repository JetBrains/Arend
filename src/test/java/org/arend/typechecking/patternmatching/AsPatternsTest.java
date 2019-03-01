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
      "\\func g : f 2 = 2 => path (\\lam _ => 2)");
  }

  @Test
  public void numberPatter() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 \\as x => x\n" +
      "  | suc n => suc n\n" +
      "\\func g : f 0 = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void typedAsPattern() {
    typeCheckModule(
      "\\func f (n : Nat) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc _ \\as x : Nat => x\n" +
      "\\func g : f 1 = 1 => path (\\lam _ => 1)");
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
      "\\func g : f (\\new R 3 4) = 14 => path (\\lam _ => 14)");
  }

  @Test
  public void typedTuplePattern() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : Nat\n" +
      "  | (a,b) \\as p : R => a Nat.+ p.x Nat.+ b Nat.+ p.y\n" +
      "\\func g : f (\\new R 3 4) = 14 => path (\\lam _ => 14)");
  }
}
