package org.arend.typechecking.definition;

import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.arend.Matchers.*;

public class CoClauseFunctionTest extends TypeCheckingTestCase {
  @Test
  public void functionTest() {
    typeCheckModule(
      "\\record C (a b : Nat) (f g : \\Pi (x : Nat) -> x = a -> x = b)\n" +
      "\\func test : C 0 \\cowith\n" +
      "  | b => 0\n" +
      "  | f x (p : x = 0) : x = 0 \\with {\n" +
      "    | 0, _ => idp\n" +
      "    | suc n, p => p\n" +
      "  }\n" +
      "  | g (x : Nat) (p : x = 0) : x = 0 \\elim x {\n" +
      "    | 0 => idp\n" +
      "    | suc n => p\n" +
      "  }");
  }

  @Test
  public void parameterDependencyError() {
    typeCheckModule(
      "\\record C (a : Nat) (f : \\Pi (x : Nat) -> x = a -> x = a)\n" +
      "\\func test : C 0 \\cowith\n" +
      "  | f x p : x = 0 \\elim x {\n" +
      "    | 0 => p\n" +
      "    | suc n => p\n" +
      "  }", 1);
  }

  @Test
  public void resultTypeDependencyError() {
    typeCheckModule(
      "\\record C (a : Nat) (f : \\Pi (x : Nat) -> x = a -> x = a)\n" +
      "\\func test : C 0 \\cowith\n" +
      "  | f x (p : x = 0) \\elim x {\n" +
      "    | 0 => p\n" +
      "    | suc n => p\n" +
      "  }", 1);
  }

  @Test
  public void functionTest2() {
    typeCheckModule(
      "\\record C (f g : Nat -> Nat)\n" +
      "\\func test : C \\cowith\n" +
      "  | f (x : Nat) \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc n => suc (g n Nat.+ test.g n)\n" +
      "  }\n" +
      "  | g x \\elim x {\n" +
      "    | 0 => 0\n" +
      "    | suc n => suc (f n Nat.+ test.f n)\n" +
      "  }");
  }

  @Test
  public void longName() {
    typeCheckModule(
      "\\record C (f : Nat -> Nat)\n" +
      "\\record D \\extends C\n" +
      "\\func test : D \\cowith\n" +
      "  | C.f (x : Nat) \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc n => suc (f n)\n" +
      "  }");
  }

  @Test
  public void fieldResolveError() {
    resolveNamesModule(
      "\\record C (a : Nat) (f : Nat -> Nat)\n" +
      "\\func test : C \\cowith\n" +
      "  | a => 0" +
      "  | f x \\with {\n" +
      "    | 0 => a\n" +
      "    | suc n => f n\n" +
      "  }", 1);
    assertThatErrorsAre(notInScope("a"));
  }

  @Test
  public void fieldTypecheckingError() {
    typeCheckModule(
      "\\record C (a : Nat) (f : Nat -> Nat)\n" +
      "\\func test : C \\cowith\n" +
      "  | a => 0" +
      "  | f x \\with {\n" +
      "    | 0 => C.a\n" +
      "    | suc n => f n\n" +
      "  }", 1);
  }

