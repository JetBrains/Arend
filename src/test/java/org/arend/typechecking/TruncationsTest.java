package org.arend.typechecking;

import org.arend.core.definition.DataDefinition;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TruncationsTest extends TypeCheckingTestCase {
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
    typeCheckModule("""
      \\truncated \\data TrP (A : \\Type) : \\Prop | inP A
      \\func inP-inv (P : \\Prop) (p : TrP P) : P \\elim p | inP p => p
      \\func trunc-eval (P : \\Prop) (p : TrP P) : \\Prop => inP {P} (inP-inv P p) = {TrP P} p
      """);
  }

  @Test
  public void setTruncationTests() {
    typeCheckModule("""
      \\data TrS' (A : \\Type0)
        | inS' A
        | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) \\elim i, j {
          | left, _ => a
          | right, _ => a'
          | i, left => p @ i
          | i, right => q @ i
        }
      \\func set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left
      \\func set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))
      """);
  }

  @Test
  public void dynamicSetTruncationTests() {
    typeCheckClass("""
      \\data TrS' (A : \\Type0)
        | inS' A
        | truncS' (a a' : TrS' A) (p q : a = a') (i j : I) \\elim i, j {
          | left, _ => a
          | right, _ => a'
          | i, left => p @ i
          | i, right => q @ i
        }
      \\func set-trunc-test (A : \\Type0) (a a' : TrS' A) (p q : a = a') : TrS' A => truncS' a a' p q left left
      \\func set-trunc-test' (A : \\Type0) (a a' : TrS' A) (p q : a = a') : p = q => path (\\lam i => path (\\lam j => truncS' a a' p q j i))
      """, "");
  }

  @Test
  public void S1Level() {
    DataDefinition definition = (DataDefinition) typeCheckDef("""
      \\data S1
        | base
        | loop I {
          | left => base
          | right => base
        }
      """);
    assertTrue(definition.getSort().getPLevel().isClosed() && definition.getSort().getPLevel().getConstant() == 0);
    assertTrue(definition.getSort().getHLevel().isInfinity());
  }

  @Test
  public void useLevel() {
    typeCheckModule("""
      \\truncated \\data D (A : \\Type) : \\Set
        | con A
        | pathCon (a a' : A) (i : I) \\elim i {
          | left => con a
          | right => con a'
        }
        \\where \\use \\level proof {A : \\Type} (d1 d2 : D A) : d1 = d2
          | con a, con a' => path (pathCon a a')
      \\sfunc f (d : D Nat) : Nat
        | con _ => 0
        | pathCon _ _ _ => 0
      """);
  }
}
