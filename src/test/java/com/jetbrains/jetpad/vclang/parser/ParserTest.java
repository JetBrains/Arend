package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr("\\lam x => \\let | x => Nat \\in x x");
    Concrete.Expression expr1 = parseExpr("\\let | x => Nat \\in \\lam x => x x");
    assertTrue(compare(cLam("x", cLet(clets(clet("x", cargs(), cNat())), cApps(cVar("x"), cVar("x")))), expr));
    assertTrue(compare(cLet(clets(clet("x", cargs(), cNat())), cLam("x", cApps(cVar("x"), cVar("x")))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr("\\let | x => Nat | y => x \\in y");
    assertTrue(compare(cLet(clets(clet("x", cNat()), clet("y", cVar("x"))), cVar("y")), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr("\\let | x : Nat => zero \\in x");
    assertTrue(compare(cLet(clets(clet("x", cargs(), cNat(), Abstract.Definition.Arrow.RIGHT, cZero())), cVar("x")), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr("\\lam x y z => y");
    assertTrue(compare(cLam(cargs(cName("x"), cName("y"), cName("z")), cVar("y")), expr));
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(cLam(cargs(cName("x"), cName("y")), cApps(cLam(cargs(cName("z"), cName("w")), cApps(cVar("y"), cVar("z"))), cVar("y"))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr("\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(cLam(cargs(cName("p"), cTele(false, cvars("x", "t"), cNat()), cName(false, "y"), cTele(cvars("a"), cPi(cNat(), cNat()))), cApps(cLam(cargs(cTele(cvars("z", "w"), cNat())), cApps(cVar("y"), cVar("z"))), cVar("y"))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr("\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(cPi(ctypeArgs(cTele(cvars("x", "y", "z"), cNat()), cTele(cvars("w", "t"), cPi(cNat(), cNat()))), cPi(ctypeArgs(cTele(cvars("a", "b"), cPi("c", cNat(), cApps(cNat(), cVar("c"))))), cApps(cNat(), cVar("b"), cVar("y"), cVar("w")))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr("\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(cPi(ctypeArgs(cTele(cvars("x", "y"), cNat()), cTele(cvars("z"), cPi(cApps(cNat(), cVar("x")), cApps(cNat(), cVar("y"))))), cApps(cNat(), cVar("z"), cVar("y"), cVar("x"))), expr));
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
    Concrete.AbstractDefinition def = (Concrete.AbstractDefinition) parseDef("\\abstract f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} (A : Nat -> Nat -> Nat -> Nat -> Nat -> Nat -> \\Type0) : A x y z w t r");
    assertEquals(5, def.getArguments().size());
    assertTrue(def.getArguments().get(0).getExplicit());
    assertFalse(def.getArguments().get(1).getExplicit());
    assertTrue(def.getArguments().get(2).getExplicit());
    assertFalse(def.getArguments().get(3).getExplicit());
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(3)).getType()));
    assertTrue(compare(cApps(cVar("A"), cVar("x"), cVar("y"), cVar("z"), cVar("w"), cVar("t"), cVar("r")), def.getResultType()));
  }

  @Test
  public void parserImplicit2() {
    Concrete.AbstractDefinition def = (Concrete.AbstractDefinition) parseDef("\\abstract f {x : Nat} (_ : Nat) {y z : Nat} (A : Nat -> Nat -> Nat -> \\Type0) (_ : A x y z) : Nat");
    assertEquals(5, def.getArguments().size());
    assertFalse(def.getArguments().get(0).getExplicit());
    assertTrue(def.getArguments().get(1).getExplicit());
    assertFalse(def.getArguments().get(2).getExplicit());
    assertTrue(def.getArguments().get(3).getExplicit());
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(cNat(), ((Concrete.TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(cApps(cVar("A"), cVar("x"), cVar("y"), cVar("z")), ((Concrete.TypeArgument) def.getArguments().get(4)).getType()));
    assertTrue(compare(cNat(), def.getResultType()));
  }

  @Test
  public void parserCase() {
    parseExpr("\\case 2 | zero => zero | suc x' => x'");
  }

  @Test
  public void elimManyMistmatch() {
    parseExpr(
        "\\static \\data D Nat | D (suc n) => dsuc\n" +
        "\\static \\function tests (n : Nat) (d : D n) : Nat <= \\elim n d\n" +
          "| suc n => 0", 1);
  }
}
