package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.missingClauses;

public class ConstructorTest extends TypeCheckingTestCase {
  @Test
  public void constructorTest() {
    typeCheckModule(
      "\\data D1 | con1 | con1' Nat\n" +
      "\\data D2 | con2 D1 | con2'\n" +
      "\\cons con (n : Nat) => con2 (con1' (suc n))");
  }

  @Test
  public void functionError() {
    typeCheckModule(
      "\\data D1 | con1 | con1' Nat\n" +
      "\\data D2 | con2 D1 | con2'\n" +
      "\\cons con (n : Nat) => con2 (con1' (n Nat.+ 1))", 1);
  }

  @Test
  public void lambdaTest() {
    typeCheckModule(
      "\\data D2 | con2 (Nat -> Nat)\n" +
      "\\cons con => con2 (\\lam _ => 0)", 1);
  }

  @Test
  public void lambdaError() {
    typeCheckModule(
      "\\data D2 | con2 (Nat -> Nat)\n" +
      "\\cons con => con2 (\\lam n => n)", 1);
  }

  @Test
  public void doubleVariableTest() {
    typeCheckModule(
      "\\data D1 | con1 | con1' Nat\n" +
      "\\data D2 | con2 D1 Nat | con2'\n" +
      "\\cons con (n m : Nat) => con2 (con1' n) m");
  }

  @Test
  public void doubleVariableError() {
    typeCheckModule(
      "\\data D1 | con1 | con1' Nat\n" +
      "\\data D2 | con2 D1 Nat | con2'\n" +
      "\\cons con (n : Nat) => con2 (con1' n) n", 1);
  }

  @Test
  public void variableTest() {
    typeCheckDef("\\cons con (n : Nat) => n");
  }

  @Test
  public void variableTest2() {
    typeCheckDef("\\cons con {A : \\Type} (a : A) => a");
  }

  @Test
  public void parametersTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil");
  }

  @Test
  public void parametersError() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single (A : \\Type) (a : A) => cons a nil", 1);
  }

  @Test
  public void parametersError2() {
    typeCheckDef("\\cons con {x : Nat} (n : Nat) => suc n", 1);
  }

  @Test
  public void elimError() {
    typeCheckDef(
      "\\cons con (n : Nat) : Nat \\elim n\n" +
      "  | 0 => 0\n" +
      "  | suc n => suc n", 1);
  }

  @Test
  public void cowithError() {
    typeCheckModule(
      "\\record Pair (A B : \\Type)\n" +
      "  | proj1 : A\n" +
      "  | proj2 : B\n" +
      "\\cons pair {A B : \\Type} (a : A) (b : B) : Pair A B \\cowith\n" +
      "  | proj1 => a\n" +
      "  | proj2 => b", 1);
  }

  @Test
  public void patternsCoverageTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat\n" +
      "  | nil => 3\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1\n" +
      "\\func test1 : f (single 5) = 2 => path (\\lam _ => 2)\n" +
      "\\func test2 : f (cons 4 nil) = 2 => path (\\lam _ => 2)");
  }

  @Test
  public void patternsCoverageError() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat\n" +
      "  | single x => 2\n" +
      "  | cons (cons _) => 1", 1);
    assertThatErrorsAre(missingClauses(1));
  }
}
