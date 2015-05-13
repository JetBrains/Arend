package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class SubstTest {
  @Test
  public void substConst() {
    // zero -> null [0 := S] = zero -> null
    Expression expr = Pi(Zero(), DefCall(null));
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
    Expression expr = Lam("x", Index(0));
    assertEquals(expr, expr.subst(Suc(Zero()), 0));
  }

  @Test
  public void substLam2() {
    // \x y. y x [0 := suc zero] = \x y. y x
    Expression expr = Lam("x", Lam("y", Apps(Index(0), Index(1))));
    assertEquals(expr, expr.subst(Suc(Zero()), 0));
  }

  @Test
  public void substLamConst() {
    // \x. var(1) [1 := suc zero] = \x. suc zero
    Expression expr = Lam("x", Index(2));
    assertEquals(Lam("y", Suc(Zero())), expr.subst(Suc(Zero()), 1));
  }

  @Test
  public void substLamInLam() {
    // \x. var(1) [1 := \y.y] = \z y. y
    Expression expr = Lam("x", Index(2));
    Expression substExpr = Lam("y", Index(0));
    assertEquals(Lam("z", substExpr), expr.subst(substExpr, 1));
  }

  @Test
  public void substLamInLamOpenConst() {
    // \x. var(1) [0 := \y. var(0)] = \z. var(0)
    Expression expr = Lam("x", Index(2));
    Expression substExpr = Lam("y", Index(1));
    assertEquals(Lam("z", Index(1)), expr.subst(substExpr, 0));
  }

  @Test
  public void substLamInLamOpen() {
    // \x. var(1) [1 := \y. var(0)] = \z t. var(0)
    Expression expr = Lam("x", Index(2));
    Expression substExpr = Lam("y", Index(1));
    assertEquals(Lam("z", Lam("t", Index(2))), expr.subst(substExpr, 1));
  }

  @Test
  public void substComplex() {
    // \x y. x (var(1)) (\z. var(0) z y) [0 := \w t. t (var(0)) (w (var(1)))] = \x y. x (var(0)) (\z. (\w t. t (var(0)) (w (var(1)))) z y)
    Expression expr = Lam("x", Lam("y", Apps(Index(1), Index(3), Lam("z", Apps(Index(3), Index(0), Index(1))))));
    Expression substExpr = Lam("w", Lam("t", Apps(Index(0), Index(2), Apps(Index(1), Index(3)))));
    Expression result = Lam("x", Lam("y", Apps(Index(1), Index(2), Lam("z", Apps(Lam("w", Lam("t", Apps(Index(0), Index(5), Apps(Index(1), Index(6))))), Index(0), Index(1))))));
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

  /*
  @Test
  public void substElimLess() {
    // (\elim <1> | con a b c => <0> <2> <4>) [0 := suc <1>] = \elim <0> | con a b c => suc <3> <1> <3>
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0), new ArrayList<TypeArgument>(), constructors);
    Constructor con = new Constructor(0, "con", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0), args(Tele(vars("a", "b", "c"), Nat())), def);
    constructors.add(con);

    List<Clause> clauses1 = new ArrayList<>(1);
    clauses1.add(new Clause(con, lamArgs(Name("a"), Name("b"), Name("c")), Abstract.Definition.Arrow.RIGHT, Apps(Index(0), Index(2), Index(4))));
    Expression expr1 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), clauses1);

    List<Clause> clauses2 = new ArrayList<>(1);
    clauses2.add(new Clause(con, lamArgs(Name("a"), Name("b"), Name("c")), Abstract.Definition.Arrow.RIGHT, Apps(Suc(), Index(3), Index(1), Index(3))));
    Expression expr2 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(0), clauses2);

    Expression result = expr1.subst(Suc(Index(1)), 0);
    assertEquals(expr2, result);
  }

  @Test
  public void substElimEquals() {
    // (\elim <1> | con a b c => <2> <3> <4>) [2, 1, 0 := <1>, con zero (suc zero) zero, Nat] = \elim con zero (suc zero) zero | con a b c => Nat <1> <4>
    List<Constructor> constructors = new ArrayList<>(1);
    DataDefinition def = new DataDefinition("D", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0), new ArrayList<TypeArgument>(), constructors);
    Constructor con = new Constructor(0, "con", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(0), args(Tele(vars("a", "b", "c"), Nat())), def);
    constructors.add(con);

    List<Clause> clauses1 = new ArrayList<>(1);
    clauses1.add(new Clause(con, lamArgs(Name("a"), Name("b"), Name("c")), Abstract.Definition.Arrow.RIGHT, Apps(Index(0), Index(2), Index(4))));
    Expression expr1 = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), clauses1);

    List<Clause> clauses2 = new ArrayList<>(1);
    clauses2.add(new Clause(con, lamArgs(Name("a"), Name("b"), Name("c")), Abstract.Definition.Arrow.RIGHT, Apps(Nat(), Index(1), Index(4))));
    Expression expr2 = Elim(Abstract.ElimExpression.ElimType.ELIM, Apps(DefCall(con), Zero(), Suc(Zero()), Zero()), clauses2);

    List<Expression> substs = new ArrayList<>(3);
    substs.add(Index(1));
    substs.add(Apps(DefCall(con), Zero(), Suc(Zero()), Zero()));
    substs.add(Nat());

    Expression result = expr1.subst(substs, 0);
    assertEquals(expr2, result);
    assertEquals(Apps(Nat(), Suc(Zero()), Index(1)), expr2.normalize(NormalizeVisitor.Mode.WHNF));
  }
  */
}
