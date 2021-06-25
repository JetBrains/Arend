package org.arend.typechecking.levels;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.FunCallExpression;
import org.arend.core.expr.UniverseExpression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LevelParametersTest extends TypeCheckingTestCase {
  @Test
  public void levelsTest() {
    typeCheckDef("\\func test \\plevels p1 >= p2 (A : \\Type p2) : \\Type p1 => A");
  }

  @Test
  public void levelsTest2() {
    typeCheckDef("\\func test \\hlevels h1 <= h2 (A : \\Type \\lp h1) : \\Type \\lp h2 => A");
  }

  @Test
  public void levelsError() {
    typeCheckDef("\\func test \\plevels p1 <= p2 (A : \\Type p2) : \\Type p1 => A", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void resolveError() {
    resolveNamesDef("\\func test \\plevels p1 <= p2 => \\Type p3", 1);
    assertThatErrorsAre(Matchers.notInScope("p3"));
  }

  @Test
  public void lpTest() {
    typeCheckDef("\\func test \\plevels p1 <= p2 (A : \\Type \\lp) : \\Type p2 => A");
  }

  @Test
  public void lpInferTest() {
    typeCheckDef("\\func test \\plevels p1 <= p2 (A : \\Type) : \\Type p2 => A");
  }

  @Test
  public void levelTypeError() {
    resolveNamesDef("\\func test \\hlevels h1 <= h2 (A : \\Type h2) => 0", 1);
    assertThatErrorsAre(Matchers.notInScope("h2"));
  }

  @Test
  public void noPLevelTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test \\plevels => \\Type");
    assertEquals(new UniverseExpression(new Sort(new Level(0), new Level(LevelVariable.HVAR))), def.getBody());
  }

  @Test
  public void noPLevelsTest2() {
    typeCheckDef("\\func test \\plevels (A : \\Type) : \\Type => A");
  }

  @Test
  public void noPLevelsTest3() {
    typeCheckDef("\\func test \\plevels \\hlevels h1 >= h2 => Nat");
  }

  @Test
  public void noHLevelTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test \\hlevels => \\Type", 1);
    assertEquals(new UniverseExpression(Sort.PROP), def.getBody());
    assertThatErrorsAre(Matchers.warning());
  }

  @Test
  public void noHLevelsTest2() {
    typeCheckDef("\\func test \\plevels p1 >= p2 \\hlevels (A : \\Type p2) : \\Type p1 => A", 2);
    assertThatErrorsAre(Matchers.warning(), Matchers.warning());
  }

  @Test
  public void noHLevelsTest3() {
    typeCheckDef("\\func test \\plevels p1 >= p2 \\hlevels => Nat");
  }

  @Test
  public void maxLevelTest() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\func test \\plevels p1 >= p2 \\hlevels h1 <= h2 (A : \\Type p1 h1) (B : \\Type p2 h2) => A -> B");
    assertEquals(new Sort(new Level(def.getLevelParameters().get(0)), new Level(def.getLevelParameters().get(3))), def.getResultType().toSort());
  }

  @Test
  public void applyLevels() {
    typeCheckModule(
      "\\func f \\plevels p1 <= p2 \\hlevels h1 >= h2 (A : \\Type) => A\n" +
      "\\func test \\plevels p1 >= p2 => f \\levels (p2,p1) (\\suc (\\suc \\lh), \\suc \\lh) Nat");
  }

  @Test
  public void applyLevels2() {
    typeCheckModule(
      "\\func f \\plevels p1 <= p2 \\hlevels h1 >= h2 (A : \\Type) => A\n" +
      "\\func test => f \\levels (3,7) (5,4) Nat");
  }

  @Test
  public void applyLevelsError() {
    typeCheckModule(
      "\\func f \\plevels p1 <= p2 \\hlevels h1 >= h2 (A : \\Type) => A\n" +
      "\\func test \\plevels p1 >= p2 => f \\levels (p1,p2) (\\suc \\lh, \\suc (\\suc \\lh)) Nat", 1);
  }

  @Test
  public void applyLevelsError2() {
    typeCheckModule(
      "\\func f \\plevels p1 <= p2 \\hlevels h1 >= h2 (A : \\Type) => A\n" +
      "\\func test => f \\levels (3,7) (4,5) Nat", 1);
  }

  @Test
  public void useTest() {
    typeCheckModule(
      "\\data D \\plevels p1 <= p2 | con Nat\n" +
      "  \\where \\use \\coerce test \\plevels p1 <= p2 (n : Nat) => con n");
  }

  @Test
  public void useError() {
    typeCheckModule(
      "\\data D \\plevels p1 <= p2 | con Nat\n" +
      "  \\where \\use \\coerce test (n : Nat) => con n", 1);
  }

  @Test
  public void defaultTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "  | f : Nat\n" +
      "\\record S \\extends R {\n" +
      "  \\default f : Nat => n\n" +
      "}");
    assertEquals(2, getDefinition("S.f").getLevelParameters().size());
  }

  @Test
  public void coclauseTest() {
    typeCheckModule(
      "\\record R (x : Nat)\n" +
      "\\func g \\plevels p1 <= p2 : R \\cowith\n" +
      "  | x : Nat => 0");
    assertEquals(3, getDefinition("g.x").getLevelParameters().size());
    Expression impl = ((ClassCallExpression) ((FunctionDefinition) getDefinition("g")).getResultType()).getAbsImplementationHere((ClassField) getDefinition("R.x"));
    assertNotNull(impl);
    List<? extends Level> levels = ((FunCallExpression) impl).getLevels().toList();
    List<LevelVariable> params = getDefinition("g").getLevelParameters();
    assertEquals(Arrays.asList(new Level(params.get(0)), new Level(params.get(1)), new Level(params.get(2))), levels);
  }
}
