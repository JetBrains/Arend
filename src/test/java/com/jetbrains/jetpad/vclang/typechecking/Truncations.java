package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;

public class Truncations {
  @Test
  public void elimInProp() {
    typeCheckDef("\\function inP-inv (P : \\Prop) (p : TrP P) : P <= \\elim p | inP p => p");
  }
}
