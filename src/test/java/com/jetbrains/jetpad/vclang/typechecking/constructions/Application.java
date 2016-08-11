package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class Application extends TypeCheckingTestCase {
  @Test
  public void idWithoutResultType() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (f : (Nat -> Nat) -> Nat) => f (\\lam x => x)", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambda() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (f : Nat -> Nat -> Nat -> Nat) x => \\lam z y => f y x z", null);
    assertNotNull(result);
  }

  @Test
  public void inferUnderLambdaDependent() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (X : Nat -> \\Type0) (Y : \\Pi (x : Nat) -> X x -> \\Type0) (f : \\Pi (a : Nat) (b : X a) -> Y a b) x y => f x y", null);
    assertNotNull(result);
  }
}
