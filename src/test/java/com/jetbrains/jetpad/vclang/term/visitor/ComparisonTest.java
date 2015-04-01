package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ComparisonTest {
  @Test
  public void lambdas() {
    Expression expr1 = Lam(lamArgs(Name("x"), Name("y")), Index(1));
    Expression expr2 = Lam("x", Lam("y", Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdasNotEqual() {
    Expression expr1 = Lam(lamArgs(Name("x"), Name("y")), Index(0));
    Expression expr2 = Lam("x", Index(0));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void lambdasTyped() {
    Expression expr1 = Lam(lamArgs(Tele(vars("x"), Nat()), Name("y")), Index(1));
    Expression expr2 = Lam("x", Lam("y", Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdasTypedNotEqual() {
    Expression expr1 = Lam(lamArgs(Tele(vars("x"), Nat()), Name("y")), Index(1));
    Expression expr2 = Lam(lamArgs(Tele(vars("x"), Pi(Nat(), Nat())), Name("y")), Index(1));
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void lambdasImplicit() {
    Expression expr1 = Lam(lamArgs(Name("x"), Name(false, "y")), Index(1));
    Expression expr2 = Lam(lamArgs(Name(false, "x"), Name("y")), Index(1));
    assertEquals(expr1, expr2);
  }

  @Test
  public void lambdas2() {
    Expression expr1 = Lam(lamArgs(Tele(vars("x", "y"), Nat()), Name("z")), Index(1));
    Expression expr2 = Lam("x", Lam(lamArgs(Tele(vars("y"), Nat()), Tele(vars("z"), Pi(Nat(), Nat()))), Index(1)));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi() {
    Expression expr1 = Pi(args(Tele(vars("x", "y"), Nat()), TypeArg(false, Nat())), Apps(Nat(), Index(2), Var("y")));
    Expression expr2 = Pi(args(Tele(vars("x"), Nat()), Tele(vars("y"), Nat())), Pi(args(TypeArg(false, Nat())), Apps(Nat(), Index(2), Var("y"))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi2() {
    Expression expr1 = Pi(args(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat()), Tele(vars("t", "s"), Nat())), Nat());
    Expression expr2 = Pi(args(Tele(vars("x", "y", "z"), Nat()), Tele(vars("w", "t", "s"), Nat())), Nat());
    assertEquals(expr1, expr2);
  }

  @Test
  public void pi3() {
    Expression expr1 = Pi(args(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat()), Tele(vars("t", "s"), Nat())), Nat());
    Expression expr2 = Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Pi(Nat(), Nat()))))));
    assertEquals(expr1, expr2);
  }

  @Test
  public void piNotEquals() {
    Expression expr1 = Pi(args(Tele(vars("x", "y"), Nat()), Tele(vars("z", "w"), Nat())), Nat());
    Expression expr2 = Pi(args(Tele(vars("x", "y", "z"), Nat()), Tele(vars("w"), Pi(Nat(), Nat()))), Nat());
    assertNotEquals(expr1, expr2);
  }

  @Test
  public void compareLeq() {
    Expression expr1 = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    Expression expr2 = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    assertNotNull(compare(expr1, expr2, CompareVisitor.CMP.LEQ));
  }

  @Test
  public void compareNotLeq() {
    Expression expr1 = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    Expression expr2 = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    assertNull(compare(expr1, expr2, CompareVisitor.CMP.LEQ));
  }
}
