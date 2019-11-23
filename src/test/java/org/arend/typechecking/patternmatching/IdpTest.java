package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class IdpTest extends TypeCheckingTestCase {
  @Test
  public void jTest() {
    typeCheckModule(
      "\\func J {A : \\Type} {a : A} (B : \\Pi {a' : A} -> a = a' -> \\Type) (b : B idp) {a' : A} (p : a = a') : B p \\elim p\n" +
      "  | idp => b\n" +
      "\\func test : J (\\lam {n} _ => n = 0) idp idp = idp => idp");
  }

  @Test
  public void kTest() {
    typeCheckModule(
      "\\func K {A : \\Type} {a : A} (B : a = a -> \\Type) (b : B idp) (p : a = a) : B p \\elim p\n" +
      "  | idp => b", 1);
  }

  @Test
  public void natTest() {
    typeCheckModule(
      "\\func test {n : Nat} (p : n = n Nat.+ n) (B : Nat -> \\Type) (b : B n) : B (n Nat.+ n) \\elim p\n" +
      "  | idp => b", 1);
  }

  @Test
  public void reorderTest() {
    typeCheckModule(
      "\\func f {A : \\Type} (B : A -> \\Type) {a a' : A} (b : B a) (b' : B a') (p : a = a') : \\Sigma (B a) (B a) (B a') (B a') \\elim p\n" +
      "  | idp => (b,b',b,b')");
  }

  @Test
  public void nestedIdpTest() {
    typeCheckModule(
      "\\data D (n : Nat) | con (n = 0) | con' (1 = n)\n" +
      "\\func f (x : Nat) (d : D x) : x Nat.<= 1 \\elim d\n" +
      "  | con idp => Nat.zero<=_\n" +
      "  | con' idp => Nat.suc<=suc Nat.zero<=_");
  }

  @Test
  public void multipleIdpTest() {
    typeCheckDef(
      "\\func f {A : \\Type} {a1 a2 a3 a4 : A} (p : a1 = a2) (q : a2 = a3) (r : a4 = a3) : a1 = a4\n" +
      "  | idp, idp, idp => idp");
  }

  @Test
  public void multipleIdpError() {
    typeCheckDef(
      "\\func f {A : \\Type} {a1 a2 a3 : A} (p : a1 = a2) (q : a2 = a3) (r : a1 = a3) : a1 = a3\n" +
      "  | idp, idp, idp => idp", 1);
  }

  @Test
  public void multipleIdpError2() {
    typeCheckDef(
      "\\func f {A : \\Type} {a1 a2 a3 : A} (p : a1 = a2) (q : a2 = a3) (r : a3 = a1) : a1 = a3\n" +
      "  | idp, idp, idp => idp", 1);
  }

  @Test
  public void caseIdpTest() {
    typeCheckModule(
      "\\func f (x : Nat) (p : x = 0) => \\case x \\as x, p : x = 0 \\return x = 0 \\with {\n" +
      "  | _, idp => idp\n" +
      "}");
  }

  @Test
  public void caseExprTest() {
    typeCheckModule(
      "\\func f (f : Nat -> Nat) (x : Nat) (p : f x = 0) => \\case f x \\as x', p : x' = 0 \\return x' = 0 \\with {\n" +
      "  | _, idp => idp\n" +
      "}");
  }

  @Test
  public void substInPattern() {
    typeCheckModule(
      "\\func K {A : \\Type} {a : A} (p : \\Sigma (x : A) (a = x)) : p = (a,idp) \\elim p\n" +
      "  | (_,idp) => idp");
  }

  @Test
  public void substInPattern2() {
    typeCheckModule(
      "\\func K {A : \\Type} {a : A} (p : \\Sigma (x : A) (x = a)) : p = (a,idp) \\elim p\n" +
      "  | (_,idp) => idp");
  }

  @Test
  public void extTest() {
    typeCheckModule(
      "\\func \\infix 4 *> {A : \\Type} {a a' a'' : A} (p : a = a') (q : a' = a'') : a = a'' \\elim q\n" +
      "  | idp => p\n" +
      "\\func ext {A B : \\Type} (f : A -> B) (b0 : B) (x1 x'1 : A) (x'2 : f x'1 = b0) (p : x1 = x'1)\n" +
      "  : (x1, path (\\lam i => f (p @ i)) *> x'2 ) = {\\Sigma (a : A) (f a = b0)} (x'1,x'2) \\elim x'2, p\n" +
      "  | idp, idp => idp\n" +
      "\\func test {A B : \\Type} (f : A -> B) (x1 : A) : ext f (f x1) x1 x1 idp idp = idp => idp");
  }

  @Test
  public void substError() {
    typeCheckDef(
      "\\func f {A : \\Type} (B : A -> \\Type) {a : A} (b : B a) {a' : b = b -> A} (q : b = b) (p : a' q = a) : Nat \\elim p\n" +
      "  | idp => 0", 1);
  }

  @Test
  public void chooseVarTest() {
    typeCheckModule(
      "\\func f {A : \\Type} (B : A -> \\Type) {a : A} (b : B a) {a' : A} (p : a = a') : B a' \\elim p\n" +
      "  | idp => b\n" +
      "\\func g {A : \\Type} (B : A -> \\Type) {a' : A} (b : B a') {a : A} (p : a = a') : B a' \\elim p\n" +
      "  | idp => b");
  }

  @Test
  public void caseVarError() {
    typeCheckModule(
      "\\func f (x : Nat) (p : x = 0) => \\case p : x = 0 \\return Nat \\with {\n" +
      "  | idp => 0\n" +
      "}", 1);
  }

  @Test
  public void parametersTest() {
    typeCheckModule(
      "\\func f {x : Nat} (t : \\Sigma (x = 0) (x = 1)) : 0 = 1\n" +
      "  | (idp,q) => q");
  }

  @Test
  public void parametersTest2() {
    typeCheckModule(
      "\\func f {x : Nat} (t : \\Sigma (x = 0) (x = 1)) : 1 = 0\n" +
      "  | (q,idp) => q");
  }
}
