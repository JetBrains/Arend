package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteGlobalReferable;
import com.jetbrains.jetpad.vclang.frontend.reference.ParsedLocalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.jetbrains.jetpad.vclang.frontend.ConcreteExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest extends NameResolverTestCase {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x => \\let | x => \\Type0 \\in x x");
    Concrete.Expression expr1 = resolveNamesExpr("\\let | x => \\Type0 \\in \\lam x => x x");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable x1 = ref("x");
    Concrete.LetClause xClause = clet(x1, cargs(), cUniverseStd(0));
    assertTrue(compareAbstract(cLam(cName(x), cLet(clets(xClause), cApps(cVar(x1), cVar(x1)))), expr));
    assertTrue(compareAbstract(cLet(clets(xClause), cLam(cName(x), cApps(cVar(x), cVar(x)))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = resolveNamesExpr("\\let | x => \\Type0 | y => x \\in y");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    Concrete.LetClause xClause = clet(x, cUniverseStd(0));
    Concrete.LetClause yClause = clet(y, cVar(x));
    assertTrue(compareAbstract(cLet(clets(xClause, yClause), cVar(y)), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = resolveNamesExpr("\\let | x : \\Type1 => \\Type0 \\in x");
    ParsedLocalReferable x = ref("x");
    Concrete.LetClause xClause = clet(x, cargs(), cUniverseStd(1), cUniverseStd(0));
    assertTrue(compareAbstract(cLet(clets(xClause), cVar(x)), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x y z => y");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    boolean res = compareAbstract(cLam(cargs(cName(x), cName(y), cName(z)), cVar(y)), expr);
    assertTrue(res);
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = resolveNamesExpr("\\lam x y => (\\lam z w => y z) y");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    ParsedLocalReferable w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(cName(x), cName(y)), cApps(cLam(cargs(cName(z), cName(w)), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = resolveNamesExpr("\\lam p {x t : \\Type0} {y} (a : \\Type0 -> \\Type0) => (\\lam (z w : \\Type0) => y z) y");
    ParsedLocalReferable p = ref("p");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable t = ref("t");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable a = ref("a");
    ParsedLocalReferable z = ref("z");
    ParsedLocalReferable w = ref("w");
    assertTrue(compareAbstract(cLam(cargs(cName(p), cTele(false, cvars(x, t), cUniverseStd(0)), cName(false, y), cTele(cvars(a), cPi(cUniverseStd(0), cUniverseStd(0)))), cApps(cLam(cargs(cTele(cvars(z, w), cUniverseStd(0))), cApps(cVar(y), cVar(z))), cVar(y))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = resolveNamesExpr("\\Pi (x y z : \\Type0) (w t : \\Type0 -> \\Type0) -> \\Pi (a b : \\Pi (c : \\Type0) -> x c) -> x b y w");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    ParsedLocalReferable w = ref("w");
    ParsedLocalReferable t = ref("t");
    ParsedLocalReferable a = ref("a");
    ParsedLocalReferable b = ref("b");
    ParsedLocalReferable c = ref("c");
    assertTrue(compareAbstract(cPi(ctypeArgs(cTele(cvars(x, y, z), cUniverseStd(0)), cTele(cvars(w, t), cPi(cUniverseStd(0), cUniverseStd(0)))), cPi(ctypeArgs(cTele(cvars(a, b), cPi(c, cUniverseStd(0), cApps(cVar(x), cVar(c))))), cApps(cVar(x), cVar(b), cVar(y), cVar(w)))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = resolveNamesExpr("\\Pi (x y : \\Type0) (z : x x -> y y) -> z z y x");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
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
    Concrete.ClassField def = ((Concrete.ClassDefinition) resolveNamesDef("\\class X { | f : \\Pi (x y : \\Type1) {z w : \\Type1} (t : \\Type1) {r : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) -> A x y z w t r }").getDefinition()).getFields().get(0);
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertTrue(pi.getParameters().get(0).getExplicit());
    assertFalse(pi.getParameters().get(1).getExplicit());
    assertTrue(pi.getParameters().get(2).getExplicit());
    assertFalse(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    ParsedLocalReferable A = ref("A");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
    ParsedLocalReferable w = ref("w");
    ParsedLocalReferable t = ref("t");
    ParsedLocalReferable r = ref("r");
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
    Concrete.ClassField def = ((Concrete.ClassDefinition) resolveNamesDef("\\class X { | f : \\Pi {x : \\Type1} (_ : \\Type1) {y z : \\Type1} (A : \\Type1 -> \\Type1 -> \\Type1 -> \\Type0) (_ : A x y z) -> \\Type1 }").getDefinition()).getFields().get(0);
    Concrete.PiExpression pi = (Concrete.PiExpression) def.getResultType();
    assertEquals(5, pi.getParameters().size());
    assertFalse(pi.getParameters().get(0).getExplicit());
    assertTrue(pi.getParameters().get(1).getExplicit());
    assertFalse(pi.getParameters().get(2).getExplicit());
    assertTrue(pi.getParameters().get(3).getExplicit());
    assertTrue(pi.getParameters().get(4).getExplicit());
    ParsedLocalReferable A = ref("A");
    ParsedLocalReferable x = ref("x");
    ParsedLocalReferable y = ref("y");
    ParsedLocalReferable z = ref("z");
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
    parseModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function f => \\case 2 \\with { zero => zero | suc x' => x' }");
  }

  @Test
  public void parseCaseFail() {
    parseModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\function f => \\case 2 | zero => zero | suc x' => x'", -1);
  }

  @Test
  public void parseIncorrectPi() {
    parseExpr("\\Pi (: Nat) -> Nat", 1);
  }

  @Test
  public void whereFieldError() {
    parseModule("\\function f => 0 \\where | x : \\Type0", 1);
  }

  @Test
  public void implementInFunctionError() {
    parseModule(
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
    parseModule(
      "\\function test (n : Nat)\n" +
      "  | zero => \\lam _ -> zero", 1);
  }

  private void postfixTest(String name) {
    Group module = resolveNamesModule(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infixl 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => A $ B `" + name + " $ C");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable named = it.next().getReferable();
    GlobalReferable d = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    List<? extends Referable> refs = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList();
    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(d), cApps(cVar(named), cApps(cVar(d), cVar(refs.get(0)), cVar(refs.get(1)))), cVar(refs.get(2)));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
  }

  @Test
  public void postfixTest() {
    postfixTest("#");
    postfixTest("foo");
  }

  private void postfixError(String name) {
    resolveNamesModule(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B `" + name + "", 1);
  }

  @Test
  public void postfixError() {
    postfixError("#");
    errorList.clear();
    postfixError("foo");
  }

  private void postfixTest2(String name) {
    Group module = resolveNamesModule(
      "\\function \\infix 5 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infixr 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => A $ B `" + name + " $ C");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable named = it.next().getReferable();
    GlobalReferable d = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    List<? extends Referable> refs = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList();
    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(d), cVar(refs.get(0)), cApps(cVar(d), cApps(cVar(named), cVar(refs.get(1))), cVar(refs.get(2))));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
  }

  @Test
  public void postfixTest2() {
    postfixTest2("#");
    postfixTest2("foo");
  }

  private void postfixTest3(String name) {
    Group module = resolveNamesModule(
      "\\function \\infix 6 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B `" + name);
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable named = it.next().getReferable();
    GlobalReferable d = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    List<? extends Referable> refs = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList();
    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(d), cVar(refs.get(0)), cApps(cVar(named), cVar(refs.get(1))));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
  }

  @Test
  public void postfixTest3() {
    postfixTest3("#");
    postfixTest3("foo");
  }

  private void postfixTest4(String name) {
    Group module = resolveNamesModule(
      "\\function \\infix 4 " + name + " (A : \\Prop) => A\n" +
      "\\function \\infix 5 $ (A B : \\Prop) => A\n" +
      "\\function f (A B : \\Prop) => A $ B `" + name);
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable named = it.next().getReferable();
    GlobalReferable d = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    List<? extends Referable> refs = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList();
    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(named), cApps(cVar(d), cVar(refs.get(0)), cVar(refs.get(1))));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
  }

  @Test
  public void postfixTest4() {
    postfixTest4("#");
    postfixTest4("foo");
  }

  private void postfixTest5(String name1, String name2, String pr1, String pr2) {
    Group module = resolveNamesModule(
      "\\function " + pr1 + " " + name1 + " (A : \\Prop) => A\n" +
      "\\function " + pr2 + " " + name2 + " (A : \\Prop) => A\n" +
      "\\function f (A : \\Prop) => A `" + name1 + " `" + name2);
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable named1 = it.next().getReferable();
    GlobalReferable named2 = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    Referable refA = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList().get(0);
    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(named2), cApps(cVar(named1), cVar(refA)));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
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

  @Test
  public void keyword() {
    parseExpr("\\lamx => x", 1);
  }

  @Test
  public void postfixTest6() {
    Group module = resolveNamesModule(
      "\\function \\infixr 1 >== (A B : \\Prop) => A\n" +
      "\\function \\infix 2 ==< (A B : \\Prop) => A\n" +
      "\\function \\infix 2 qed (A : \\Prop) => A\n" +
      "\\function g (A : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => g A ==< g B >== g C `qed");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable rightP = it.next().getReferable();
    GlobalReferable leftP = it.next().getReferable();
    GlobalReferable qed = it.next().getReferable();
    GlobalReferable g = it.next().getReferable();
    Concrete.FunctionDefinition function = (Concrete.FunctionDefinition) ((ConcreteGlobalReferable) it.next().getReferable()).getDefinition();
    List<? extends Referable> refs = ((Concrete.TelescopeParameter) function.getParameters().get(0)).getReferableList();

    Concrete.Expression actualTerm = ((Concrete.TermFunctionBody) function.getBody()).getTerm();
    Concrete.Expression expectedTerm = cApps(cVar(rightP), cApps(cVar(leftP), cApps(cVar(g), cVar(refs.get(0))), cApps(cVar(g), cVar(refs.get(1)))), cApps(cVar(qed), cApps(cVar(g), cVar(refs.get(2)))));
    assertTrue(compareAbstract(actualTerm, expectedTerm));
  }

  @Test
  public void infixTest() {
    resolveNamesModule(
      "\\function \\infixr 1 >== (A B : \\Prop) => A\n" +
      "\\function \\infix 2 ==< (A B : \\Prop) => A\n" +
      "\\function \\infix 2 qed (A : \\Prop) => A\n" +
      "\\function g (A : \\Prop) => A\n" +
      "\\function f (A B C : \\Prop) => g A ==< g B >== g C qed", 1);
  }
}
