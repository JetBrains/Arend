package org.arend.typechecking.implicitargs;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

public class DeferredTest extends TypeCheckingTestCase {
  @Ignore
  @Test
  public void sigmaTest() {
    typeCheckModule(
      "\\func pmap {A B : \\Type} (f : A -> B) {a a' : A} (p : a = a') => path (\\lam i => f (p @ i))\n" +
      "\\func test (x : \\Sigma Nat Nat) (s : x = (0,0)) => pmap (\\lam t => t.1) s");
  }

  @Ignore
  @Test
  public void piTest() {
    typeCheckModule(
      "\\func pmap {A B : \\Type} (f : A -> B) {a a' : A} (p : a = a') => path (\\lam i => f (p @ i))\n" +
      "\\func test (x y : Nat -> Nat) (s : x = y) => pmap (\\lam t => t 0) s");
  }
}
