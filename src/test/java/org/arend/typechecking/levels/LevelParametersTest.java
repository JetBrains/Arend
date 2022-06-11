package org.arend.typechecking.levels;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ListLevels;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

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
      "\\func test \\plevels p1 >= p2 => f \\levels (p1,p2) (\\suc \\lh, \\suc (\\suc \\lh)) Nat", 2);
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
      "  \\where \\use \\coerce test \\plevels p3 <= p4 (A : \\Type p4) (n : Nat) => con n");
    assertEquals(3, getDefinition("D.test").getLevelParameters().size());
  }

  @Test
  public void useTest2() {
    typeCheckModule(
      "\\data D \\plevels p1 <= p2 | con Nat\n" +
      "  \\where \\use \\coerce test (A : \\Type p2) (n : Nat) => con n");
    assertEquals(3, getDefinition("D.test").getLevelParameters().size());
  }

  @Test
  public void useError() {
    typeCheckModule(
      "\\data D \\plevels p1 <= p2 | con Nat\n" +
      "  \\where \\use \\coerce test \\plevels p1 >= p2 (n : Nat) => con n", 1);
  }

  @Test
  public void useDerived() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\data D (r : R) | con Nat\n" +
      "  \\where \\use \\coerce test (n : Nat) => con n");
    assertEquals(2, getDefinition("D.test").getLevelParameters().size());
  }

  @Test
  public void useDerived2() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\data D (r : R) | con Nat\n" +
      "  \\where \\use \\coerce test (r : R) (n : Nat) => con n");
    Definition def = getDefinition("D.test");
    List<? extends LevelVariable> params = def.getLevelParameters();
    assertEquals(2, params.size());
    assertEquals(new ListLevels(Arrays.asList(new Level(params.get(0)), new Level(params.get(1)))), ((ClassCallExpression) def.getParameters().getTypeExpr()).getLevels());
  }

  @Test
  public void useDerivedError() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\data D (r : R) | con Nat\n" +
      "  \\where \\use \\coerce test \\plevels p1 >= p2 (n : Nat) => con n", 1);
  }

  @Test
  public void useDerivedError2() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "\\data D (r : R) | con Nat\n" +
      "  \\where \\use \\coerce test \\plevels p1 >= p2 (r : R) (n : Nat) => con n", 1);
  }

  @Test
  public void defaultTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2 <= p3\n" +
      "  | f : Nat\n" +
      "\\record S \\extends R {\n" +
      "  \\default f : Nat => 0\n" +
      "}");
    assertEquals(3, getDefinition("R").getLevelParameters().size());
    assertEquals(3, getDefinition("S").getLevelParameters().size());
    assertEquals(3, getDefinition("S.f").getLevelParameters().size());
    ClassDefinition classDef = (ClassDefinition) getDefinition("S");
    Expression impl = classDef.getDefault((ClassField) getDefinition("R.f")).getExpression();
    List<? extends Level> levels = ((FunCallExpression) impl).getLevels().toList();
    List<? extends LevelVariable> params = classDef.getLevelParameters();
    assertEquals(Arrays.asList(new Level(params.get(0)), new Level(params.get(1)), new Level(params.get(2))), levels);
  }

  @Ignore
  @Test
  public void defaultTest2() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2\n" +
      "  | f : \\hType (\\suc p2)\n" +
      "\\record S \\extends R {\n" +
      "  \\default f \\plevels p1 <= p2 : \\hType (\\suc p2) => \\hType p1\n" +
      "}");
    assertEquals(3, getDefinition("S.f").getLevelParameters().size());
    ClassDefinition classDef = (ClassDefinition) getDefinition("S");
    Expression impl = classDef.getDefault((ClassField) getDefinition("R.f")).getExpression();
    List<? extends Level> levels = ((FunCallExpression) impl).getLevels().toList();
    List<? extends LevelVariable> params = classDef.getLevelParameters();
    assertEquals(Arrays.asList(new Level(params.get(0)), new Level(params.get(1)), new Level(params.get(2))), levels);
  }

  @Test
  public void coclauseTest() {
    typeCheckModule(
      "\\record R (A : \\Type)\n" +
      "\\func g \\plevels p1 <= p2 : R \\cowith\n" +
      "  | A : \\Type => \\Sigma");
    assertEquals(3, getDefinition("g.A").getLevelParameters().size());
    Expression impl = ((ClassCallExpression) ((FunctionDefinition) getDefinition("g")).getResultType()).getAbsImplementationHere((ClassField) getDefinition("R.A"));
    assertNotNull(impl);
    List<? extends Level> levels = ((FunCallExpression) impl).getLevels().toList();
    List<? extends LevelVariable> params = getDefinition("g").getLevelParameters();
    assertEquals(Arrays.asList(new Level(params.get(0)), new Level(params.get(1)), new Level(params.get(2))), levels);
  }

  @Test
  public void coclauseTest2() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2 (x : \\hType (\\suc p2))\n" +
      "\\func g \\plevels p1 <= p2 : R \\levels (p1,p2) _ \\cowith\n" +
      "  | x : \\hType (\\suc p2) => \\hType p1");
  }

  @Test
  public void metaTest() {
    typeCheckModule(
      "\\meta m \\plevels p1, p2 => \\Sigma (\\Type p1) (\\Type p2)\n" +
      "\\func f => m \\levels (1,2) _");
    DependentLink params = ((SigmaExpression) Objects.requireNonNull(((FunctionDefinition) getDefinition("f")).getBody())).getParameters();
    assertEquals(new UniverseExpression(new Sort(new Level(1), new Level(LevelVariable.HVAR))), params.getTypeExpr());
    assertEquals(new UniverseExpression(new Sort(new Level(2), new Level(LevelVariable.HVAR))), params.getNext().getTypeExpr());
  }

  @Test
  public void dynamicTest() {
    typeCheckModule(
      "\\record R \\plevels p1 <= p2 <= p3 {\n" +
      "  \\func f => 0\n" +
      "}");
    assertEquals(3, getDefinition("R.f").getLevelParameters().size());
    Concrete.ReferenceExpression type = (Concrete.ReferenceExpression) ((Concrete.ClassExtExpression) Objects.requireNonNull(((Concrete.FunctionDefinition) getConcrete("R.f")).getParameters().get(0).getType())).getBaseClassExpression();
    assertNotNull(type.getPLevels());
    assertNull(type.getHLevels());
  }

  @Test
  public void dynamicTest2() {
    typeCheckModule(
      "\\record S \\plevels p1 <= p2 <= p3\n" +
      "\\record R \\extends S {\n" +
      "  \\func f => 0\n" +
      "}");
    assertEquals(3, getDefinition("R.f").getLevelParameters().size());
    Concrete.ReferenceExpression type = (Concrete.ReferenceExpression) ((Concrete.ClassExtExpression) Objects.requireNonNull(((Concrete.FunctionDefinition) getConcrete("R.f")).getParameters().get(0).getType())).getBaseClassExpression();
    assertNotNull(type.getPLevels());
    assertNull(type.getHLevels());
  }

  @Test
  public void levelsNotErased() {
    Definition def = typeCheckDef("\\record C \\hlevels lh (A : \\hType)");
    assertEquals(2, def.getLevelParameters().size());
  }
}
