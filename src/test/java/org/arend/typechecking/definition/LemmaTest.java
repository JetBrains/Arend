package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.LevelMismatchError;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;
import static org.arend.Matchers.typecheckingError;

public class LemmaTest extends TypeCheckingTestCase {
  @Test
  public void lemmaTyped() {
    typeCheckModule("\\lemma f : 0 = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void lemmaUntyped() {
    typeCheckModule("\\lemma f => path (\\lam _ => 0)");
  }

  @Test
  public void lemmaNotPropTyped() {
    typeCheckModule("\\lemma f : Nat => 0", 1);
    assertThatErrorsAre(typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void lemmaNotPropUntyped() {
    typeCheckModule("\\lemma f => 0", 1);
    assertThatErrorsAre(typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void lemmaRecursive() {
    typeCheckModule("""
      \\lemma f (n : Nat) : 0 Nat.+ n = n
        | 0 => idp
        | suc n => path (\\lam i => suc (f n @ i))
      """);
  }

  @Test
  public void lemmaNotPropRecursive() {
    typeCheckModule("""
      \\lemma f (n : Nat) : Nat
        | 0 => 0
        | suc n => n
      """, 1);
    assertThatErrorsAre(typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void lemmaCowith() {
    typeCheckModule("""
      \\class C (n m : Nat)
      \\lemma f : C \\cowith
        | n => 0
        | m => 1
      """, 1);
    assertThatErrorsAre(typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void lemmaNew() {
    typeCheckModule("""
      \\class C (n m : Nat)
      \\lemma f => \\new C {
        | n => 0
        | m => 1
      }
      """);
  }

  @Test
  public void lemmaCowithFieldProp() {
    typeCheckModule("""
      \\class C (n : Nat) { \\field x : 0 = 0 }
      \\lemma f : C 0 \\cowith
        | x => idp
      \\func g : f.x = idp => idp
      """, 1);
    assertThatErrorsAre(typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void lemmaLevel() {
    typeCheckModule("""
      \\data Empty
      \\data Bool | true | false
      \\func E (b : Bool) : \\Set0 | true => Empty | false => Empty
      \\func E-isProp (b : Bool) (x y : E b) : x = y \\elim b, x | true, () | false, ()
      \\lemma f (b : Bool) (x : E b) : \\level (E b) (E-isProp b) => x
      """);
  }

  @Test
  public void lemmaLevelError() {
    typeCheckModule("\\lemma f {X : \\Type} (Xs : \\Pi (x x' : X) (p q : x = x') -> p = q) (x : X) : \\level X Xs => x", 2);
    assertThatErrorsAre(typeMismatchError(), typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void canBeLemmaTest() {
    typeCheckModule("""
      \\record R (x y : Nat) (p : x = x)
      \\func test : R { | x => 0 } \\cowith
        | y => 1
        | p => idp
      """);
  }
}