  @Test
  public void parameterSubtypeError() {
    typeCheckModule(
      "\\record C (f : \\Pi (A : \\Prop) (x : Nat) -> A -> A)\n" +
      "\\lemma test : C \\cowith\n" +
      "  | f (A : \\Set0) x a \\elim x {\n" +
      "    | 0 => a\n" +
      "    | suc n => a\n" +
      "  }", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void parameterSubtypeError2() {
    typeCheckModule(
      "\\record C (f : \\Pi (A : \\Set0) (x : Nat) -> A -> A)\n" +
      "\\func test : C \\cowith\n" +
      "  | f (A : \\Prop) x a \\elim x {\n" +
      "    | 0 => a\n" +
      "    | suc n => a\n" +
      "  }", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void resultSubtypeTest() {
    typeCheckModule(
      "\\record C (f : \\Prop -> Nat -> \\Set0)\n" +
      "\\func test : C \\cowith\n" +
      "  | f A x : \\Prop \\elim x {\n" +
      "    | 0 => A\n" +
      "    | suc n => A\n" +
      "  }");
  }

  @Test
  public void resultSubtypeError() {
    typeCheckModule(
      "\\record C (f : \\Prop -> Nat -> \\Prop)\n" +
      "\\func test : C \\cowith\n" +
      "  | f A x : \\Set0 \\elim x {\n" +
      "    | 0 => A\n" +
      "    | suc n => A\n" +
      "  }", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Ignore
  @Test
  public void recordTest() {
    typeCheckModule(
      "\\record R (f g : Nat -> Nat)\n" +
      "\\record S \\extends R\n" +
      "  | f x \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc n => f n\n" +
      "  }");
  }

  @Ignore
  @Test
  public void recordError() {
    typeCheckModule(
      "\\record R (f g : Nat -> Nat)\n" +
      "\\record S \\extends R\n" +
      "  | f x \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc n => g n\n" +
      "  }", 1);
  }

  @Ignore
  @Test
  public void recordTest2() {
    typeCheckModule(
      "\\record R (a : Nat) (f : \\Pi (x : Nat) -> x = a -> Nat)\n" +
      "\\record S \\extends R\n" +
      "  | a => 0\n" +
      "  | f x _ \\elim x {\n" +
      "    | 0 => 0\n" +
      "    | suc n => n\n" +
      "  }");
  }

  @Ignore
  @Test
  public void recordError2() {
    typeCheckModule(
      "\\record R (a : Nat) (f : \\Pi (x : Nat) -> x = a -> Nat)\n" +
      "\\record S \\extends R\n" +
      "  | f x _ \\elim x {\n" +
      "    | 0 => 0\n" +
      "    | suc n => n\n" +
      "  }", 1);
  }

  @Test
  public void termTest() {
    typeCheckModule(
      "\\class D | \\infix 3 func (x y : Nat) : Nat\n" +
      "\\instance D-inst : D\n" +
      "  | func \\as \\infix 3 func (x y : Nat) : Nat => x");
  }

  @Test
  public void termTest2() {
    typeCheckModule(
      "\\class D | \\infix 3 func (x y : Nat) : Nat\n" +
      "\\instance D-inst : D\n" +
      "  | func \\as \\infix 3 func (x y : Nat) => x");
  }

  @Test
  public void levelTest() {
    typeCheckModule(
      "\\data Wrap (A : \\Type) | in A\n" +
      "\\record R | field {A : \\Type} (p : \\Pi (a a' : A) -> a = a') (t s : Wrap A) : A \\level p\n" +
      "\\func test : R \\cowith | field {A : \\Type} (p : \\Pi (a a' : A) -> a = a') (t s : Wrap A) : A \\elim t { | in a => a }");
  }

  @Test
  public void levelTest2() {
    typeCheckModule(
      "\\data Wrap (A : \\Type) | in A\n" +
      "\\record R | field {A : \\Type} (p : \\Pi (a a' : A) -> a = a') (t s : Wrap A) : A \\level p\n" +
      "\\func test : R \\cowith | field {A} p t s \\elim t { | in a => a }");
  }

  @Test
  public void implicitParameterError() {
    typeCheckModule(
      "\\record R | field {A : \\Type} : A -> A\n" +
      "\\func test : R \\cowith | field \\as \\fix 5 field t => {?}", 1);
    assertThatErrorsAre(typecheckingError(ArgumentExplicitnessError.class));
  }

  @Test
  public void infixTest() {
    typeCheckModule(
      "\\record R | \\infixl 5 % : Nat -> Nat -> Nat\n" +
      "\\func test : R \\cowith | % n m \\elim n {\n" +
      "  | 0 => m\n" +
      "  | suc n => suc (n % m)\n" +
      "}");
  }

  @Test
  public void parametersTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (f : Nat -> A -> A)\n" +
      "\\func g (B : \\Type) (b' : B) : C B \\cowith\n" +
      "  | f n (b : B) : B \\elim n {\n" +
      "    | 0 => b\n" +
      "    | suc n => b'\n" +
      "  }\n" +
      "\\func test (X : \\Type) (x : X) : C.f {g X x} = g.f {X} {x} => idp");
  }

  @Test
  public void explicitParametersTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (f : Nat -> A -> A)\n" +
      "\\func g (B : \\Type) : C B \\cowith\n" +
      "  | f n (b : B) : B \\elim n {\n" +
      "    | 0 => b\n" +
      "    | suc n => f {B} n b\n" +
      "  }");
  }

  @Test
  public void withParametersTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (f : Nat -> A -> A)\n" +
      "\\func g (B : \\Type) : C B \\cowith\n" +
      "  | f n (b : B) : B \\with {\n" +
      "    | 0, b => b\n" +
      "    | suc n, b => f {B} n b\n" +
      "  }");
  }

