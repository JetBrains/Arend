package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class SubstTest {
  @Test
  public void substConst() {
    // zero -> N x [y := S] = zero -> N x
    Expression expr = Pi(Zero(), Apps(DataCall(Prelude.NAT), Reference(new TypedBinding("x", Nat()))));
    assertEquals(expr, expr.subst(new TypedBinding("x", Nat()), Suc()));
  }

  @Test
  public void substIndexEquals() {
    // zero -> N x [x := S] = zero -> N S
    Binding x = new TypedBinding("x", Nat());
    Expression expr = Pi(Zero(), Apps(DataCall(Prelude.NAT), Reference(x)));
    assertEquals(expr, expr.subst(x, Suc()));
  }

  @Test
  public void substLam() {
    // \x y. y x [y := suc zero] = \x y. y x
    DependentLink xy = param(true, vars("x", "Y"), Nat());
    Expression expr = Lam(xy, Apps(Reference(xy.getNext()), Reference(xy)));
    assertEquals(expr, expr.subst(xy.getNext(), Suc(Zero())));
  }

  @Test
  public void substLamConst() {
    // \x y. z y x [x := suc zero] = \x. suc zero
    DependentLink xy = param(true, vars("x", "Y"), Nat());
    Binding z = new TypedBinding("z", Nat());
    Expression expr = Lam(xy, Apps(Reference(z), Reference(xy.getNext()), Reference(xy)));
    assertEquals(Lam(xy, Apps(Suc(Zero()), Reference(xy.getNext()), Reference(xy))), expr.subst(z, Suc(Zero())));
  }

  @Test
  public void substLamInLamOpen() {
    // \ x y. z x y [z := x] = \x' y'. x x' y'
    DependentLink xy = param(true, vars("x", "y"), Nat());
    Binding z = new TypedBinding("z", Nat());
    Expression expr = Lam(xy, Apps(Reference(z), Reference(xy), Reference(xy.getNext())));

    DependentLink xy1 = param(true, vars("x'", "y'"), Nat());
    Expression expr1 = Lam(xy1, Apps(Reference(xy), Reference(xy1), Reference(xy1.getNext())));
    assertEquals(expr1, expr.subst(z, Reference(xy)));
  }

  @Test
  public void substComplex() {
    // \x y. x b (\z. a z y) [a := \w t. t b (w c)] = \x y. x b (\z. (\w t. t b (w c)) z y)
    Binding a = new TypedBinding("a", Nat());
    Binding b = new TypedBinding("b", Nat());
    Binding c = new TypedBinding("c", Nat());
    DependentLink xy = param(true, vars("x", "y"), Nat());
    DependentLink z = param("z", Nat());
    DependentLink wt = param(true, vars("w", "t"), Nat());

    Expression expr = Lam(xy, Apps(Reference(xy), Reference(b), Lam(z, Apps(Reference(a), Reference(z), Reference(xy.getNext())))));
    Expression substExpr = Lam(wt, Apps(Reference(wt.getNext()), Reference(b), Apps(Reference(wt), Reference(c))));
    Expression result = Lam(xy, Apps(Reference(xy), Reference(b), Lam(z, Apps(Lam(wt, Apps(Reference(wt.getNext()), Reference(b), Apps(Reference(wt), Reference(c)))), Reference(z), Reference(xy.getNext())))));
    assertEquals(result, expr.subst(a, substExpr));
  }

  @Test
  public void substPiClosed() {
    // (x : N) -> N x [x := zero] = (x : N) -> N x
    DependentLink x = param("x", Nat());
    Expression expr = Pi(x, Apps(Nat(), Reference(x)));
    assertEquals(expr, expr.subst(x, Zero()));
  }

  @Test
  public void substPiOpen() {
    // (x : N) -> N z [z := zero] = (y : N) -> N zero
    DependentLink x = param("x", Nat());
    DependentLink z = param("z", Nat());
    Expression expr1 = Pi(x, Apps(Nat(), Reference(z)));
    Expression expr2 = Pi(x, Apps(Nat(), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substArr() {
    // N -> N z [z := zero] = N -> N zero
    DependentLink z = param("z", Nat());
    Expression expr1 = Pi(Nat(), Apps(Nat(), Reference(z)));
    Expression expr2 = Pi(Nat(), Apps(Nat(), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substLet() {
    // \let | x (z : N) => z | y (w : N) => a \in a [a := zero] = \let | x (z : N) => z | y (w : N) => zero \in zero
    Binding a = new TypedBinding("a", Nat());
    DependentLink z = param("z", Nat());
    DependentLink w = param("w", Nat());

    Expression expr1 = Let(lets(let("x", z, Reference(z)), let("y", w, Reference(a))), Reference(a));
    Expression expr2 = Let(lets(let("x", z, Reference(z)), let("y", w, Zero())), Zero());
    assertEquals(expr2, expr1.subst(a, Zero()));
  }
}
