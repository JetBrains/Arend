package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;

public class ConditionsTest {
  @Test
  public void dataTypeWithConditions() {
    typeCheckClass(
        "\\static \\data Z | zpos Nat | zneg Nat\n" +
            "\\with | zpos zero => zneg zero"
    );
  }

  @Test
  public void dataTypeWithConditionsWrongType() {
    typeCheckClass(
        "\\static \\data Z | zpos Nat | zneg Nat\n" +
            "\\with | zpos zero => zero", 1
    );
  }

  @Test
  public void dataTypeWithConditionsTCFailed1() {
    typeCheckClass(
        "\\static \\data Z | zpos Nat | zneg Nat\n" +
            "\\with | zpos zero => zpos 1"
    , 1);
  }

  @Test
  public void dataTypeWithConditionsTCFailed2() {
    typeCheckClass(
        "\\static \\data Z | zpos | zneg \n" +
            "\\with | zpos => zpos"
    , 1);
  }

  @Test
  public void dataTypeWithConditionsMutualDep() {
    typeCheckClass(
       "\\static \\data Z | zpos | zneg \n"  +
           "\\with | zpos => zneg | zneg => zpos\n"
    , 1);
  }

  @Test
  public void simpleTest() {
    typeCheckClass(
        "\\static \\data Z | zpos Nat | zneg Nat \n" +
            "\\with | zneg zero => zpos zero\n" +
        "\\static \\function test (x : Z) : Nat <= \\elim x\n" +
            "| zneg (suc (suc _)) => 0\n" +
            "| zneg (suc zero) => 1\n" +
            "| zneg zero => 2\n" +
            "| zpos x => suc (suc x)"
    );
  }

  @Test
  public void simpleTestError() {
    typeCheckClass(
        "\\static \\data Z | zpos Nat | zneg Nat \n" +
            "\\with | zneg zero => zpos zero\n" +
        "\\static \\function test (x : Z) : Nat <= \\elim x\n" +
            "| zneg (suc (suc _)) => 0\n" +
            "| zneg (suc zero) => 1\n" +
            "| zneg zero => 2\n" +
            "| zpos x => suc x"
    );
  }

  @Test
  public void multipleArgTest() {
    typeCheckClass(
        "\\static \\data Z  | positive Nat | negative Nat\n" +
        "  \\with | positive zero => negative zero\n" +
        "\n" +
        "\\static \\function test (n : Z) (m : Nat) : Nat <= \\elim n, m\n" +
        "| positive (suc n), m => n\n" +
        "| positive zero, m => m\n" +
        "| negative n, zero => zero\n" +
        "| negative n, suc m => suc m");
  }

  @Test
  public void multipleArgTestError() {
    typeCheckClass(
        "\\static \\data Z  | positive Nat | negative Nat\n" +
        "  \\with | positive zero => negative zero\n" +
        "\n" +
        "\\static \\function test (n : Z) (m : Nat) : Nat <= \\elim n, m\n" +
        "| positive (suc n), m => n\n" +
        "| positive zero, m => m\n" +
        "| negative n, zero => zero\n" +
        "| negative n, suc m => suc (suc m)", 1);
  }}
