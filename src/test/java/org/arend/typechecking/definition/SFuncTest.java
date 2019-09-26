package org.arend.typechecking.definition;

import org.arend.core.expr.Expression;
import org.arend.core.expr.ExpressionFactory;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.typechecking.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SFuncTest extends TypeCheckingTestCase {
  @Test
  public void normTest() {
    typeCheckModule("\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n");
    Expression expr = getDefinition("pred").getDefCall(Sort.STD, Collections.singletonList(ExpressionFactory.Zero()));
    assertSame(expr, expr.normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void evalTest() {
    typeCheckModule(
      "\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test => \\eval pred 2");
    Expression expr = getDefinition("test").getDefCall(Sort.STD, Collections.emptyList());
    assertEquals(ExpressionFactory.Suc(ExpressionFactory.Zero()), expr.normalize(NormalizeVisitor.Mode.WHNF));
  }

  @Test
  public void pevalTest() {
    typeCheckModule(
      "\\sfunc pred (n : Nat) : Nat | 0 => 0 | suc n => n\n" +
      "\\func test : pred 3 = 2 => \\peval pred 3");
  }

  @Test
  public void pevalPropTest() {
    typeCheckModule(
      "\\data D (A : \\Type) (p : \\Pi (x y : A) -> x = y) | con A\n" +
      "  \\where \\use \\level levelProp {A : \\Type} {p : \\Pi (x y : A) -> x = y} (d1 d2 : D A p) : d1 = d2 \\elim d1, d2\n" +
      "    | con a1, con a2 => path (\\lam i => con (p a1 a2 @ i))\n" +
      "\\sfunc f {A : \\Type} (p : \\Pi (x y : A) -> x = y) (d : D A p) : \\level A p \\elim d | con a => a\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func test {A : \\Type} (p : \\Pi (x y : A) -> x = y) (a1 a2 : A) : (\\peval f p (con a1)) = (\\peval f p (con a2)) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void pevalCmpTest() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\sfunc f (b : Bool) : Nat | true => 0 | false => 0\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func test : (\\peval f true) = (\\peval f true) => idp");
  }

  @Test
  public void pevalCmpErrorTest() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\sfunc f (b : Bool) : Nat | true => 0 | false => 0\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func test : (\\peval f true) = (\\peval f false) => idp", 1);
    assertThatErrorsAre(typeMismatchError());
  }
}