package org.arend.typechecking.levels;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.sort.Level;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LevelsDefinitionTest extends TypeCheckingTestCase {
  @Test
  public void defTest() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A");
  }

  @Test
  public void notInScopeError() {
    resolveNamesModule(
      "\\hlevels p1 <= p2\n" +
      "\\func test (A : \\Type p1) : \\Type p2 => A", 2);
    assertThatErrorsAre(Matchers.notInScope("p1"), Matchers.notInScope("p2"));
  }

  @Test
  public void differentVarsError() {
    typeCheckModule(
      """
        \\plevels p1
        \\plevels p2
        \\func test (A : \\Type p1) (B : \\Type p2) => A
        """, 1);
  }

  @Test
  public void alreadyWithVarsTest() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test \\hlevels h (A : \\Type p1 h) : \\Type p2 => A");
  }

  @Test
  public void alreadyWithVarsError() {
    typeCheckModule(
      "\\plevels p1 <= p2\n" +
      "\\func test \\plevels p3 (A : \\Type p1) : \\Type p2 => A", 1);
  }

  @Test
  public void openTest() {
    typeCheckModule(
      """
        \\module M \\where {
          \\plevels p1 <= p2
        }
        \\open M
        \\func test (A : \\Type p1) : \\Type p2 => A
        """);
  }

  @Test
  public void openTest2() {
    typeCheckModule(
      """
        \\module M \\where {
          \\plevels p1 <= p2
        }
        \\open M(p1,p2)
        \\func test (A : \\Type p1) : \\Type p2 => A
        """);
  }

  @Test
  public void derivedTest() {
    typeCheckModule(
      """
        \\plevels p1 <= p2
        \\record R (A : \\Type p2)
        \\func test (r : R) => 0
        """);
    FunctionDefinition def = (FunctionDefinition) getDefinition("test");
    List<? extends LevelVariable> levels = def.getLevelParameters();
    assertEquals(3, levels.size());
    assertEquals(List.of(new Level(levels.get(0)), new Level(levels.get(1)), new Level(levels.get(2))), ((ClassCallExpression) def.getParameters().getTypeExpr()).getLevels().toList());
  }

  @Test
  public void derivedTest2() {
    typeCheckModule(
      """
        \\plevels p1 <= p2
        \\record R (A : \\Type p2)
        \\func test (r : R) (B : \\Type p2) => 0
        """);
    FunctionDefinition def = (FunctionDefinition) getDefinition("test");
    List<? extends LevelVariable> levels = def.getLevelParameters();
    assertEquals(3, levels.size());
    assertEquals(List.of(new Level(levels.get(0)), new Level(levels.get(1)), new Level(levels.get(2))), ((ClassCallExpression) def.getParameters().getTypeExpr()).getLevels().toList());
  }

  @Test
  public void derivedTest3() {
    typeCheckModule(
      """
        \\plevels p1 <= p2
        \\record R (A : \\Type p1)
        \\record S (A : \\Type p2)
        \\func test (r : R) (s : S) => 0
        """);
    FunctionDefinition def = (FunctionDefinition) getDefinition("test");
    List<? extends LevelVariable> levels = def.getLevelParameters();
    assertEquals(3, levels.size());
    assertEquals(List.of(new Level(levels.get(0)), new Level(levels.get(1)), new Level(levels.get(2))), ((ClassCallExpression) def.getParameters().getTypeExpr()).getLevels().toList());
  }
}
