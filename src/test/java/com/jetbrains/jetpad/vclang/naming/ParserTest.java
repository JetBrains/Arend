package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.frontend.parser.Position;
import com.jetbrains.jetpad.vclang.frontend.reference.LocalReference;
import com.jetbrains.jetpad.vclang.term.Concrete;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest extends NameResolverTestCase {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\lam x => \\let | x => \\Type0 \\in x x");
    Concrete.Expression<Position> expr1 = resolveNamesExpr("\\let | x => \\Type0 \\in \\lam x => x x");
    Concrete.NameParameter<Position> x = cName("x");
    LocalReference x1 = ref("x");
    Concrete.LetClause<Position> xClause = clet(x1, cargs(), cUniverseStd(0));
    assertTrue(compareAbstract(cLam(x, cLet(clets(xClause), cApps(cVar(x1), cVar(x1)))), expr));
    assertTrue(compareAbstract(cLet(clets(xClause), cLam(x, cApps(cVar(x), cVar(x)))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\let | x => \\Type0 | y => x \\in y");
    LocalReference x = ref("x");
    LocalReference y = ref("y");
    Concrete.LetClause<Position> xClause = clet(x, cUniverseStd(0));
    Concrete.LetClause<Position> yClause = clet(y, cVar(x));
    assertTrue(compareAbstract(cLet(clets(xClause, yClause), cVar(y)), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\let | x : \\Type1 => \\Type0 \\in x");
    LocalReference x = ref("x");
    Concrete.LetClause<Position> xClause = clet(x, cargs(), cUniverseStd(1), cUniverseStd(0));
    assertTrue(compareAbstract(cLet(clets(xClause), cVar(x)), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\lam x y z => y");
    Concrete.NameParameter<Position> x = cName("x");
    Concrete.NameParameter<Position> y = cName("y");
    Concrete.NameParameter<Position> z = cName("z");
    boolean res = compareAbstract(cLam(cargs(x, y, z), cVar(y)), expr);
    assertTrue(res);
  }

  @Test
  public void parserLam2() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\lam x y => (\\lam z w => y z) y");
    Concrete.NameParameter<Position> x = cName("x");
    Concrete.NameParameter<Position> y = cName("y");
    Concrete.NameParameter<Position> z = cName("z");
    Concrete.NameParameter<Position> w = cName("w");
    assertTrue(compareAbstract(cLam(cargs(x, y), cApps(cLam(cargs(z, w), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\lam p {x t : \\Type0} {y} (a : \\Type0 -> \\Type0) => (\\lam (z w : \\Type0) => y z) y");
    Concrete.NameParameter<Position> p = cName("p");
    LocalReference x = ref("x");
    LocalReference t = ref("t");
    Concrete.NameParameter<Position> y = cName(false, "y");
    LocalReference a = ref("a");
    LocalReference z = ref("z");
    LocalReference w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(p, cTele(false, cvars(x, t), cUniverseStd(0)), y, cTele(cvars(a), cPi(cUniverseStd(0), cUniverseStd(0)))), cApps(cLam(cargs(cTele(cvars(z, w), cUniverseStd(0))), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\Pi (x y z : \\Type0) (w t : \\Type0 -> \\Type0) -> \\Pi (a b : \\Pi (c : \\Type0) -> x c) -> x b y w");
    LocalReference x = ref("x");
    LocalReference y = ref("y");
    LocalReference z = ref("z");
    LocalReference w = ref("w");
    LocalReference t = ref("t");
    LocalReference a = ref("a");
    LocalReference b = ref("b");
    LocalReference c = ref("c");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y, z), cUniverseStd(0)), cTele(cvars(w, t), cPi(cUniverseStd(0), cUniverseStd(0)))), cPi(ctypeArgs(cTele(cvars(a, b), cPi(c, cUniverseStd(0), cApps(cVar(x), cVar(c))))), cApps(cVar(x), cVar(b), cVar(y), cVar(w)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression<Position> expr = resolveNamesExpr("\\Pi (x y : \\Type0) (z : x x -> y y) -> z z y x");
    LocalReference x = ref("x");
    LocalReference y = ref("y");
    LocalReference z = ref("z");
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
    Concrete.ClassField<Position> def = ((Concrete.ClassDefinition<Position>) resolveNamesDef("\\class X { | f : \\Pi (x y : \\Type1) {z w : \\Type1} (t : \\Type1) {r : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) -> A x y z w t r }")).getFields().get(0);
    Concrete.PiExpression<Position> pi = (Concrete.PiExpression<Position>) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertTrue(pi.getParameters().get(0).getExplicit());
    assertFalse(pi.getParameters().get(1).getExplicit());
    assertTrue(pi.getParameters().get(2).getExplicit());
    assertFalse(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    LocalReference A = ref("A");
    LocalReference x = ref("x");
    LocalReference y = ref("y");
    LocalReference z = ref("z");
    LocalReference w = ref("w");
    LocalReference t = ref("t");
    LocalReference r = ref("r");
    List<Concrete.TypeParameter<Position>> params = new ArrayList<>();
    params.add(cTele(cvars(x, y), cUniverseStd(1)));
    params.add(cTele(false, cvars(z, w), cUniverseStd(1)));
    params.add(cTele(cvars(t), cUniverseStd(1)));
    params.add(cTele(false, cvars(r), cUniverseStd(1)));
    params.add(cTele(cvars(A), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cPi(cUniverseStd(1), cUniverseStd(0)))))))));
    assertTrue(compareAbstract(cPi(params, cApps(cVar(A), cVar(x), cVar(y), cVar(z), cVar(w), cVar(t), cVar(r))), pi));
  }

  @Test
  public void parserImplicit2() {
    Concrete.ClassField<Position> def = ((Concrete.ClassDefinition<Position>) resolveNamesDef("\\class X { | f : \\Pi {x : \\Type1} (_ : \\Type1) {y z : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) (_ : A x y z) -> \\Type1 }")).getFields().get(0);
    Concrete.PiExpression<Position> pi = (Concrete.PiExpression<Position>) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertFalse(pi.getParameters().get(0).getExplicit());
    assertTrue(pi.getParameters().get(1).getExplicit());
    assertFalse(pi.getParameters().get(2).getExplicit());
    assertTrue(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    LocalReference A = ref("A");
    LocalReference x = ref("x");
    LocalReference y = ref("y");
    LocalReference z = ref("z");
    List<Concrete.TypeParameter<Position>> params = new ArrayList<>();
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
      "\\function f => \\case 2 \\with { zero => zero | suc x' => x' }");
  }

  @Test
  public void parseCaseFail() {
    parseClass("test",
      "\\data Nat | zero | suc Nat\n" +
      "\\function f => \\case 2 | zero => zero | suc x' => x'", -1);
  }

  @Test
  public void parseIncorrectPi() {
    parseExpr("\\Pi (: Nat) -> Nat", 1);
  }

  @Test
  public void whereFieldError() {
    parseClass("test", "\\function f => 0 \\where | x : \\Type0", 1);
  }

  @Test
  public void implementInFunctionError() {
    parseClass("test",
        "\\class X {\n" +
        "  | x : Nat\n" +
        "} \\where {\n" +
        "  \\function f => 0\n" +
        "    \\where\n" +
        "      | x => 1\n" +
        "}", 1);
  }

  @Test
  public void incorrectDefinitionName() {
    parseDef("\\function | => \\Prop", -1);
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
      "  | zero => \\lam _ -> zero", 1);
  }

  private void postfixTest(String name) {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infixl 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => A $ B " + name + "` $ C");
    Concrete.BinOpSequenceExpression expr = (Concrete.BinOpSequenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((Concrete.DefineStatement) classDef.getGlobalStatements().get(2)).getDefinition()).getBody()).getTerm();
    assertEquals(0, expr.getSequence().size());
    assertTrue(expr.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals("$", ((Concrete.BinOpExpression) expr.getLeft()).getReferent().getName());
    assertTrue(((Concrete.BinOpExpression) expr.getLeft()).getLeft() instanceof Concrete.BinOpExpression);
    assertEquals(name, ((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getReferent().getName());
    assertNull(((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getRight());
    assertTrue(((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getLeft() instanceof Concrete.BinOpExpression);
    assertEquals("$", ((Concrete.BinOpExpression) ((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getLeft()).getReferent().getName());
  }

  @Test
  public void postfixTest() {
    postfixTest("#");
    postfixTest("foo");
  }

  private void postfixError(String name) {
    resolveNamesClass(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B " + name + "`", 1);
  }

  @Test
  public void postfixError() {
    postfixError("#");
    errorList.clear();
    postfixError("foo");
  }

  private void postfixTest2(String name) {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infixr 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => A $ B " + name + "` $ C");
    Concrete.BinOpSequenceExpression expr = (Concrete.BinOpSequenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((Concrete.DefineStatement) classDef.getGlobalStatements().get(2)).getDefinition()).getBody()).getTerm();
    assertEquals(0, expr.getSequence().size());
    assertTrue(expr.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals("$", ((Concrete.BinOpExpression) expr.getLeft()).getReferent().getName());
    assertTrue(((Concrete.BinOpExpression) expr.getLeft()).getRight() instanceof Concrete.BinOpExpression);
    Concrete.Expression expr1 = ((Concrete.BinOpExpression) expr.getLeft()).getRight();
    assertNotNull(expr1);
    assertEquals("$", ((Concrete.BinOpExpression) expr1).getReferent().getName());
    Concrete.BinOpExpression expr2 = (Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getRight();
    assertNotNull(expr2);
    assertTrue(expr2.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals(name, ((Concrete.BinOpExpression) expr2.getLeft()).getReferent().getName());
    assertNull(((Concrete.BinOpExpression) expr2.getLeft()).getRight());
  }

  @Test
  public void postfixTest2() {
    postfixTest2("#");
    postfixTest2("foo");
  }

  private void postfixTest3(String name) {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\function \\infix 6 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B " + name + "`");
    Concrete.BinOpSequenceExpression expr = (Concrete.BinOpSequenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((Concrete.DefineStatement) classDef.getGlobalStatements().get(2)).getDefinition()).getBody()).getTerm();
    assertEquals(0, expr.getSequence().size());
    assertTrue(expr.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals("$", ((Concrete.BinOpExpression) expr.getLeft()).getReferent().getName());
    assertTrue(((Concrete.BinOpExpression) expr.getLeft()).getRight() instanceof Concrete.BinOpExpression);
    Concrete.BinOpExpression expr1 = (Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getRight();
    assertNotNull(expr1);
    assertEquals(name, expr1.getReferent().getName());
    assertNull(expr1.getRight());
  }

  @Test
  public void postfixTest3() {
    postfixTest3("#");
    postfixTest3("foo");
  }

  private void postfixTest4(String name) {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\function \\infix 4 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B " + name + "`");
    Concrete.BinOpSequenceExpression expr = (Concrete.BinOpSequenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((Concrete.DefineStatement) classDef.getGlobalStatements().get(2)).getDefinition()).getBody()).getTerm();
    assertEquals(0, expr.getSequence().size());
    assertTrue(expr.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals(name, ((Concrete.BinOpExpression) expr.getLeft()).getReferent().getName());
    assertTrue(((Concrete.BinOpExpression) expr.getLeft()).getLeft() instanceof Concrete.BinOpExpression);
    assertNull(((Concrete.BinOpExpression) expr.getLeft()).getRight());
    assertEquals("$", ((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getReferent().getName());
  }

  @Test
  public void postfixTest4() {
    postfixTest4("#");
    postfixTest4("foo");
  }

  private void postfixTest5(String name1, String name2, String pr1, String pr2) {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\function " + pr1 + " " + name1 + " (A : \\Prop) => A\n" +
      "\\function " + pr2 + " " + name2 + " (A : \\Prop) => A\n" +
      "\\function f (A : \\Prop) => A " + name1 + "` " + name2 + "`");
    Concrete.BinOpSequenceExpression expr = (Concrete.BinOpSequenceExpression) ((Concrete.TermFunctionBody) ((Concrete.FunctionDefinition) ((Concrete.DefineStatement) classDef.getGlobalStatements().get(2)).getDefinition()).getBody()).getTerm();
    assertEquals(0, expr.getSequence().size());
    assertTrue(expr.getLeft() instanceof Concrete.BinOpExpression);
    assertEquals(name2, ((Concrete.BinOpExpression) expr.getLeft()).getReferent().getName());
    assertNull(((Concrete.BinOpExpression) expr.getLeft()).getRight());
    assertTrue(((Concrete.BinOpExpression) expr.getLeft()).getLeft() instanceof Concrete.BinOpExpression);
    assertEquals(name1, ((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getReferent().getName());
    assertNull(((Concrete.BinOpExpression) ((Concrete.BinOpExpression) expr.getLeft()).getLeft()).getRight());
  }

  @Test
  public void postfixTest5() {
    postfixTest5("#", "$", "\\infix 5", "\\infix 5");
    postfixTest5("foo", "$", "\\infix 6", "\\infix 5");
    postfixTest5("#", "bar", "\\infix 5", "\\infix 6");
    postfixTest5("foo", "bar", "\\infixl 5", "\\infix 5");
    postfixTest5("foo", "$", "\\infix 5", "\\infixl 5");
    postfixTest5("#", "bar", "\\infixr 5", "\\infixl 5");
    postfixTest5("foo", "$", "\\infixl 6", "\\infixr 5");
    postfixTest5("#", "$", "\\infixl 5", "\\infixr 6");
  }
}
