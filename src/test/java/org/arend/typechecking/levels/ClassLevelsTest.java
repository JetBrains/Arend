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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClassLevelsTest extends TypeCheckingTestCase {
  @Test
  public void superTest() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\func test (s : S \\lp) : \\Type (\\suc \\lp) => s.A");
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 2), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("S")).getSort());
  }

  @Test
  public void superError() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\func test (s : S \\lp) : \\Type \\lp => s.A", 1);
  }

  @Test
  public void multiTest() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record R' (B : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp), R'\n" +
      "\\func test (s : S \\lp) : \\Type \\lp => s.B");
  }

  @Test
  public void transitiveTest() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\record T \\extends S\n" +
      "\\func test (t : T \\lp) : \\Type (\\suc \\lp) => t.A");
  }

  @Test
  public void transitiveError() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\record T \\extends S\n" +
      "\\func test (t : T \\lp) : \\Type \\lp => t.A", 1);
  }

  @Test
  public void transitiveTest2() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S (B : \\Type) \\extends R (\\suc \\lp)\n" +
      "\\record T \\extends S\n" +
      "\\func test (t : T \\lp) : \\Type \\lp => t.B");
  }

  @Test
  public void doubleTransitiveTest() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\record T \\extends S (\\suc \\lp)\n" +
      "\\func test (t : T \\lp) : \\Type (\\suc (\\suc \\lp)) => t.A");
  }

  @Test
  public void doubleTransitiveError() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\record S \\extends R (\\suc \\lp)\n" +
      "\\record T \\extends S (\\suc \\lp)\n" +
      "\\func test (t : T \\lp) : \\Type (\\suc \\lp) => t.A", 1);
  }

  @Test
  public void diamondTest() {
    typeCheckModule(
      "\\record Base\n" +
      "\\record R \\extends Base (\\suc \\lp)\n" +
      "\\record S \\extends Base (\\suc \\lp)\n" +
      "\\record T \\extends R, S");
    Levels levels = ((ClassDefinition) getDefinition("T")).getSuperLevels().get((ClassDefinition) getDefinition("Base"));
    assertEquals(new LevelPair(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR)), levels);
  }

  @Test
  public void diamondTest2() {
    typeCheckModule(
      "\\record Base (A : \\Type)\n" +
      "\\record R \\extends Base (\\suc \\lp)\n" +
      "\\record S \\extends Base\n" +
      "\\record T \\extends R, S (\\suc \\lp)");
    Levels levels = ((ClassDefinition) getDefinition("T")).getSuperLevels().get((ClassDefinition) getDefinition("Base"));
    assertEquals(new LevelPair(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR)), levels);
  }

  @Test
  public void diamondError() {
    typeCheckModule(
      "\\record Base\n" +
      "\\record R \\extends Base (\\suc \\lp)\n" +
      "\\record S \\extends Base\n" +
      "\\record T \\extends R, S", 1);
    assertThatErrorsAre(Matchers.typecheckingError(SuperLevelsMismatchError.class));
  }

  @Test
  public void diamondError2() {
    typeCheckModule(
      "\\record Base\n" +
      "\\record R \\extends Base (\\suc (\\suc \\lp))\n" +
      "\\record S \\extends Base (\\suc \\lp)\n" +
      "\\record T \\extends R, S", 1);
    assertThatErrorsAre(Matchers.typecheckingError(SuperLevelsMismatchError.class));
  }

  @Test
  public void extendsTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R\n" +
      "  | A : \\Type p1\n" +
      "\\record T \\extends R\n" +
      "\\record X \\extends S, T\n" +
      "  | B : \\Type p2");
    assertEquals(3, getDefinition("S").getLevelParameters().size());
    assertEquals(3, getDefinition("T").getLevelParameters().size());
    assertEquals(3, getDefinition("X").getLevelParameters().size());
  }

  @Test
  public void extendsMin() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\plevels p3 <= p4 \\extends R", 1);
  }

  @Test
  public void extendsTest2() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\plevels p1 <= p2\n" +
      "\\record T \\extends R, S\n" +
      "  | A : \\Type p2", 1);
  }

  @Test
  public void extendsTest3() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\plevels p3 >= p4\n" +
      "\\record T \\extends R, S", 1);
  }

  @Test
  public void extendsTest4() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\plevels p3 >= p4 \\extends R (p4,p3)\n" +
      "  | A : \\Type p4\n" +
      "\\record T \\plevels p5 <= p6 <= p7 \\extends R (p6,p7)\n" +
      "\\record X \\extends S (\\lp,\\lp), T (\\lp,\\lp,\\lp)\n" +
      "  | B : \\Type \\lp");
    assertNull(getDefinition("X").getLevelParameters());
  }

  @Test
  public void lpTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R (\\lp,\\lp)");
    assertNull(getDefinition("S").getLevelParameters());
  }

  @Test
  public void lpError() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\record S \\extends R (\\suc \\lp, \\lp)", 1);
  }
}
