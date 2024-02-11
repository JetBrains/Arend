package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.junit.Test;

import static org.junit.Assert.*;

public class UseLevelTest extends TypeCheckingTestCase {
  @Test
  public void testData() {
    typeCheckModule("""
      \\data Empty
      \\func absurd {A : \\Type} (e : Empty) : A
      \\axiom prop-pi {A : \\Prop} (a a' : A) : a = a'
      \\data Dec (A : \\Prop) | yes A | no (A -> Empty)
        \\where
          \\use \\level isProp {A : \\Prop} (d1 d2 : Dec A) : d1 = d2
            | yes a1, yes a2 => path (\\lam i => yes (prop-pi a1 a2 @ i))
            | yes a1, no na2 => absurd (na2 a1)
            | no na1, yes a2 => absurd (na1 a2)
            | no na1, no na2 => path (\\lam i => no (\\lam a => (absurd (na1 a) : na1 a = na2 a) @ i))
      """);
    DataDefinition data = (DataDefinition) getDefinition("Dec");
    assertEquals(Sort.PROP, data.getSort());
    assertFalse(data.isSquashed());
    FunctionDefinition def = (FunctionDefinition) getDefinition("Dec.isProp");
    assertNull(def.getBody());
    assertNotNull(def.getActualBody());
  }

