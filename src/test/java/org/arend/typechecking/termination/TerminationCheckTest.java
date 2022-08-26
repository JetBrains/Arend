/*
 * Copyright 2003-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arend.typechecking.termination;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TerminationCheckTest extends TypeCheckingTestCase {

  @Test
  public void test31_1() {
    typeCheckModule("\\func \\infixl 9 ++ (a b : Nat) : Nat \\elim a | suc a' => suc (a' ++ b) | zero => b", 0);
  }

  @Test
  public void test31_2() {
    typeCheckModule("\\func \\infixl 9 + (a b : Nat) : Nat \\elim a | suc a' => suc (suc a' + b) | zero => b", 1);
  }

  private static final String minus =
    "\\func \\infix 9 - (x y : Nat) : Nat \\elim x | zero => zero | suc x' => x' - p y\n" +
      "\\where \\func p (z : Nat) : Nat | zero => zero | suc z' => z'\n";

  private static final String list =
    "\\data List (A : \\Type0) | nil | \\infixr 5 :-: A (List A)\n";

  @Test
  public void test32() {
    typeCheckModule(minus, 0);
  }

  @Test
  public void test33() {
    typeCheckModule(minus + "\\func \\infix 9 / (x y : Nat) : Nat => div' x (-.p x - y)\n" +
      "\\where \\func div' (x : Nat) (y' : Nat) : Nat\n" +
      "\\elim y' | zero => zero | suc y'' => suc (div' x (x - suc y''))\n", 1);
  }

  @Test
  public void test34_2() {
    typeCheckModule("\\func ack (x y : Nat) : Nat\n" +
      "| zero, y => suc y\n" +
      "| suc x', zero => ack x' (suc zero)\n" +
      "| suc x', suc y' => ack (suc x') y'", 0);
  }

  @Test
  public void test36_1() {
    typeCheckModule(list + "\\func flatten {A : \\Type0} (l : List (List A)) : List A \\elim l\n" +
      "| nil => nil\n" +
      "| :-: nil xs => flatten xs\n" +
      "| :-: (:-: y ys) xs => y :-: flatten (ys :-: xs)", 0);
  }

  @Test
  public void test36_2() {
    typeCheckModule(list + "\\func f {A : \\Type0} (l : List (List A)) : List A \\elim l | nil => nil | :-: x xs => g x xs\n" +
      "\\func g {A : \\Type0} (l : List A) (ls : List (List A)) : List A \\elim l | nil => f ls | :-: x xs => x :-: g xs ls", 0);
  }

  @Test
  public void test38_1() {
    typeCheckModule(list + "\\func zip1 {A : \\Type0} (l1 l2 : List A) : List A \\elim l1\n" +
      "| nil => l2\n" +
      "| :-: x xs => x :-: zip2 l2 xs\n" +
      "\\func zip2 {A : \\Type0} (l1 l2 : List A) : List A \\elim l1\n" +
      "| nil => l2\n" +
      "| :-: x xs => x :-: zip1 l2 xs\n", 0);
  }

  @Test
  public void test38_2() {
    typeCheckModule(list + "\\func zip-bad {A : \\Type0} (l1 l2 : List A) : List A \\elim l1\n" +
      "| nil => l2\n" +
      "| :-: x xs => x :-: zip-bad l2 xs", 1);
  }

  @Test
  public void test310() {
    typeCheckModule("\\data ord | O | S (_ : ord) | Lim (_ : Nat -> ord)\n" +
      "\\func addord (x y : ord) : ord \\elim x\n" +
      "| O => y\n" +
      "| S x' => S (addord x' y)\n" +
      "| Lim f => Lim (\\lam z => addord (f z) y)", 0);
  }

  @Test
  public void test312_2() {
    typeCheckModule("\\func h (x y : Nat) : Nat\n" +
      "| zero, zero => zero\n" +
      "| zero, suc y' => h zero y'\n" +
      "| suc x', y' => h x' y'\n" +
      "\\func f (x y : Nat) : Nat\n" +
      "| zero, _ => zero\n" +
      "| suc x', zero => zero\n" +
      "| suc x', suc y' => h (g x' (suc y')) (f (suc (suc (suc x'))) y')\n" +
      "\\func g (x y : Nat) : Nat\n" +
      "| zero, _ => zero\n" +
      "| suc x', zero => zero\n" +
      "| suc x', suc y' => h (f (suc x') (suc y')) (g x' (suc (suc y')))", 2);
  }

  @Test
  public void selfCallInType() {
    typeCheckModule(
      "\\data D Nat | con\n" +
      "\\func f (x : Nat) (y : D (f x con)) : Nat => x", 1);
  }

  @Test
  public void headerCycle() {
    typeCheckModule(
      "\\func he1 : he2 = he2 => path (\\lam _ => he2)\n" +
      "\\func he2 : he1 = he1 => path (\\lam _ => he1)\n", 1);
  }

  @Test
  public void headerNoCycle() {
    typeCheckModule(
      "\\func he1 (n : Nat) : Nat | zero => 0 | suc n => he2 n @ right\n" +
      "\\func he2 (n : Nat) : he1 n = he1 n | zero => path (\\lam _ => he1 0) | suc n => path (\\lam _ => he1 (suc n))");
  }

  @Test
  public void twoErrors() {
    typeCheckModule(
      "\\data D Nat | con\n" +
      "\\func f (x : Nat) (y : D (f x con)) : Nat => x\n" +
      "\\func g : Nat => f 0 con", 2);
  }

  @Test
  public void nonMonomialCallMatrixTest() {
    typeCheckModule(
      "\\data Int : \\Set0 | pos Nat | neg Nat { zero => pos zero }\n" +
      "\\func \\infixl 6 +$ (n m : Int) : Int \\elim n\n" +
      "  | pos zero => m\n" +
      "  | pos (suc n) => pos n +$ m\n" +
      "  | neg zero => m\n" +
      "  | neg (suc n) => neg n +$ m\n", 0);
  }

  @Test
  public void test121_1() {
    typeCheckModule("\\func foo (p : \\Sigma Nat Nat) : Nat\n" +
      "  | (n, 0) => 0\n" +
      "  | (n, suc m) => foo (7, m)", 0);
  }

  @Test
  public void test121_2() {
    typeCheckModule("\\data List (A : \\Type) | nil | cons A (List A)\n\n" +
            "\\data All {A : \\Type} (P : A -> \\Type) (xs : List A) \\elim xs\n" +
            "  | nil => nilAll\n" +
            "  | cons x xs => consAll (P x) (All P xs)\n\n" +
            "\\data End1 (n : Nat)\n" +
            "  | end1 (\\Pi (m : Nat) -> End1 m)\n\n" +
            "\\func foo1 (xs : List Nat) (ys : All End1 xs) : Nat\n" +
            "  | nil, _ => 0\n" +
            "  | cons x xs, consAll (end1 e) ys => foo1 (cons x xs) (consAll (e x) ys)\n\n" +
            "\\data End2 (n : Nat)\n" +
            "  | end2 (m : Nat) (\\Sigma -> End2 m)\n\n" +
            "\\func foo2 (xs : List Nat) (ys : All End2 xs) : Nat\n" +
            "  | nil, _ => 0\n" +
            "  | cons x xs, consAll (end2 y e) ys => foo2 (cons y xs) (consAll (e ()) ys)\n\n" +
            "\\func bar1 (x : Nat) (e : End1 x) : Nat \\elim e\n" +
            "  | end1 e => bar1 x (e x)\n\n" +
            "\\func bar2 (x : Nat) (e : End2 x) : Nat \\elim e\n" +
            "  | end2 y e => bar2 y (e ())", 0);
  }

  @Test
  public void test_loop1() {
    typeCheckModule("\\func lol (a : \\Sigma Nat Nat) (b : \\Sigma Nat Nat) : Nat \\elim a, b {\n" +
            "  | (n,n1), (n2,n3) => lol (n, n1) (n2, n3)\n" +
            "}", 1);
  }

  @Test
  public void test_loop2() {
    typeCheckModule(
            "\\func fooA (p : \\Sigma Nat (\\Sigma Nat Nat)) : Nat \\elim p\n" +
            "  | (a, (b, c)) => fooB a b c\n\n" +
            "\\func fooB (a b c : Nat) : Nat \\with | a, b, c => fooA (a, (b, c))", 2);
  }

  @Test
  public void test_200() {
    typeCheckModule("\\data List (A : \\Type) \n  | nil \n  | cons A (List A)\n\n" +
            "\\func f (xs : List Nat) : Nat \n  | nil => 0 \n  | cons _ nil => 1 \n  | cons _ (cons x xs) => f (cons x xs)", 0);
  }

  @Test
  public void test_ise(){
    typeCheckModule("\\func h (a : Nat) : Nat \\elim a\n  | zero => 1\n  | suc a => \\case (a, a) \\with {\n    | p => g a p\n  }\n\n" +
            "\\func g (a : Nat) (p : \\Sigma Nat Nat) : Nat \\elim a\n  | 0 => 0\n  | suc a => h a\n", 0);
  }

  @Test
  public void testBug(){
    typeCheckModule("\\data Bool | true | false\n\\func f (p : \\Sigma Bool Nat) => f p\n", 2);
  }

  @Test
  public void test34() {
    TestVertex ack = new TestVertex("ack", "x", "y");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", ack, ack, '<', 0, '?'));
    cms.add(new TestCallMatrix("1", ack, ack, '=', 0, '<', 1));
    assert TestCallGraph.testTermination(cms);
  }

  @Test
  public void artificial1() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", f, f, '<', 0, '?', '?', '?'));
    cms.add(new TestCallMatrix("2", f, f, '=', 0, '<', 1, '?', '?'));
    cms.add(new TestCallMatrix("3", f, f, '=', 0, '=', 1, '<', 2, '?'));
    cms.add(new TestCallMatrix("4", f, f, '=', 0, '=', 1, '=', 2, '<', 3));
    assert TestCallGraph.testTermination(cms);
  }

  @Test
  public void artificial2() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", f, f, '<', 0, '?', '?', '?'));
    cms.add(new TestCallMatrix("2", f, f, '=', 0, '<', 1, '?', '?'));
    cms.add(new TestCallMatrix("3", f, f, '=', 0, '=', 1, '<', 2, '?'));
    cms.add(new TestCallMatrix("4", f, f, '=', 0, '=', 1, '=', 2, '=', 3));
    assert !TestCallGraph.testTermination(cms);
  }

  @Test
  public void artificial3() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("2", f, f, '?', '<', 1, '?', '=', 3));
    cms.add(new TestCallMatrix("3", f, f, '?', '=', 1, '<', 2, '=', 3));
    cms.add(new TestCallMatrix("1", f, f, '?', '?', '?', '<', 3));
    cms.add(new TestCallMatrix("4", f, f, '<', 0, '=', 1, '=', 2, '=', 3));
    assert TestCallGraph.testTermination(cms);
  }

  @Test
  public void test312() {
    TestVertex h = new TestVertex("h", "hx", "hy");
    TestVertex f = new TestVertex("f", "fx", "fy");
    TestVertex g = new TestVertex("g", "gx", "gy");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("h-h-1", h, h, '<', 0, '=', 1));
    cms.add(new TestCallMatrix("h-h-2", h, h, '=', 0, '<', 1));
    cms.add(new TestCallMatrix("f-f", f, f, '?', '<', 1));
    cms.add(new TestCallMatrix("f-h", f, h, '?', '?'));
    cms.add(new TestCallMatrix("f-g", f, g, '<', 0, '=', 1));
    cms.add(new TestCallMatrix("g-f", g, f, '=', 0, '=', 1));
    cms.add(new TestCallMatrix("g-g", g, g, '<', 0, '?'));
    cms.add(new TestCallMatrix("g-h", g, h, '?', '?'));
    assert !TestCallGraph.testTermination(cms);
  }

  @Test
  public void compareTest() {
    var v = new TestVertex("v");
    var e1 = new TestCallMatrix("1", v, v,'<', 0);
    var e2 = new TestCallMatrix("1", v, v,'=', 0);
    var e3 = new TestCallMatrix("1", v, v,'<', 0, '=', 1);
    var e4 = new TestCallMatrix("1", v, v,'=', 0, '<', 1);
    var e5 = new TestCallMatrix("1", v, v,'<', 0, '<', 1);
    var e6 = new TestCallMatrix("1", v, v,'?');
    assert (e6.compare(e1) == BaseCallMatrix.R.LessThan && e6.compare(e2) == BaseCallMatrix.R.LessThan &&
            e6.compare(e3) == BaseCallMatrix.R.LessThan && e6.compare(e4) == BaseCallMatrix.R.LessThan &&
            e6.compare(e5) == BaseCallMatrix.R.LessThan);
    assert (e2.compare(e1) == BaseCallMatrix.R.LessThan && e1.compare(e2) == BaseCallMatrix.R.Unknown);
    assert (e1.compare(e3) == BaseCallMatrix.R.LessThan && e2.compare(e3) == BaseCallMatrix.R.LessThan &&
            e3.compare(e1) == BaseCallMatrix.R.Unknown && e3.compare(e2) == BaseCallMatrix.R.Unknown);
    assert (e3.compare(e4) == BaseCallMatrix.R.Unknown && e4.compare(e3) == BaseCallMatrix.R.Unknown);
    assert (e3.compare(e5) == BaseCallMatrix.R.LessThan && e4.compare(e5) == BaseCallMatrix.R.LessThan);
  }

  @Test
  public void performanceTest() {
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    TestVertex Cut = new TestVertex("a","T", "k", "n", "D", "I", "G", "M", "R", "p1", "p2");
    TestVertex CCut = new TestVertex("b","T", "k", "n", "D", "I", "G", "M", "R", "p1", "p2");

    cms.add(new TestCallMatrix("ab", Cut, CCut, '=', 0, '<', 1, '?', '=', 3, '<', 4, '-', '<', 6, '=', 5, '=', 7, '<', 8, '?'));
    cms.add(new TestCallMatrix("ba", CCut, Cut, '=', 0, '=', 1, '?', '=', 3, '=', 4, '=', 5, '?', '=', 6, '?', '?'));

    cms.add(new TestCallMatrix("aa1", Cut, Cut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '?', '=', 7, '<', 8));
    cms.add(new TestCallMatrix("aa2", Cut, Cut, '=', 0, '=', 1, '<', 2, '?', '=', 4, '=', 5, '=', 6, '?', '<', 8));
    cms.add(new TestCallMatrix("aa3", Cut, Cut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '<', 8));
    cms.add(new TestCallMatrix("aa4", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '<', 5, '-', '<', 6, '?', '?'));
    cms.add(new TestCallMatrix("aa5", Cut, Cut, '=', 0, '?', '?', '=', 3, '<', 4, '=', 5, '=', 6, '?', '?'));
    cms.add(new TestCallMatrix("aa6", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '=', 6, '?', '?'));
    cms.add(new TestCallMatrix("aa7", Cut, Cut, '=', 0, '<', 1, '=', 2, '?', '=', 4, '=', 5, '=', 6, '<', 7, '?'));
    cms.add(new TestCallMatrix("aa8", Cut, Cut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '<', 7, '=', 8));
    cms.add(new TestCallMatrix("aa9", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '?', '=', 7, '<', 8));
    cms.add(new TestCallMatrix("aa10", Cut, Cut, '=', 0, '=', 1, '<', 2, '?', '=', 4, '=', 5, '=', 6, '?', '?'));
    cms.add(new TestCallMatrix("aa11", Cut, Cut, '=', 0, '<', 1, '=', 2, '=', 3, '=', 4, '?', '=', 6, '<', 7, '?'));
    cms.add(new TestCallMatrix("aa12", Cut, Cut, '=', 0, '<', 2, '?', '=', 3, '<', 4, '=', 5, '?', '<', 8, '?'));
    cms.add(new TestCallMatrix("aa13", Cut, Cut, '=', 0, '<', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '<', 7, '=', 8));
    cms.add(new TestCallMatrix("aa14", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '<', 8));
    cms.add(new TestCallMatrix("aa15", Cut, Cut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '<', 6, '=', 7, '<', 8));
    cms.add(new TestCallMatrix("aa16", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '?', '=', 7, '<', 6, '-', '<', 8));
    cms.add(new TestCallMatrix("aa17", Cut, Cut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '?', '=', 7, '?'));

    cms.add(new TestCallMatrix("bb1", CCut, CCut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '?', '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb2", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '=', 6, '?', '=', 8, '<', 7, '-', '<', 9));
    cms.add(new TestCallMatrix("bb3", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '=', 6, '=', 7, '=', 8, '?'));
    cms.add(new TestCallMatrix("bb4", CCut, CCut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb5", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '=', 6, '-', '=', 7, '?', '=', 8, '?'));
    cms.add(new TestCallMatrix("bb6", CCut, CCut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '<', 8, '=', 9));
    cms.add(new TestCallMatrix("bb7", CCut, CCut, '=', 0, '=', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '<', 7, '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb8", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb9", CCut, CCut, '=', 0, '<', 1, '=', 2, '=', 3, '=', 4, '=', 5, '=', 6, '=', 7, '<', 8, '=', 9));
    cms.add(new TestCallMatrix("bb10", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '=', 5, '=', 6, '?', '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb11", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '=', 6, '=', 7, '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb12", CCut, CCut, '=', 0, '=', 1, '<', 2, '?', '=', 4, '=', 5, '=', 6, '=', 7, '?', '?'));
    cms.add(new TestCallMatrix("bb13", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '=', 6, '<', 5, '-', '<', 7, '=', 8, '<', 9));
    cms.add(new TestCallMatrix("bb14", CCut, CCut, '=', 0, '=', 1, '<', 2, '=', 3, '=', 4, '?', '=', 6, '<', 6, '-', '=', 7, '=', 8, '?'));

    assert TestCallGraph.testTermination(cms);
  }

  @Test
  public void factorialTest() {
    typeCheckModule(
            "\\func bad_rec (x1 x2 x3 x4 x5 x6 x7 x8 x9 x10 : Nat) : Nat \\elim x1\n" +
            "  | zero => zero\n" +
            "  | suc x1 => bad_rec x2 x1 x3 x4 x5 x6 x7 x8 x9 x10 Nat.+ bad_rec x10 x1 x2 x3 x4 x5 x6 x7 x8 x9", 1);
  }

}
