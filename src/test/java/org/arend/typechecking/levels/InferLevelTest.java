package org.arend.typechecking.levels;

import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.definition.UniverseKind;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.expr.ExpressionFactory;
import org.arend.core.expr.PathExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.ext.core.ops.CMP;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.doubleChecker.CoreExpressionChecker;
import org.arend.typechecking.implicitargs.equations.DummyEquations;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import static org.arend.Matchers.typeMismatchError;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InferLevelTest extends TypeCheckingTestCase {
  @Test
  public void noEquations() {
    // no equations
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f => A");
  }

  @Test
  public void metaVarEquation() {
    // ?l <= ?l'
    // error: cannot infer ?l, ?l'
    typeCheckModule("""
      \\func A => \\Type
      \\func f (A : \\Type) => A
      \\func g => f A
      """);
  }

  @Test
  public void universeTest() {
    typeCheckModule("\\func f (A : \\Type) : \\Type => A = A");
  }

  @Test
  public void belowTen() {
    // ?l <= 10
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type10 => A");
  }

  @Test
  public void belowParam() {
    // ?l <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f : \\Type (\\suc \\lp) (\\suc \\lh) => A");
  }

  @Test
  public void belowParam2() {
    typeCheckModule(
        "\\func A => \\Type\n" +
        "\\func f : \\oo-Type (\\suc \\lp) => A");
  }

  @Test
  public void belowParam3() {
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type => A");
  }

  @Test
  public void belowParamError() {
    // ?l + 1 <= c
    // error: cannot infer ?l
    typeCheckModule(
        "\\func A => \\oo-Type\n" +
        "\\func f : \\oo-Type \\lp => A", 1);
  }

  @Test
  public void btwZeroAndParam() {
    // 0 <= ?l, 0 <= c
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type => f Nat");
  }

  @Test
  public void btwOneAndParam() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type \\lp (\\suc \\lh) => f \\Type0", 1);
  }

  @Test
  public void btwOneAndParamWithH() {
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type => f \\Type0");
  }

  @Test
  public void btwOneAndParamWithHError() {
    // 1 <= ?l, 1 <= c
    // error: cannot solve 1 <= c
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g : \\Type \\lp \\lh => f \\Type0", 2);
  }

  @Test
  public void btwZeroAndTen() {
    // 0 <= ?l <= 10
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type10 => f Nat");
  }

  @Test
  public void btwOneAndTen() {
    // 1 <= ?l <= 10
    // ok: ?l = 1
    typeCheckModule(
        "\\func f (A : \\oo-Type) => A\n" +
        "\\func g : \\oo-Type10 => f \\oo-Type0");
  }

  @Test
  public void greaterThanZero() {
    // 0 <= ?l
    // ok: ?l = 0
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g => f Nat");
  }

  @Test
  public void greaterThanOne() {
    // 1 <= ?l
    // ok: ?l = 1
    typeCheckModule(
        "\\func f (A : \\Type) => A\n" +
        "\\func g => f \\Type0");
  }

  @Test
  public void propImpredicative() {
    typeCheckModule("\\func g (X : \\Set10) (P : X -> \\Prop) : \\Prop => \\Pi (a : X) -> P a");
  }

  @Test
  public void propImpredicative2() {
    typeCheckModule(
      "\\func f (X : \\Set10) (P : X -> \\Type) => \\Pi (a : X) -> P a\n" +
      "\\func g (X : \\Set10) (P : X -> \\Prop) : \\Prop => f X P");
  }

  @Test
  public void levelOfPath() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\Prop => x = x");
  }

  @Test
  public void levelOfPath2() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\1-Type1 => x = x -> \\Set0");
  }

  @Test
  public void levelOfPath3() {
    typeCheckModule("\\func f (X : \\Set10) (x : X) : \\1-Type1 => (x = x : \\Prop) -> \\Set0");
  }

  @Test
  public void constantUpperBound() {
    typeCheckModule(
      "\\func f (A : \\Type) => A\n" +
      "\\func g (B : \\Type) : \\Set => f B", 1);
  }

  @Test
  public void expectedType() {
    typeCheckModule(
      "\\func X => \\Type\n" +
      "\\func f : X => \\Type"
    );
  }

  @Test
  public void parameters() {
    typeCheckModule("""
      \\func X => \\Type
      \\func f (A : X) => 0
      \\func g => f \\Set0
      """
    );
  }

  @Test
  public void lhLessThanInf() {
    typeCheckModule("""
      \\func f (A : \\Type) (a a' : A) (p : a = a') => p
      \\func X : \\oo-Type => Nat
      \\func g : X = X => f \\Type X X idp
      """);
  }

  @Test
  public void pLevelTest() {
    typeCheckModule("""
      \\func squeeze1 (i j : I) : I =>
        coe (\\lam x => left = x) idp j @ i
      \\func squeeze (i j : I) =>
        coe (\\lam i => Path (\\lam j => left = squeeze1 i j) idp (path (\\lam j => squeeze1 i j))) idp right @ i @ j
      \\func psqueeze {A : \\Type} {a a' : A} (p : a = a') (i : I) : a = p @ i =>
        path (\\lam j => p @ squeeze i j)
      \\func Jl {A : \\Type} {a : A} (B : \\Pi (a' : A) -> a = a' -> \\Type) (b : B a idp) {a' : A} (p : a = a') : B a' p =>
        coe (\\lam i => B (p @ i) (psqueeze p i)) b right
      \\func foo (A : \\Type) (a0 a1 : A) (p : a0 = a1) =>
        Jl (\\lam _ q => (idp {A} {a0} = idp {A} {a0}) = (q = q)) idp p
      """);
  }

  @Test
  public void classLevelTest() {
    typeCheckModule("""
      \\class A {
        | X : \\oo-Type
      }
      \\func f : A \\levels 0 _ => \\new A { | X => \\oo-Type0 }
      """, 1);
  }

  @Test
  public void setIsNotProp() {
    typeCheckDef(
      "\\func isSur {A B : \\Set} (f : A -> B) : \\Prop =>\n" +
      "  \\Pi (b : B) -> \\Sigma (a : A) (b = f a)", 1);
  }

  @Test
  public void idTest() {
    typeCheckModule("""
      \\class Functor (F : \\Type -> \\Type)
        | fmap {A B : \\Type} : (A -> B) -> F A -> F B

      \\data Maybe (A : \\Type) | nothing | just A
      \\func id' {A : \\Type} (a : A) => a
      \\func idTest : \\Type1 => id' (\\suc \\lp) (Functor Maybe)
      """, 1);
  }

  @Test
  public void idTest2() {
    typeCheckModule("""
      \\class Functor (F : \\Type -> \\Type)
        | fmap {A B : \\Type} : (A -> B) -> F A -> F B

      \\data Maybe (A : \\Type) | nothing | just A
      \\func id' {A : \\Type} (a : A) => a
      \\func idTest : \\Type1 => id' (\\suc (\\suc \\lp)) (Functor Maybe)
      """);
  }

  @Test
  public void idTest3() {
    typeCheckModule("""
      \\class Functor (F : \\Type -> \\Type)
        | fmap {A B : \\Type} : (A -> B) -> F A -> F B

      \\data Maybe (A : \\Type) | nothing | just A
      \\func id' {A : \\Type} (a : A) => a
      \\func idTest => id' (\\suc (\\suc \\lp)) (Functor Maybe)
      """);
  }

  @Test
  public void dataLevelsTest1() {
    typeCheckModule(
      "\\data D | con \\Type\n" +
      "\\func f (d : D \\levels 1 0) : D \\levels 0 0 => d", 1);
  }

  @Test
  public void dataLevelsTest2() {
    typeCheckModule("""
      \\data D | con \\Type
      \\func fromD (d : D) : \\Type | con A => A
      \\func ddd : \\Type0 => fromD (con \\Type0)
      """, 1);
  }

  @Test
  public void funcLevelsTest() {
    typeCheckModule(
      "\\func F => \\Type\n" +
      "\\func f (d : F \\levels 1 0) : F \\levels 0 0 => d", 1);
  }

  @Test
  public void funTest() {
    Levels levels = ((PathExpression) Objects.requireNonNull(((FunctionDefinition) typeCheckDef("\\func pmap {A B : \\Type} (f : A -> B) {a a' : A} (p : a = a') : f a = f a' => path (\\lam i => f (p @ i))")).getBody())).getLevels();
    assertEquals(LevelPair.STD, levels);
  }

  @Test
  public void classTest() {
    typeCheckModule("\\class B (F : \\Type -> \\Type) | foo : F Nat", 1);
  }

  @Test
  public void fieldTest() {
    typeCheckModule("""
      \\record R
        | f : \\Type -> \\Type
      \\record S
        | inst : R
        | func (X : \\oo-Type) : f {inst} X
      """, 1);
  }

  @Test
  public void setTest() {
    typeCheckModule("""
      \\func func (f : \\Type -> \\Type) => 0
      \\data Maybe (A : \\Type) | nothing | just A
      \\func test => func \\lp \\lh (\\lam X => Maybe X)
      """, 1);
  }

  @Test
  public void funcTest() {
    typeCheckModule("""
      \\data Bool | true | false
      \\func T (b : Bool) : \\Type
        | true => Nat
        | false => \\Sigma
      """, 1);
  }

  @Test
  public void funcTest2() {
    typeCheckModule("""
      \\data Bool | true | false
      \\func T (b : Bool) : \\Set
        | true => Nat
        | false => \\Sigma
      \\func test (b : Bool) : \\Prop => T b
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void pathTest() {
    typeCheckModule("""
      \\func eq {A : \\Type} (x y : A) => x = y
      \\func id {A : \\Prop} (a : A) => a
      \\func test {A : \\Set} {x y : A} (p : eq x y) => id p
      """);
  }

  @Test
  public void pathTest2() {
    typeCheckModule("""
      \\data Test {A : \\Type} (x y : A)
        | con (x = y)
      \\func test {A : \\Type} (As : \\Pi {a a' : A} (p q : a = a') -> p = q) {x y : A} (t s : Test x y) : t = s \\elim t, s
        | con p, con q => path (\\lam i => con (As p q @ i))
      """);
  }

  @Test
  public void propTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test => \\Pi (A : \\Set) (a : A) -> a = a");
    assertTrue(Level.compare(new Level(0), ((UniverseExpression) def.getResultType()).getSort().getPLevel(), CMP.EQ, DummyEquations.getInstance(), null));
  }

  @Test
  public void recordTest() {
    typeCheckModule("""
      \\record R (A : \\Type) (a : A) (p : a = a)
      \\lemma test {A : \\Set} (a : A) : R A a \\cowith
        | p => idp
      """);
  }

  @Test
  public void fieldLevelTest() {
    typeCheckModule("""
      \\record SomeSigma (A : \\Type) (J : \\Set)
      \\class SomeWrapper (X : SomeSigma Nat)
      \\func test (w : SomeWrapper) : \\Type => w.X.J
      """);
  }

  @Ignore
  @Test
  public void noUniverseTest() {
    typeCheckModule(
      "\\data Wrap (A : \\Type) | wrap A\n" +
      "\\func test : \\Set0 => Wrap \\levels 1 1 Nat");
    DataDefinition wrap = (DataDefinition) getDefinition("Wrap");
    assertEquals(UniverseKind.NO_UNIVERSES, wrap.getUniverseKind());
    DataCallExpression.make(wrap, new LevelPair(new Level(1), new Level(1)), Collections.singletonList(ExpressionFactory.Nat())).accept(new CoreExpressionChecker(new HashSet<>(), DummyEquations.getInstance(), null), new UniverseExpression(new Sort(new Level(0), new Level(0))));
  }

  @Ignore
  @Test
  public void noUniverseTest2() {
    typeCheckModule(
      "\\data Wrap (A : \\Type) | wrap A\n" +
      "\\func test : \\Set0 => Wrap \\levels 1 0 Nat -> Wrap \\levels 0 1 Nat");
  }

  @Test
  public void transitivityTest() {
    typeCheckModule("""
      \\class C (A : \\hType) (a : A)
      \\data Wrap (A : \\hType) | wrap A
      \\func foo {A : \\hType} (c : C (Wrap A)) => c.a
      \\func test {A : \\hType} (c : C (Wrap (\\suc \\lp) A)) => foo c
      """);
  }

  @Test
  public void transitivityTest2() {
    typeCheckModule("""
      \\class C {A : \\hType} (a : A)
      \\data Wrap (A : \\hType) | wrap A
      \\class D (B : \\hType) \\extends C
        | A => Wrap B
      \\func foo (d : D) => d.a
      \\func test {B : \\hType} (d : D (\\suc \\lp) { | B => B }) => foo d
      """);
  }

  @Test
  public void transitivityTest3() {
    typeCheckModule("""
      \\class C (A : \\hType) (a : A)
      \\data Wrap (A : \\hType) | wrap A
      \\func test1 {A : \\hType} (c : C (Wrap (\\suc \\lp) A)) : C (Wrap \\lp A) => c
      \\func test2 {A : \\hType} (c : C (Wrap \\lp A)) : C \\lp => c
      \\func test {A : \\hType} (c : C (Wrap (\\suc \\lp) A)) : C \\lp => c
      """);
  }

  @Test
  public void transitivityTest4() {
    typeCheckModule("""
      \\class C {A : \\hType} (a : A)
      \\class D \\extends C
        | A => Nat
      \\class E (B : \\hType) \\extends D
      \\func test1 (e : E (\\suc \\lp)) : D \\lp => e
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
