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

  @Test
  public void varTest() {
    typeCheckModule(
      "\\func f {A : \\Type} {a a' : A} (q : a' = a) (p : a = a') : \\Sigma (x y : A) (x = y) \\elim p\n" +
      "  | idp => (a,a',q)\n" +
      "\\func test {A : \\Type} {a : A} (q : a = a) : f q idp = (a,a,q) => idp");
  }

  @Test
  public void twoIdpData() {
    typeCheckModule(
      "\\data D {A B C : \\Type} (f : A -> B) (g : B -> C) (a : A) (b : B) (c : C)\n" +
      "  | con (f a = b) (g b = c)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : D f g a b c) : t = t \\elim t\n" +
      "  | con idp idp => idp {D f g a (f a) (g (f a))} {con idp idp}");
  }

  @Test
  public void twoIdpData2() {
    typeCheckModule(
      "\\data D {A B C : \\Type} (f : A -> B) (g : B -> C) (a : A) (b : B) (c : C)\n" +
      "  | con (g b = c) (f a = b)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : D f g a b c) : t = t \\elim t\n" +
      "  | con idp idp => idp {D f g a (f a) (g (f a))} {con idp idp}");
  }

  @Test
  public void twoIdpData3() {
    typeCheckModule(
      "\\data D {A B C : \\Type} (f : A -> B) (g : B -> C) (b : B) (c : C)\n" +
      "  | con (a : A) (f a = b) (g b = c)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {b : B} {c : C} (t : D f g b c) : t = t \\elim t\n" +
      "  | con a idp idp => idp {D f g (f a) (g (f a))} {con a idp idp}");
  }

  @Test
  public void twoIdpData4() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\data D {B C : \\Type} (f : Bool -> B) (g : B -> C) (b : B) (c : C)\n" +
      "  | con (a : Bool) (f a = b) (g b = c)\n" +
      "\\func f {B C : \\Type} (f : Bool -> B) (g : B -> C) {b : B} {c : C} (t : D f g b c) : t = t \\elim t\n" +
      "  | con true idp idp => idp {D f g (f true) (g (f true))} {con true idp idp}\n" +
      "  | con false idp idp => idp {D f g (f false) (g (f false))} {con false idp idp}");
  }

  @Test
  public void twoIdpRecord() {
    typeCheckModule(
      "\\record R {A B C : \\Type} (f : A -> B) (g : B -> C) (a : A) (b : B) (c : C) (p : f a = b) (q : g b = c)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : R f g a b c) : t = t \\elim t\n" +
      "  | (idp,idp) => idp {R f g a (f a) (g (f a))} {\\new R f g a (f a) (g (f a)) idp idp}");
  }

  @Test
  public void twoIdpRecord2() {
    typeCheckModule(
      "\\record R {A B C : \\Type} (f : A -> B) (g : B -> C) (a : A) (b : B) (c : C) (p : g b = c) (q : f a = b)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : R f g a b c) : t = t \\elim t\n" +
      "  | (idp,idp) => idp {R f g a (f a) (g (f a))} {\\new R f g a (f a) (g (f a)) idp idp}");
  }

  @Test
  public void twoIdpRecord3() {
    typeCheckModule(
      "\\record R {A B C : \\Type} (f : A -> B) (g : B -> C) (b : B) (c : C) (a : A) (p : f a = b) (q : g b = c)\n" +
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {b : B} {c : C} (t : R f g b c) : t = t \\elim t\n" +
      "  | (a,idp,idp) => idp {R f g (f a) (g (f a))} {\\new R f g (f a) (g (f a)) a idp idp}");
  }

  @Test
  public void twoIdpRecord4() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\record R {B C : \\Type} (f : Bool -> B) (g : B -> C) (b : B) (c : C) (a : Bool) (p : f a = b) (q : g b = c)\n" +
      "\\func f {B C : \\Type} (f : Bool -> B) (g : B -> C) {b : B} {c : C} (t : R f g b c) : t = t \\elim t\n" +
      "  | (true,idp,idp) => idp {R f g (f true) (g (f true))} {\\new R f g (f true) (g (f true)) true idp idp}\n" +
      "  | (false,idp,idp) => idp {R f g (f false) (g (f false))} {\\new R f g (f false) (g (f false)) false idp idp}");
  }

  @Test
  public void twoIdpSigma() {
    typeCheckModule(
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : \\Sigma (f a = b) (g b = c)) : t = t \\elim t\n" +
      "  | (idp,idp) => idp {\\Sigma (f a = f a) (g (f a) = g (f a))} {(idp,idp)}");
  }

  @Test
  public void twoIdpSigma2() {
    typeCheckModule(
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {a : A} {b : B} {c : C} (t : \\Sigma (g b = c) (f a = b)) : t = t \\elim t\n" +
      "  | (idp,idp) => idp {\\Sigma (g (f a) = g (f a)) (f a = f a)} {(idp,idp)}");
  }

  @Test
  public void twoIdpSigma3() {
    typeCheckModule(
      "\\func f {A B C : \\Type} (f : A -> B) (g : B -> C) {b : B} {c : C} (t : \\Sigma (a : A) (f a = b) (g b = c)) : t = t \\elim t\n" +
      "  | (a,idp,idp) => idp {\\Sigma (a' : A) (f a' = f a) (g (f a) = g (f a))} {(a,idp,idp)}");
  }

  @Test
  public void twoIdpSigma4() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func f {B C : \\Type} (f : Bool -> B) (g : B -> C) {b : B} {c : C} (t : \\Sigma (a : Bool) (f a = b) (g b = c)) : t = t \\elim t\n" +
      "  | (true,idp,idp) => idp {\\Sigma (a : Bool) (f a = f true) (g (f true) = g (f true))} {(true,idp,idp)}\n" +
      "  | (false,idp,idp) => idp {\\Sigma (a : Bool) (f a = f false) (g (f false) = g (f false))} {(false,idp,idp)}");
  }
}
