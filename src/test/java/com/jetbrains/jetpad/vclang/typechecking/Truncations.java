package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Truncations extends TypeCheckingTestCase {
  @Test
  public void elimInProp() {
    typeCheckDef("\\function inP-inv (P : \\Prop) (p : TrP P) : P => \\elim p | inP p => p");
  }

  @Test
  public void elimInSet1() {
    typeCheckDef("\\function inS-inv (A : \\Set) (x : TrS A) : A => \\elim x | inS x => x");
  }

  @Test
  public void elimInSet2() {
    typeCheckDef("\\function trSToNat (A : \\Type) (x : TrS A) : Nat => \\elim x | inS x => 0");
  }

  @Test
  public void truncPEval() {
    typeCheckModule(
        "\\function inP-inv (P : \\Prop) (p : TrP P) : P => \\elim p | inP p => p\n" +
        "\\function trunc-eval (P : \\Prop) (p : TrP P) : (Path (\\lam _ => TrP P) (inP {P} (inP-inv P p)) p) => path (truncP {P} (inP {P} (inP-inv P p)) p)");
  }

  @Test
  public void setTruncationTests() {
    typeCheckModule(
        "\\data TrS' (A : \\Type0)\n" +
        "  | inS' A\n" +
        "  | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) => \\elim i, j {\n" +
        "    | left, _ => a\n" +
        "    | right, _ => a'\n" +
        "    | i, left => p @ i\n" +
        "    | i, right => q @ i\n" +
        "  }\n" +
        "\\function\n" +
        "set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left\n" +
        "\n" +
        "\\function\n" +
        "set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))");
  }

  @Test
  public void dynamicSetTruncationTests() {
    typeCheckClass(
        "\\data TrS' (A : \\Type0)\n" +
        "  | inS' A\n" +
        "  | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) => \\elim i, j {\n" +
        "    | left, _ => a\n" +
        "    | right, _ => a'\n" +
        "    | i, left => p @ i\n" +
        "    | i, right => q @ i\n" +
        "  }\n" +
        "\\function\n" +
        "set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left\n" +
        "\n" +
        "\\function\n" +
        "set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))", "");
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
}
