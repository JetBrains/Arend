package org.arend.typechecking.patternmatching;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

public class LamPatternTest extends TypeCheckingTestCase {
  @Test
  public void tupleTest() {
    typeCheckDef("\\func test : Nat -> (\\Sigma Nat Nat) -> Nat -> Nat => \\lam a (x,_) b => x");
  }

  @Test
  public void tupleTest2() {
    typeCheckDef("\\func test : (\\Sigma Nat Nat) -> (\\Sigma Nat (\\Sigma Nat Nat)) -> Nat => \\lam (a,_) (_,(b,_)) => a Nat.+ b");
  }

  @Test
  public void finTest() {
    typeCheckDef("\\func test : Fin 1 -> Nat => \\lam 0 => 3");
  }

  @Test
  public void recordTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func test : R -> Nat => \\lam (x,y) => y");
  }

  @Test
  public void dataTest() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "\\func test : D -> Nat => \\lam (con x) => x");
  }

  @Test
  public void implicitTest() {
    typeCheckDef("\\func test : \\Pi {x : Fin 1} -> x = 0 => \\lam {(zero)} => idp");
  }

  @Test
  public void implicitError() {
    typeCheckDef("\\func test : \\Pi {x : Fin 1} -> x = 0 => \\lam {zero} => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void projTest() {
    typeCheckModule(
      "\\func proj : (\\Sigma Nat Nat) -> Nat => \\lam (x,_) => x\n" +
      "\\func test : proj = (\\lam p => p.1) => idp");
  }

  @Test
  public void dependencyTest() {
    typeCheckDef("\\func test : \\Pi (x : Fin 1) -> x = 0 -> 0 = 0 => \\lam 0 p => p");
  }

  @Test
  public void absurdPatternTest() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\data D (x y : Bool) \\with\n" +
      "  | false, true => con\n" +
      "\\func test : \\Pi (x : Bool) -> D x x -> 0 = 1 => \\lam (true) ()");
  }

  @Test
  public void idpTest() {
    typeCheckDef("\\func test : \\Pi (x : Nat) -> x = 0 -> 0 = x => \\lam x (idp) => idp");
  }
}
