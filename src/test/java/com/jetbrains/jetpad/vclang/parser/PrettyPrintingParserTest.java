package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseDef;
import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.parseExpr;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;

public class PrettyPrintingParserTest {
  private void testExpr(Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    expr.prettyPrint(builder, new ArrayList<String>(), 0);
    Expression result = parseExpr(builder.toString());
    assertEquals(expected, result);
  }

  private void testDef(FunctionDefinition expected, FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.prettyPrint(builder, new ArrayList<String>(), 0);
    FunctionDefinition result = (FunctionDefinition) parseDef(builder.toString());
    assertEquals(expected.getSignature().getType(), result.getSignature().getType());
    assertEquals(expected.getTerm(), result.getTerm());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Expression expected = Apps(Lam("x", Lam("y", Apps(Var("x"), Apps(Var("x"), Var("y"))))), Lam("x", Lam("y", Var("x"))), Apps(Lam("x", Var("x")), Lam("x", Var("x"))));
    Expression expr = Apps(Lam("x", Lam("y", Apps(Index(1), Apps(Index(1), Index(0))))), Lam("x", Lam("y", Index(1))), Apps(Lam("x", Index(0)), Lam("x", Index(0))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : N) -> N -> N -> (x y -> y x) -> N x y
    Expression expected = Pi("x", Nat(), Pi("y", Nat(), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Var("x"), Var("y")), Apps(Var("y"), Var("x"))), Apps(Nat(), Var("x"), Var("y")))))));
    Expression expr = Pi("x", Nat(), Pi("y", Nat(), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0)))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : N) {y z : N} -> N -> (t z' : N) {x' : N -> N} -> N x' y z' t
    Expression expected = Pi("x", Nat(), Pi(false, "y", Nat(), Pi(false, "z", Nat(), Pi(Nat(), Pi("t", Nat(), Pi("z", Nat(), Pi(false, "x", Pi(Nat(), Nat()), Apps(Nat(), Var("x'"), Var("y"), Var("z'"), Var("t")))))))));
    Expression expr = Pi("x", Nat(), Pi(false, "y", Nat(), Pi(false, "z", Nat(), Pi(Nat(), Pi("t", Nat(), Pi("z", Nat(), Pi(false, "x", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2)))))))));
    testExpr(expected, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f : (x : N) -> N x = \y z. y z;
    FunctionDefinition expected = new FunctionDefinition("f", new Signature(Pi("x", Nat(), Apps(Nat(), Var("x")))), Lam("y", Lam("z", Apps(Var("y"), Var("z")))));
    FunctionDefinition def = new FunctionDefinition("f", new Signature(Pi("x", Nat(), Apps(Nat(), Index(0)))), Lam("y", Lam("z", Apps(Index(1), Index(0)))));
    testDef(expected, def);
  }
}
