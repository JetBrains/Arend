package org.arend.typechecking;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VarsTest extends TypeCheckingTestCase {
  @Test
  public void whereTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "  \\where\n" +
      "    \\func bar (p : x = x) => p");
  }

  @Test
  public void embeddedTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) => y\n" +
      "      \\where\n" +
      "        \\func test (p : x = y) => p");
  }

  @Test
  public void invokeTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => bar Nat.+ x\n" +
      "  \\where\n" +
      "    \\func bar => x\n" +
      "\\func test : foo 3 = foo.bar 6 => idp");
  }

  @Test
  public void invokeTest2() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz = x => idp\n" +
      "  \\where\n" +
      "    \\func bar : baz = x => idp\n" +
      "      \\where\n" +
      "        \\func baz => x");
  }

  @Test
  public void invokeTest3() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz 3 = (x,3) => idp\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) : baz = (x,y) => idp\n" +
      "      \\where\n" +
      "        \\func baz => (x,y)");
  }

  @Test
  public void invokeTest4() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz 3 = (x,3) => idp\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) : baz = {\\Sigma Nat Nat} (_,y) => idp\n" +
      "      \\where\n" +
      "        \\func baz => (x,y)");
  }

  @Test
  public void invokeTest5() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz = x => idp\n" +
      "  \\where\n" +
      "    \\func bar : baz = {Nat} _ => idp\n" +
      "      \\where\n" +
      "        \\func baz => x");
  }

  @Test
  public void invokeTest6() {
    typeCheckModule(
      "\\func bar : foo.baz = (\\lam x => x) => idp\n" +
      "  \\where\n" +
      "    \\func foo (x : Nat) : baz = x => idp\n" +
      "      \\where\n" +
      "        \\func baz => x\n");
  }

  @Test
  public void invokeTest7() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz 3 = idp {_} {x,3} => idp\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) : baz = idp {_} {x,y} => idp\n" +
      "      \\where\n" +
      "        \\func baz : (x,bak) = (x,y) => idp\n" +
      "          \\where\n" +
      "            \\func bak => y");
  }

  @Test
  public void invokeTest8() {
    typeCheckModule(
      "\\func foo (x : Nat) : bar.baz 3 = idp {_} {x,3} => idp\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) : baz = idp {_} {x,y} => idp\n" +
      "      \\where\n" +
      "        \\func baz : (x,bak) = (x,_) => idp\n" +
      "          \\where\n" +
      "            \\func bak => y");
  }

  @Test
  public void invokeTest9() {
    typeCheckModule(
      "\\func foo (x : Nat) => baz\n" +
      "  \\where {\n" +
      "    \\func baz => bar\n" +
      "    \\func bar => x\n" +
      "  }\n" +
      "\\func test : foo.baz 3 = foo.bar 3 => idp");
  }

  @Test
  public void invokeTest10() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "  \\where {\n" +
      "    \\func bar (y : Nat) => y\n" +
      "      \\where\n" +
      "        \\func bak => x Nat.+ y\n" +
      "    \\func baz (z : Nat) => z\n" +
      "      \\where\n" +
      "        \\func qux => bar.bak 5\n" +
      "  }\n" +
      "\\func test : foo.baz.qux 4 = 9 => idp");
  }

  @Test
  public void patternMatchingTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => bar 2 Nat.+ x\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) : Nat\n" +
      "      | 0 => 0\n" +
      "      | suc y => x Nat.* y\n" +
      "\\func test : foo 3 = foo.bar 2 4 => idp");
  }

  @Test
  public void recursiveTest() {
    typeCheckModule(
      "\\func foo (x y : Nat) => bar 4\n" +
      "  \\where\n" +
      "    \\func bar (n : Nat) : Nat\n" +
      "      | 0 => 0\n" +
      "      | suc n => bar n Nat.+ x\n" +
      "\\func test : (foo.bar 5 4, foo 3 100) = (20, 12) => idp");
  }

  @Test
  public void mutualRecursiveTest() {
    typeCheckModule(
      "\\func foo (x y : Nat) => baz\n" +
      "  \\where {\n" +
      "    \\func bar (n : Nat) : Nat\n" +
      "      | 0 => 0\n" +
      "      | suc n => baz n Nat.+ x\n" +
      "    \\func baz (n : Nat) : Nat\n" +
      "      | 0 => 0\n" +
      "      | suc n => bar n Nat.+ y\n" +
      "  }\n" +
      "\\func test : (foo.bar 5 7 1, foo.baz 5 7 1) = (5, 7) => idp");
  }

  @Test
  public void orderTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) => y\n" +
      "      \\where {\n" +
      "        \\func baz => (x,y)\n" +
      "        \\func qux => (y,x)\n" +
      "      }\n" +
      "\\func test : (foo.bar.baz 1 2, foo.bar.qux 3 4) = ((1,2),(4,3)) => idp");
  }

  @Test
  public void orderTest2() {
    typeCheckModule(
      "\\func foo (x : Nat) => x\n" +
      "  \\where\n" +
      "    \\func bar (y : Nat) => y\n" +
      "      \\where {\n" +
      "        \\func x' => x\n" +
      "        \\func y' => y\n" +
      "        \\func baz => (x',y')\n" +
      "        \\func qux => (y',x')\n" +
      "      }\n" +
      "\\func test : (foo.bar.baz 1 2, foo.bar.qux 3 4) = ((1,2),(4,3)) => idp");
  }

  @Test
  public void dynamicTest() {
    typeCheckModule(
      "\\record R {\n" +
      "  \\func foo (x : Nat) => bar\n" +
      "    \\where\n" +
      "      \\func bar => x\n" +
      "}\n" +
      "\\func test (r : R) : r.foo 7 = R.foo.bar {r} 7 => idp");
  }

  @Test
  public void levelsTest1() {
    typeCheckModule(
      "\\func foo (A : \\Type) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p1 >= p2 (a : A) => a");
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar").getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest2() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3\n" +
      "  \\where\n" +
      "    \\func bar (a : A) => a");
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar").getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest3() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type p1) => 3\n" +
      "  \\where\n" +
      "    \\func bar (a : A) => a");
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(bar.getLevelParameters().get(0)), new Level(LevelVariable.HVAR))), bar.getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest4() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p3 >= p4 (a : A) => a", -1);
  }

  @Test
  public void levelsTest5() {
    typeCheckModule(
      "\\plevels p1 >= p2\n" +
      "\\func foo (A : \\Type p1) => 3\n" +
      "  \\where\n" +
      "    \\func bar (a : A) => a");
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(bar.getLevelParameters().get(0)), new Level(LevelVariable.HVAR))), bar.getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest6() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p3 >= p4 (B : \\Type p4) => 4\n" +
      "      \\where\n" +
      "        \\func baz (a : A) (b : B) => 5", -1);
  }

  @Test
  public void levelsTest7() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p3 >= p4 (B : \\Type) (x : Nat) => 4\n" +
      "      \\where\n" +
      "        \\func baz (a : A) => x");
  }

  @Test
  public void levelsTest8() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type) (x : Nat) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p3 >= p4 (B : \\Type) => 4\n" +
      "      \\where\n" +
      "        \\func baz (b : B) => x");
  }

  @Test
  public void levelsTest9() {
    typeCheckModule(
      "\\plevels p1 >= p2\n" +
      "\\func foo (A : \\Type p1) => 3\n" +
      "  \\where\n" +
      "    \\func bar \\plevels p3 >= p4 (a : A) => a", 1);
  }

  @Test
  public void levelsTest10() {
    typeCheckModule(
      "\\plevels p1 >= p2\n" +
      "\\func foo (A : \\Type p1) => 3\n" +
      "  \\where\n" +
      "    \\func bar (B : \\Type p2) => 4\n" +
      "      \\where\n" +
      "        \\func baz (a : A) (b : B) => 5");
  }

  @Test
  public void levelsTest11() {
    typeCheckModule(
      "\\plevels p1 >= p2\n" +
      "\\plevels p3 >= p4\n" +
      "\\func foo (A : \\Type p1) => 3\n" +
      "  \\where\n" +
      "    \\func bar (B : \\Type p3) => 4\n" +
      "      \\where\n" +
      "        \\func baz (a : A) (b : B) => 5", 1);
  }

  @Test
  public void levelsTest12() {
    typeCheckModule(
      "\\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3\n" +
      "  \\where\n" +
      "    \\func bar (a : A) => a\n" +
      "      \\where\n" +
      "        \\func baz => bar");
    Definition baz = getDefinition("foo.bar.baz");
    assertNotNull(baz.getLevelParameters());
    assertEquals(3, baz.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar.baz").getParameters().getTypeExpr());
  }

  @Test
  public void notUsedTest() {
    typeCheckModule(
      "\\func foo (x : Nat) => bar Nat.+ x\n" +
      "  \\where\n" +
      "    \\func bar => 2\n" +
      "\\func test : foo.bar = 2 => idp");
  }

  @Test
  public void dependencyTest() {
    typeCheckModule(
      "\\func foo {A : \\Type} (a : A) => a\n" +
      "  \\where\n" +
      "    \\func bar => a\n" +
      "\\func test : foo.bar {Nat} 5 = 5 => idp");
  }

  @Test
  public void dependencyTest2() {
    typeCheckModule(
      "\\func foo {A : \\Type} {a : A} (p : a = a) => a\n" +
      "  \\where\n" +
      "    \\func bar => p\n" +
      "\\func test : foo.bar {Nat} {5} idp = idp => idp");
  }

  @Test
  public void shadowingTest() {
    typeCheckModule(
      "\\func foo (var : Nat) => test\n" +
      "  \\where {\n" +
      "    \\func bar => var\n" +
      "    \\func var => 4\n" +
      "  }\n" +
      "\\func test : foo.bar 4 = 4 => idp");
  }

  @Test
  public void shadowingTest2() {
    typeCheckModule(
      "\\func foo (var : Nat) => test\n" +
      "  \\where {\n" +
      "    \\func bar => var\n" +
      "      \\where \\func var => 4\n" +
      "  }\n" +
      "\\func test : foo.bar = 4 => idp");
  }

  @Test
  public void shadowingTest3() {
    typeCheckModule(
      "\\func var => 4\n" +
      "\\func foo (var : Nat) => test\n" +
      "  \\where\n" +
      "    \\func bar => var\n" +
      "\\func test : foo.bar 6 = 6 => idp");
  }

  @Test
  public void inClassTest() {
    typeCheckModule(
      "\\lemma foo (var : Nat) (r : R) : var = var => r.f\n" +
      "  \\where\n" +
      "    \\record R (f : var = var)\n" +
      "\\func test : foo.R 3 => \\new foo.R { | f => idp {Nat} {3} }");
  }

  @Test
  public void inClassTeleTest() {
    typeCheckModule(
      "\\func foo (var var' : Nat Nat) => 0\n" +
      "  \\where\n" +
      "    \\record R (f : var = var)", 2);
  }

  @Test
  public void fieldClashTest() {
    typeCheckModule(
      "\\func foo (var : Nat) => 0\n" +
      "  \\where {\n" +
      "    \\func bar (p : var = var) => 0\n" +
      "    \\record R (f : bar idp = bar idp) (var : Nat -> Nat)\n" +
      "  }\n" +
      "\\func test (r : foo.R) : Nat -> Nat => r.var");
  }

  @Test
  public void inMetaTest() {
    resolveNamesModule(
      "\\func foo (var : Nat) => var\n" +
      "  \\where\n" +
      "    \\meta test => (var,var,var)", 3);
  }

  @Test
  public void telescopeTest1() {
    typeCheckModule(
      "\\func foo (x y : Nat) => x\n" +
      "  \\where\n" +
      "    \\func test (p : x = x) => p");
    assertEquals(2, getConcrete("foo.test").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest2() {
    typeCheckModule(
      "\\func foo (x y : Nat) => x\n" +
      "  \\where\n" +
      "    \\func test (p : y = y) => p");
    assertEquals(2, getConcrete("foo.test").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest3() {
    typeCheckModule(
      "\\func foo (x y : Nat) => x\n" +
        "  \\where\n" +
        "    \\func test (p : x = y) => p");
    assertEquals(2, getConcrete("foo.test").getParameters().size());
    assertEquals(3, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest4() {
    typeCheckModule(
      "\\func foo (x y : Nat) => x\n" +
      "  \\where {\n" +
      "    \\func bar => x Nat.+ y\n" +
      "    \\func baz => bar\n" +
      "  }\n" +
      "\\func test : foo.baz 2 3 = 5 => idp");
    assertEquals(1, getConcrete("foo.baz").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.baz").getParameters()));
  }

  @Test
  public void notImplementedFieldsError() {
    typeCheckModule(
      "\\func foo (var : Nat) => 0\n" +
      "  \\where\n" +
      "    \\record R (f : var = var)\n" +
      "\\func test : foo.R \\cowith", 2);
    assertThatErrorsAre(Matchers.typecheckingError(), Matchers.fieldsImplementation(false, Collections.singletonList(get("foo.R.f"))));
  }

  @Test
  public void notImplementedFieldsError2() {
    typeCheckModule(
      "\\func foo (var : Nat) => 0\n" +
      "  \\where\n" +
      "    \\record R (f : var = var)\n" +
      "\\func test : foo.R 3 \\cowith", 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("foo.R.f"))));
  }

  @Test
  public void notImplementedFieldsError3() {
    typeCheckModule(
      "\\func foo (var : Nat) => 0\n" +
      "  \\where\n" +
      "    \\record R (f : var = var)\n" +
      "\\func test : foo.R \\cowith\n" +
      "  | f => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError());
  }

  @Test
  public void piTest() {
    typeCheckModule(
      "\\func foo (var : \\Pi (x : Nat) -> x = x) => 0\n" +
      "  \\where\n" +
      "    \\func test => var");
  }

  @Test
  public void fieldResolveTest() {
    typeCheckModule(
      "\\record R (f : Nat)\n" +
      "\\func foo (r : R) => r.f\n" +
      "  \\where\n" +
      "    \\func test => r.f");
  }

  @Test
  public void fieldResolveTest2() {
    typeCheckModule(
      "\\func foo (r : R) => r.f\n" +
      "  \\where {\n" +
      "    \\record R (f : Nat)\n" +
      "    \\func test => r.f\n" +
      "  }");
  }

  @Test
  public void fieldResolveTest3() {
    resolveNamesModule(
      "\\record R (f : Nat)\n" +
      "\\func foo (r : R) => 0\n" +
      "  \\where {\n" +
      "    \\record R (g : Nat)\n" +
      "    \\func test => r.f\n" +
      "  }", 1);
    assertThatErrorsAre(Matchers.notInScope("f"));
  }

  @Test
  public void fieldResolveTest4() {
    typeCheckModule(
      "\\record R (f : Nat)\n" +
      "\\func foo (r : R) => r.f\n" +
      "  \\where\n" +
      "    \\func test => r.f\n" +
      "      \\where\n" +
      "        \\record R (g : Nat)");
  }
}
