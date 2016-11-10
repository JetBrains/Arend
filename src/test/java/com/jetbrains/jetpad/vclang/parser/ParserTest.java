package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import org.junit.Ignore;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest extends ParserTestCase {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr("\\lam x => \\let | x => \\Type0 \\in x x");
    Concrete.Expression expr1 = parseExpr("\\let | x => \\Type0 \\in \\lam x => x x");
    assertTrue(compareAbstract(cLam("x", cLet(clets(clet("x", cargs(), cUniverse(0))), cApps(cVar("x"), cVar("x")))), expr));
    assertTrue(compareAbstract(cLet(clets(clet("x", cargs(), cUniverse(0))), cLam("x", cApps(cVar("x"), cVar("x")))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr("\\let | x => \\Type0 | y => x \\in y");
    assertTrue(compareAbstract(cLet(clets(clet("x", cUniverse(0)), clet("y", cVar("x"))), cVar("y")), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr("\\let | x : \\Type1 => \\Type0 \\in x");
    assertTrue(compareAbstract(cLet(clets(clet("x", cargs(), cUniverse(1), Abstract.Definition.Arrow.RIGHT, cUniverse(0))), cVar("x")), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr("\\lam x y z => y");
    boolean res = compareAbstract(cLam(cargs(cName("x"), cName("y"), cName("z")), cVar("y")), expr);
    assertTrue(res);
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    assertTrue(compareAbstract(cLam(cargs(cName("x"), cName("y")), cApps(cLam(cargs(cName("z"), cName("w")), cApps(cVar("y"), cVar("z"))), cVar("y"))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr("\\lam p {x t : \\Type0} {y} (a : \\Type0 -> \\Type0) => (\\lam (z w : \\Type0) => y z) y");
    assertTrue(compareAbstract(cLam(cargs(cName("p"), cTele(false, cvars("x", "t"), cUniverse(0)), cName(false, "y"), cTele(cvars("a"), cPi(cUniverse(0), cUniverse(0)))), cApps(cLam(cargs(cTele(cvars("z", "w"), cUniverse(0))), cApps(cVar("y"), cVar("z"))), cVar("y"))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr("\\Pi (x y z : \\Type0) (w t : \\Type0 -> \\Type0) -> \\Pi (a b : \\Pi (c : \\Type0) -> \\Type0 c) -> \\Type0 b y w");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars("x", "y", "z"), cUniverse(0)), cTele(cvars("w", "t"), cPi(cUniverse(0), cUniverse(0)))), cPi(ctypeArgs(cTele(cvars("a", "b"), cPi("c", cUniverse(0), cApps(cUniverse(0), cVar("c"))))), cApps(cUniverse(0), cVar("b"), cVar("y"), cVar("w")))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr("\\Pi (x y : \\Type0) (z : \\Type0 x -> \\Type0 y) -> \\Type0 z y x");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars("x", "y"), cUniverse(0)), cTele(cvars("z"), cPi(cApps(cUniverse(0), cVar("x")), cApps(cUniverse(0), cVar("y"))))), cApps(cUniverse(0), cVar("z"), cVar("y"), cVar("x"))), expr));
  }

  @Test
  public void parserLamOpenError() {
    assertNotNull(parseExpr("\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y"));
  }

  @Test
  public void parserPiOpenError() {
    assertNotNull(parseExpr("\\Pi (a b : Nat a) -> Nat a b"));
  }

  @Test
  public void parserImplicit() {
    Concrete.ClassField def = (Concrete.ClassField) parseDef("\\field f : \\Pi (x y : \\Type1) {z w : \\Type1} (t : \\Type1) {r : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) -> A x y z w t r");
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getArguments().size());
    assertTrue(pi.getArguments().get(0).getExplicit());
    assertFalse(pi.getArguments().get(1).getExplicit());
    assertTrue(pi.getArguments().get(2).getExplicit());
    assertFalse(pi.getArguments().get(3).getExplicit());
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(0).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(1).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(2).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(3).getType()));
    assertTrue(compareAbstract(cApps(cVar("A"), cVar("x"), cVar("y"), cVar("z"), cVar("w"), cVar("t"), cVar("r")), pi.getCodomain()));
  }

  @Test
  public void parserImplicit2() {
    Concrete.ClassField def = (Concrete.ClassField) parseDef("\\field f : \\Pi {x : \\Type1} (_ : \\Type1) {y z : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) (_ : A x y z) -> \\Type1");
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getArguments().size());
    assertFalse(pi.getArguments().get(0).getExplicit());
    assertTrue(pi.getArguments().get(1).getExplicit());
    assertFalse(pi.getArguments().get(2).getExplicit());
    assertTrue(pi.getArguments().get(3).getExplicit());
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(0).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(1).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getArguments().get(2).getType()));
    assertTrue(compareAbstract(cApps(cVar("A"), cVar("x"), cVar("y"), cVar("z")), pi.getArguments().get(4).getType()));
    assertTrue(compareAbstract(cUniverse(1), pi.getCodomain()));
  }

  @Test
  public void parserCase() {
    parseExpr("\\case 2 | zero => zero | suc x' => x'");
  }

  @Test
  public void elimManyMismatch() {
    parseExpr(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function tests (n : Nat) (d : D n) : Nat <= \\elim n d\n" +
          "| suc n => 0", 1);
  }

  @Test
  public void parseIncorrectPi() {
    parseExpr("\\Pi (: Nat) -> Nat", 2);
  }

  @Ignore("#29")
  @Test
  public void whereAbstractError() {
    parseClass("test", "\\function f => 0 \\where \\abstract x : \\Type0", 1);
  }
}
