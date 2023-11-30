package org.arend.typechecking.constructions;

import org.arend.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class BoxTest extends TypeCheckingTestCase {
  @Test
  public void compareTest() {
    typeCheckDef("\\func test {A : \\Prop} (a a' : A) : (\\box a) = \\box a' => idp");
  }

  @Test
  public void notPropTest() {
    typeCheckDef("\\func test {A : \\Set} (a : A) => \\box a", 1);
  }

  @Test
  public void functionTest() {
    typeCheckModule(
      "\\sfunc foo {A : \\Prop} (\\property a : A) => 0\n" +
      "\\func test {A : \\Prop} (a a' : A) : foo a = foo a' => idp");
  }

  @Test
  public void dataTest() {
    typeCheckModule(
      "\\data D {A : \\Prop} (\\property a : A) | con\n" +
      "\\func test {A : \\Prop} (a a' : A) : D a = D a' => idp");
  }

  @Test
  public void conTest() {
    typeCheckModule(
      "\\data D {A : \\Prop} | con (\\property a : A)\n" +
      "\\func test {A : \\Prop} (a a' : A) : con a = con a' => idp");
  }

  @Test
  public void patternTest() {
    typeCheckModule("""
      \\data D (A : \\Prop) | con (\\property a : A)
      \\func test {A : \\Prop} (a : A) (d : D A) : d = con a \\elim d
        | con a' => idp
      """);
  }

  @Test
  public void notPropPropertyTest() {
    typeCheckDef("\\func foo {A : \\Set} (\\property a : A) => 0", 1);
  }

  @Test
  public void classUseLevelTest() {
    typeCheckModule("""
      \\record B (X : \\Type) (p : \\Pi (x x' : X) -> x = x') (x0 : X)
        \\where \\use \\level levelProp {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = b' => path (\\lam i => \\new B X p (p b.x0 b'.x0 @ i))
      \\func test {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : B.x0 {\\box b} = B.x0 {\\box b'} => idp
      """);
  }

  @Test
  public void classUseLevelError() {
    typeCheckModule("""
      \\record B (X : \\Type) (p : \\Pi (x x' : X) -> x = x') (x0 : X)
        \\where \\use \\level levelProp {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = b' => path (\\lam i => \\new B X p (p b.x0 b'.x0 @ i))
      \\func f {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : B.x0 {\\box b} = B.x0 {\\box b'} => idp
      \\func test {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (x1 x2 : X) : x1 = x2 => f (\\new B X p x1) (\\new B X p x2)
      """, 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }
}
