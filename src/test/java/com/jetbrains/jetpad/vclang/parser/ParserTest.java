package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Signature;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLam() {
    Expression expr = parseExpr("\\lam x y z => y");
    assertEquals(Lam("x", Lam("y", Lam("z", Var("y")))), expr);
  }

  @Test
  public void parserLam2() {
    Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    assertEquals(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Var("y"), Var("z")))), Var("y")))), expr);
  }

  @Test
  public void parserLamTele() {
    Expression expr = parseExpr("\\lam p {x t : N} {y} (a : N -> N) => (\\lam (z w : N) => y z) y");
    assertEquals(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), Nat()), Name(false, "y"), Tele(vars("a"), Pi(Nat(), Nat()))), Apps(Lam(lamArgs(Tele(vars("z", "w"), Nat())), Apps(Var("y"), Var("z"))), Var("y"))), expr);
  }

  @Test
  public void parserPi() {
    Expression expr = parseExpr("\\Pi (x y z : N) (w t : N -> N) -> \\Pi (a b : \\Pi (c : N) -> N c) -> N b y w");
    assertEquals(Pi(args(Tele(vars("x", "y", "z"), Nat()), Tele(vars("w", "t"), Pi(Nat(), Nat()))), Pi(args(Tele(vars("a", "b"), Pi("c", Nat(), Apps(Nat(), Var("c"))))), Apps(Nat(), Var("b"), Var("y"), Var("w")))), expr);
  }

  @Test
  public void parserPi2() {
    Expression expr = parseExpr("\\Pi (x y : N) (z : N x -> N y) -> N z y x");
    assertEquals(Pi(args(Tele(vars("x", "y"), Nat()), Tele(vars("z"), Pi(Apps(Nat(), Var("x")), Apps(Nat(), Var("y"))))), Apps(Nat(), Var("z"), Var("y"), Var("x"))), expr);
  }

  @Test
  public void parserLamOpen() {
    Expression expr = parseExpr("\\lam x => (\\Pi (y : N) -> (\\lam y => y)) y");
    assertEquals(Lam("x", Apps(Pi("y", Nat(), Lam("y", Var("y"))), Var("y"))), expr);
  }

  @Test
  public void parserPiOpen() {
    Expression expr = parseExpr("\\Pi (a b : N a) -> N a b");
    assertEquals(Pi(args(Tele(vars("a", "b"), Apps(Nat(), Var("a")))), Apps(Nat(), Var("a"), Var("b"))), expr);
  }

  @Test
  public void parserDef() {
    List<Definition> defs = parseDefs(
        "\\function x : N => 0\n" +
            "\\function y : N => x");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserDefType() {
    List<Definition> defs = parseDefs(
        "\\function x : \\Type0 => N\n" +
            "\\function y : x => 0");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserImplicit() {
    FunctionDefinition def = (FunctionDefinition)parseDef("\\function f : \\Pi (x y : N) {z w : N} (t : N) {r : N} -> N x y z w t r => N");
    def = new FunctionDefinition(def.getName(), new Signature(def.getSignature().getType()), Definition.Arrow.RIGHT, def.getTerm());
    assertEquals(4, def.getSignature().getArguments().size());
    assertTrue(def.getSignature().getArgument(0).getExplicit());
    assertFalse(def.getSignature().getArgument(1).getExplicit());
    assertTrue(def.getSignature().getArgument(2).getExplicit());
    assertFalse(def.getSignature().getArgument(3).getExplicit());
    assertEquals(Pi(args(Tele(vars("x", "y"), Nat()), Tele(false, vars("z", "w"), Nat()), Tele(vars("t"), Nat()), Tele(false, vars("r"), Nat())), Apps(Nat(), Var("x"), Var("y"), Var("z"), Var("w"), Var("t"), Var("r"))), def.getSignature().getType());
  }

  @Test
  public void parserImplicit2() {
    FunctionDefinition def = (FunctionDefinition)parseDef("\\function f : \\Pi {x : N} (N) {y z : N} (N x y z) -> N => N");
    def = new FunctionDefinition(def.getName(), new Signature(def.getSignature().getType()), Definition.Arrow.RIGHT, def.getTerm());
    assertEquals(4, def.getSignature().getArguments().size());
    assertFalse(def.getSignature().getArgument(0).getExplicit());
    assertTrue(def.getSignature().getArgument(1).getExplicit());
    assertFalse(def.getSignature().getArgument(2).getExplicit());
    assertTrue(def.getSignature().getArgument(3).getExplicit());
    assertEquals(Pi(args(Tele(false, vars("x"), Nat()), TypeArg(Nat()), Tele(false, vars("y", "z"), Nat()), TypeArg(Apps(Nat(), Var("x"), Var("y"), Var("z")))), Nat()), def.getSignature().getType());
  }

  @Test
  public void parserInfix() {
    Map<String, Definition> definitions = new HashMap<>();
    Definition plus = new FunctionDefinition("+", new Signature(Nat()), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, Definition.Arrow.LEFT, Var("+"));
    Definition mul = new FunctionDefinition("*", new Signature(Nat()), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), Definition.Fixity.INFIX, Definition.Arrow.LEFT, Var("*"));
    definitions.put("+", plus);
    definitions.put("*", mul);
    Expression expr = parseExpr("a + b * c + d * (e * f) * (g + h)", definitions);
    assertEquals(BinOp(BinOp(Var("a"), plus, BinOp(Var("b"), mul, Var("c"))), plus, BinOp(BinOp(Var("d"), mul, BinOp(Var("e"), mul, Var("f"))), mul, BinOp(Var("g"), plus, Var("h")))), expr);
  }

  @Test
  public void parserInfixDef() {
    List<Definition> defs = parseDefs(
        "\\function (+) : N -> N -> N => \\lam x y => x\n" +
            "\\function (*) : N -> N=> \\lam x => x + 0");
    assertEquals(2, defs.size());
  }

  @Test
  public void parserInfixError() {
    Map<String, Definition> definitions = new HashMap<>();
    Definition plus = new FunctionDefinition("+", new Signature(Nat()), new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), Definition.Fixity.INFIX, Definition.Arrow.LEFT, Var("+"));
    Definition mul = new FunctionDefinition("*", new Signature(Nat()), new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), Definition.Fixity.INFIX, Definition.Arrow.LEFT, Var("*"));
    definitions.put("+", plus);
    definitions.put("*", mul);

    BuildVisitor builder = new BuildVisitor(definitions);
    builder.visitExpr(parse("a + b * c").expr());
    assertEquals(1, builder.getErrors().size());
  }
}
