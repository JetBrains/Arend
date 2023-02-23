package org.arend.typechecking;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.util.Pair;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.visitor.WhereVarsFixVisitor;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VarsTest extends TypeCheckingTestCase {
  private Concrete.Definition getConcreteFixed(String path) {
    Concrete.Definition def = (Concrete.Definition) getConcrete(path);
    WhereVarsFixVisitor.fixDefinition(Collections.singletonList(def), errorReporter);
    return def;
  }

  @Test
  public void whereTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => x
          \\where
            \\func bar (p : x = x) => p
        """);
  }

  @Test
  public void embeddedTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => x
          \\where
            \\func bar (y : Nat) => y
              \\where
                \\func test (p : x = y) => p
        """);
  }

  @Test
  public void invokeTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => bar Nat.+ x
          \\where
            \\func bar => x
        \\func test : foo 3 = foo.bar 6 => idp
        """);
  }

  @Test
  public void invokeTest2() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz = x => idp
          \\where
            \\func bar : baz = x => idp
              \\where
                \\func baz => x
        """);
  }

  @Test
  public void invokeTest3() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz 3 = (x,3) => idp
          \\where
            \\func bar (y : Nat) : baz = (x,y) => idp
              \\where
                \\func baz => (x,y)
        """);
  }

  @Test
  public void invokeTest4() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz 3 = (x,3) => idp
          \\where
            \\func bar (y : Nat) : baz = {\\Sigma Nat Nat} (_,y) => idp
              \\where
                \\func baz => (x,y)
        """);
  }

  @Test
  public void invokeTest5() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz = x => idp
          \\where
            \\func bar : baz = {Nat} _ => idp
              \\where
                \\func baz => x
        """);
  }

  @Test
  public void invokeTest6() {
    typeCheckModule(
      """
        \\func bar : foo.baz = (\\lam x => x) => idp
          \\where
            \\func foo (x : Nat) : baz = x => idp
              \\where
                \\func baz => x
        """);
  }

  @Test
  public void invokeTest7() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz 3 = idp {_} {x,3} => idp
          \\where
            \\func bar (y : Nat) : baz = idp {_} {x,y} => idp
              \\where
                \\func baz : (x,bak) = (x,y) => idp
                  \\where
                    \\func bak => y
        """);
  }

  @Test
  public void invokeTest8() {
    typeCheckModule(
      """
        \\func foo (x : Nat) : bar.baz 3 = idp {_} {x,3} => idp
          \\where
            \\func bar (y : Nat) : baz = idp {_} {x,y} => idp
              \\where
                \\func baz : (x,bak) = (x,_) => idp
                  \\where
                    \\func bak => y
        """);
  }

  @Test
  public void invokeTest9() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => baz
          \\where {
            \\func baz => bar
            \\func bar => x
          }
        \\func test : foo.baz 3 = foo.bar 3 => idp
        """);
  }

  @Test
  public void invokeTest10() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => x
          \\where {
            \\func bar (y : Nat) => y
              \\where
                \\func bak => x Nat.+ y
            \\func baz (z : Nat) => z
              \\where
                \\func qux => bar.bak 5
          }
        \\func test : foo.baz.qux 4 = 9 => idp
        """);
  }

  @Test
  public void patternMatchingTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => bar 2 Nat.+ x
          \\where
            \\func bar (y : Nat) : Nat
              | 0 => 0
              | suc y => x Nat.* y
        \\func test : foo 3 = foo.bar 2 4 => idp
        """);
  }

  @Test
  public void recursiveTest() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => bar 4
          \\where
            \\func bar (n : Nat) : Nat
              | 0 => 0
              | suc n => bar n Nat.+ x
        \\func test : (foo.bar 5 4, foo 3 100) = (20, 12) => idp
        """);
  }

  @Test
  public void mutuallyRecursiveTest() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => baz
          \\where {
            \\func bar (n : Nat) : Nat
              | 0 => 0
              | suc n => baz n Nat.+ x
            \\func baz (n : Nat) : Nat
              | 0 => 0
              | suc n => bar n Nat.+ y
          }
        \\func test : (foo.bar 5 7 1, foo.baz 5 7 1) = (5, 7) => idp
        """);
  }

  @Test
  public void mutuallyRecursiveTest2() {
    typeCheckModule(
      """
        \\func foo (m n : Nat) : Nat \\elim n
          | 0 => 0
          | suc n => bar n Nat.+ foo m n
          \\where
            \\func bar (n : Nat) : Nat => m Nat.+ foo m n
        \\func test : (foo 2 1, foo.bar 2 1) = (2,4) => idp
        """);
  }

  @Test
  public void orderTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => x
          \\where
            \\func bar (y : Nat) => y
              \\where {
                \\func baz => (x,y)
                \\func qux => (y,x)
              }
        \\func test : (foo.bar.baz 1 2, foo.bar.qux 3 4) = ((1,2),(4,3)) => idp
        """);
  }

  @Test
  public void orderTest2() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => x
          \\where
            \\func bar (y : Nat) => y
              \\where {
                \\func x' => x
                \\func y' => y
                \\func baz => (x',y')
                \\func qux => (y',x')
              }
        \\func test : (foo.bar.baz 1 2, foo.bar.qux 3 4) = ((1,2),(4,3)) => idp
        """);
  }

  @Test
  public void dynamicTest() {
    typeCheckModule(
      """
        \\record R {
          \\func foo (x : Nat) => bar
            \\where
              \\func bar => x
        }
        \\func test (r : R) : r.foo 7 = R.foo.bar {r} 7 => idp
        """);
  }

  @Test
  public void levelsTest1() {
    typeCheckModule(
      """
        \\func foo (A : \\Type) => 3
          \\where
            \\func bar \\plevels p1 >= p2 (a : A) => a
        """);
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar").getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest2() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3
          \\where
            \\func bar (a : A) => a
        """);
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar").getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest3() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type p1) => 3
          \\where
            \\func bar (a : A) => a
        """);
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(bar.getLevelParameters().get(0)), new Level(LevelVariable.HVAR))), bar.getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest4() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3
          \\where
            \\func bar \\plevels p3 >= p4 (a : A) => a
        """, -1);
  }

  @Test
  public void levelsTest5() {
    typeCheckModule(
      """
        \\plevels p1 >= p2
        \\func foo (A : \\Type p1) => 3
          \\where
            \\func bar (a : A) => a
        """);
    Definition bar = getDefinition("foo.bar");
    assertNotNull(bar.getLevelParameters());
    assertEquals(3, bar.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(bar.getLevelParameters().get(0)), new Level(LevelVariable.HVAR))), bar.getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest6() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3
          \\where
            \\func bar \\plevels p3 >= p4 (B : \\Type p4) => 4
              \\where
                \\func baz (a : A) (b : B) => 5
        """, -1);
  }

  @Test
  public void levelsTest7() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type) => 3
          \\where
            \\func bar \\plevels p3 >= p4 (B : \\Type) (x : Nat) => 4
              \\where
                \\func baz (a : A) => x
        """);
  }

  @Test
  public void levelsTest8() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type) (x : Nat) => 3
          \\where
            \\func bar \\plevels p3 >= p4 (B : \\Type) => 4
              \\where
                \\func baz (b : B) => x
        """);
  }

  @Test
  public void levelsTest9() {
    typeCheckModule(
      """
        \\plevels p1 >= p2
        \\func foo (A : \\Type p1) => 3
          \\where
            \\func bar \\plevels p3 >= p4 (a : A) => a
        """, 1);
  }

  @Test
  public void levelsTest10() {
    typeCheckModule(
      """
        \\plevels p1 >= p2
        \\func foo (A : \\Type p1) => 3
          \\where
            \\func bar (B : \\Type p2) => 4
              \\where
                \\func baz (a : A) (b : B) => 5
        """);
  }

  @Test
  public void levelsTest11() {
    typeCheckModule(
      """
        \\plevels p1 >= p2
        \\plevels p3 >= p4
        \\func foo (A : \\Type p1) => 3
          \\where
            \\func bar (B : \\Type p3) => 4
              \\where
                \\func baz (a : A) (b : B) => 5
        """, 1);
  }

  @Test
  public void levelsTest12() {
    typeCheckModule(
      """
        \\func foo \\plevels p1 >= p2 (A : \\Type p2) => 3
          \\where
            \\func bar (a : A) => a
              \\where
                \\func baz => bar
        """);
    Definition baz = getDefinition("foo.bar.baz");
    assertNotNull(baz.getLevelParameters());
    assertEquals(3, baz.getLevelParameters().size());
    assertEquals(new UniverseExpression(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), getDefinition("foo.bar.baz").getParameters().getTypeExpr());
  }

  @Test
  public void levelsTest13() {
    typeCheckModule(
      """
        \\plevels p1 >= p2
        \\record R (A : \\Type p1)
        \\func foo (r : R) => 4
          \\where
            \\func bar => r
        """);
  }

  @Test
  public void notUsedTest() {
    typeCheckModule(
      """
        \\func foo (x : Nat) => bar Nat.+ x
          \\where
            \\func bar => 2
        \\func test : foo.bar = 2 => idp
        """);
  }

  @Test
  public void dependencyTest() {
    typeCheckModule(
      """
        \\func foo {A : \\Type} (a : A) => a
          \\where
            \\func bar => a
        \\func test : foo.bar {Nat} 5 = 5 => idp
        """);
  }

  @Test
  public void dependencyTest2() {
    typeCheckModule(
      """
        \\func foo {A : \\Type} {a : A} (p : a = a) => a
          \\where
            \\func bar => p
        \\func test : foo.bar {Nat} {5} idp = idp => idp
        """);
  }

  @Test
  public void shadowingTest() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => test
          \\where {
            \\func bar => var
            \\func var => 4
          }
        \\func test : foo.bar 4 = 4 => idp
        """);
  }

  @Test
  public void shadowingTest2() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => test
          \\where {
            \\func bar => var
              \\where \\func var => 4
          }
        \\func test : foo.bar = 4 => idp
        """);
  }

  @Test
  public void shadowingTest3() {
    typeCheckModule(
      """
        \\func var => 4
        \\func foo (var : Nat) => test
          \\where
            \\func bar => var
        \\func test : foo.bar 6 = 6 => idp
        """);
  }

  @Test
  public void inClassTest() {
    typeCheckModule(
      """
        \\lemma foo (var : Nat) (r : R) : var = var => r.f
          \\where
            \\record R (f : var = var)
        \\func test : foo.R 3 => \\new foo.R { | f => idp {Nat} {3} }
        """);
  }

  @Test
  public void inClassTeleTest() {
    typeCheckModule(
      """
        \\func foo (var var' : Nat Nat) => 0
          \\where
            \\record R (f : var = var)
        """, 2);
  }

  @Test
  public void fieldClashTest() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => 0
          \\where {
            \\func bar (p : var = var) => 0
            \\record R (f : bar idp = bar idp) (var : Nat -> Nat)
          }
        \\func test (r : foo.R) : Nat -> Nat => r.var
        """);
  }

  @Test
  public void inMetaTest() {
    resolveNamesModule(
      """
        \\func foo (var : Nat) => var
          \\where
            \\meta test => (var,var,var)
        """, 3);
  }

  @Test
  public void telescopeTest1() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => x
          \\where
            \\func test (p : x = x) => p
        """);
    assertEquals(2, getConcreteFixed("foo.test").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest2() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => x
          \\where
            \\func test (p : y = y) => p
        """);
    assertEquals(2, getConcreteFixed("foo.test").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest3() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => x
          \\where
            \\func test (p : x = y) => p
        """);
    assertEquals(2, getConcreteFixed("foo.test").getParameters().size());
    assertEquals(3, DependentLink.Helper.size(getDefinition("foo.test").getParameters()));
  }

  @Test
  public void telescopeTest4() {
    typeCheckModule(
      """
        \\func foo (x y : Nat) => x
          \\where {
            \\func bar => x Nat.+ y
            \\func baz => bar
          }
        \\func test : foo.baz 2 3 = 5 => idp
        """);
    assertEquals(1, getConcreteFixed("foo.baz").getParameters().size());
    assertEquals(2, DependentLink.Helper.size(getDefinition("foo.baz").getParameters()));
  }

  @Test
  public void notImplementedFieldsError() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => 0
          \\where
            \\record R (f : var = var)
        \\func test : foo.R \\cowith
        """, 2);
    assertThatErrorsAre(Matchers.typecheckingError(), Matchers.fieldsImplementation(false, Collections.singletonList(get("foo.R.f"))));
  }

  @Test
  public void notImplementedFieldsError2() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => 0
          \\where
            \\record R (f : var = var)
        \\func test : foo.R 3 \\cowith
        """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("foo.R.f"))));
  }

  @Test
  public void notImplementedFieldsError3() {
    typeCheckModule(
      """
        \\func foo (var : Nat) => 0
          \\where
            \\record R (f : var = var)
        \\func test : foo.R \\cowith
          | f => idp
        """, 1);
    assertThatErrorsAre(Matchers.typecheckingError());
  }

  @Test
  public void piTest() {
    typeCheckModule(
      """
        \\func foo (var : \\Pi (x : Nat) -> x = x) => 0
          \\where
            \\func test => var
        """);
  }

  @Test
  public void fieldResolveTest() {
    typeCheckModule(
      """
        \\record R (f : Nat)
        \\func foo (r : R) => r.f
          \\where
            \\func test => r.f
        """);
  }

  @Test
  public void fieldResolveTest2() {
    typeCheckModule(
      """
        \\func foo (r : R) => r.f
          \\where {
            \\record R (f : Nat)
            \\func test => r.f
          }
        """);
  }

  @Test
  public void fieldResolveTest3() {
    resolveNamesModule(
      """
        \\record R (f : Nat)
        \\func foo (r : R) => 0
          \\where {
            \\record R (g : Nat)
            \\func test => r.f
          }
        """, 1);
    assertThatErrorsAre(Matchers.notInScope("f"));
  }

  @Test
  public void fieldResolveTest4() {
    typeCheckModule(
      """
        \\record R (f : Nat)
        \\func foo (r : R) => r.f
          \\where
            \\func test => r.f
              \\where
                \\record R (g : Nat)
        """);
  }

  @Test
  public void elimTest() {
    resolveNamesModule(
      """
        \\func foo (n : Nat) : Nat
          | 0 => 0
          | suc n => n
          \\where
            \\func test => n
        """, 1);
    assertThatErrorsAre(Matchers.notInScope("n"));
  }

  @Test
  public void elimTest2() {
    resolveNamesModule(
      """
        \\func foo (n m k l : Nat) : Nat \\elim n, k
          | n, k => 0
          \\where
            \\func test => (n,m,k,l)
        """, 2);
    assertThatErrorsAre(Matchers.notInScope("n"), Matchers.notInScope("k"));
  }

  @Test
  public void classTest() {
    typeCheckModule(
      """
        \\class C (x : Nat) {
          \\func f (p : x = x) => test
            \\where
              \\func test => p
        }
        """);
    TCDefReferable f = getDefinition("C.f").getRef();
    assertEquals(Collections.singletonList(new Pair<>(f, 0)), getDefinition("C.f.test").getParametersOriginalDefinitions());
  }

  @Test
  public void coclauseTest() {
    typeCheckModule("""
      \\record R (g : Nat -> Nat)
      \\func f (x : Nat) => 0
        \\where
          \\func test (p : x = 0) : R \\cowith
            | g (n : Nat) : Nat => n
      """);
  }

  @Test
  public void classTest2() {
    typeCheckModule(
      """
        \\class C (x : Nat) {
          \\func f {y : Nat} (p : x = y) => g
            \\where
              \\func g => p
        }
        """);
  }
}
