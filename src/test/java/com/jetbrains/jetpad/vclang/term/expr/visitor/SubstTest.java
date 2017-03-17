package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class SubstTest extends TypeCheckingTestCase {
  @Test
  public void substConst() {
    // Nat -> A x [y := zero] = Nat -> A x
    Expression expr = Pi(Nat(), Apps(Reference(new TypedBinding("A", Pi(Nat(), Universe(0)))), Reference(new TypedBinding("x", Nat()))));
    assertEquals(expr, expr.subst(new TypedBinding("x", Nat()), Zero()));
  }

  @Test
  public void substIndexEquals() {
    // Nat -> A x [x := zero] = Nat -> A zero
    Binding x = new TypedBinding("x", Pi(Nat(), Nat()));
    Binding A = new TypedBinding("A", Pi(Pi(Nat(), Nat()), Universe(0)));
    Expression expr = Pi(Nat(), Apps(Reference(A), Reference(x)));
    assertEquals(Pi(Nat(), Apps(Reference(A), Zero())), expr.subst(x, Zero()));
  }

  @Test
  public void substLam() {
    // \x y. y x [y := suc zero] = \x y. y x
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Pi(Nat(), Nat()));
    Expression expr = Lam(x, Lam(y, Apps(Reference(y), Reference(x))));
    assertEquals(expr, expr.subst(y, Suc(Zero())));
  }

  @Test
  public void substLamConst() {
    // \x y. z y x [z := suc zero] = \x. suc zero
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    Binding z = new TypedBinding("z", Pi(Nat(), Pi(Nat(), Nat())));
    Expression expr = Lam(xy, Apps(Reference(z), Reference(xy.getNext()), Reference(xy)));
    assertEquals(Lam(xy, Apps(Lam(xy, Suc(Zero())), Reference(xy.getNext()), Reference(xy))), expr.subst(z, Lam(xy, Suc(Zero()))));
  }

  @Test
  public void substLamInLamOpen() {
    // \ x y. z x y [z := x] = \x' y'. x x' y'
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    Binding z = new TypedBinding("z", Nat());
    Expression expr = Lam(xy, Apps(Reference(z), Reference(xy), Reference(xy.getNext())));

    SingleDependentLink xy1 = singleParam(true, vars("x'", "y'"), Nat());
    Expression expr1 = Lam(xy1, Apps(Reference(xy), Reference(xy1), Reference(xy1.getNext())));
    assertEquals(expr1, expr.subst(z, Reference(xy)));
  }

  @Test
  public void substComplex() {
    // \x y. x b (\z. a z y) [a := \w t. t b (w c)] = \x y. x b (\z. (\w t. t b (w c)) z y)
    Binding a = new TypedBinding("a", Pi(Nat(), Pi(Nat(), Nat())));
    Binding b = new TypedBinding("b", Nat());
    Binding c = new TypedBinding("c", Nat());
    SingleDependentLink x = singleParam("x", Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
    SingleDependentLink y = singleParam("y", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    SingleDependentLink w = singleParam("w", Pi(Nat(), Nat()));
    SingleDependentLink t = singleParam("t", Pi(Nat(), Pi(Nat(), Nat())));

    Expression expr = Lam(x, Lam(y, Apps(Reference(x), Reference(b), Lam(z, Apps(Reference(a), Reference(z), Reference(y))))));
    Expression substExpr = Lam(w, Lam(t, Apps(Reference(t), Reference(b), Apps(Reference(w), Reference(c)))));
    Expression result = Lam(x, Lam(y, Apps(Reference(x), Reference(b), Lam(z, Apps(Lam(w, Lam(t, Apps(Reference(t), Reference(b), Apps(Reference(w), Reference(c))))), Reference(z), Reference(y))))));
    assertEquals(result, expr.subst(a, substExpr));
  }

  @Test
  public void substPiClosed() {
    // (x : Nat) -> A x [x := zero] = (x : Nat) -> A x
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Pi(x, Apps(Reference(new TypedBinding("A", Pi(Nat(), Universe(0)))), Reference(x)));
    assertEquals(expr, expr.subst(x, Zero()));
  }

  @Test
  public void substPiOpen() {
    // (x : Nat) -> A z [z := zero] = (y : Nat) -> A zero
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Universe(0)));
    Expression expr1 = Pi(x, Apps(Reference(A), Reference(z)));
    Expression expr2 = Pi(x, Apps(Reference(A), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substArr() {
    // Nat -> A z [z := zero] = Nat -> A zero
    DependentLink z = param("z", Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Universe(0)));
    Expression expr1 = Pi(Nat(), Apps(Reference(A), Reference(z)));
    Expression expr2 = Pi(Nat(), Apps(Reference(A), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substLet() {
    // \let | x (z : N) => z | y (w : N) => a \in a [a := zero] = \let | x (z : N) => z | y (w : N) => zero \in zero
    Binding a = new TypedBinding("a", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    SingleDependentLink w = singleParam("w", Nat());

    Expression expr1 = Let(lets(let("x", z, Reference(z)), let("y", w, Reference(a))), Reference(a));
    Expression expr2 = Let(lets(let("x", z, Reference(z)), let("y", w, Zero())), Zero());
    assertEquals(expr2, expr1.subst(a, Zero()));
  }
}
