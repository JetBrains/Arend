package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typechecking.TypeMismatchException;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

public class TypeCheckingTest {
  @Test
  public void typeCheckingLam() {
    // \x. x : N -> N
    Expression expr = Lam("x", Index(0));
    expr.checkType(new ArrayList<Definition>(), Pi(Nat(), Nat()));
  }

  @Test(expected = TypeMismatchException.class)
  public void typeCheckingLamError() {
    // \x. x : N -> N -> N
    Expression expr = Lam("x", Index(0));
    expr.checkType(new ArrayList<Definition>(), Pi(Nat(), Pi(Nat(), Nat())));
  }

  @Test
  public void typeCheckingApp() {
    // \x y. y (y x) : N -> (N -> N) -> N
    Expression expr = Lam("x", Lam("y", Apps(Index(0), Apps(Index(0), Index(1)))));
    expr.checkType(new ArrayList<Definition>(), Pi(Nat(), Pi(Pi(Nat(), Nat()), Nat())));
  }

  @Test
  public void typeCheckingAppPi() {
    // \f g. g zero (f zero) : (f : (x : N) -> N x) -> ((x : N) -> N x -> N (f x)) -> N (f zero)
    Expression expr = Lam("f", Lam("g", Apps(Index(0), Zero(), Apps(Index(1), Zero()))));
    Expression type = Pi("f", Pi("x", Nat(), Apps(Nat(), Index(0))), Pi(Pi("x", Nat(), Pi(Apps(Nat(), Index(0)), Apps(Nat(), Apps(Index(1), Index(0))))), Apps(Nat(), Apps(Index(0), Zero()))));
    expr.checkType(new ArrayList<Definition>(), type);
  }

  @Test
  public void typeCheckingAppLamPi() {
    // \f h. h (\k -> k (suc zero)) : (f : (g : N -> N) -> N (g zero)) -> ((z : (N -> N) -> N) -> N (f (\x. z (\_. x)))) -> N (f (\x. x))
    Expression expr = Lam("f", Lam("h", Apps(Index(0), Lam("k", Apps(Index(0), Apps(Suc(), Zero()))))));
    Expression type = Pi("f", Pi("g", Pi(Nat(), Nat()), Apps(Nat(), Apps(Index(0), Zero()))), Pi(Pi("z", Pi(Pi(Nat(), Nat()), Nat()), Apps(Nat(), Apps(Index(1), Lam("x", Apps(Index(1), Lam("_", Index(1))))))), Apps(Nat(), Apps(Index(0), Lam("x", Index(0))))));
    expr.checkType(new ArrayList<Definition>(), type);
  }

  @Test
  public void typeCheckingPi() {
    // (X : Type1) -> X -> X : Type2
    Expression expr = Pi("X", Universe(1), Pi(Index(0), Index(0)));
    assertEquals(Universe(2), expr.inferType(new ArrayList<Definition>()));
  }
}
