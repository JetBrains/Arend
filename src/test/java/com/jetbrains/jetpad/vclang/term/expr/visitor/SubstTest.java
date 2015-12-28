package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class SubstTest {
  @Test
  public void substConst() {
    // zero -> N [0 := S] = zero -> N
    Expression expr = Pi(Zero(), DataCall(Prelude.NAT));
    assertEquals(expr, expr.subst(Suc(), 0));
  }

  @Test
  public void substIndexLess() {
    // var(2) [3 := null] = var(2)
    Expression expr = Index(2);
    assertEquals(expr, expr.subst((Expression) null, 3));
  }

  @Test
  public void substIndexEquals() {
    // var(2) [2 := suc zero] = suc zero
    Expression expr = Index(2);
    assertEquals(Suc(Zero()), expr.subst(Suc(Zero()), 2));
  }

  @Test
  public void substIndexGreater() {
    // var(4) [1 := null] = var(3)
    Expression expr = Index(4);
    assertEquals(Index(3), expr.subst((Expression) null, 1));
  }

  @Test
  public void substLam1() {
    // \x.x [0 := suc zero] = \x.x
    Expression expr = Lam("x", Nat(), Index(0));
    assertEquals(expr, expr.subst(Suc(Zero()), 0));
  }

  @Test
  public void substLam2() {
    // \x y. y x [0 := suc zero] = \x y. y x
    Expression expr = Lam("x", Nat(), Lam("y", Nat(), Apps(Index(0), Index(1))));
    assertEquals(expr, expr.subst(Suc(Zero()), 0));
  }

  @Test
  public void substLamConst() {
    // \x. var(1) [1 := suc zero] = \x. suc zero
    Expression expr = Lam("x", Nat(), Index(2));
    assertEquals(Lam("y", Nat(), Suc(Zero())), expr.subst(Suc(Zero()), 1));
  }

  @Test
  public void substLamInLam() {
    // \x. var(1) [1 := \y.y] = \z y. y
    Expression expr = Lam("x", Nat(), Index(2));
    Expression substExpr = Lam("y", Nat(), Index(0));
    assertEquals(Lam("z", Nat(), substExpr), expr.subst(substExpr, 1));
  }

  @Test
  public void substLamInLamOpenConst() {
    // \x. var(1) [0 := \y. var(0)] = \z. var(0)
    Expression expr = Lam("x", Nat(), Index(2));
    Expression substExpr = Lam("y", Nat(), Index(1));
    assertEquals(Lam("z", Nat(), Index(1)), expr.subst(substExpr, 0));
  }

  @Test
  public void substLamInLamOpen() {
    // \x. var(1) [1 := \y. var(0)] = \z t. var(0)
    Expression expr = Lam("x", Nat(), Index(2));
    Expression substExpr = Lam("y", Nat(), Index(1));
    assertEquals(Lam("z", Nat(), Lam("t", Nat(), Index(2))), expr.subst(substExpr, 1));
  }

  @Test
  public void substComplex() {
    // \x y. x (var(1)) (\z. var(0) z y) [0 := \w t. t (var(0)) (w (var(1)))] = \x y. x (var(0)) (\z. (\w t. t (var(0)) (w (var(1)))) z y)
    Expression expr = Lam("x", Nat(), Lam("y", Nat(), Apps(Index(1), Index(3), Lam("z", Nat(), Apps(Index(3), Index(0), Index(1))))));
    Expression substExpr = Lam("w", Nat(), Lam("t", Nat(), Apps(Index(0), Index(2), Apps(Index(1), Index(3)))));
    Expression result = Lam("x", Nat(), Lam("y", Nat(), Apps(Index(1), Index(2), Lam("z", Nat(), Apps(Lam("w", Nat(), Lam("t", Nat(), Apps(Index(0), Index(5), Apps(Index(1), Index(6))))), Index(0), Index(1))))));
    assertEquals(result, expr.subst(substExpr, 0));
  }

  @Test
  public void substPiClosed() {
    // (x : N) -> N x [0 := zero] = (x : N) -> N x
    Expression expr = Pi("x", Nat(), Apps(Nat(), Index(0)));
    assertEquals(expr, expr.subst(Zero(), 0));
  }

  @Test
  public void substPiOpen() {
    // (x : N) -> N (var(0)) [0 := zero] = (y : N) -> N zero
    Expression expr1 = Pi("x", Nat(), Apps(Nat(), Index(1)));
    Expression expr2 = Pi("y", Nat(), Apps(Nat(), Zero()));
    assertEquals(expr2, expr1.subst(Zero(), 0));
  }

  @Test
  public void substArr() {
    // N -> N (var(0)) [0 := zero] = N -> N zero
    Expression expr1 = Pi(Nat(), Apps(Nat(), Index(0)));
    Expression expr2 = Pi(Nat(), Apps(Nat(), Zero()));
    assertEquals(expr2, expr1.subst(Zero(), 0));
  }

  @Test
  public void substLet() {
    // \let | x (z : N) => z | y (w : N) => <2> => <2> [0 := zero] = \let | x (z : N) => z | y (w : N) => zero \in zero
    Expression expr1 = Let(lets(let("x", typeArgs(Tele(vars("z"), Nat())), Index(0)), let("y", typeArgs(Tele(vars("w"), Nat())), Index(2))), Index(2));
    Expression expr2 = Let(lets(let("x", typeArgs(Tele(vars("z"), Nat())), Index(0)), let("y", typeArgs(Tele(vars("w"), Nat())), Zero())), Zero());
    assertEquals(expr1.subst(Zero(), 0), expr2);
  }
}
