package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class NewInstanceExtensionTest extends TypeCheckingTestCase {
  @Test
  public void extTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : \\new r { | y => 2 } = \\new R r.x 2 => path (\\lam _ => \\new R r.x 2)");
  }

  @Test
  public void withoutNew() {
    // This test produces an error on resolving since the resolver preprocess bodies and types of functions and R is not resolved at this stage.
    resolveNamesModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => r { | x => 2 }", 1);
  }

  @Test
  public void withoutNew2() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => (1, r { | x => 2 })", 1);
  }

  @Test
  public void extDepTest() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R) (q : r.x = r.x) : \\new r { | p => q } = \\new R r.x q => path (\\lam _ => \\new R r.x q)");
  }

  @Test
  public void extDepTest2() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R) => \\new r { | x => 2 }", 1);
  }

  @Test
  public void extDepTest3() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\func f (r : R 2) => \\new r { | x => 2 }", 1);
  }

  @Test
  public void extDepTest4() {
    typeCheckModule(
      "\\record R (x : Nat) (p : x = x)\n" +
      "\\record S \\extends R | x => 2\n" +
      "\\func f (s : S) => \\new s");
  }

  @Test
  public void newInstTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) => \\let t : R r.x r.y => \\new r \\in t");
  }
}
