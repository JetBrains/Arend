package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.*;

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
  public void patternsTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat\n" +
      "  | nil => 3\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1\n" +
      "\\func test1 : f (single 5) = 2 => idp\n" +
      "\\func test2 : f (cons 4 nil) = 2 => idp");
  }

  @Test
  public void patternsElimTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat \\elim xs\n" +
      "  | nil => 3\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1\n" +
      "\\func test1 : f (single 5) = 2 => idp\n" +
      "\\func test2 : f (cons 4 nil) = 2 => idp");
  }

  @Test
  public void patternsCaseTest() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat => \\case xs \\with {\n" +
      "  | nil => 3\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1\n" +
      "}\n" +
      "\\func test1 : f (single 5) = 2 => idp\n" +
      "\\func test2 : f (cons 4 nil) = 2 => idp");
  }

  @Test
  public void patternsCoverageError() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void patternsCaseCoverageError() {
    typeCheckModule(
      "\\data List (A : \\Type) | cons A (List A) | nil\n" +
      "\\cons single {A : \\Type} (a : A) => cons a nil\n" +
      "\\func f {A : \\Type} (xs : List A) : Nat => \\case xs \\with {\n" +
      "  | single x => 2\n" +
      "  | cons _ (cons _ _) => 1\n" +
      "}", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void patternsParametersTest() {
    typeCheckModule(
      "\\data D2 (n : Nat) | con2 (n = 0)\n" +
      "\\cons single (p : 0 = 0) => con2 p\n" +
      "\\func f (d : D2 0) : Nat\n" +
      "  | single _ => 3\n" +
      "\\func test1 : f (single idp) = 3 => idp\n" +
      "\\func test2 : f (con2 idp) = 3 => idp");
  }

  @Test
  public void patternsParametersCaseTest() {
    typeCheckModule(
      "\\data D2 (n : Nat) | con2 (n = 0)\n" +
      "\\cons single (p : 0 = 0) => con2 p\n" +
      "\\func f (d : D2 0) : Nat => \\case d \\with {\n" +
      "  | single _ => 3\n" +
      "}\n" +
      "\\func test1 : f (single idp) = 3 => idp\n" +
      "\\func test2 : f (con2 idp) = 3 => idp");
  }

  @Test
  public void patternsParametersMismatchError() {
    typeCheckModule(
      "\\data D2 (n : Nat) | con2 (n = 0)\n" +
      "\\cons single (p : 0 = 0) => con2 p\n" +
      "\\func f (d : D2 1) : Nat\n" +
      "  | single _ => 0", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void patternsParametersMismatchCaseError() {
    typeCheckModule(
      "\\data D2 (n : Nat) | con2 (n = 0)\n" +
      "\\cons single (p : 0 = 0) => con2 p\n" +
      "\\func f (d : D2 1) : Nat => \\case d \\with {\n" +
      "  | single _ => 0\n" +
      "}", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void tupleTest() {
    typeCheckModule(
      "\\data D1 | con1 Nat\n" +
      "\\data D2 (d1 : D1) | con2 (d1' : D1) (p : d1 = d1')\n" +
      "\\cons con (n : Nat) (d : D1) (p : con1 (suc n) = d) : \\Sigma (x : D1) (D2 x) => (con1 (suc n), con2 d p)\n" +
      "\\func f (q : \\Sigma (x : D1) (D2 x)) : Nat\n" +
      "  | con n _ _ => n\n" +
      "  | _ => 0");
  }

  @Test
  public void cowithTest() {
    typeCheckModule(
      "\\record Pair (A B : \\Type)\n" +
      "  | proj1 : A\n" +
      "  | proj2 : B\n" +
      "\\cons pair {A B : \\Type} (a : A) (b : B) : Pair A B\n" +
      "  | proj1 => a\n" +
      "  | proj2 => b\n" +
      "\\data D1 | con1 (Pair Nat Nat)\n" +
      "\\data D2 | con2 D1 Nat\n" +
      "\\cons con (n m k : Nat) => con2 (con1 (pair (suc n) m)) (suc k)\n" +
      "\\func f (d : D2) : Nat\n" +
      "  | con n m k => n Nat.+ m Nat.+ k\n" +
      "  | _ => 0\n" +
      "\\func test : f (con2 (con1 (\\new Pair Nat Nat 7 12)) 3) = 20 => idp");
  }

  @Test
  public void newTest() {
    typeCheckModule(
      "\\record Pair (A B : \\Type)\n" +
      "  | proj1 : A\n" +
      "  | proj2 : B\n" +
      "\\cons pair {A B : \\Type} (a : A) (b : B) : Pair A B\n" +
      "  => \\new Pair {\n" +
      "    | proj1 => a\n" +
      "    | proj2 => b\n" +
      "  }\n" +
      "\\data D1 | con1 (Pair Nat Nat)\n" +
      "\\data D2 | con2 D1 Nat\n" +
      "\\cons con (n m k : Nat) => con2 (con1 (pair (suc n) m)) (suc k)\n" +
      "\\func f (d : D2) : Nat\n" +
      "  | con n m k => n Nat.+ m Nat.+ k\n" +
      "  | _ => 0\n" +
      "\\func test : f (con2 (con1 (\\new Pair Nat Nat 7 12)) 3) = 20 => idp");
  }

  @Test
  public void numberTest() {
    typeCheckModule(
      "\\cons one => 1\n" +
      "\\func f (x : Nat) : Nat\n" +
      " | 0 => 10\n" +
      " | one => 20\n" +
      " | suc (suc x) => x");
  }

  @Test
  public void numberError() {
    typeCheckDef("\\cons one => 200", 1);
  }

  @Test
  public void goalTest() {
    typeCheckDef("\\cons test => suc {?}", 1);
    assertThatErrorsAre(goalError());
  }

  @Test
  public void dependentTypeTest() {
    typeCheckModule(
      "\\data D (n : Nat) | con {m : Nat} (m = n)\n" +
      "\\cons con2 (x : Nat) : D x => con idp\n" +
      "\\func f (d : D 0) : Nat | con2 y => y\n" +
      "\\func test : f (con idp) = 0 => idp");
  }
}
