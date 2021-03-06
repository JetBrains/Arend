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
    typeCheckModule(
      "\\data Empty\n" +
      "\\func absurd {A : \\Type} (e : Empty) : A\n" +
      "\\data Dec (A : \\Prop) | yes A | no (A -> Empty)\n" +
      "  \\where\n" +
      "    \\use \\level isProp {A : \\Prop} (d1 d2 : Dec A) : d1 = d2\n" +
      "      | yes a1, yes a2 => path (\\lam i => yes (Path.inProp a1 a2 @ i))\n" +
      "      | yes a1, no na2 => absurd (na2 a1)\n" +
      "      | no na1, yes a2 => absurd (na1 a2)\n" +
      "      | no na1, no na2 => path (\\lam i => no (\\lam a => (absurd (na1 a) : na1 a = na2 a) @ i))");
    DataDefinition data = (DataDefinition) getDefinition("Dec");
    assertEquals(Sort.PROP, data.getSort());
    assertFalse(data.isSquashed());
    FunctionDefinition def = (FunctionDefinition) getDefinition("Dec.isProp");
    assertNull(def.getBody());
    assertNotNull(def.getActualBody());
  }

  @Test
  public void testDataNotExact() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\func absurd {A : \\Type} (e : Empty) : A\n" +
      "\\data Dec (A : \\Type) | yes A | no (A -> Empty)\n" +
      "  \\where\n" +
      "    \\use \\level isProp {A : \\Prop} (d1 d2 : Dec A) : d1 = d2\n" +
      "      | yes a1, yes a2 => path (\\lam i => yes (Path.inProp a1 a2 @ i))\n" +
      "      | yes a1, no na2 => absurd (na2 a1)\n" +
      "      | no na1, yes a2 => absurd (na1 a2)\n" +
      "      | no na1, no na2 => path (\\lam i => no (\\lam a => (absurd (na1 a) : na1 a = na2 a) @ i))");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, 0, 0)), ((DataDefinition) getDefinition("Dec")).getSort());
  }

  @Test
  public void testDataSet() {
    typeCheckModule(
      "\\data D\n" +
      "  | con Nat Nat\n" +
      "  | con2 (n m k l : Nat) (p : n Nat.* l = k Nat.* m) (i : I) \\elim i {\n" +
      "    | left => con n m\n" +
      "    | right => con k l\n" +
      "  }\n" +
      "  | con3 (x y : D) (p q : x = y) (i j : I) \\elim i, j {\n" +
      "    | left, j => p @ j\n" +
      "    | right, j => q @ j\n" +
      "    | _, left => x\n" +
      "    | _, right => y\n" +
      "  }\n" +
      "  \\where\n" +
      "    \\use \\level isSet (d1 d2 : D) (p q : d1 = d2) => path (\\lam i => path (con3 d1 d2 p q i))");
    assertEquals(Sort.SET0, ((DataDefinition) getDefinition("D")).getSort());
  }

  @Test
  public void testClass() {
    typeCheckModule(
      "\\data D : \\Set\n" +
      "\\func absurd {A : \\Type} (e : D) : A\n" +
      "\\class C (d : D)\n" +
      "  \\where\n" +
      "    \\use \\level isProp (c1 c2 : C) : c1 = c2 => absurd c1.d");
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("D")).getSort());
    assertEquals(Sort.PROP, ((ClassDefinition) getDefinition("C")).getSort());
  }

  @Test
  public void testDataSet2() {
    typeCheckModule(
      "\\data Trunc (A : \\Type)\n" +
      "  | inc A\n" +
      "  | trunc (x y : Trunc A) (p q : x = y) (i j : I) \\elim i, j {\n" +
      "    | left, j => p @ j\n" +
      "    | right, j => q @ j\n" +
      "    | _, left => x\n" +
      "    | _, right => y\n" +
      "  }\n" +
      "  \\where\n" +
      "    \\use \\level isProp {A : \\Type} (d1 : Trunc A) : \\Pi (d2 : Trunc A) (p : d1 = d2) -> \\Pi (q : d1 = d2) -> p = q => \\lam d2 p q => path (\\lam i => path (trunc d1 d2 p q i))");
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("Trunc")).getSort());
  }

  @Test
  public void testClassFields() {
    typeCheckModule(
      "\\data D : \\Set\n" +
      "\\func absurd {A : \\Type} (d : D) : A\n" +
      "\\class C (x : Nat) (d : D)\n" +
      "  \\where\n" +
      "    \\use \\level isProp (x : Nat) (c1 c2 : C x) : c1 = c2 => absurd c1.d\n" +
      "\\func f : \\Prop => C 0");
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("D")).getSort());
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("C")).getSort());
  }

  @Test
  public void testClassFields2() {
    typeCheckModule(
      "\\data D\n" +
      "\\func absurd {A : \\Type} (d : D) : A\n" +
      "\\class C {A B : \\Type} (f : A -> B) (d : D)\n" +
      "  \\where\n" +
      "    \\use \\level isProp {A B : \\Type} (f : A -> B) (c1 c2 : C f) : c1 = c2 => absurd c1.d\n" +
      "\\func f : \\Prop => C (\\lam (x : Nat) => x)");
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
    typeCheckModule(
      "\\func f : \\Prop => D\n" +
      "\\data Empty\n" +
      "\\func absurd {A : \\Type} (e : Empty) : A\n" +
      "\\data D | con1 | con2 Empty \\where\n" +
      "  \\use \\level isProp (d1 d2 : D) : d1 = d2 \\elim d1, d2\n" +
      "    | con1, con1 => idp\n" +
      "    | _, con2 e => absurd e\n" +
      "    | con2 e, _ => absurd e");
  }

  @Test
  public void useRecordTest() {
    typeCheckModule(
      "\\record R\n" +
      "  \\where \\use \\level isProp : \\Pi (x y : R) -> x = y => \\lam x y => idp");
  }

  @Test
  public void restrictedDataInCaseTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (x : TrP Nat) : D (\\new B) => \\case x \\with { | inP _ => ddd }");
  }

  @Test
  public void restrictedDataInCaseTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (x : TrP Nat) : D (\\new A) => \\case x \\with { | inP _ => ddd }", 1);
  }

  @Test
  public void restrictedClassInCaseTest() {
    typeCheckModule(
      "\\record A (x : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (d : D) (x : TrP Nat) : C (\\new B 0) => \\case x \\with { | inP _ => \\new C { | df => d } }");
  }

  @Test
  public void restrictedClassInCaseTestError() {
    typeCheckModule(
      "\\record A (x : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (d : D) (x : TrP Nat) : C (\\new A 0) => \\case x \\with { | inP _ => \\new C { | df => d } }", 1);
  }

  @Test
  public void restrictedDataInLemmaTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\lemma f : D (\\new B) => ddd");
  }

  @Test
  public void restrictedDataInLemmaTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\lemma f : D (\\new A) => ddd", 1);
  }

  @Test
  public void restrictedClassInLemmaTest() {
    typeCheckModule(
      "\\record A (x : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\lemma f (d : D) : C (\\new B 0) => \\new C { | df => d }");
  }

  @Test
  public void restrictedClassInLemmaTestError() {
    typeCheckModule(
      "\\record A (x : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\lemma f (d : D) : C (\\new A 0) => \\new C { | df => d }", 1);
  }

  @Test
  public void restrictedDataInPropertyTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\record R { \\property prop : D (\\new B) }");
    assertEquals(Sort.SET0, ((ClassDefinition) getDefinition("R")).getSort());
  }

  @Test
  public void restrictedDataInPropertyTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => idp\n" +
      "\\record R { \\property prop : D (\\new A) }", 1);
  }

  @Test
  public void restrictedClassInPropertyTest() {
    typeCheckModule(
      "\\record A (X : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\record R { \\property prop : C (\\new B 0) }");
  }

  @Test
  public void restrictedClassInPropertyTestError() {
    typeCheckModule(
      "\\record A (x : Nat)\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\record R { \\property prop : C (\\new A 0) }", 1);
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
    typeCheckModule(
      "\\data D : \\oo-Type\n" +
      "\\func f => D\n" +
      "  \\where {\n" +
      "    \\use \\level levelProp (x y : f) : x = y\n" +
      "    \\use \\level levelSet (x y : f) (p q : x = y) : p = q\n" +
      "  }");
    assertEquals(1, getDefinition("f").getParametersLevels().size());
    assertEquals(-1, getDefinition("f").getParametersLevels().get(0).level);
  }

  @Test
  public void severalUseLevelsTest2() {
    typeCheckModule(
      "\\data D : \\Set\n" +
      "\\func f (A : \\Set0) => D\n" +
      "  \\where {\n" +
      "    \\use \\level levelSet (A : \\Set0) (x y : f A) (p q : x = y) : p = q\n" +
      "    \\use \\level levelProp (A : \\Set0) (x y : f A) : x = y\n" +
      "  }");
    assertEquals(1, getDefinition("f").getParametersLevels().size());
    assertEquals(-1, getDefinition("f").getParametersLevels().get(0).level);
  }

  @Test
  public void severalUseLevelsTest3() {
    typeCheckModule(
      "\\data D : \\Set\n" +
      "\\func f (A : \\Set2) => D\n" +
      "  \\where {\n" +
      "    \\use \\level levelProp1 (A : \\Set1) (x y : f A) : x = y\n" +
      "    \\use \\level levelProp2 (A : \\Set2) (x y : f A) : x = y\n" +
      "  }");
    assertEquals(2, getDefinition("f").getParametersLevels().size());
  }

  @Test
  public void severalUseLevelsTest4() {
    typeCheckModule(
      "\\data D : \\Set\n" +
      "\\func f (A : \\Set2) => D\n" +
      "  \\where {\n" +
      "    \\use \\level levelProp1 (A : \\Set0) (x y : f A) : x = y\n" +
      "    \\use \\level levelProp2 (A : \\Set1) (x y : f A) : x = y\n" +
      "  }");
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
  public void sortArgError() {
    typeCheckModule(
      "\\record R {A B : \\Type} (prop : \\Pi (b b' : B) -> b = b') (a a' : A) (proof : a = a') (bField : B)\n" +
      "  \\where \\use \\level levelProp (A B : \\Set) (prop : \\Pi (b b' : B) -> b = b') (a a' : A) (r1 r2 : R prop a a') : r1 = r2\n" +
      "    => path (\\lam i => \\new R { | bField => prop r1.bField r2.bField @ i | proof => Path.inProp {a = a'} r1.proof r2.proof @ i })\n" +
      "\\func f {B : \\Set} (prop : \\Pi (b b' : B) -> b = b') : \\Prop => R prop 0 1", 1);
  }

  @Test
  public void classArgTest() {
    typeCheckModule(
      "\\record C\n" +
      "  | T : \\Type\n" +
      "\\record D \\extends C\n" +
      "  | tProp (x y : T) : x = y\n" +
      "\\record R (cField : C) (tField : cField.T)\n" +
      "  \\where \\use \\level levelProp (d : D) (r1 r2 : R d) : r1 = r2\n" +
      "    => path (\\lam i => \\new R { | tField => d.tProp r1.tField r2.tField @ i })\n" +
      "\\func f (d : D) : \\Prop => R d");
  }

  @Test
  public void useFunctionTest() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\func empty (e : Empty) : \\Set\n" +
      "  \\where \\use \\level isProp (e : Empty) (x y : empty e) : x = y\n" +
      "\\lemma lem (e : Empty) : empty e");
    assertFalse(getDefinition("empty").getParametersLevels().isEmpty());
  }

  @Test
  public void useFunctionDefTest() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\func empty (e : Empty) : \\Set\n" +
      "  \\where \\use \\level isProp (e : Empty) (x y : empty e) : x = y\n" +
      "\\func empty2 (e : Empty) => empty e\n" +
      "\\lemma lem (e : Empty) : empty2 e");
    assertFalse(getDefinition("empty2").getParametersLevels().isEmpty());
  }
}