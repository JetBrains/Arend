package com.jetbrains.jetpad.vclang.typechecking.definition;

import com.jetbrains.jetpad.vclang.core.context.binding.LevelVariable;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.sort.LevelMax;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.sort.SortMax;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FunctionPolyTest extends TypeCheckingTestCase {
  @Test
  public void funWithoutTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (n : Nat) => Nat");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (n : Nat) : \\Set1 => Nat");
    assertEquals(Sort.SetOfLevel(1), funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicitError() {
    typeCheckDef("\\function f (n : Nat) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funWithTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => n = n");
    assertEquals(Sort.PROP, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Set0 => n = n");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitRecursive() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Prop <= \\elim n | zero => 0 = 0 | suc n => f A n");
    assertEquals(Sort.PROP, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaResultRecursive() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef(
      "\\function f (A : \\Type) (n : Nat) : \\oo-Type (\\max \\lp 1) <= \\elim n | zero => \\Set0 | suc n => A");
    assertEquals(new SortMax(
        new LevelMax(new Level(LevelVariable.PVAR)).max(new LevelMax(new Level(1))),
        new LevelMax(new Level(LevelVariable.HVAR)).max(new LevelMax(new Level(2)))), /* \Type (max \lp 1, max \lh 1) */
      funDefinition.getResultType().toSorts());
  }

  @Test
  public void funWithTypeOmegaExplicitSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Set0 => \\Sigma (n = n) Nat");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSorts().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitError() {
    typeCheckDef("\\function f (A : \\Type) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) => A");
    assertEquals(new SortMax(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) : \\Type => A");
    assertEquals(new SortMax(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaProp() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => \\Sigma A (n = n)");
    assertEquals(new SortMax(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaPropExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\Type => \\Sigma (n = n) A");
    assertEquals(new SortMax(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR))), funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) => \\Sigma (n = n) A Nat");
    assertEquals(new SortMax(
        new LevelMax(new Level(LevelVariable.PVAR)),
        new LevelMax(new Level(LevelVariable.HVAR)).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      funDefinition.getResultType().toSorts());
  }

  @Test
  public void funOmegaSetExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\function f (A : \\Type) (n : Nat) : \\oo-Type => \\Sigma (n = n) A Nat");
    assertEquals(new SortMax(
        new LevelMax(new Level(LevelVariable.PVAR)),
        new LevelMax(new Level(LevelVariable.HVAR)).max(new Level(1))), /* \Type (\lp, max \lh 0) */
      funDefinition.getResultType().toSorts());
  }
}
