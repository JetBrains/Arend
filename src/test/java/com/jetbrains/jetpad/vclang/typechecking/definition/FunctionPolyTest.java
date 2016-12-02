package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;
import com.jetbrains.jetpad.vclang.term.expr.sort.LevelMax;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FunctionPolyTest extends TypeCheckingTestCase {
  @Test
  public void funWithoutTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (n : Nat) => Nat");
    assertThat(funDefinition.getPolyParams(), is(empty()));
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (n : Nat) : \\Set1 => Nat");
    assertThat(funDefinition.getPolyParams(), is(empty()));
    assertEquals(Sort.SetOfLevel(1), funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicitError() {
    typeCheckDef("\\function f (n : Nat) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funWithTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => n = n");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.PROP, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Set0 => n = n");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitRecursive() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Prop <= \\elim n | zero => 0 = 0 | suc n => f A n");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.PROP, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Set0 => \\Sigma (n = n) Nat");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitError() {
    typeCheckDef("\\function f (A : \\Type) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) => A");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(funDefinition.getPolyParams().get(0)), new Level(funDefinition.getPolyParams().get(1)))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) : \\Type => A");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(funDefinition.getPolyParams().get(0)), new Level(funDefinition.getPolyParams().get(1)))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaProp() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => \\Sigma A (n = n)");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(funDefinition.getPolyParams().get(0)), new Level(funDefinition.getPolyParams().get(1)))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaPropExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Type => \\Sigma (n = n) A");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new Sort(new Level(funDefinition.getPolyParams().get(0)), new Level(funDefinition.getPolyParams().get(1)))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => \\Sigma (n = n) A Nat");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new LevelMax(new Level(funDefinition.getPolyParams().get(0))), new LevelMax(new Level(funDefinition.getPolyParams().get(1))).max(new Level(1))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaSetExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Type => \\Sigma (n = n) A Nat");
    assertThat(funDefinition.getPolyParams(), hasSize(2));
    assertEquals(new SortMax(new LevelMax(new Level(funDefinition.getPolyParams().get(0))), new LevelMax(new Level(funDefinition.getPolyParams().get(1))).max(new Level(1))), funDefinition.getResultType().toSorts());
  }
}