  @Test
  public void testDataNotExact() {
    typeCheckModule("""
      \\data Empty
      \\func absurd {A : \\Type} (e : Empty) : A
      \\data Dec (A : \\Type) (p : \\Pi (a a' : A) -> a = a') | yes A | no (A -> Empty)
        \\where
          \\use \\level isProp {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (d1 d2 : Dec A p) : d1 = d2 \\elim d1, d2
            | yes a1, yes a2 => path (\\lam i => yes (p a1 a2 @ i))
            | yes a1, no na2 => absurd (na2 a1)
            | no na1, yes a2 => absurd (na1 a2)
            | no na1, no na2 => path (\\lam i => no (\\lam a => (absurd (na1 a) : na1 a = na2 a) @ i))
      """);
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, 0, 0)), ((DataDefinition) getDefinition("Dec")).getSort());
  }

  @Test
  public void testDataSet() {
    typeCheckModule("""
      \\data D
        | con Nat Nat
        | con2 (n m k l : Nat) (p : n Nat.* l = k Nat.* m) (i : I) \\elim i {
          | left => con n m
          | right => con k l
        }
        | con3 (x y : D) (p q : x = y) (i j : I) \\elim i, j {
          | left, j => p @ j
          | right, j => q @ j
          | _, left => x
          | _, right => y
        }
        \\where
          \\use \\level isSet (d1 d2 : D) (p q : d1 = d2) => path (\\lam i => path (con3 d1 d2 p q i))
      """);
    assertEquals(Sort.SET0, ((DataDefinition) getDefinition("D")).getSort());
  }

  @Test
  public void testClass() {
    typeCheckModule("""
      \\data D : \\Set
      \\func absurd {A : \\Type} (e : D) : A
      \\class C (d : D)
        \\where
          \\use \\level isProp (c1 c2 : C) : c1 = c2 => absurd c1.d
      """);
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("D")).getSort());
    assertEquals(Sort.PROP, ((ClassDefinition) getDefinition("C")).getSort());
  }

  @Test
  public void testDataSet2() {
    typeCheckModule("""
      \\data Trunc (A : \\Type)
        | inc A
        | trunc (x y : Trunc A) (p q : x = y) (i j : I) \\elim i, j {
          | left, j => p @ j
          | right, j => q @ j
          | _, left => x
          | _, right => y
        }
        \\where
          \\use \\level isProp {A : \\Type} (d1 : Trunc A) : \\Pi (d2 : Trunc A) (p : d1 = d2) -> \\Pi (q : d1 = d2) -> p = q => \\lam d2 p q => path (\\lam i => path (trunc d1 d2 p q i))
      """);
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("Trunc")).getSort());
  }

  @Test
  public void testClassFields() {
    typeCheckModule("""
      \\data D : \\Set
      \\func absurd {A : \\Type} (d : D) : A
      \\class C (x : Nat) (d : D)
        \\where
          \\use \\level isProp (x : Nat) (c1 c2 : C x) : c1 = c2 => absurd c1.d
      \\func f : \\Prop => C 0
      """);
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("D")).getSort());
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("C")).getSort());
  }

  @Test
  public void testClassFields2() {
    typeCheckModule("""
      \\data D
      \\func absurd {A : \\Type} (d : D) : A
      \\class C {A B : \\Type} (f : A -> B) (d : D)
        \\where
          \\use \\level isProp {A B : \\Type} (f : A -> B) (c1 c2 : C f) : c1 = c2 => absurd c1.d
      \\func f : \\Prop => C (\\lam (x : Nat) => x)
      """);
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("C")).getSort());
  }

  @Test
  public void mutualRecursion() {
    typeCheckModule(
      "\\func f (c : C) : c = c => idp\n" +
      "\\class C \\where \\use \\level isProp (c1 c2 : C) : c1 = c2 => f c1");
  }

  @Test
  public void mutualRecursion2() {
    typeCheckModule(
      "\\class C \\where \\use \\level isProp (c1 c2 : C) : c1 = c2 => f c1\n" +
      "\\func f (c : C) : c = c => idp");
  }

  @Test
  public void orderTest() {
    typeCheckModule("""
      \\func f : \\Prop => D
      \\data Empty
      \\func absurd {A : \\Type} (e : Empty) : A
      \\data D | con1 | con2 Empty \\where
        \\use \\level isProp (d1 d2 : D) : d1 = d2 \\elim d1, d2
          | con1, con1 => idp
          | _, con2 e => absurd e
          | con2 e, _ => absurd e
      """);
  }

  @Test
  public void useRecordTest() {
    typeCheckModule(
      "\\record R\n" +
      "  \\where \\use \\level isProp : \\Pi (x y : R) -> x = y => \\lam x y => idp");
  }

  @Test
  public void restrictedDataInCaseTest() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }
      \\func f (x : TrP Nat) : D (\\new B) => \\case x \\with { | inP _ => ddd }
      """);
  }

  @Test
  public void restrictedDataInCaseTestError() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }
      \\func f (x : TrP Nat) : D (\\new A) => \\case x \\with { | inP _ => ddd }
      """, 1);
  }

  @Test
  public void restrictedClassInCaseTest() {
    typeCheckModule("""
      \\record A (x : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }
      \\func f (d : D) (x : TrP Nat) : C (\\new B 0) => \\case x \\with { | inP _ => \\new C { | df => d } }
      """);
  }

  @Test
  public void restrictedClassInCaseTestError() {
    typeCheckModule("""
      \\record A (x : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }
      \\func f (d : D) (x : TrP Nat) : C (\\new A 0) => \\case x \\with { | inP _ => \\new C { | df => d } }
      """, 1);
  }

  @Test
  public void restrictedDataInLemmaTest() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\lemma f : D (\\new B) => ddd
      """);
  }

  @Test
  public void restrictedDataInLemmaTestError() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\lemma f : D (\\new A) => ddd
      """, 1);
  }

  @Test
  public void restrictedClassInLemmaTest() {
    typeCheckModule("""
      \\record A (x : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\lemma f (d : D) : C (\\new B 0) => \\new C { | df => d }
      """);
  }

  @Test
  public void restrictedClassInLemmaTestError() {
    typeCheckModule("""
      \\record A (x : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\lemma f (d : D) : C (\\new A 0) => \\new C { | df => d }
      """, 1);
  }

  @Test
  public void restrictedDataInPropertyTest() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\record R { \\property prop : D (\\new B) }
      """);
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("R")).getSort());
  }

  @Test
  public void restrictedDataInPropertyTestError() {
    typeCheckModule("""
      \\record A
      \\record B \\extends A
      \\data D (a : A) : \\Set | ddd
        \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp
      \\record R { \\property prop : D (\\new A) }
      """, 1);
  }

  @Test
  public void restrictedClassInPropertyTest() {
    typeCheckModule("""
      \\record A (X : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\record R { \\property prop : C (\\new B 0) }
      """);
  }

  @Test
  public void restrictedClassInPropertyTestError() {
    typeCheckModule("""
      \\record A (x : Nat)
      \\record B \\extends A
      \\data D : \\Set
      \\record C (a : A) (df : D)
        \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))
      \\record R { \\property prop : C (\\new A 0) }
      """, 1);
  }

  @Test
  public void resultTypeTest() {
    typeCheckModule(
      "\\func test (A : \\Type) (p : \\Pi (x y : A) -> x = y) => A\n" +
      "  \\where \\use \\level levelProp (A : \\Type) (p : \\Pi (x y : A) -> x = y) : \\Pi (x y : A) -> x = y => p");
    assertEquals(-1, getDefinition("test").getParametersLevels().get(0).level);
  }

  @Test
  public void missingResultTypeTest() {
    typeCheckModule(
      "\\func test (A : \\Type) (p : \\Pi (x y : A) -> x = y) => A\n" +
      "  \\where \\use \\level levelProp (A : \\Type) (p : \\Pi (x y : A) -> x = y) => p", 1);
  }

  @Test
  public void severalUseLevelsTest1() {
    typeCheckModule("""
      \\data D : \\oo-Type
      \\func f => D
        \\where {
          \\use \\level levelProp (x y : f) : x = y
          \\use \\level levelSet (x y : f) (p q : x = y) : p = q
        }
      """);
    assertEquals(1, getDefinition("f").getParametersLevels().size());
    assertEquals(-1, getDefinition("f").getParametersLevels().get(0).level);
  }

  @Test
  public void severalUseLevelsTest2() {
    typeCheckModule("""
      \\data D : \\Set
      \\func f (A : \\Set0) => D
        \\where {
          \\use \\level levelSet (A : \\Set0) (x y : f A) (p q : x = y) : p = q
          \\use \\level levelProp (A : \\Set0) (x y : f A) : x = y
        }
      """);
    assertEquals(1, getDefinition("f").getParametersLevels().size());
    assertEquals(-1, getDefinition("f").getParametersLevels().get(0).level);
  }

  @Test
  public void severalUseLevelsTest3() {
    typeCheckModule("""
      \\data D : \\Set
      \\func f (A : \\Set2) => D
        \\where {
          \\use \\level levelProp1 (A : \\Set1) (x y : f A) : x = y
          \\use \\level levelProp2 (A : \\Set2) (x y : f A) : x = y
        }
      """);
    assertEquals(2, getDefinition("f").getParametersLevels().size());
  }

  @Test
  public void severalUseLevelsTest4() {
    typeCheckModule("""
      \\data D : \\Set
      \\func f (A : \\Set2) => D
        \\where {
          \\use \\level levelProp1 (A : \\Set0) (x y : f A) : x = y
          \\use \\level levelProp2 (A : \\Set1) (x y : f A) : x = y
        }
      """);
    assertEquals(2, getDefinition("f").getParametersLevels().size());
  }

  @Test
  public void selfDataTest() {
    typeCheckModule(
      "\\data D | con Nat\n" +
      "  \\where \\use \\level levelProp (d1 d2 : D) : d1 = d2 => idp", 1);
  }

  @Test
  public void selfClassTest() {
    typeCheckModule(
      "\\record C (x : Nat)\n" +
      "  \\where \\use \\level levelProp (c1 c2 : C) : c1 = {C} c2 => idp", 1);
  }

  @Test
  public void selfClassTest2() {
    typeCheckModule(
      "\\record C (x : Nat)\n" +
      "  \\where \\use \\level levelProp (c1 c2 : C) : c1 = c2 => idp", 1);
  }

  @Test
  public void selfClassTest3() {
    typeCheckModule(
      "\\record C (x y : Nat)\n" +
      "  \\where \\use \\level levelProp (x : Nat) (c1 c2 : C x) : c1 = {C x} c2 => idp", 1);
  }

  @Test
  public void selfClassTest4() {
    typeCheckModule(
      "\\record C (x y : Nat)\n" +
      "  \\where \\use \\level levelProp (x : Nat) (c1 c2 : C x) : c1 = c2 => idp", 1);
  }

  @Test
  public void levelsTest() {
    typeCheckModule("""
      \\record R (A : \\Type) (p : \\Pi (a a' : A) -> a = a') (a : A)
        \\where \\use \\level levelProp (A : \\Prop) (p : \\Pi (a a' : A) -> a = a') (r1 r2 : R A p) : r1 = r2
          => path (\\lam i => \\new R A p { | a => p r1.a r2.a @ i })
      """);
    assertEquals(Sort.STD.succ(), ((ClassDefinition) getDefinition("R")).getSort());
  }

  @Test
  public void classArgTest() {
    typeCheckModule("""
      \\record C
        | T : \\Type
      \\record D \\extends C
        | tProp (x y : T) : x = y
      \\record R (cField : C) (tField : cField.T)
        \\where \\use \\level levelProp (d : D) (r1 r2 : R d) : r1 = r2
          => path (\\lam i => \\new R { | tField => d.tProp r1.tField r2.tField @ i })
      \\func f (d : D) : \\Prop => R d
      """);
  }

  @Test
  public void useFunctionTest() {
    typeCheckModule("""
      \\data Empty
      \\func empty (e : Empty) : \\Set
        \\where \\use \\level isProp (e : Empty) (x y : empty e) : x = y
      \\lemma lem (e : Empty) : empty e
      """);
    assertFalse(getDefinition("empty").getParametersLevels().isEmpty());
  }

  @Test
  public void useFunctionDefTest() {
    typeCheckModule("""
      \\data Empty
      \\func empty (e : Empty) : \\Set
        \\where \\use \\level isProp (e : Empty) (x y : empty e) : x = y
      \\func empty2 (e : Empty) => empty e
      \\lemma lem (e : Empty) : empty2 e
      """);
    assertFalse(getDefinition("empty2").getParametersLevels().isEmpty());
  }
}