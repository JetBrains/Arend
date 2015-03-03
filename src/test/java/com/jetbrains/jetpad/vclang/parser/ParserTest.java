package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.Parser.*;
import static com.jetbrains.jetpad.vclang.term.expr.Expression.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLam() {
    Expression expr = parseExpr("\\x y z -> y");
    assertEquals(Lam("x", Lam("y", Lam("z", Var("y")))), expr);
  }

  @Test
  public void parserLam2() {
    Expression expr = parseExpr("\\x y -> (\\z w -> y z) y");
    assertEquals(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Var("y"), Var("z")))), Var("y")))), expr);
  }

  @Test
  public void parserPi() {
    Expression expr = parseExpr("(x y z : N) (w t : N -> N) -> (a b : ((c : N) -> N c)) -> N b y w");
    Expression natNat = Pi(Nat(), Nat());
    Expression piNat = Pi("c", Nat(), Apps(Nat(), Var("c")));
    assertEquals(Pi("x", Nat(), Pi("y", Nat(), Pi("z", Nat(), Pi("w", natNat, Pi("t", natNat, Pi("a", piNat, Pi("b", piNat, Apps(Nat(), Var("b"), Var("y"), Var("w"))))))))), expr);
  }

  @Test
  public void parserPi2() {
    Expression expr = parseExpr("(x y : N) (z : N x -> N y) -> N z y x");
    assertEquals(Pi("x'", Nat(), Pi("y'", Nat(), Pi("z'", Pi(Apps(Nat(), Var("x")), Apps(Nat(), Var("y"))), Apps(Nat(), Var("z"), Var("y"), Var("x"))))), expr);
  }

  @Test
  public void parserLamOpen() {
    Expression expr = parseExpr("\\x -> ((y : N) -> (\\y -> y)) y");
    assertEquals(Lam("x", Apps(Pi("y", Nat(), Lam("y", Var("y"))), Var("y"))), expr);
  }

  @Test
  public void parserPiOpen() {
    Expression expr = parseExpr("(a b : N a) -> N a b");
    Expression natVar = Apps(Nat(), Var("a"));
    assertEquals(Pi("a", natVar, Pi("b", natVar, Apps(Nat(), Var("a"), Var("b")))), expr);
  }

  @Test
  public void parserDef() {
    List<Definition> defs = parseDefs(
        "function x : N = 0\n" +
        "function y : N = x");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserDefType() {
    List<Definition> defs = parseDefs(
        "function x : Type0 = N\n" +
        "function y : x = 0");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserImplicit() {
    FunctionDefinition def = (FunctionDefinition)parseDef("function f : (x y : N) {z w : N} -> (t : N) -> {r : N} -> N x y z w t r = N");
    def = new FunctionDefinition(def.getName(), new Signature(def.getSignature().getType()), def.getTerm());
    assertEquals(6, def.getSignature().getArguments().length);
    assertTrue(def.getSignature().getArgument(0).isExplicit());
    assertTrue(def.getSignature().getArgument(1).isExplicit());
    assertFalse(def.getSignature().getArgument(2).isExplicit());
    assertFalse(def.getSignature().getArgument(3).isExplicit());
    assertTrue(def.getSignature().getArgument(4).isExplicit());
    assertFalse(def.getSignature().getArgument(5).isExplicit());
    assertEquals(Pi("x", Nat(), Pi("y", Nat(), Pi("z", Nat(), Pi("w", Nat(), Pi("t", Nat(), Pi("r", Nat(), Apps(Nat(), Var("x"), Var("y"), Var("z"), Var("w"), Var("t"), Var("r")))))))), def.getSignature().getType());
  }

  @Test
  public void parserImplicit2() {
    FunctionDefinition def = (FunctionDefinition)parseDef("function f : {x : N} -> N -> {y z : N} -> N x y z -> N = N");
    def = new FunctionDefinition(def.getName(), new Signature(def.getSignature().getType()), def.getTerm());
    assertEquals(5, def.getSignature().getArguments().length);
    assertFalse(def.getSignature().getArgument(0).isExplicit());
    assertTrue(def.getSignature().getArgument(1).isExplicit());
    assertFalse(def.getSignature().getArgument(2).isExplicit());
    assertFalse(def.getSignature().getArgument(3).isExplicit());
    assertTrue(def.getSignature().getArgument(4).isExplicit());
    assertEquals(Pi("x", Nat(), Pi(Nat(), Pi("y", Nat(), Pi("z", Nat(), Pi(Apps(Nat(), Var("x"), Var("y"), Var("z")), Nat()))))), def.getSignature().getType());
  }
}
