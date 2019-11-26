package org.arend.typechecking.constructions;

import org.arend.typechecking.Matchers;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.notInScope;
import static org.arend.typechecking.Matchers.typeMismatchError;

public class Case extends TypeCheckingTestCase {
  @Test
  public void testCase() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func f (b : Bool) : Bool => \\case b \\with { | true => false | false => true }");
  }

  @Test
  public void testCaseReturn() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\func f (b : Bool) => \\case b \\as x \\return not (not x) = x \\with { | true => idp | false => idp }");
  }

  @Test
  public void testCaseReturnError() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\func f (b : Bool) => \\case b \\return not (not b) = b \\with { | true => idp | false => idp }", 2);
    assertThatErrorsAre(Matchers.error(), Matchers.error());
  }

  @Test
  public void testCaseArguments() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\data Or (A B : \\Type) | inl A | inr B\n" +
      "\\func f (b : Bool) : (b = true) `Or` (b = false) => \\case b \\as x, idp : b = x \\with { | true, p => inl p | false, p => inr p }");
  }

  @Test
  public void testCaseMultipleArguments() {
    typeCheckModule(
      "\\func \\infix 4 < (n m : Nat) => Nat\n" +
      "\\func f1 (n k : Nat) : Nat => \\case k \\as z, n < z \\as r, idp : r = n < z \\with { | k, T, P => 0 }\n" +
      "\\func f2 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < z} p \\with { | k, p, s => 0 }\n" +
      "\\func f3 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < k} p \\with { | k, p, s => 0 }");
  }

  @Test
  public void caseElimResolveError() {
    resolveNamesDef(
      "\\func f (x : Nat) : Nat => \\case \\elim x \\with {\n" +
      "  | _ => x\n" +
      "}", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void caseElim() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) => \\case \\elim x, p : x = 0 \\return x = 0 \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimSubst() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, p \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimSubstType() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, \\elim p : x = 0 \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimTypeError() {
    typeCheckDef(
      "\\func f (x : \\Set0) => \\case \\elim x : \\Set1 \\return Nat \\with {\n" +
      "  | _ => 0\n" +
      "}", 1);
    assertThatErrorsAre(typeMismatchError());
  }
}
