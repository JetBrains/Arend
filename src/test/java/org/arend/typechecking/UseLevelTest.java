package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    assertEquals(Sort.PROP, ((DataDefinition) getDefinition("Dec")).getSort());
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
    assertEquals(Sort.STD, ((DataDefinition) getDefinition("Dec")).getSort());
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
      "  }" +
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
      "\\func f (c : C) : c = c => path (\\lam _ => c)\n" +
      "\\class C \\where \\use \\level isProp (c1 c2 : C) : c1 = c2 => f c1");
  }

  @Test
  public void mutualRecursion2() {
    typeCheckModule(
      "\\class C \\where \\use \\level isProp (c1 c2 : C) : c1 = c2 => f c1\n" +
      "\\func f (c : C) : c = c => path (\\lam _ => c)");
  }

  @Test
  public void orderTest() {
    typeCheckModule(
      "\\func f : \\Prop => D\n" +
      "\\data Empty\n" +
      "\\func absurd {A : \\Type} (e : Empty) : A\n" +
      "\\data D | con1 | con2 Empty \\where\n" +
      "  \\use \\level isProp (d1 d2 : D) : d1 = d2 \\elim d1, d2\n" +
      "    | con1, con1 => path (\\lam _ => con1)\n" +
      "    | _, con2 e => absurd e\n" +
      "    | con2 e, _ => absurd e");
  }

  @Test
  public void useFunctionTest() {
    typeCheckModule(
      "\\data Empty : \\Set\n" +
      "\\func empty => Empty\n" +
      "  \\where \\use \\level isProp (x y : empty) : x = y\n" +
      "\\lemma lem (x : empty) : empty => x");
  }

  @Test
  public void useRecordTest() {
    typeCheckModule(
      "\\record R\n" +
      "  \\where \\use \\level isProp : \\Pi (x y : R) -> x = y => \\lam x y => path (\\lam _ => x)");
  }

  @Test
  public void restrictedDataInCaseTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (x : TrP Nat) : D (\\new B) => \\case x \\with { | inP _ => ddd }");
  }

  @Test
  public void restrictedDataInCaseTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (x : TrP Nat) : D (\\new A) => \\case x \\with { | inP _ => ddd }", 1);
  }

  @Test
  public void restrictedClassInCaseTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (d : D) (x : TrP Nat) : C (\\new B) => \\case x \\with { | inP _ => \\new C { | df => d } }");
  }

  @Test
  public void restrictedClassInCaseTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\data TrP (A : \\Type) | inP A | truncP (x y : TrP A) (i : I) \\elim i { | left => x | right => y }\n" +
      "\\func f (d : D) (x : TrP Nat) : C (\\new A) => \\case x \\with { | inP _ => \\new C { | df => d } }", 1);
  }

  @Test
  public void restrictedDataInLemmaTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\lemma f : D (\\new B) => ddd");
  }

  @Test
  public void restrictedDataInLemmaTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\lemma f : D (\\new A) => ddd", 1);
  }

  @Test
  public void restrictedClassInLemmaTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\lemma f (d : D) : C (\\new B) => \\new C { | df => d }");
  }

  @Test
  public void restrictedClassInLemmaTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\lemma f (d : D) : C (\\new A) => \\new C { | df => d }", 1);
  }

  @Test
  public void restrictedDataInPropertyTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\record R { \\property prop : D (\\new B) }");
  }

  @Test
  public void restrictedDataInPropertyTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D (a : A) : \\Set | ddd\n" +
      "  \\where \\use \\level isProp {b : B} (x y : D b) : x = y | ddd, ddd => path (\\lam _ => ddd)\n" +
      "\\record R { \\property prop : D (\\new A) }", 1);
  }

  @Test
  public void restrictedClassInPropertyTest() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\record R { \\property prop : C (\\new B) }");
  }

  @Test
  public void restrictedClassInPropertyTestError() {
    typeCheckModule(
      "\\record A\n" +
      "\\record B \\extends A\n" +
      "\\data D : \\Set\n" +
      "\\record C (a : A) (df : D)\n" +
      "  \\where \\use \\level isProp {b : B} (x y : C b) : x = y => path (\\lam i => \\new C b ((\\case x.df \\return x.df = y.df \\with {}) @ i))\n" +
      "\\record R { \\property prop : C (\\new A) }", 1);
  }
}