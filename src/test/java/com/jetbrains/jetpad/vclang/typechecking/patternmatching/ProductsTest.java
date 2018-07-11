package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.missingClauses;

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
      "\\func g1 : f (0,0) = 0 => path (\\lam _ => 0)\n" +
      "\\func g2 : f (0,5) = 1 => path (\\lam _ => 1)\n" +
      "\\func g3 : f (5,0) = 2 => path (\\lam _ => 2)\n" +
      "\\func g4 : f (7,2) = 6 => path (\\lam _ => 6)");
  }

  @Test
  public void embeddedTupleTest2() {
    typeCheckModule(
      "\\data D | con \\Sigma Nat Nat | con'\n" +
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
      "\\func g1 : f (\\new A 0 0) = 0 => path (\\lam _ => 0)\n" +
      "\\func g2 : f (\\new A 0 5) = 2 => path (\\lam _ => 2)\n" +
      "\\func g3 : f (\\new A 5 0) = 1 => path (\\lam _ => 1)\n" +
      "\\func g4 : f (\\new A 7 5) = 4 => path (\\lam _ => 4)");
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
      "\\func g1 : f (con (\\new A 5 0)) = 5 => path (\\lam _ => 5)\n" +
      "\\func g2 : f (con (\\new A 8 5)) = 4 => path (\\lam _ => 4)");
  }
}
