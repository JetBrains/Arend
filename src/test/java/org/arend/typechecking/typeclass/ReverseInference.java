package org.arend.typechecking.typeclass;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;

@Ignore("Not implemented yet")
public class ReverseInference extends TypeCheckingTestCase {
  @Test
  public void reverseInference() {
    typeCheckModule(
      "\\class Pointed (X : \\Type) | base : X\n" +
      "\\instance NatPointed : Pointed Nat | base => 2\n" +
      "\\func f (P : Pointed) => P.base\n" +
      "\\func g : f Nat = 2 => idp");
  }

  @Test
  public void reverseInference2() {
    typeCheckModule(
      "\\class C (x : Nat) | p : x = x\n" +
      "\\instance inst : C 2 | p => idp\n" +
      "\\func f (c : C) => c.p\n" +
      "\\func g : f 2 = idp => idp");
  }

  @Test
  public void reverseInferenceError() {
    typeCheckModule(
      "\\class C (x : Nat) | p : x = x\n" +
      "\\instance inst : C 2 | p => idp\n" +
      "\\func f (c : C) => c.p\n" +
      "\\func g (n : Nat) => f (suc n)", 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
