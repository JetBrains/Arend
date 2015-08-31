package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.LetExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDefs;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class PrettyPrintingTest {
  @Test
  public void prettyPrintingLam() {
    // \x. x x
    Expression expr = Lam("x", Apps(Index(0), Index(0)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC);
  }

  @Test
  public void prettyPrintingLam2() {
    // \x. x (\y. y x) (\z w. x w z)
    Expression expr = Lam("x", Apps(Index(0), Lam("y", Apps(Index(0), Index(1))), Lam("z", Lam("w", Apps(Index(2), Index(0), Index(1))))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC);
  }

  @Test
  public void prettyPrintingU() {
    // (X : Type0) -> X -> X
    Expression expr = Pi("X", Universe(0), Pi(Index(0), Index(0)));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC);
  }

  @Test
  public void prettyPrintingPi() {
    // (x y : N) (z w : N -> N) -> ((s : N) -> N (z s) (w x)) -> N
    Expression expr = Pi("x", Nat(), Pi("y", Nat(), Pi("z", Pi(Nat(), Nat()), Pi("w", Pi(Nat(), Nat()), Pi(Pi("s", Nat(), Apps(Nat(), Apps(Index(2), Index(0)), Apps(Index(1), Index(4)))), Nat())))));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC);
  }

  @Test
  public void prettyPrintingFunDef() {
    // f (X : Type0) (x : X) : X => x;
    List<Argument> arguments = new ArrayList<>(2);
    arguments.add(Tele(vars("X"), Universe(0)));
    arguments.add(Tele(vars("x"), Index(0)));
    FunctionDefinition def = new FunctionDefinition(new Namespace(new Utils.Name("f"), null), Abstract.Definition.DEFAULT_PRECEDENCE, arguments, Index(1), Definition.Arrow.RIGHT, Lam("X", Lam("x", Index(0))));
    def.accept(new DefinitionPrettyPrintVisitor(new StringBuilder(), new ArrayList<String>(), 0), null);
  }

  @Test
  public void prettyPrintingLet() {
    // \let x {A : Type0} (y ; A) : A => y \in x Zero()
    LetExpression expr = Let(lets(let("x", lamArgs(Tele(false, vars("A"), Universe(0)), Tele(vars("y"), Index(0))), Index(0))), Apps(Index(0), Zero()));
    expr.prettyPrint(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC);
  }

  @Test
  public void prettyPrintingPatternDataDef() {
    ClassDefinition def = parseDefs("\\data LE (n m : Nat) | LE (zero) m => LE-zero | LE (suc n) (suc m) => LE-suc (LE n m)");
    def.accept(new DefinitionPrettyPrintVisitor(new StringBuilder(), new ArrayList<String>(), Abstract.Expression.PREC), null);
  }
}
