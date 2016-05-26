package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;

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
            "| zpos x => suc x", 1
    );
  }

  @Test
  public void multipleArgTest() {
    typeCheckClass(
        "\\static \\data Z  | positive Nat | negative Nat\n" +
        "  \\with | positive zero => negative zero\n" +
        "\n" +
        "\\static \\function test (x : Z) (y : Nat) : Nat <= \\elim x, y\n" +
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
        "\\static \\function test (x : Z) (y : Nat) : Nat <= \\elim x, y\n" +
        "| positive (suc n), m => n\n" +
        "| positive zero, m => m\n" +
        "| negative n, zero => zero\n" +
        "| negative n, suc m => suc (suc m)", 1);
  }

  @Test
  public void bidirectionalList() {
    typeCheckClass(
        "\\static \\data BD-list (A : \\Type0) | nil | cons A (BD-list A) | snoc (BD-list A) A\n" +
        "  \\with | snoc (cons x xs) y => cons x (snoc xs y) | snoc nil x => cons x nil\n" +
        "\\static \\function length {A : \\Type0} (x : BD-list A) : Nat <= \\elim x\n" +
        "  | nil => 0\n" +
        "  | cons x xs => suc (length xs)\n" +
        "  | snoc xs x => suc (length xs)\n"
    );
  }

  @Test
  public void conditionsInLet() {
    typeCheckClass(
        "\\static \\data Z | pos Nat | neg Nat \n" +
        "\\with | neg zero => pos zero\n" +
        "\\static \\function test (x : Z) =>" +
            "\\let | f (x : Z) : Nat <= \\elim x\n" +
            "          | pos x => 1\n" +
            "          | neg x => 0\n" +
            "\\in f x"
    , 1);
  }

  @Test
  public void dataTypeWithIndicies() {
    typeCheckClass(
        "\\static \\data S | base | loop I \n" +
        "  \\with | loop left => base\n" +
        "         | loop right => base\n" +
        "\\static \\data D Nat | D zero => di I | D _ => d \n" +
        "  \\with | di left => d | di right => d\n" +
        "\\static \\function test (x : Nat) (y : D x) : S <= \\elim x, y\n" +
        "  | suc _, d => base\n" +
        "  | zero, d => base\n" +
        "  | zero, di i => loop i\n"
    );
  }

  @Test
  public void testSelfConditionsError() {
    typeCheckDef(
      "\\data D | nil0 | nil1 | cons0 D | cons1 D | cons2 D\n" +
      "  \\with | nil1 => nil0\n" +
      "         | cons0 nil0 => cons1 nil0\n" +
      "         | cons0 nil1 => cons2 nil1"
    , 1);
  }

  @Test
  public void testSelfConditions() {
    typeCheckDef(
      "\\data D | nil0 | nil1 | cons0 D | cons1 D | cons2 D\n" +
      "  \\with | nil1 => nil0\n" +
      "         | cons0 nil0 => cons1 nil0\n" +
      "         | cons0 nil1 => cons2 nil1\n" +
      "         | cons2 x => cons1 x\n"
    , 0);
  }

  @Test
  public void nestedCheck() {
    typeCheckClass(
      "\\static \\data Z | pos Nat | neg Nat \\with | neg zero => pos zero\n" +
      "\\static \\function test (x y z : Z) : Nat <= \\elim x, y, z\n" +
      "  | pos zero, pos zero, neg zero => 0\n" +
      "  | _, _, _ => 1\n"
    , 1);
  }

  @Test
  public void nonStatic() {
    typeCheckClass(
        "\\static \\data S | base | loop I \n" +
            "  \\with | loop left => base\n" +
            "         | loop right => base\n" +
        "\\abstract S' : \\Type0\n" +
        "\\abstract base' : S'\n" +
        "\\abstract loop' : I -> S'\n" +
        "\\function test (s : S) : S' <= \\elim s" +
        "  | base => base'\n" +
        "  | loop i => loop' i\n"
    , 1);
  }

  @Test
  public void cc() {
    typeCheckClass(
      "\\static \\data Z | pos Nat | neg Nat \\with neg zero => pos zero\n" +
      "\\static \\function test (z : Z) : Nat <= \\elim z\n" +
          " | pos n => 0\n" +
          " | neg (suc n) => 1\n"
    );
  }

  @Test
  public void ccOtherDirectionError() {
    typeCheckClass(
      "\\static \\data Z | pos Nat | neg Nat \\with neg zero => pos zero\n" +
      "\\static \\function test (z : Z) : Nat <= \\elim z\n" +
          " | pos (suc n) => 0\n" +
          " | neg n => 1\n"
    , 1);
  }

  @Test
  public void ccComplexBranch() {
    typeCheckClass(
      "\\static \\data D | fst Nat | snd \\with | fst zero => snd | fst (suc _) => snd \n" +
      "\\static \\function test (d : D) : Nat <= \\elim d\n" +
          " | snd => zero\n"
    );
  }

  @Test
  public void whatIfNormalizeError() {
    typeCheckClass(
        "\\static \\data Z | pos Nat | neg Nat \\with neg zero => pos zero\n" +
        "\\static \\function test (x : Z) : Nat <= \\elim x\n" +
        " | neg x => 1\n" +
        " | pos x => 2\n"
    , 1);
  }

  @Test
  public void whatIfDontNormalizeConditionRHS() {
    typeCheckClass(
        "\\static \\data D | d1 | d2 \\with d1 => d2\n"+
        "\\static \\data E | e1 D | e2 D \\with e2 x => e1 d1\n"+
        "\\static \\function test (e : E) : Nat <= \\elim e\n" +
        " | e2 d2 => 1\n" +
        " | e1 d1 => 2\n" +
        " | e1 d2 => 1\n"
    , 1);
  }
}
