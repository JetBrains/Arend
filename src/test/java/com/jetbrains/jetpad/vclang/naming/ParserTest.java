package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.frontend.Concrete;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest extends NameResolverTestCase {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x => \\let | x => \\Type0 \\in x x");
    Concrete.Expression expr1 = resolveNamesExpr("\\let | x => \\Type0 \\in \\lam x => x x");
    Concrete.NameParameter x = cName("x");
    Concrete.LetClause x1 = clet("x", cargs(), cUniverseStd(0));
    assertTrue(compareAbstract(cLam(x, cLet(clets(x1), cApps(cVar(x1), cVar(x1)))), expr));
    assertTrue(compareAbstract(cLet(clets(x1), cLam(x, cApps(cVar(x), cVar(x)))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = resolveNamesExpr("\\let | x => \\Type0 | y => x \\in y");
    Concrete.LetClause x = clet("x", cUniverseStd(0));
    Concrete.LetClause y = clet("y", cVar(x));
    assertTrue(compareAbstract(cLet(clets(x, y), cVar(y)), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = resolveNamesExpr("\\let | x : \\Type1 => \\Type0 \\in x");
    Concrete.LetClause x = clet("x", cargs(), cUniverseStd(1), cUniverseStd(0));
    assertTrue(compareAbstract(cLet(clets(x), cVar(x)), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x y z => y");
    Concrete.NameParameter x = cName("x");
    Concrete.NameParameter y = cName("y");
    Concrete.NameParameter z = cName("z");
    boolean res = compareAbstract(cLam(cargs(x, y, z), cVar(y)), expr);
    assertTrue(res);
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x y => (\\lam z w => y z) y");
    Concrete.NameParameter x = cName("x");
    Concrete.NameParameter y = cName("y");
    Concrete.NameParameter z = cName("z");
    Concrete.NameParameter w = cName("w");
    assertTrue(compareAbstract(cLam(cargs(x, y), cApps(cLam(cargs(z, w), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = resolveNamesExpr("\\lam p {x t : \\Type0} {y} (a : \\Type0 -> \\Type0) => (\\lam (z w : \\Type0) => y z) y");
    Concrete.NameParameter p = cName("p");
    Concrete.LocalVariable x = ref("x");
    Concrete.LocalVariable t = ref("t");
    Concrete.NameParameter y = cName(false, "y");
    Concrete.LocalVariable a = ref("a");
    Concrete.LocalVariable z = ref("z");
    Concrete.LocalVariable w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(p, cTele(false, cvars(x, t), cUniverseStd(0)), y, cTele(cvars(a), cPi(cUniverseStd(0), cUniverseStd(0)))), cApps(cLam(cargs(cTele(cvars(z, w), cUniverseStd(0))), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = resolveNamesExpr("\\Pi (x y z : \\Type0) (w t : \\Type0 -> \\Type0) -> \\Pi (a b : \\Pi (c : \\Type0) -> x c) -> x b y w");
    Concrete.LocalVariable x = ref("x");
    Concrete.LocalVariable y = ref("y");
    Concrete.LocalVariable z = ref("z");
    Concrete.LocalVariable w = ref("w");
    Concrete.LocalVariable t = ref("t");
    Concrete.LocalVariable a = ref("a");
    Concrete.LocalVariable b = ref("b");
    Concrete.LocalVariable c = ref("c");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y, z), cUniverseStd(0)), cTele(cvars(w, t), cPi(cUniverseStd(0), cUniverseStd(0)))), cPi(ctypeArgs(cTele(cvars(a, b), cPi(c, cUniverseStd(0), cApps(cVar(x), cVar(c))))), cApps(cVar(x), cVar(b), cVar(y), cVar(w)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = resolveNamesExpr("\\Pi (x y : \\Type0) (z : x x -> y y) -> z z y x");
    Concrete.LocalVariable x = ref("x");
    Concrete.LocalVariable y = ref("y");
    Concrete.LocalVariable z = ref("z");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y), cUniverseStd(0)), cTele(cvars(z), cPi(cApps(cVar(x), cVar(x)), cApps(cVar(y), cVar(y))))), cApps(cVar(z), cVar(z), cVar(y), cVar(x))), expr));
  }

  @Test
  public void parserLamOpenError() {
    assertNotNull(resolveNamesExpr("\\lam x => (\\Pi (y : \\Type0) -> (\\lam y => y)) y", 1));
  }

  @Test
  public void parserPiOpenError() {
    assertNotNull(resolveNamesExpr("\\Pi (X : \\Type) (a b : X a) -> X a b", 1));
  }

  @Test
  public void parserImplicit() {
    Concrete.ClassField def = (Concrete.ClassField) resolveNamesDef("\\field f : \\Pi (x y : \\Type1) {z w : \\Type1} (t : \\Type1) {r : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) -> A x y z w t r");
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertTrue(pi.getParameters().get(0).getExplicit());
    assertFalse(pi.getParameters().get(1).getExplicit());
    assertTrue(pi.getParameters().get(2).getExplicit());
    assertFalse(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    Concrete.LocalVariable A = ref("A");
    Concrete.LocalVariable x = ref("x");
    Concrete.LocalVariable y = ref("y");
    Concrete.LocalVariable z = ref("z");
    Concrete.LocalVariable w = ref("w");
    Concrete.LocalVariable t = ref("t");
    Concrete.LocalVariable r = ref("r");
    List<Concrete.TypeParameter> params = new ArrayList<>();
    params.add(cTele(cvars(x, y), cUniverseStd(1)));
    params.add(cTele(false, cvars(z, w), cUniverseStd(1)));
    params.add(cTele(cvars(t), cUniverseStd(1)));
    params.add(cTele(false, cvars(r), cUniverseStd(1)));
    params.add(cTele(cvars(A), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cUniverseStd(0)))))))));
    assertTrue(compareAbstract(cPi(params, cApps(cVar(A), cVar(x), cVar(y), cVar(z), cVar(w), cVar(t), cVar(r))), pi));
  }

  @Test
  public void parserImplicit2() {
    Concrete.ClassField def = (Concrete.ClassField) resolveNamesDef("\\field f : \\Pi {x : \\Type1} (_ : \\Type1) {y z : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) (_ : A x y z) -> \\Type1");
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertFalse(pi.getParameters().get(0).getExplicit());
    assertTrue(pi.getParameters().get(1).getExplicit());
    assertFalse(pi.getParameters().get(2).getExplicit());
    assertTrue(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    Concrete.LocalVariable A = ref("A");
    Concrete.LocalVariable x = ref("x");
    Concrete.LocalVariable y = ref("y");
    Concrete.LocalVariable z = ref("z");
    List<Concrete.TypeParameter> params = new ArrayList<>();
    params.add(cTele(false, cvars(x), cUniverseStd(1)));
    params.add(cTele(cvars(ref(null)), cUniverseStd(1)));
    params.add(cTele(false, cvars(y, z), cUniverseStd(1)));
    params.add(cTele(cvars(A), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cUniverseStd(0))))));
    params.add(cTele(cvars(ref(null)), cApps(cVar(A), cVar(x), cVar(y), cVar(z))));
    assertTrue(compareAbstract(cPi(params, cUniverseStd(1)), pi));
  }

  @Test
  public void parseCase() {
    parseClass("test",
      "\\data Nat | zero | suc Nat\n" +
      "\\function f => \\case 2 { zero => zero | suc x' => x' }");
  }

  @Test
  public void parseCaseFail() {
    parseClass("test",
      "\\data Nat | zero | suc Nat\n" +
      "\\function f => \\case 2 | zero => zero | suc x' => x'", -1);
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
    parseDef("\\function (|) => \\Prop", -1);
  }

  @Test
  public void lineComment() {
    parseDef("\\function f => -- ^_^ @_@ >.<\n  \\Prop");
  }

  @Test
  public void blockComment() {
    parseDef("\\function f => {- ^_^ @_@ >.< wow!!!-}\n  \\Prop");
  }

  @Test
  public void lineCommentLastLine() {
    parseDef("\\function f => \\Prop  -- ^_^ @_@ >.< wow!!!");
  }

  @Test
  public void elimUnderLetError() {
    parseDef("\\function test (n : Nat) : Nat => \\let x => 0 \\in \\elim n | _ => 0", 1);
  }

  @Test
  public void testSide() {
    parseDef("\\function test (n : Nat) => suc (\\elim n | suc n => n | zero => 0)", 1);
  }

  @Test
  public void insideBody() {
    parseClass("test",
      "\\function test (n : Nat)\n" +
      "  | zero => \\lam _ -> zero", 2);
  }
}
