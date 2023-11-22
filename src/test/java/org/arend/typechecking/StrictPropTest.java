package org.arend.typechecking;

import org.arend.Matchers;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

public class StrictPropTest extends TypeCheckingTestCase {
  @Test
  public void parametersError() {
    typeCheckDef("\\func f {A : \\Prop} (x y : A) : x = y => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void parametersTest() {
    typeCheckDef("\\func f {A : \\Prop} (x y : A) : x = y => Path.inProp _ _");
  }

  @Test
  public void setError() {
    typeCheckDef("\\func f {A : \\Set0} (x y : A) : x = y => idp", 1);
  }

  @Test
  public void setPathError() {
    typeCheckDef("\\func f {A : \\Set} (x y : A) (p q : x = y) : p = q => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void setPathTest() {
    typeCheckDef("\\func f {A : \\Set} (x y : A) (p q : x = y) : p = q => Path.inProp _ _");
  }

  @Test
  public void setPiError() {
    typeCheckDef("\\func f {A : \\Set} (x y : A) : \\Pi (p q : = \\levels \\lp 0 x y) -> p = q => \\lam p q => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void setPiTest() {
    typeCheckDef("\\lemma f {A : \\Set} (x y : A) : \\Pi (p q : x = y) -> p = q => Path.inProp");
  }

  @Test
  public void classTest() {
    typeCheckModule(
      "\\record B\n" +
      "\\func f (b b' : B) : b = b' => idp");
  }

  @Test
  public void classUseLevelError() {
    typeCheckModule(
      "\\record B (X : \\Type) (p : \\Pi (x x' : X) -> x = x') (x0 : X)\n" +
      " \\where \\use \\level levelProp {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = b' => path (\\lam i => \\new B X p (p b.x0 b'.x0 @ i))\n" +
      "\\func f {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = {B X p} b' => idp", 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void classUseLevelTest() {
    typeCheckModule(
      "\\record B (X : \\Type) (p : \\Pi (x x' : X) -> x = x') (x0 : X)\n" +
      " \\where \\use \\level levelProp {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = b' => path (\\lam i => \\new B X p (p b.x0 b'.x0 @ i))\n" +
      "\\func f {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = {B X p} b' => Path.inProp b b'");
  }

  @Test
  public void lemmaTest() {
    typeCheckModule(
      "\\lemma f (x : Nat) : 0 = 0 => idp\n" +
      "\\func test : f 0 = f 1 => idp");
  }

  @Test
  public void emptyDataTest() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\func test (x y : Empty) : x = y => idp");
  }

  @Test
  public void comparisonTest() {
    typeCheckModule("""
      \\func unique {A : \\Set} {a0 a a' : A} (p : a0 = a) (q : a0 = a') : (a,p) = {\\Sigma (a1 : A) (a0 = a1)} (a',q)
        | idp, idp => idp
      \\lemma lem {A : \\Set} (a0 : A) : \\Sigma (a : A) (a0 = a) \\level \\lam s t => unique s.2 t.2
        => (a0,idp)
      \\func test {A : \\Set} (a1 a2 : A) : (lem a1).1 = (lem a2).1
        => idp
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void comparisonTest2() {
    typeCheckModule("""
      \\func unique {A : \\Set} {a0 a a' : A} (p : a0 = a) (q : a0 = a') : (a,p) = {\\Sigma (a1 : A) (a0 = a1)} (a',q)
        | idp, idp => idp
      \\lemma lem {A : \\Set} (a0 x : A) : \\Sigma (a : A) (a0 = a) \\level \\lam s t => unique s.2 t.2
        => (a0,idp)
      \\func test {A : \\Set} (a a1 a2 : A) : (lem a a1).1 = (lem a a2).1
        => idp
      """);
  }
}
