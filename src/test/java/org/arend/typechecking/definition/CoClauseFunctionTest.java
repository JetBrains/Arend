package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

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
  public void parameterSubtypeTest() {
    typeCheckModule(
      "\\record C (f : \\Pi (A : \\Prop) (x : Nat) -> A -> A)\n" +
      "\\func test : C \\cowith\n" +
      "  | f (A : \\Set0) x a \\elim x {\n" +
      "    | 0 => a\n" +
      "    | suc n => a\n" +
      "  }");
  }

  @Test
  public void parameterSubtypeError() {
    typeCheckModule(
      "\\record C (f : \\Pi (A : \\Set0) (x : Nat) -> A -> A)\n" +
      "\\func test : C \\cowith\n" +
      "  | f (A : \\Prop) x a \\elim x {\n" +
      "    | 0 => a\n" +
      "    | suc n => a\n" +
      "  }", 1);
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
}
