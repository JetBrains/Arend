package org.arend.typechecking.constructions;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class LetTest extends TypeCheckingTestCase {
  @Test
  public void tuplePattern() {
    typeCheckModule(
      "\\func f (p : \\Sigma Nat Nat) => \\let (x,y) => p \\in x Nat.+ y\n" +
      "\\func g : f (3,5) = 8 => idp");
  }

  @Test
  public void tuplePattern2() {
    typeCheckModule(
      "\\func f (p : \\Sigma (\\Sigma Nat (Nat -> Nat)) Nat) => \\let ((x,y),z) => p \\in y x Nat.+ z\n" +
      "\\func g : f ((3, \\lam n => n Nat.* 2), 5) = 11 => idp");
  }

  @Test
  public void classPattern() {
    typeCheckModule(
      "\\record R (x y z : Nat)\n" +
      "\\record S \\extends R | y => 3\n" +
      "\\func f (s : S) => \\let (a,b) => s \\in a Nat.+ b\n" +
      "\\func g : f (\\new S 1 8) = 9 => idp");
  }

  @Test
  public void classPattern2() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\record S (x : Nat) (r : R)\n" +
      "\\func f (s : S) => \\let (a,(b,c)) => s \\in (a Nat.+ b) Nat.* c\n" +
      "\\func g : f (\\new S 2 (\\new R 3 4)) = 20 => idp");
  }

  @Test
  public void mixedPattern() {
    typeCheckModule(
      "\\record R (x : Nat) (p : \\Sigma (Nat -> Nat) Nat)\n" +
      "\\func f (p : \\Sigma R Nat) => \\let ((x,(f,y)),z) => p \\in (f x Nat.+ y) Nat.* z\n" +
      "\\func g : f (\\new R 3 (\\lam n => n Nat.* 2, 10), 5) = 80 => idp");
  }

  @Test
  public void typedPattern() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => \\let (a : Nat, b) => r \\in a");
  }

  @Test
  public void typedPatternError() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => \\let (a : R, b) => r \\in a", 1);
  }

  @Test
  public void typedLetClause() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R 0) => \\let r' : R => r \\in r'.x");
  }

  @Test
  public void typedLetClauseError() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R 0) => \\let r' : Nat => r \\in r'", 1);
  }

  @Test
  public void dependentSigma() {
    typeCheckExpr("\\lam (p : \\Sigma (A : \\Type) (A -> Nat)) => \\let (A,f) => p \\in \\lam (a : A) => f a", null);
  }
}
