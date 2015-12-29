package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.factory.ConcreteExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ToAbstractVisitor;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest {
  private void testExpr(Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    List<String> context = new ArrayList<>();
    ToAbstractVisitor visitor = new ToAbstractVisitor(new ConcreteExpressionFactory(), context);
    visitor.setFlags(EnumSet.of(ToAbstractVisitor.Flag.SHOW_IMPLICIT_ARGS, ToAbstractVisitor.Flag.SHOW_TYPES_IN_LAM));
    expr.accept(visitor, null).prettyPrint(builder, context, Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(builder.toString());
    assertTrue(compare(expected, result));
  }

  private void testDef(Concrete.FunctionDefinition expected, Concrete.FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);

    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) parseDef(builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compare(((Concrete.TypeArgument) expected.getArguments().get(i)).getType(), ((Concrete.TypeArgument) result.getArguments().get(i)).getType()));
    }
    assertTrue(compare(expected.getResultType(), result.getResultType()));
    assertNotNull(result.getTerm());
    // TODO: fix
    //assertTrue(compare(expected.getElimTree(), result.getElimTree()));
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Expression expected = Apps(Lam(teleArgs(Tele(vars("x", "y"), Nat())), Apps(Var("x"), Apps(Var("x"), Var("y")))), Lam(teleArgs(Tele(vars("x"), Nat()), Tele(vars("y"), Nat())), Var("x")), Apps(Lam("x", Nat(), Var("x")), Lam("x", Nat(), Var("x"))));
    Expression expr     = Apps(Lam(teleArgs(Tele(vars("x", "y"), Nat())), Apps(Index(1), Apps(Index(1), Index(0)))), Lam(teleArgs(Tele(vars("x"), Nat()), Tele(vars("y"), Nat())), Index(1)), Apps(Lam("x", Nat(), Index(0)), Lam("x", Nat(), Index(0))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : Nat) -> Nat -> Nat -> (x y -> y x) -> Nat x y
    Expression expected = Pi(typeArgs(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Var("x"), Var("y")), Apps(Var("y"), Var("x"))), Apps(Nat(), Var("x"), Var("y"))))));
    Expression expr = Pi(typeArgs(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : Nat) {y z : Nat} -> Nat -> (t z' : Nat) {x' : Nat -> Nat} -> Nat x' y z' t
    Expression expected = Pi("x", Nat(), Pi(typeArgs(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(typeArgs(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Var("x'"), Var("y"), Var("z'"), Var("t")))))));
    Expression expr = Pi("x", Nat(), Pi(typeArgs(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(typeArgs(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2)))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f {x : Nat} (A : Nat -> \Type0) : A x -> (Nat -> Nat) -> Nat -> Nat => \t y z. y z;
    Concrete.FunctionDefinition def = new Concrete.FunctionDefinition(POSITION, new Name("f"), Abstract.Definition.DEFAULT_PRECEDENCE, cargs(cTele(false, cvars("x"), cNat()), cTele(cvars("A"), cPi(cNat(), cUniverse(0)))), cPi(cApps(cVar("A"), cVar("x")), cPi(cPi(cNat(), cNat()), cPi(cNat(), cNat()))), Definition.Arrow.RIGHT, cLam(cargs(cName("t"), cName("y"), cName("z")), cApps(cVar("y"), cVar("z"))), false, null, new ArrayList<Concrete.Statement>());
    testDef(def, def);
  }
}
