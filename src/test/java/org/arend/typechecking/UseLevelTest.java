package org.arend.typechecking;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.DataDefinition;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UseLevelTest extends TypeCheckingTestCase {
  @Test
  public void testPrelude() {
    assertEquals(Sort.PROP, Prelude.PROP_TRUNC.getSort());
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), Prelude.SET_TRUNC.getSort());
  }

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
      "      | no na1, no na2 => path (\\lam i => no (\\lam a => (absurd (na1 a) : na1 a = na2 a) @ i))", 1);
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
      "    \\use \\level isProp {A : \\Type} (d1 : Trunc A) => \\lam (d2 : Trunc A) (p : d1 = d2) => \\lam (q : d1 = d2) => path (\\lam i => path (trunc d1 d2 p q i))");
    assertEquals(Sort.SetOfLevel(new Level(LevelVariable.PVAR)), ((DataDefinition) getDefinition("Trunc")).getSort());
  }
}