package org.arend.typechecking.levels;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.core.subst.Levels;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.SuperLevelsMismatchError;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClassLevelsTest extends TypeCheckingTestCase {
  @Test
  public void superTest() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\func test (s : S \\lp) : \\Type (\\suc \\lp) => s.A
        """);
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 2), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("S")).getSort());
  }

  @Test
  public void superError() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\func test (s : S \\lp) : \\Type \\lp => s.A
        """, 1);
  }

  @Test
  public void multiTest() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record R' (B : \\Type)
        \\record S \\extends R (\\suc \\lp), R'
        \\func test (s : S \\lp) : \\Type \\lp => s.B
        """);
  }

  @Test
  public void transitiveTest() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\record T \\extends S
        \\func test (t : T \\lp) : \\Type (\\suc \\lp) => t.A
        """);
  }

  @Test
  public void transitiveError() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\record T \\extends S
        \\func test (t : T \\lp) : \\Type \\lp => t.A
        """, 1);
  }

  @Test
  public void transitiveTest2() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S (B : \\Type) \\extends R (\\suc \\lp)
        \\record T \\extends S
        \\func test (t : T \\lp) : \\Type \\lp => t.B
        """);
  }

  @Test
  public void doubleTransitiveTest() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\record T \\extends S (\\suc \\lp)
        \\func test (t : T \\lp) : \\Type (\\suc (\\suc \\lp)) => t.A
        """);
  }

  @Test
  public void doubleTransitiveError() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R (\\suc \\lp)
        \\record T \\extends S (\\suc \\lp)
        \\func test (t : T \\lp) : \\Type (\\suc \\lp) => t.A
        """, 1);
  }

  @Test
  public void diamondTest() {
    typeCheckModule(
      """
        \\record Base (A : \\Type)
        \\record R \\extends Base (\\suc \\lp)
        \\record S \\extends Base (\\suc \\lp)
        \\record T \\extends R, S
        """);
    Levels levels = ((ClassDefinition) getDefinition("T")).getSuperLevels().get((ClassDefinition) getDefinition("Base"));
    assertEquals(new LevelPair(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR)), levels);
  }

  @Test
  public void diamondTest2() {
    typeCheckModule(
      """
        \\record Base (A : \\Type)
        \\record R \\extends Base (\\suc \\lp)
        \\record S \\extends Base
        \\record T \\extends R, S (\\suc \\lp)
        """);
    Levels levels = ((ClassDefinition) getDefinition("T")).getSuperLevels().get((ClassDefinition) getDefinition("Base"));
    assertEquals(new LevelPair(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR)), levels);
  }

  @Test
  public void diamondError() {
    typeCheckModule(
      """
        \\record Base (A : \\Type)
        \\record R \\extends Base (\\suc \\lp)
        \\record S \\extends Base
        \\record T \\extends R, S
        """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(SuperLevelsMismatchError.class));
  }

  @Test
  public void diamondError2() {
    typeCheckModule(
      """
        \\record Base (A : \\Type)
        \\record R \\extends Base (\\suc (\\suc \\lp))
        \\record S \\extends Base (\\suc \\lp)
        \\record T \\extends R, S
        """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(SuperLevelsMismatchError.class));
  }

  @Test
  public void extendsTest() {
    typeCheckModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S \\extends R
          | A : \\Type \\lp
        \\record T \\extends R
        \\record X \\extends S, T
          | B : \\Type \\lp
        """);
    assertEquals(2, getDefinition("R").getLevelParameters().size());
    assertEquals(3, getDefinition("S").getLevelParameters().size());
    assertEquals(2, getDefinition("T").getLevelParameters().size());
    assertEquals(3, getDefinition("X").getLevelParameters().size());
  }

  @Test
  public void extendsMin() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\plevels p3 <= p4 \\extends R");
  }

  @Test
  public void extendsTest2() {
    typeCheckModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S \\plevels p1 <= p2
        \\record T \\extends R, S
          | A : \\Type \\lp
        """);
  }

  @Test
  public void extendsTest3() {
    typeCheckModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S \\plevels p3 >= p4
        \\record T \\extends R, S
        """);
  }

  @Test
  public void extendsTest4() {
    typeCheckModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S \\plevels p3 >= p4 \\extends R (p4,p3)
          | A : \\Type p4
        \\record T \\plevels p5 <= p6 <= p7 \\extends R (p6,p7)
        \\record X \\extends S (\\lp,\\lp), T (\\lp,\\lp,\\lp)
          | B : \\Type \\lp
        """);
    assertNull(getDefinition("X").getLevelParameters());
  }

  @Test
  public void lpTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R (\\lp,\\lp)");
    assertEquals(Collections.singletonList(LevelVariable.PVAR), getDefinition("S").getLevelParameters());
  }

  @Test
  public void lpError() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R (\\suc \\lp, \\lp)", 1);
  }

  @Test
  public void extendsTest5() {
    typeCheckModule(
      """
        \\record R
        \\record S \\extends R
        \\record T \\plevels p1 <= p2 \\extends S
        """);
  }

  @Test
  public void extendsTest6() {
    typeCheckModule(
      """
        \\record R (A : \\Type)
        \\record S \\extends R
        \\record T \\plevels p1 <= p2 \\extends S
        """);
    ClassDefinition tClass = ((ClassDefinition) getDefinition("T"));
    assertEquals(LevelPair.STD, tClass.getSuperLevels().get((ClassDefinition) getDefinition("S")));
    assertEquals(LevelPair.STD, tClass.getSuperLevels().get((ClassDefinition) getDefinition("R")));
  }

  @Test
  public void extendsResolveTest() {
    typeCheckModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S (A : \\Type)
        \\record T \\extends R, S \\lp
        """);
  }

  @Test
  public void extendsResolveError() {
    resolveNamesModule(
      """
        \\record R \\plevels p1 <= p2
        \\record S
        \\record T \\extends R, S p3
        """, 1);
    assertThatErrorsAre(Matchers.notInScope("p3"));
  }

  @Test
  public void derivedLevels() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R");
    assertNull(((ClassDefinition) getDefinition("S")).getSuperLevels().get((ClassDefinition) getDefinition("R")));
  }

  @Test
  public void subclassTest() {
    typeCheckModule(
      """
        \\record A (X : \\Set)
        \\record B (Y : \\Type)
        \\record C \\extends A, B
        \\func test : A => \\new C Nat Nat
        """);
  }
}
