package com.jetbrains.jetpad.vclang.frontend.parser;

import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest extends ParserTestCase {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr("\\lam x => \\let | x => \\Type0 \\in x x");
    Concrete.Expression expr1 = parseExpr("\\let | x => \\Type0 \\in \\lam x => x x");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode x1 = ref("x");
    assertTrue(compareAbstract(cLam(x, cLet(clets(clet(x1.getName(), cargs(), cUniverseStd(0))), cApps(cVar(x1), cVar(x1)))), expr));
    assertTrue(compareAbstract(cLet(clets(clet(x1.getName(), cargs(), cUniverseStd(0))), cLam(x, cApps(cVar(x), cVar(x)))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr("\\let | x => \\Type0 | y => x \\in y");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    assertTrue(compareAbstract(cLet(clets(clet(x.getName(), cUniverseStd(0)), clet(y.getName(), cVar(x))), cVar(y)), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr("\\let | x : \\Type1 => \\Type0 \\in x");
    Concrete.ReferableSourceNode x = ref("x");
    assertTrue(compareAbstract(cLet(clets(clet(x.getName(), cargs(), cUniverseStd(1), Abstract.Definition.Arrow.RIGHT, cUniverseStd(0))), cVar(x)), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr("\\lam x y z => y");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    boolean res = compareAbstract(cLam(cargs(cName(x), cName(y), cName(z)), cVar(y)), expr);
    assertTrue(res);
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.ReferableSourceNode w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(cName(x), cName(y)), cApps(cLam(cargs(cName(z), cName(w)), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr("\\lam p {x t : \\Type0} {y} (a : \\Type0 -> \\Type0) => (\\lam (z w : \\Type0) => y z) y");
    Concrete.ReferableSourceNode p = ref("p");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode t = ref("t");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode a = ref("a");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.ReferableSourceNode w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(cName(p), cTele(false, cvars(x, t), cUniverseStd(0)), cName(false, y), cTele(cvars(a), cPi(cUniverseStd(0), cUniverseStd(0)))), cApps(cLam(cargs(cTele(cvars(z, w), cUniverseStd(0))), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr("\\Pi (x y z : \\Type0) (w t : \\Type0 -> \\Type0) -> \\Pi (a b : \\Pi (c : \\Type0) -> x c) -> x b y w");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.ReferableSourceNode w = ref("w");
    Concrete.ReferableSourceNode t = ref("t");
    Concrete.ReferableSourceNode a = ref("a");
    Concrete.ReferableSourceNode b = ref("b");
    Concrete.ReferableSourceNode c = ref("c");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y, z), cUniverseStd(0)), cTele(cvars(w, t), cPi(cUniverseStd(0), cUniverseStd(0)))), cPi(ctypeArgs(cTele(cvars(a, b), cPi(c, cUniverseStd(0), cApps(cVar(x), cVar(c))))), cApps(cVar(x), cVar(b), cVar(y), cVar(w)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr("\\Pi (x y : \\Type0) (z : x x -> y y) -> z z y x");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y), cUniverseStd(0)), cTele(cvars(z), cPi(cApps(cVar(x), cVar(x)), cApps(cVar(y), cVar(y))))), cApps(cVar(z), cVar(z), cVar(y), cVar(x))), expr));
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
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(0).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(1).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(2).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(3).getType()));
    Concrete.ReferableSourceNode A = ref("A");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    Concrete.ReferableSourceNode w = ref("w");
    Concrete.ReferableSourceNode t = ref("t");
    Concrete.ReferableSourceNode r = ref("r");
    assertTrue(compareAbstract(cApps(cVar(A), cVar(x), cVar(y), cVar(z), cVar(w), cVar(t), cVar(r)), pi.getCodomain()));
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
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(0).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(1).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getArguments().get(2).getType()));
    Concrete.ReferableSourceNode A = ref("A");
    Concrete.ReferableSourceNode x = ref("x");
    Concrete.ReferableSourceNode y = ref("y");
    Concrete.ReferableSourceNode z = ref("z");
    assertTrue(compareAbstract(cApps(cVar(A), cVar(x), cVar(y), cVar(z)), pi.getArguments().get(4).getType()));
    assertTrue(compareAbstract(cUniverseStd(1), pi.getCodomain()));
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

  @Test
  public void whereAbstractError() {
    parseClass("test", "\\function f => 0 \\where \\field x : \\Type0", 1);
  }

  @Test
  public void implementInFunctionError() {
    parseClass("test",
        "\\class X {\n" +
        "  \\field x : Nat\n" +
        "} \\where {\n" +
        "  \\function f => 0\n" +
        "    \\where\n" +
        "      \\implement x => 1\n" +
        "}", 1);
  }

  @Test
  public void incorrectDefinitionName() {
    parseDef("\\function (|) => \\Prop", 1);
  }
}
