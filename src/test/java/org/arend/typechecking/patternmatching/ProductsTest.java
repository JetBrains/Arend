package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.arend.Matchers.missingClauses;

public class ProductsTest extends TypeCheckingTestCase {
  @Test
  public void tupleTest() {
    typeCheckModule(
      "\\func swap {A B : \\Type} (p : \\Sigma A B) : \\Sigma B A\n" +
      "  | (x,y) => (y,x)");
  }

  @Test
  public void emptyTupleTest() {
    typeCheckModule(
      "\\func f (p : \\Sigma) : \\Sigma\n" +
      "  | () => ()");
  }

  @Test
  public void embeddedTupleTest() {
    typeCheckModule(
      "\\func f (p : \\Sigma Nat Nat) : Nat\n" +
      "  | (zero , zero ) => 0\n" +
      "  | (zero , suc _) => 1\n" +
      "  | (suc _, zero ) => 2\n" +
      "  | (suc x, suc _) => x\n" +
      "\\func g1 : f (0,0) = 0 => idp\n" +
      "\\func g2 : f (0,5) = 1 => idp\n" +
      "\\func g3 : f (5,0) = 2 => idp\n" +
      "\\func g4 : f (7,2) = 6 => idp");
  }

  @Test
  public void embeddedTupleTest2() {
    typeCheckModule(
      "\\data D | con (\\Sigma Nat Nat) | con'\n" +
      "\\func f (d : D) : Nat\n" +
      "  | con (zero , y) => y\n" +
      "  | con (suc x, _) => x\n" +
      "  | con' => 0");
  }

  @Test
  public void embeddedTupleTestError() {
    typeCheckModule(
      "\\func f (p : \\Sigma Nat Nat) : Nat\n" +
      "  | (zero, zero) => 0\n" +
      "  | (zero, suc _) => 1\n" +
      "  | (suc _, zero) => 2\n" +
      "  | (suc (suc x), suc _) => x", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void classTest() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\func f (p : A) : \\Sigma Nat Nat\n" +
      "  | (n,m) => (n,m)");
  }

  /*
  @Test
  public void newClassTest() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\func f (p : A) : \\Sigma Nat Nat\n" +
      "  | \\new A n m => (n,m)");
  }

  @Test
  public void namedClassTest() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\func f (p : A) : \\Sigma Nat Nat\n" +
      "  | \\new A { n => n' | m } => (n',m)");
  }

  @Test
  public void unnamedClassTest() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\func f (p : A) : \\Sigma Nat Nat\n" +
      "  | { n' | m } => (n',m)");
  }
  */

  @Test
  public void embeddedClassTest() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\func f (p : A) : Nat\n" +
      "  | (zero , zero ) => 0\n" +
      "  | (suc _, zero ) => 1\n" +
      "  | (zero , suc _) => 2\n" +
      "  | (suc _, suc y) => y\n" +
      // "  |         { |      zero  |      zero } => 0\n" +
      // "  | \\new A { | m => zero  | n => suc _ } => 1\n" +
      // "  |         {        zero  |      suc _ } => 2\n" +
      // "  | \\new A {        suc _ |      suc y } => y\n" +
      "\\func g1 : f (\\new A 0 0) = 0 => idp\n" +
      "\\func g2 : f (\\new A 0 5) = 2 => idp\n" +
      "\\func g3 : f (\\new A 5 0) = 1 => idp\n" +
      "\\func g4 : f (\\new A 7 5) = 4 => idp");
  }

  @Test
  public void embeddedClassTest2() {
    typeCheckModule(
      "\\class A (n m : Nat)\n" +
      "\\data D | con A | con'\n" +
      "\\func f (d : D) : Nat\n" +
      "  | con' => 0\n" +
      "  | con (_, suc y) => y\n" +
      // "  | con (\\new A { m => suc y }) => y\n" +
      "  | con (x, _) => x\n" +
      // "  | con (\\new A { n => x }) => x\n" +
      "\\func g1 : f (con (\\new A 5 0)) = 5 => idp\n" +
      "\\func g2 : f (con (\\new A 8 5)) = 4 => idp");
  }

  @Test
  public void dependentRecordTest() {
    typeCheckModule(
      "\\record Pair (A B : \\Type) | fst : A | snd : B\n" +
      "\\func swap {A' B' : \\Type} (p : Pair A' B') : Pair B' A'\n" +
      "  | (a, b) => \\new Pair { | fst => b | snd => a }");
  }

  @Test
  public void extensionRecordTest() {
    typeCheckModule(
      "\\record R (a : Nat)\n" +
      "\\record S \\extends R | b : Nat\n" +
      "\\func f (s : S) : Nat\n" +
      "  | (a,b) => a Nat.+ b");
  }

  @Ignore
  @Test
  public void singleFieldTest() {
    typeCheckModule(
      "\\record S (a : Nat)\n" +
      "\\func f (s : S) : Nat\n" +
      "  | 0 => 0\n" +
      "  | suc n => n\n" +
      "\\func g : f (\\new S 2) = 1 => idp");
  }

  @Test
  public void implRecordTest() {
    typeCheckModule(
      "\\record R (A : \\Type) (a : A)\n" +
      "\\record S \\extends R | A => Nat | b : Nat\n" +
      "\\func f (s : S) : Nat\n" +
      "  | (0, m) => m\n" +
      "  | (suc n, _) => n");
  }

  @Test
  public void swapTest() {
    typeCheckModule(
      "\\record Pair (A B : \\Type)\n" +
      "  | fst : A\n" +
      "  | snd : B\n" +
      "\\func swap {A B : \\Type} (p : Pair A B) : Pair B A \\cowith\n" +
      "  | fst => p.snd\n" +
      "  | snd => p.fst\n" +
      "\\func swap-involutive {A B : \\Type} (p : Pair A B) : swap (swap p) = p\n" +
      "  | (a,b) => idp");
  }

  @Test
  public void etaTest() {
    typeCheckModule(
      "\\record R (x y : Nat)\n" +
      "\\func f (r : R) : Nat\n" +
      "  | (t,_) => t\n" +
      "\\func g (r : R) : f r = r.x => idp");
  }
}
