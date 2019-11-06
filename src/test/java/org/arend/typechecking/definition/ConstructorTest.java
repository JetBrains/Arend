package org.arend.typechecking.definition;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

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
      "\\cons con => con2 (\\lam _ => 0)");
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
  public void variableError() {
    typeCheckDef("\\cons con {A : \\Type} (a : A) => a", 1);
  }

  @Test
  public void parametersError() {
    typeCheckDef("\\cons con (n m : Nat) => suc n", 1);
  }
}
