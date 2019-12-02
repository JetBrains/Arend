package org.arend.classes;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.cycle;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LocalThisTest extends TypeCheckingTestCase {
  @Test
  public void mutualRecursionError() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func test => \\new R { | x => R.y {\\this} | y => R.x {\\this} }", 1);
    assertThatErrorsAre(cycle());
  }

  @Test
  public void mutualRecursionError2() {
    typeCheckModule(
      "\\record S (x y : Nat)\n" +
      "\\record R \\extends S\n" +
      "  | x => y\n" +
      "\\func test => \\new R { | y => S.x {\\this} }", 1);
    assertThatErrorsAre(cycle());
  }

  @Test
  public void thisRecursive() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func test => \\new (R { | X => Nat | y n => S.f {\\this} n n }) { | x => suc }");
  }

  @Test
  public void thisRecursive2() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\record T \\extends R { | X => Nat | y n => S.f n n }\n" +
      "\\func test => \\new T { | x => suc }");
  }

  @Test
  public void thisRecursiveAlt() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func ext => R { | y t => S.f 0 t }");
  }

  @Test
  public void thisRecursiveError() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\func ext => R { | X => Nat | x n => R.f {\\this} n n }", 1);
    assertThatErrorsAre(cycle(get("R.x")));
  }

  @Test
  public void thisRecursiveErrorAlt() {
    typeCheckModule(
      "\\record R (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\func ext => R { | x t => R.f {\\this} 0 t }", 1);
    assertThatErrorsAre(cycle(get("R.x")));
  }

  @Test
  public void thisRecursiveExt() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\record T \\extends R { | X => Nat | y => S.x }\n" +
      "\\func test => \\new T { | x => suc }");
  }

  @Test
  public void thisRecursiveExt2() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func test => \\new (R { | X => Nat | y => S.x {\\this} }) { | x => suc }");
  }

  @Test
  public void thisRecursiveExtError() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\record T \\extends R { | X => Nat | y n => S.f n n }\n" +
      "\\func test => \\new T { | x => R.y {\\this} }", 1);
    assertThatErrorsAre(cycle());
  }

  @Test
  public void thisRecursiveExtError2() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X) {\n" +
      "  \\func f (n : Nat) : X -> X \\elim n\n" +
      "    | 0 => x\n" +
      "    | suc n => f n\n" +
      "}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func test => \\new (R { | X => Nat | y n => S.f {\\this} n n }) { | x => R.y {\\this} }", 1);
    assertThatErrorsAre(cycle());
  }

  @Test
  public void thisRecursiveData() {
    typeCheckModule(
      "\\record S {\n" +
      "  \\data D\n" +
      "    | con1 D\n" +
      "    | con2\n" +
      "}\n" +
      "\\record R (X : \\Type) \\extends S\n" +
      "\\func test => R { | X => S.D {\\this} }");
  }

  @Test
  public void thisRecursiveDataError() {
    typeCheckModule(
      "\\record S (X : \\Type) {\n" +
      "  \\data D\n" +
      "    | con1 D\n" +
      "    | con2\n" +
      "}\n" +
      "\\record R \\extends S\n" +
      "\\func test => R { | X => S.D {\\this} }", 1);
    assertThatErrorsAre(cycle(get("S.X")));
  }

  @Test
  public void explicitThisRecursive2() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\func f (s : S) (n : Nat) : s.X -> s.X \\elim n\n" +
      "  | 0 => s.x\n" +
      "  | suc n => f s n\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\record T \\extends R { | X => Nat | y n => f \\this n n }\n" +
      "\\func test => \\new T { | x => suc }");
  }

  @Test
  public void explicitThisRecursiveAlt() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\func f (s : S) (n : Nat) : s.X -> s.X \\elim n\n" +
      "  | 0 => s.x\n" +
      "  | suc n => f s n\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func ext => R { | y t => f \\this 0 t }");
  }

  @Test
  public void explicitThisRecursiveExtError() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\func f (s : S) (n : Nat) : s.X -> s.X \\elim n\n" +
      "  | 0 => s.x\n" +
      "  | suc n => f s n\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\record T \\extends R { | X => Nat | y n => f \\this n n }\n" +
      "\\func test => \\new T { | x => R.y {\\this} }", 1);
    assertThatErrorsAre(cycle());
  }

  @Test
  public void thisBadRecursiveArgument() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\func f (s : S) (n : Nat) : s.X -> s.X \\elim n\n" +
      "  | 0 => s.x\n" +
      "  | suc n => f (\\let s' => s \\in s') n\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func test => R { | y t => f \\this 0 t }", 1);
    assertThat(getDefinition("f").getGoodThisParameters(), is(empty()));
  }

  @Test
  public void thisClassExt() {
    typeCheckModule(
      "\\record S (X : \\Type)\n" +
      "\\record R (Y : \\Type) \\extends S\n" +
      "\\record D (s : S)\n" +
      "\\func test => R { | Y => D (\\this : R) }");
  }

  @Test
  public void thisClassExtError() {
    typeCheckModule(
      "\\record S (X : \\Type)\n" +
      "\\record R (Y : \\Type) \\extends S\n" +
      "\\record D (s : S)\n" +
      "\\func test => R { | X => D (\\this : R) }", 1);
    assertThatErrorsAre(cycle(get("S.X")));
  }

  @Test
  public void thisTest() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func f (s : S) => s.x\n" +
      "\\func test => R { | y t => f \\this t }");
  }

  @Test
  public void thisError() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func f (s : S) => s.x\n" +
      "\\func test => R { | y t => f (\\let y => \\this \\in y) t }", 1);
  }

  @Test
  public void thisBadArgument() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\func f (s : S) => S.x {\\let s' => s \\in s'}\n" +
      "\\record R (y : X -> X) \\extends S\n" +
      "\\func test => R { | y t => f \\this t }", 1);
  }

  @Test
  public void thisBadField() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record F (s : S) | f (t : s.X) : S.x {\\let s' => s \\in s'} t = t\n" +
      "\\record R (Y : \\Type) \\extends S\n" +
      "\\func test => R { | Y => F \\this }", 1);
  }

  @Test
  public void thisBadFieldSubclass() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record F (s : S)\n" +
      "\\record G \\extends F | f (t : s.X) : S.x {\\let s' => s \\in s'} t = t\n" +
      "\\record R (Y : \\Type) \\extends S\n" +
      "\\func test => R { | Y => G \\this }", 1);
  }

  @Test
  public void thisBadFieldSuperclass() {
    typeCheckModule(
      "\\record S (X : \\Type) (x : X -> X)\n" +
      "\\record F (s : S) | f (t : s.X) : S.x {\\let s' => s \\in s'} t = t\n" +
      "\\record G \\extends F\n" +
      "\\record R (Y : \\Type) \\extends S\n" +
      "\\func test => R { | Y => G \\this }", 1);
  }

  @Test
  public void typedThisInDynamicDefinition() {
    typeCheckModule(
      "\\record S (X : \\Type)\n" +
      "\\record T (s : S)\n" +
      "\\record R \\extends S {\n" +
      "  \\func f => T (\\this : R)\n" +
      "}");
  }
}
