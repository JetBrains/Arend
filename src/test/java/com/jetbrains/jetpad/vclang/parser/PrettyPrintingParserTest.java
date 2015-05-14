package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrettyPrintingParserTest {
  private void testExpr(Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    expr.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(builder.toString());
    assertTrue(compare(expected, result));
  }

  private void testDef(FunctionDefinition expected, FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>()), Abstract.Expression.PREC);
    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) parseDef(builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compare(expected.getArgument(i).getType(), result.getArgument(i).getType()));
    }
    assertTrue(compare(expected.getResultType(), result.getResultType()));
    assertEquals(expected.getArrow(), result.getArrow());
    assertTrue(compare(expected.getTerm(), result.getTerm()));
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Expression expected = Apps(Lam(lamArgs(Name("x"), Name("y")), Apps(Var("x"), Apps(Var("x"), Var("y")))), Lam(lamArgs(Name("x"), Name("y")), Var("x")), Apps(Lam("x", Var("x")), Lam("x", Var("x"))));
    Expression expr = Apps(Lam(lamArgs(Name("x"), Name("y")), Apps(Index(1), Apps(Index(1), Index(0)))), Lam(lamArgs(Name("x"), Name("y")), Index(1)), Apps(Lam("x", Index(0)), Lam("x", Index(0))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : Nat) -> Nat -> Nat -> (x y -> y x) -> Nat x y
    Expression expected = Pi(args(Tele(vars("x", "y"), Var("Nat"))), Pi(Var("Nat"), Pi(Var("Nat"), Pi(Pi(Apps(Var("x"), Var("y")), Apps(Var("y"), Var("x"))), Apps(Var("Nat"), Var("x"), Var("y"))))));
    Expression expr = Pi(args(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : Nat) {y z : Nat} -> Nat -> (t z' : Nat) {x' : Nat -> Nat} -> Nat x' y z' t
    Expression expected = Pi("x", Var("Nat"), Pi(args(Tele(false, vars("y", "z"), Var("Nat"))), Pi(Var("Nat"), Pi(args(Tele(vars("t", "z'"), Var("Nat"))), Pi(false, "x'", Pi(Var("Nat"), Var("Nat")), Apps(Var("Nat"), Var("x'"), Var("y"), Var("z'"), Var("t")))))));
    Expression expr = Pi("x", Nat(), Pi(args(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(args(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2)))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f (x : Nat) : Nat x => \y z. y z;
    FunctionDefinition expected = new FunctionDefinition("f", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("x"), Var("Nat"))), Apps(Var("Nat"), Var("x")), Definition.Arrow.RIGHT, Lam(lamArgs(Name("y"), Name("z")), Apps(Var("y"), Var("z"))));
    FunctionDefinition def      = new FunctionDefinition("f", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("x"), Nat())), Apps(Nat(), Index(0)), Definition.Arrow.RIGHT, Lam(lamArgs(Name("y"), Name("z")), Apps(Index(1), Index(0))));
    testDef(expected, def);
  }

  @Test
  public void prettyPrintingParserElim() throws UnsupportedEncodingException {
    // \function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \elim x | zero => y | suc x' => z (foo z x')
    List<Clause> clausesExpected = new ArrayList<>();
    Expression termExpected = Elim(Abstract.ElimExpression.ElimType.ELIM, Var("x"), clausesExpected, null);
    FunctionDefinition expected = new FunctionDefinition("foo", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("z"), Pi(Pi(Var("Nat"), Var("Nat")), Var("Nat"))), Tele(vars("x", "y"), Var("Nat"))), Var("Nat"), Abstract.Definition.Arrow.LEFT, termExpected);
    clausesExpected.add(new Clause(Prelude.ZERO, lamArgs(), Abstract.Definition.Arrow.RIGHT, Var("y")));
    clausesExpected.add(new Clause(Prelude.SUC, lamArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Var("z"), Apps(Var("foo"), Var("z"), Var("x'")))));

    List<Clause> clausesActual = new ArrayList<>();
    Expression termActual = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), clausesActual, null);
    FunctionDefinition actual = new FunctionDefinition("foo", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("z"), Pi(Pi(Nat(), Nat()), Nat())), Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, termActual);
    clausesActual.add(new Clause(Prelude.ZERO, lamArgs(), Abstract.Definition.Arrow.RIGHT, Index(0)));
    clausesActual.add(new Clause(Prelude.SUC, lamArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(2), Apps(DefCall(actual), Index(2), Index(1)))));

    testDef(expected, actual);
  }
}
