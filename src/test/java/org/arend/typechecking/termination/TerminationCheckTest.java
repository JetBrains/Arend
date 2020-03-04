/*
 * Copyright 2003-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
  public void test34() {
    TestVertex ack = new TestVertex("ack", "x", "y");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", ack, ack, '<', 0, '?'));
    cms.add(new TestCallMatrix("1", ack, ack, '=', 0, '<', 1));
    BaseCallGraph callCategory = TestCallGraph.calculateClosure(cms);
    assert callCategory.checkTermination();
  }

  @Test
  public void artificial1() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", f, f, '<', 0, '?', '?', '?'));
    cms.add(new TestCallMatrix("2", f, f, '=', 0, '<', 1, '?', '?'));
    cms.add(new TestCallMatrix("3", f, f, '=', 0, '=', 1, '<', 2, '?'));
    cms.add(new TestCallMatrix("4", f, f, '=', 0, '=', 1, '=', 2, '<', 3));
    BaseCallGraph callCategory = TestCallGraph.calculateClosure(cms);
    assert callCategory.checkTermination();
  }

  @Test
  public void artificial2() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("1", f, f, '<', 0, '?', '?', '?'));
    cms.add(new TestCallMatrix("2", f, f, '=', 0, '<', 1, '?', '?'));
    cms.add(new TestCallMatrix("3", f, f, '=', 0, '=', 1, '<', 2, '?'));
    cms.add(new TestCallMatrix("4", f, f, '=', 0, '=', 1, '=', 2, '=', 3));
    BaseCallGraph callCategory = TestCallGraph.calculateClosure(cms);
    assert !callCategory.checkTermination();
  }

  @Test
  public void artificial3() {
    TestVertex f = new TestVertex("f", "x", "y", "z", "w");
    Set<BaseCallMatrix<TestVertex>> cms = new HashSet<>();
    cms.add(new TestCallMatrix("2", f, f, '?', '<', 1, '?', '=', 3));
    cms.add(new TestCallMatrix("3", f, f, '?', '=', 1, '<', 2, '=', 3));
    cms.add(new TestCallMatrix("1", f, f, '?', '?', '?', '<', 3));
    cms.add(new TestCallMatrix("4", f, f, '<', 0, '=', 1, '=', 2, '=', 3));
    BaseCallGraph callCategory = TestCallGraph.calculateClosure(cms);
    assert callCategory.checkTermination();
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
    BaseCallGraph callCategory = TestCallGraph.calculateClosure(cms);
    assert !callCategory.checkTermination();
  }

}
