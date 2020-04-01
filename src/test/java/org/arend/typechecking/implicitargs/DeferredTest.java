package org.arend.typechecking.implicitargs;

import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class DeferredTest extends TypeCheckingTestCase {
  @Test
  public void preludeTest() {
    assertThat(Prelude.AT.getParametersTypecheckingOrder(), is(nullValue()));
    assertThat(Prelude.ISO.getParametersTypecheckingOrder(), is(nullValue()));
  }

  @Test
  public void sigmaTest() {
    typeCheckModule(
      "\\func pmap {A B : \\Type} (f : A -> B) {a a' : A} (p : a = a') => path (\\lam i => f (p @ i))\n" +
      "\\func test (x : \\Sigma Nat Nat) (s : x = (0,0)) => pmap (\\lam t => t.1) s");
  }

  @Test
  public void piTest() {
    typeCheckModule(
      "\\func pmap {A B : \\Type} (f : A -> B) {a a' : A} (p : a = a') => path (\\lam i => f (p @ i))\n" +
      "\\func test (x y : Nat -> Nat) (s : x = y) => pmap (\\lam t => t 0) s");
  }

  @Test
  public void freeVarsTest() {
    typeCheckModule(
      "\\data D (n m : Nat)\n" +
      "\\func f {n : Nat} (C : \\Type) (p : \\Sigma Nat C) (d : D n (p.1)) => 0");
    assertNull(getDefinition("f").getParametersTypecheckingOrder());
  }
}
