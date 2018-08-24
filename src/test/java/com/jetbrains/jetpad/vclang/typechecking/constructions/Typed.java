package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.typechecking.Matchers;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Zero;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Typed extends TypeCheckingTestCase {
  @Test
  public void typedExpr() {
    CheckTypeVisitor.Result result = typeCheckExpr("(0 : Nat)", null);
    assertNotNull(result);
    assertEquals(Zero(), result.expression);
    assertEquals(Nat(), result.type);
  }

  @Test
  public void typedError() {
    typeCheckExpr("(0 : Nat -> Nat)", null, 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void typedTuple() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func f => ((0,idp) : \\Sigma (x : Nat) (x = 0))");
  }

  @Test
  public void typedTupleField() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func f => ((0, idp : 0 = 0) : \\Sigma (x : Nat) (x = 0))\n" +
      "\\func g (p : \\Sigma (x : Nat) (x = 0)) => p.1\n" +
      "\\func h => g f");
  }

  @Test
  public void typedTupleFieldError() {
    typeCheckModule(
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func f => (0, idp : 0 = 0)\n" +
      "\\func g (p : \\Sigma (x : Nat) (x = 0)) => p.1\n" +
      "\\func h => g f", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }
}
