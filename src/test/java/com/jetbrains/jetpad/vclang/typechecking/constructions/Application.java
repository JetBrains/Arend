package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.assertNotNull;

public class Application {
  @Test
  public void idWithoutResultType() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (f : (Nat -> _) -> Nat) => f (\\lam x => x)", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambda() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (f : Nat -> Nat -> Nat -> Nat) x => \\lam z y => f y x z", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambdaDependent() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (X : Nat -> \\Type) (Y : (x : Nat) -> X x -> \\Type) (f : (a : Nat) (b : X a) -> Y a b) x y => f x y", null);
    assertNotNull(result);
  }
}
