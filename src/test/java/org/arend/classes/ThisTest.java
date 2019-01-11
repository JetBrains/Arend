package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class ThisTest extends TypeCheckingTestCase {
  @Test
  public void thisArgument() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (r : R) => r.x\n" +
      "\\record S \\extends R | t : X | g : f \\this t = t");
  }

  @Test
  public void thisClassExt() {
    typeCheckModule(
      "\\record R (X : \\Type)\n" +
      "\\record D (r : R)\n" +
      "\\record S \\extends R | g : D \\this");
  }

  @Test
  public void thisEquality() {
    typeCheckModule(
      "\\record R\n" +
      "\\record S \\extends R | g : \\this = {R} \\this");
  }

  @Test
  public void thisError() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (r : R) => r.x\n" +
      "\\record S \\extends R | t : X | g : f ((\\lam (x : R) => x) \\this) t = t", 1);
  }

  @Test
  public void thisErrorInferred() {
    typeCheckModule(
      "\\record R (X : \\Type) (f : Nat -> Nat)\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\record S \\extends R | g : (idp : \\this = {R} \\this) = idp", 1);
  }
}
