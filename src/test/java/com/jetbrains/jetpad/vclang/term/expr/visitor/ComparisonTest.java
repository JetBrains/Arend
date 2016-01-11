package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.*;

public class ComparisonTest {
  @Test
  public void lambdas() {
    Expression expr1 = Lam(teleArgs(Tele(vars("x"), Nat()), Tele(vars("y"), Nat())), Index(1));
    Expression expr2 = Lam(teleArgs(Tele(vars("x"), Nat())), Lam(teleArgs(Tele(vars("y"), Nat())), Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas2() {
    Expression expr1 = Lam(teleArgs(Tele(vars("x", "y"), Nat())), Index(1));
    Expression expr2 = Lam(teleArgs(Tele(vars("x"), Nat())), Lam(teleArgs(Tele(vars("y"), Nat())), Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas3() {
    Expression expr1 = Lam(teleArgs(Tele(vars("x", "y"), Nat()), Tele(vars("z"), Nat())), Index(1));
    Expression expr2 = Lam("x", Nat(), Lam(teleArgs(Tele(vars("y"), Nat()), Tele(vars("z"), Nat())), Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdasNotEqual() {
    Expression expr1 = Lam(teleArgs(Tele(vars("x", "y"), Nat())), Index(0));
    Expression expr2 = Lam(teleArgs(Tele(vars("x"), Nat())), Index(0));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void lambdasImplicit() {
    Expression expr1 = Lam(teleArgs(Tele(vars("x"), Nat()), Tele(false, vars("y"), Nat())), Index(1));
    Expression expr2 = Lam(teleArgs(Tele(false, vars("x"), Nat()), Tele(vars("y"), Nat())), Index(1));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi() {
    Expression expr1 = Pi(typeArgs(Tele(vars("x", "y"), Nat()), TypeArg(false, Nat())), Apps(Nat(), Index(2), Index(1)));
    Expression expr2 = Pi(typeArgs(Tele(vars("x"), Nat()), Tele(vars("y"), Nat())), Pi(typeArgs(TypeArg(false, Nat())), Apps(Nat(), Index(2), Index(1))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi2() {
    Expression expr1 = Pi(typeArgs(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat()), Tele(vars("t", "s"), Nat())), Nat());
    Expression expr2 = Pi(typeArgs(Tele(vars("x", "y", "z"), Nat()), Tele(vars("w", "t", "s"), Nat())), Nat());
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi3() {
    Expression expr1 = Pi(typeArgs(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat()), Tele(vars("t", "s"), Nat())), Nat());
    Expression expr2 = Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Nat()))))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void piNotEquals() {
    Expression expr1 = Pi(typeArgs(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat())), Nat());
    Expression expr2 = Pi(typeArgs(Tele(vars("x", "y", "z"), Nat()), Tele(vars("w"), Pi(Nat(), Nat()))), Nat());
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void compareLeq() {
    // Expression expr1 = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    // Expression expr2 = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    Expression expr1 = Universe(0);
    Expression expr2 = Universe(1);
    assertTrue(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void compareNotLeq() {
    Expression expr1 = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    Expression expr2 = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    assertFalse(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void letsNotEqual() {
    Expression expr1 = Let(lets(let("x", typeArgs(Tele(vars("y"), Nat())), Index(0))), Apps(Index(0), Zero()));
    Expression expr2 = Let(lets(let("x", typeArgs(Tele(vars("y"), Universe(0))), Index(0))), Apps(Index(0), Nat()));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void letsTelesEqual() {
    Expression expr1 = Let(lets(let("x", typeArgs(Tele(vars("y", "z"), Index(0))), Index(0))), Apps(Index(0), Zero()));
    Expression expr2 = Let(lets(let("x", typeArgs(Tele(vars("y"), Index(0)), Tele(vars("z"), Index(1))), Index(0))), Apps(Index(0), Zero()));
    assertEquals(expr1, expr2);
    assertEquals(expr2, expr1);
  }

  @Test
  public void letsTelesNotEqual() {
    Expression expr1 = Let(lets(let("x", typeArgs(Tele(vars("y", "z"), Index(0))), Index(0))), Apps(Index(0), Zero()));
    Expression expr2 = Let(lets(let("x", typeArgs(Tele(vars("y"), Index(0)), Tele(vars("z"), Index(0))), Index(0))), Apps(Index(0), Zero()));
    assertNotEquals(expr1, expr2);
    assertNotEquals(expr2, expr1);
  }

  @Test
  public void letsNotEquiv() {
    Expression expr1 = Let(lets(let("x", Universe(0))), Index(0));
    Expression expr2 = Let(lets(let("x", Universe(1))), Index(0));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void letsLess() {
    Expression expr1 = Let(lets(let("x", Nat())), Universe(0));
    Expression expr2 = Let(lets(let("x", Nat())), Universe(1));
    assertTrue(compare(expr1, expr2, Equations.CMP.LE));
  }

  @Test
  public void letsNested() {
    Definition def1 = typeCheckDef("\\function test => \\let x => 0 \\in \\let y => 1 \\in zero");
    Definition def2 = typeCheckDef("\\function test => \\let | x => 0 | y => 1 \\in zero");
    assertEquals(((FunctionDefinition) def1).getElimTree(), ((FunctionDefinition) def2).getElimTree());
  }

  @Test
  public void etaLam() {
    Expression type = Pi(Nat(), Apps(Prelude.PATH.getDefCall(), Lam("i", Prelude.INTERVAL.getDefCall(), Nat()), Zero(), Zero()));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a x => path (\\lam i => a x @ i)", Pi(type, type));
    assertNotNull(result1);
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(type, type));
    assertNotNull(result2);
    assertEquals(result2.expression, result1.expression);
  }

  @Test
  public void etaPath() {
    Expression type = Apps(Prelude.PATH.getDefCall(), Lam("i", Prelude.INTERVAL.getDefCall(), Pi(Nat(), Nat())), Lam("x", Nat(), Index(0)), Lam("x", Nat(), Index(0)));
    CheckTypeVisitor.Result result1 = typeCheckExpr("\\lam a => path (\\lam i x => (a @ i) x)", Pi(type, type));
    assertNotNull(result1);
    CheckTypeVisitor.Result result2 = typeCheckExpr("\\lam a => a", Pi(type, type));
    assertNotNull(result2);
    assertEquals(result2.expression, result1.expression);
  }
}
