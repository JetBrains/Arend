package org.arend.typechecking;

import org.arend.core.definition.DataDefinition;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Truncations extends TypeCheckingTestCase {
  @Test
  public void elimInProp() {
    typeCheckModule(
      "\\truncated \\data TrP (A : \\Type) : \\Prop | inP A\n" +
      "\\func inP-inv (P : \\Prop) (p : TrP P) : P \\elim p | inP p => p");
  }

  @Test
  public void elimInSet1() {
    typeCheckModule(
      "\\truncated \\data TrS (A : \\Type) : \\Set | inS A\n" +
      "\\func inS-inv (A : \\Set) (x : TrS A) : A \\elim x | inS x => x");
  }

  @Test
  public void elimInSet2() {
    typeCheckModule(
      "\\truncated \\data TrS (A : \\Type) : \\Set | inS A\n" +
      "\\func trSToNat (A : \\Type) (x : TrS A) : Nat \\elim x | inS x => 0");
  }

  @Test
  public void truncPEval() {
    typeCheckModule(
      "\\truncated \\data TrP (A : \\Type) : \\Prop | inP A\n" +
      "\\func inP-inv (P : \\Prop) (p : TrP P) : P \\elim p | inP p => p\n" +
      "\\func trunc-eval (P : \\Prop) (p : TrP P) : inP {P} (inP-inv P p) = {TrP P} p => Path.inProp _ _");
  }

  @Test
  public void setTruncationTests() {
    typeCheckModule(
        "\\data TrS' (A : \\Type0)\n" +
        "  | inS' A\n" +
        "  | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) \\elim i, j {\n" +
        "    | left, _ => a\n" +
        "    | right, _ => a'\n" +
        "    | i, left => p @ i\n" +
        "    | i, right => q @ i\n" +
        "  }\n" +
        "\\func set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left\n" +
        "\\func set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))");
  }

  @Test
  public void dynamicSetTruncationTests() {
    typeCheckClass(
        "\\data TrS' (A : \\Type0)\n" +
        "  | inS' A\n" +
        "  | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) \\elim i, j {\n" +
        "    | left, _ => a\n" +
        "    | right, _ => a'\n" +
        "    | i, left => p @ i\n" +
        "    | i, right => q @ i\n" +
        "  }\n" +
        "\\func set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left\n" +
        "\\func set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))", "");
  }

  @Test
  public void S1Level() {
    DataDefinition definition = (DataDefinition) typeCheckDef(
        "\\data S1\n" +
        "  | base\n" +
        "  | loop I {\n" +
        "    | left => base\n" +
        "    | right => base\n" +
        "  }");
    assertTrue(definition.getSort().getPLevel().isClosed() && definition.getSort().getPLevel().getConstant() == 0);
    assertTrue(definition.getSort().getHLevel().isInfinity());
  }

  @Test
  public void useLevel() {
    typeCheckModule(
      "\\truncated \\data D (A : \\Type) : \\Set\n" +
      "  | con A\n" +
      "  | pathCon (a a' : A) (i : I) \\elim i {\n" +
      "    | left => con a\n" +
      "    | right => con a'\n" +
      "  }\n" +
      "  \\where \\use \\level proof {A : \\Type} (d1 d2 : D A) : d1 = d2\n" +
      "    | con a, con a' => path (pathCon a a')\n" +
      "\\sfunc f (d : D Nat) : Nat\n" +
      "  | con _ => 0\n" +
      "  | pathCon _ _ _ => 0");
  }
}
