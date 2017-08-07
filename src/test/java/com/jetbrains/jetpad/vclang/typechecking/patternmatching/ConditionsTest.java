package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class ConditionsTest extends TypeCheckingTestCase {
  @Test
  public void dataTypeWithConditions() {
    typeCheckClass(
        "\\data Z | zneg Nat | zpos Nat { zero => zneg zero }");
  }

  @Test
  public void dataTypeWithConditionsWrongType() {
    typeCheckClass(
        "\\data Z | zneg Nat | zpos Nat { zero => zero }", 1);
  }

  @Test
  public void dataTypeWithConditionsTCFailed1() {
    typeCheckClass(
        "\\data Z | zneg Nat | zpos Nat { zero => zpos 1 }", 1);
  }

  @Test
  public void dataTypeWithConditionsTCFailed2() {
    typeCheckClass(
        "\\data Unit | unit\n" +
        "\\data Z | zneg | zpos Unit { _ => zpos }", 1);
  }

  @Test
  public void dataTypeWithConditionsMutualDep() {
    typeCheckClass(
        "\\data Unit | unit\n" +
        "\\data Z | zpos Unit { _ => zneg unit } | zneg Unit { _ => zpos unit }", 1);
  }

  @Test
  public void simpleTest() {
    typeCheckClass(
        "\\data Z | zpos Nat | zneg Nat { zero => zpos zero }\n" +
        "\\function test (x : Z) : Nat\n" +
        "  | zneg (suc (suc _)) => 0\n" +
        "  | zneg (suc zero) => 1\n" +
        "  | zneg zero => 2\n" +
        "  | zpos x => suc (suc x)");
  }

  @Test
  public void simpleTestError() {
    typeCheckClass(
        "\\data Z | zpos Nat | zneg Nat { zero => zpos zero }\n" +
        "\\function test (x : Z) : Nat\n" +
        "  | zneg (suc (suc _)) => 0\n" +
        "  | zneg (suc zero) => 1\n" +
        "  | zneg zero => 2\n" +
        "  | zpos x => suc x", 1);
  }

  @Test
  public void multipleArgTest() {
    typeCheckClass(
        "\\data Z | negative Nat | positive Nat { zero => negative zero }\n" +
        "\\function test (x : Z) (y : Nat) : Nat\n" +
        "  | positive (suc n), m => n\n" +
        "  | positive zero, m => m\n" +
        "  | negative n, zero => zero\n" +
        "  | negative n, suc m => suc m", 1);
  }

  @Test
  public void multipleArgTestError() {
    typeCheckClass(
        "\\data Z | negative Nat | positive Nat { zero => negative zero }\n" +
        "\\function test (x : Z) (y : Nat) : Nat\n" +
        "  | positive (suc n), m => n\n" +
        "  | positive zero, m => m\n" +
        "  | negative n, zero => zero\n" +
        "  | negative n, suc m => suc (suc m)", 1);
  }

  @Test
  public void bidirectionalList() {
    typeCheckClass(
        "\\data BD-list (A : \\Type0) | nil | cons A (BD-list A) | snoc (xs : BD-list A) (y : A) => \\elim xs\n" +
        "  { | cons x xs => cons x (snoc xs y) | nil => cons y nil }\n" +
        "\\function length {A : \\Type0} (x : BD-list A) : Nat => \\elim x\n" +
        "  | nil => 0\n" +
        "  | cons x xs => suc (length xs)\n" +
        "  | snoc xs x => suc (length xs)", 1);
  }

  @Test
  public void dataTypeWithIndices() {
    typeCheckClass(
        "\\data S | base | loop I \n" +
        "  { left => base\n" +
        "  | right => base\n" +
        "  }\n" +
        "\\data D Nat \\with | _ => d | zero => di I\n" +
        "  { | left => d | right => d }\n" +
        "\\function test (x : Nat) (y : D x) : S\n" +
        "  | suc _, d => base\n" +
        "  | zero, d => base\n" +
        "  | zero, di i => loop i");
  }

  @Test
  public void testSelfConditionsError() {
    typeCheckClass(
      "\\data Unit | unit\n" +
      "\\data D\n" +
      "  | nil0\n" +
      "  | nil1 Unit { _ => nil0 }\n" +
      "  | cons1 D\n" +
      "  | cons2 D\n" +
      "  | cons0 D { | nil0 => cons1 nil0 | nil1 x => cons2 (nil1 x) }", 1);
  }

  @Test
  public void testSelfConditions() {
    typeCheckClass(
      "\\data Unit | unit\n" +
      "\\data D\n" +
      "  | nil0\n" +
      "  | nil1 Unit { _ => nil0 }\n" +
      "  | cons1 D\n" +
      "  | cons2 D { x => cons1 x }\n" +
      "  | cons0 D { | nil0 => cons1 nil0 | nil1 x => cons2 (nil1 x) }");
  }

  @Test
  public void nestedCheck() {
    typeCheckClass(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\function test (x y z : Z) : Nat\n" +
      "  | pos zero, pos zero, neg zero => 0\n" +
      "  | _, _, _ => 1", 1);
  }

  @Test
  public void nonStatic() {
    typeCheckClass(
      "| S' : \\Type0\n" +
      "| base' : S'\n" +
      "| loop' : I -> S'\n" +
      "\\data S | base | loop I\n" +
      "  { left => base\n" +
      "  | right => base\n" +
      "  }\n" +
      "\\function test (s : S) : S'\n" +
      "  | base => base'\n" +
      "  | loop i => loop' i", "", 2);
  }

  @Test
  public void constructorArgumentWithCondition() {
    typeCheckClass(
        "\\data S | base | loop Nat { zero => base }\n" +
        "\\data D | cons' | cons S { loop zero => cons' }", 1);
  }

  @Test
  public void cc() {
    typeCheckClass(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\function test (z : Z) : Nat\n" +
      "  | pos n => 0\n" +
      "  | neg (suc n) => 1");
  }

  @Test
  public void ccOtherDirectionError() {
    typeCheckClass(
      "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\function test (z : Z) : Nat\n" +
      "  | pos (suc n) => 0\n" +
      "  | neg n => 1", 2);
  }

  @Test
  public void ccComplexBranch() {
    typeCheckClass(
      "\\data D | snd | fst Nat { | zero => snd | suc _ => snd }\n" +
      "\\function test (d : D) : Nat\n" +
      "  | snd => zero", 1);
  }

  @Test
  public void whatIfNormalizeError() {
    typeCheckClass(
        "\\data Z | pos Nat | neg Nat { zero => pos zero }\n" +
        "\\function test (x : Z) : Nat\n" +
        " | neg x => 1\n" +
        " | pos x => 2", 1);
  }

  @Test
  public void whatIfDontNormalizeConditionRHS() {
    typeCheckClass(
        "\\data Unit | unit\n" +
        "\\data D | d2 | d1 Unit { _ => d2 }\n"+
        "\\data E | e1 D | e2 D { _ => e1 (d1 unit) }\n"+
        "\\function test (e : E) : Nat\n" +
        "  | e2 d2 => 1\n" +
        "  | e1 (d1 _) => 2\n" +
        "  | e1 d2 => 1", 1);
  }

  @Test
  public void dataIntervalCondition() {
    typeCheckClass("\\data D I \\with | left => c", 1);
  }

  @Test
  public void dataCondition() {
    typeCheckClass(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | c => e");
  }

  @Test
  public void dataConditionError() {
    typeCheckClass(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | l i => e", 1);
  }

  @Test
  public void dataConditionEmptyPatternError() {
    typeCheckClass(
      "\\data D | c | c' | l I\n" +
      "  { left => c\n" +
      "  | right => c'\n" +
      "  }\n" +
      "\\data E D \\with\n" +
      " | () => e", 1);
  }
}
