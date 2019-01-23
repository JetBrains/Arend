package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ThisTest extends TypeCheckingTestCase {
  @Test
  public void thisRecursive() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record S \\extends R | t : X | g : R.f 0 t = t");
  }

  @Test
  public void thisRecursiveData() {
    typeCheckModule(
      "\\record R (X : \\Type) {\n" +
      "  \\data D\n" +
      "    | con1 D\n" +
      "    | con2\n" +
      "}\n" +
      "\\record S \\extends R | g : R.D");
  }

  @Test
  public void constructorsWithPatterns() {
    typeCheckModule(
      "\\record R (X : \\Type) {\n" +
      "  \\data D (n : Nat) \\with\n" +
      "    | zero => con1\n" +
      "    | suc n => con2 (D n)\n" +
      "}\n" +
      "\\record S \\extends R | g : R.D 0");
  }

  @Test
  public void thisArgument() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (r : R) => r.x\n" +
      "\\record S \\extends R | t : X | g : f \\this t = t");
  }

  @Test
  public void thisRecursiveArgument() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (n : Nat) (r : R) : r.X -> r.X \\elim n\n" +
      "  | 0 => r.x\n" +
      "  | suc n => f n r\n" +
      "\\record S \\extends R | t : X | g : f 0 \\this t = t");
  }

  @Test
  public void thisBadRecursiveArgument() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (n : Nat) (r : R) : r.X -> r.X \\elim n\n" +
      "  | 0 => r.x\n" +
      "  | suc n => f n (\\let r' => r \\in r')\n" +
      "\\record S \\extends R | t : X | g : f 0 \\this t = t", 1);
    assertThat(getDefinition("f").getGoodThisParameters(), is(empty()));
  }

  @Test
  public void thisRecursiveDataArgument() {
    typeCheckModule(
      "\\record R (X : \\Type)\n" +
      "\\data D (r : R)\n" +
      "  | con1 (D r)\n" +
      "  | con2\n" +
      "\\record S \\extends R | g : D \\this");
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
      "\\record S \\extends R | t : X | g : f (\\let y => \\this \\in y) t = t", 1);
  }

  @Test
  public void thisErrorInferred() {
    typeCheckModule(
      "\\record R (X : \\Type) (f : Nat -> Nat)\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\record S \\extends R | g : (idp : \\this = {R} \\this) = idp", 1);
  }

  @Test
  public void thisBadArgument() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\func f (r : R) => R.x {\\let r' => r \\in r'}\n" +
      "\\record S \\extends R | t : X | g : f \\this t = t", 1);
  }

  @Test
  public void thisBadField() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\record F (r : R) | f (t : r.X) : R.x {\\let r' => r \\in r'} t = t\n" +
      "\\record S \\extends R | t : X | g : F \\this", 1);
  }

  @Test
  public void thisBadFieldSubclass() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\record F (r : R)\n" +
      "\\record G \\extends F | f (t : r.X) : R.x {\\let r' => r \\in r'} t = t\n" +
      "\\record S \\extends R | t : X | g : G \\this", 1);
  }

  @Test
  public void thisBadFieldSuperclass() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X)\n" +
      "\\record F (r : R) | f (t : r.X) : R.x {\\let r' => r \\in r'} t = t\n" +
      "\\record G \\extends F\n" +
      "\\record S \\extends R | t : X | g : G \\this", 1);
  }
}
