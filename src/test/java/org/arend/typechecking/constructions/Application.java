package org.arend.typechecking.constructions;

import org.arend.typechecking.result.TypecheckingResult;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Application extends TypeCheckingTestCase {
  @Test
  public void idWithoutResultType() {
    TypecheckingResult result = typeCheckExpr("\\lam (f : (Nat -> Nat) -> Nat) => f (\\lam x => x)", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambda() {
    TypecheckingResult result = typeCheckExpr("\\lam (f : Nat -> Nat -> Nat -> Nat) x => \\lam z y => f y x z", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambdaDependent() {
    TypecheckingResult result = typeCheckExpr("\\lam (X : Nat -> \\Type0) (Y : \\Pi (x : Nat) -> X x -> \\Type0) (f : \\Pi (a : Nat) (b : X a) -> Y a b) x y => f x y", null);
    assertNotNull(result);
  }

  @Test
  public void implicitArgument() {
    typeCheckModule(
      "\\func f (A : \\Type) (a : A) => a\n" +
      "\\func g => f {_} 0", 1);
  }
}
