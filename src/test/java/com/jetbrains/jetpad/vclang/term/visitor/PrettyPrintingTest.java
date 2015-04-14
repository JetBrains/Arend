package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class PrettyPrintingTest {
  @Test
  public void prettyPrintingLam() {
    // \x. x x
    Expression expr = Lam("x", Apps(Index(0), Index(0)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), (byte) 0);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    Expression expr = Lam("x", Apps(Index(0), Lam("y", Apps(Index(0), Index(1))), Lam("z", Lam("w", Apps(Index(2), Index(0), Index(1))))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), (byte) 0);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    Expression expr = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), (byte) 0);
  }

  @Test
  public void prettyPrintingPi() {
    // (x y : N) (z w : N -> N) -> ((s : N) -> N (z s) (w x)) -> N
    Expression expr = Pi("x", Nat(), Pi("y", Nat(), Pi("z", Pi(Nat(), Nat()), Pi("w", Pi(Nat(), Nat()), Pi(Pi("s", Nat(), Apps(Nat(), Apps(Index(2), Index(0)), Apps(Index(1), Index(4)))), Nat())))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), (byte) 0);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f : (X : Type0) -> X -> X => \X x -> x;
    FunctionDefinition def = new FunctionDefinition("f", new Signature(Pi("X", Universe(0), Pi(Index(0), Index(0)))), Definition.Arrow.RIGHT, Lam("X", Lam("x", Index(0))));
    def.prettyPrint(new StringBuilder(), new ArrayList<String>(), (byte) 0);
  }
}
