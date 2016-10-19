package com.jetbrains.jetpad.vclang.typechecking.constructions;


import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeOmega extends TypeCheckingTestCase {
  @Test
  public void dataExpansion() {
    typeCheckClass(
      "\\data D (A : \\Type) (a : A) | d (B : \\Type)\n" +
      "\\function f : (\\Prop -> D \\Set0 Nat) => d");
  }
}
