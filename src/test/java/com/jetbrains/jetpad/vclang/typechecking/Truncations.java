package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Truncations extends TypeCheckingTestCase {
  @Test
  public void elimInProp() {
    typeCheckDef("\\function inP-inv (P : \\Prop) (p : TrP P) : P <= \\elim p | inP p => p");
  }

  @Test
  public void truncPEval() {
    typeCheckClass(
        "\\function inP-inv (P : \\Prop) (p : TrP P) : P <= \\elim p | inP p => p\n" +
        "\\function trunc-eval (P : \\Prop) (p : TrP P) : (Path (\\lam _ => TrP P) ((TrP P).inP (inP-inv P p)) p) => path ((TrP P).truncP ((TrP P).inP (inP-inv P p)) p)");
  }

  @Test
  public void setTruncationTests() {
    typeCheckClass(
        "\\data TrS' (A : \\Type0)\n" +
        "    | inS' A\n" +
        "    | truncS' (a a' : TrS' A) (p q : a = a') I I\n" +
        "  \\with\n" +
        "    | truncS' a _ _ _ left _ => a\n" +
        "    | truncS' _ a' _ _ right _ => a'\n" +
        "    | truncS' _ _ p _ i left => p @ i\n" +
        "    | truncS' _ _ _ q i right => q @ i\n" +
        "\n" +
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
        "    | inS' A\n" +
        "    | truncS' (a a' : TrS' A) (p q : a = a') I I\n" +
        "  \\with\n" +
        "    | truncS' a _ _ _ left _ => a\n" +
        "    | truncS' _ a' _ _ right _ => a'\n" +
        "    | truncS' _ _ p _ i left => p @ i\n" +
        "    | truncS' _ _ _ q i right => q @ i\n" +
        "\n" +
        "\\function\n" +
        "set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left\n" +
        "\n" +
        "\\function\n" +
        "set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))", "");
  }

  @Test
  public void S1Level() {
    DataDefinition definition = (DataDefinition) typeCheckDef(
        "\\data S1 | base | loop I\n" +
        "\\with\n" +
        "  | loop left => base\n" +
        "  | loop right => base");
    assertTrue(definition.getSorts().getPLevel().isMinimum());
    assertTrue(definition.getSorts().getHLevel().isInfinity());
  }
}
