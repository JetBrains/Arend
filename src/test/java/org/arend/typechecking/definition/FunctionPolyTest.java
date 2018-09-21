package org.arend.typechecking.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FunctionPolyTest extends TypeCheckingTestCase {
  @Test
  public void funWithoutTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (n : Nat) => Nat");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (n : Nat) : \\Set1 => Nat");
    assertEquals(Sort.SetOfLevel(1), funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithoutTypeOmegaExplicitError() {
    typeCheckDef("\\func f (n : Nat) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funWithTypeOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) => n = n");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR, -1)), funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmega2() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\Prop => n = n");
    assertEquals(Sort.PROP, funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\Set0 => n = n");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitRecursive() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\Prop \\elim n | zero => 0 = 0 | suc n => f A n");
    assertEquals(Sort.PROP, funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmegaResultRecursive() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef(
      "\\func f (A : \\Type) (n : Nat) : \\oo-Type (\\max \\lp 1) \\elim n | zero => \\Set0 | suc n => A");
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 0, 1), Level.INFINITY), funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\Set0 => \\Sigma (n = n) Nat");
    assertEquals(Sort.SET0, funDefinition.getResultType().toSort());
  }

  @Test
  public void funWithTypeOmegaExplicitError() {
    typeCheckDef("\\func f (A : \\Type) : \\Set1 => \\Set0", 1);
  }

  @Test
  public void funOmega() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) => A");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)), funDefinition.getResultType().toSort());
  }

  @Test
  public void funOmegaExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) : \\Type => A");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)), funDefinition.getResultType().toSort());
  }

  @Test
  public void funOmegaProp() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) => \\Sigma A (n = n)");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)), funDefinition.getResultType().toSort());
  }

  @Test
  public void funOmegaPropExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\Type => \\Sigma (n = n) A");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), new Level(LevelVariable.HVAR)), funDefinition.getResultType().toSort());
  }

  @Test
  public void funOmegaSet() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) => \\Sigma (n = n) A Nat");
    assertEquals(Sort.STD, funDefinition.getResultType().toSort());
  }

  @Test
  public void funOmegaSetExplicit() {
    FunctionDefinition funDefinition = (FunctionDefinition) typeCheckDef("\\func f (A : \\Type) (n : Nat) : \\oo-Type => \\Sigma (n = n) A Nat");
    assertEquals(new Sort(new Level(LevelVariable.PVAR), Level.INFINITY), funDefinition.getResultType().toSort());
  }
}