  @Test
  public void missingClausesTest() {
    typeCheckModule(
      "\\record C (f : Nat -> Nat)\n" +
      "\\func g (m : Nat) : C \\cowith\n" +
      "  | f n \\with", 1);
    assertThatErrorsAre(missingClauses(2));
  }

  @Test
  public void withImplicitParametersTest() {
    typeCheckModule(
      "\\record C (A : \\Type) (f : \\Pi {n : Nat} -> A -> A)\n" +
      "\\func g (B : \\Type) : C B \\cowith\n" +
      "  | f {n} (b : B) : B \\with {\n" +
      "    | {0}, b => b\n" +
      "    | {suc n}, b => f {B} {n} b\n" +
      "  }");
  }

  @Test
  public void withImplicitParametersTest2() {
    typeCheckModule(
      "\\record C (A : \\Type) (f : \\Pi {m : Nat} -> Nat -> A -> A)\n" +
      "\\func g (B : \\Type) : C B \\cowith\n" +
      "  | f {m} n (b : B) : B \\with {\n" +
      "    | 0, b => b\n" +
      "    | suc n, b => f {B} {0} n b\n" +
      "  }");
  }

  @Test
  public void dynamicTest() {
    typeCheckModule(
      "\\record R (x : Nat)\n" +
      "\\record S (r : R)\n" +
      "\\record T {\n" +
      "  \\func foo : S \\cowith\n" +
      "    | r : R \\cowith {\n" +
      "      | x => 0\n" +
      "    }\n" +
      "}");
  }

  @Test
  public void parametersSubstTest() {
    typeCheckModule(
      "\\record C (f : Nat -> Nat) (g : \\Pi (x : Nat) -> f x = 0)\n" +
      "\\func test (n : Nat) : C \\cowith\n" +
      "  | f (m : Nat) : Nat \\with {\n" +
      "    | 0 => 0\n" +
      "    | suc m => 0\n" +
      "  }\n" +
      "  | g (m : Nat) : f m = 0 \\with {\n" +
      "    | 0 => idp\n" +
      "    | suc m => idp\n" +
      "  }");
  }

  @Test
  public void addedLevels() {
    typeCheckModule(
      "\\record C \\plevels p1 <= p2\n" +
      "\\record D \\plevels p3 <= p4\n" +
      "\\record E (f : D -> Nat)\n" +
      "\\func test (c : C) : E \\cowith\n" +
      "  | f (d : D) : Nat => 0");
  }

  @Test
  public void addedLevels2() {
    typeCheckModule(
      "\\record C \\plevels p1 <= p2\n" +
      "\\record E (f : C -> Nat)\n" +
      "\\func test (c : C) : E \\cowith\n" +
      "  | f (c : C) : Nat => 0");
  }

  @Test
  public void addedLevels3() {
    typeCheckModule(
      "\\record C \\plevels p1 <= p2\n" +
      "\\record E (f : C -> Nat)\n" +
      "\\func test : E \\cowith\n" +
      "  | f (c : C) : Nat => 0");
  }
}
