package org.arend.term.expr.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.expr.Expression;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.ExpressionFactory.*;
import static org.arend.core.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class SubstTest extends TypeCheckingTestCase {
  @Test
  public void substConst() {
    // Nat -> A x [y := zero] = Nat -> A x
    Expression expr = Pi(Nat(), Apps(Ref(new TypedBinding("A", Pi(Nat(), Universe(0)))), Ref(new TypedBinding("x", Nat()))));
    assertEquals(expr, expr.subst(new TypedBinding("x", Nat()), Zero()));
  }

  @Test
  public void substIndexEquals() {
    // Nat -> A x [x := zero] = Nat -> A zero
    Binding x = new TypedBinding("x", Pi(Nat(), Nat()));
    Binding A = new TypedBinding("A", Pi(Pi(Nat(), Nat()), Universe(0)));
    Expression expr = Pi(Nat(), Apps(Ref(A), Ref(x)));
    assertEquals(Pi(Nat(), Apps(Ref(A), Zero())), expr.subst(x, Zero()));
  }

  @Test
  public void substLam() {
    // \x y. y x [y := suc zero] = \x y. y x
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink y = singleParam("y", Pi(Nat(), Nat()));
    Expression expr = Lam(x, Lam(y, Apps(Ref(y), Ref(x))));
    assertEquals(expr, expr.subst(y, Suc(Zero())));
  }

  @Test
  public void substLamConst() {
    // \x y. z y x [z := suc zero] = \x. suc zero
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Nat());
    Binding z = new TypedBinding("z", Pi(Nat(), Pi(Nat(), Nat())));
    Expression expr = Lam(xy, Apps(Ref(z), Ref(xy.getNext()), Ref(xy)));
    assertEquals(Lam(xy, Apps(Lam(xy, Suc(Zero())), Ref(xy.getNext()), Ref(xy))), expr.subst(z, Lam(xy, Suc(Zero()))));
  }

  @Test
  public void substLamInLamOpen() {
    // \ x y. z x y [z := x] = \x' y'. x x' y'
    SingleDependentLink xy = singleParam(true, vars("x", "y"), Pi(Nat(), Pi(Nat(), Nat())));
    Binding z = new TypedBinding("z", Nat());
    Expression expr = Lam(xy, Apps(Ref(z), Ref(xy), Ref(xy.getNext())));

    SingleDependentLink xy1 = singleParam(true, vars("x'", "y'"), Pi(Nat(), Pi(Nat(), Nat())));
    Expression expr1 = Lam(xy1, Apps(Ref(xy), Ref(xy1), Ref(xy1.getNext())));
    Expression expr2 = expr.subst(z, Ref(xy));
    assertEquals(expr1, expr2);
  }

  @Test
  public void substComplex() {
    // \x y. x b (\z. a z y) [a := \w t. t b (w c)] = \x y. x b (\z. (\w t. t b (w c)) z y)
    Binding a = new TypedBinding("a", Pi(Pi(Nat(), Nat()), Pi(Pi(Nat(), Pi(Nat(), Nat())), Nat())));
    Binding b = new TypedBinding("b", Nat());
    Binding c = new TypedBinding("c", Nat());
    SingleDependentLink x = singleParam("x", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink y = singleParam("y", Pi(Nat(), Pi(Nat(), Nat())));
    SingleDependentLink z = singleParam("z", Pi(Nat(), Nat()));
    SingleDependentLink w = singleParam("w", Pi(Nat(), Nat()));
    SingleDependentLink t = singleParam("t", Pi(Nat(), Pi(Nat(), Nat())));

    Expression expr = Lam(x, Lam(y, Apps(Ref(x), Ref(b), Lam(z, Apps(Ref(a), Ref(z), Ref(y))))));
    Expression substExpr = Lam(w, Lam(t, Apps(Ref(t), Ref(b), Apps(Ref(w), Ref(c)))));
    Expression result = Lam(x, Lam(y, Apps(Ref(x), Ref(b), Lam(z, Apps(Lam(w, Lam(t, Apps(Ref(t), Ref(b), Apps(Ref(w), Ref(c))))), Ref(z), Ref(y))))));
    assertEquals(result, expr.subst(a, substExpr));
  }

  @Test
  public void substPiClosed() {
    // (x : Nat) -> A x [x := zero] = (x : Nat) -> A x
    SingleDependentLink x = singleParam("x", Nat());
    Expression expr = Pi(x, Apps(Ref(new TypedBinding("A", Pi(Nat(), Universe(0)))), Ref(x)));
    assertEquals(expr, expr.subst(x, Zero()));
  }

  @Test
  public void substPiOpen() {
    // (x : Nat) -> A z [z := zero] = (y : Nat) -> A zero
    SingleDependentLink x = singleParam("x", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Universe(0)));
    Expression expr1 = Pi(x, Apps(Ref(A), Ref(z)));
    Expression expr2 = Pi(x, Apps(Ref(A), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substArr() {
    // Nat -> A z [z := zero] = Nat -> A zero
    DependentLink z = param("z", Nat());
    Binding A = new TypedBinding("A", Pi(Nat(), Universe(0)));
    Expression expr1 = Pi(Nat(), Apps(Ref(A), Ref(z)));
    Expression expr2 = Pi(Nat(), Apps(Ref(A), Zero()));
    assertEquals(expr2, expr1.subst(z, Zero()));
  }

  @Test
  public void substLet() {
    // \let | x => \lam (z : N) => z | y => \lam (w : N) => a \in a [a := zero] = \let | x => \lam (z : N) => z | y => \lam (w : N) => zero \in zero
    Binding a = new TypedBinding("a", Nat());
    SingleDependentLink z = singleParam("z", Nat());
    SingleDependentLink w = singleParam("w", Nat());

    Expression expr1 = let(lets(let("x", Lam(z, Ref(z))), let("y", Lam(w, Ref(a)))), Ref(a));
    Expression expr2 = let(lets(let("x", Lam(z, Ref(z))), let("y", Lam(w, Zero()))), Zero());
    assertEquals(expr2, expr1.subst(a, Zero()));
  }
}
