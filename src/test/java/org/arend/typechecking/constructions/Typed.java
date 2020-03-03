package org.arend.typechecking.constructions;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.arend.core.expr.ExpressionFactory.Zero;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Typed extends TypeCheckingTestCase {
  @Test
  public void typedExpr() {
    TypecheckingResult result = typeCheckExpr("(0 : Nat)", null);
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
    typeCheckDef("\\func f => ((0,idp) : \\Sigma (x : Nat) (x = 0))");
  }

  @Test
  public void typedTupleField() {
    typeCheckModule(
      "\\func f => ((0, idp : 0 = 0) : \\Sigma (x : Nat) (x = 0))\n" +
      "\\func g (p : \\Sigma (x : Nat) (x = 0)) => p.1\n" +
      "\\func h => g f");
  }

  @Test
  public void typedTupleFieldError() {
    typeCheckModule(
      "\\func f => (0, idp : 0 = 0)\n" +
      "\\func g (p : \\Sigma (x : Nat) (x = 0)) => p.1\n" +
      "\\func h => g f", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }
}
