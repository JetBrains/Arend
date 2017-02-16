package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelMax;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DataPolyTest extends TypeCheckingTestCase {
  @Test
  public void dataWithoutTypeOmega() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (n : Nat) | con1 (n = n) | con2 Nat");
    assertEquals(Sort.SET0, dataDefinition.getSorts().toSort());
  }

  @Test
  public void dataWithoutTypeOmegaExplicit() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (n : Nat) : \\Set1 | con1 (n = n) | con2 Nat");
    assertEquals(Sort.SetOfLevel(1), dataDefinition.getSorts().toSort());
  }

  @Test
  public void dataWithoutTypeOmegaExplicitError() {
    typeCheckDef("\\data D (n : Nat) : \\Set1 | con1 \\Set0 | con2 Nat", 1);
  }

  @Test
  public void dataWithTypeOmega() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) | con1 (n = n) | con2 Nat | con3 (D A n)");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.SET0, dataDefinition.getSorts().toSort());
  }

  @Test
  public void dataWithTypeOmegaExplicit() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) : \\Set1 | con1 (n = n) | con2 Nat");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.SetOfLevel(1), dataDefinition.getSorts().toSort());
  }

  @Test
  public void dataWithTypeOmegaExplicitSet() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) : \\Set0 | con1 (n = n) | con2 Nat");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.SET0, dataDefinition.getSorts().toSort());
  }

  @Test
  public void dataWithTypeOmegaExplicitError() {
    typeCheckDef("\\data D (A : \\Type) (n : Nat) : \\Set1 | con1 (n = n) | con2 \\Set0", 1);
  }

  @Test
  public void dataOmega() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) | con A");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(dataDefinition.getPolyParams().get(0)), new Level(dataDefinition.getPolyParams().get(1)))), dataDefinition.getSorts());
  }

  @Test
  public void dataOmegaExplicit() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) : \\Type | con A");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(dataDefinition.getPolyParams().get(0)), new Level(dataDefinition.getPolyParams().get(1)))), dataDefinition.getSorts());
  }

  @Test
  public void dataOmegaProp() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) | con1 A | con2 (n = n)");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(
        new LevelMax(new Level(dataDefinition.getPolyParams().get(0))),
        new LevelMax(new Level(dataDefinition.getPolyParams().get(1))).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      dataDefinition.getSorts());
  }

  @Test
  public void dataOmegaPropExplicit() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) : \\Type | con1 (n = n) | con2 A");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(
        new LevelMax(new Level(dataDefinition.getPolyParams().get(0))),
        new LevelMax(new Level(dataDefinition.getPolyParams().get(1))).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      dataDefinition.getSorts());
  }

  @Test
  public void dataOmegaSet() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) | con1 (n = n) | con2 A | con3 Nat");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(
        new LevelMax(new Level(dataDefinition.getPolyParams().get(0))),
        new LevelMax(new Level(dataDefinition.getPolyParams().get(1))).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      dataDefinition.getSorts());
  }

  @Test
  public void dataOmegaSetExplicit() {
    DataDefinition dataDefinition = (DataDefinition) typeCheckDef("\\data D (A : \\Type) (n : Nat) : \\Type | con1 (n = n) | con2 A | con3 Nat");
    assertThat(dataDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(
        new LevelMax(new Level(dataDefinition.getPolyParams().get(0))),
        new LevelMax(new Level(dataDefinition.getPolyParams().get(1))).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      dataDefinition.getSorts());
  }
}
