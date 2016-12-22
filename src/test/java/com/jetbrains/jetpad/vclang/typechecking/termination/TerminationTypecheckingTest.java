package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TerminationTypecheckingTest extends TypeCheckingTestCase {
  @Test
  public void selfCallInType() {
    typeCheckClass(
        "\\data D Nat | con\n" +
        "\\function f (x : Nat) (y : D (f x con)) : Nat => x", 1);
  }
}
