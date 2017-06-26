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
package com.jetbrains.jetpad.vclang.typechecking.termination;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by user on 10/28/16.
 */
public class TerminationCheckTest extends TypeCheckingTestCase {

  @Test
  public void test31_1() {
    typeCheckClass("\\function \\infixl 9 (++) (a b : Nat) : Nat => \\elim a | suc a' => suc (a' ++ b) | zero => b", 0);
  }

  @Test
  public void test31_2() {
    typeCheckClass("\\function \\infixl 9 (+) (a b : Nat) : Nat => \\elim a | suc a' => suc (suc a' + b) | zero => b", 1);
  }

  private static final String minus =
    "\\function \\infix 9 (-) (x y : Nat) : Nat => \\elim x | zero => zero | suc x' => x' - (p y)\n" +
      "\\where \\function p (z : Nat) : Nat | zero => zero | suc z' => z'\n";

  private static final String list =
    "\\data List (A : \\Type0) | nil | \\infixr 5 (:-:) A (List A)\n";

  @Test
  public void test32() {
    typeCheckClass(minus, 0);
  }

  @Test
  public void test33() {
    typeCheckClass(minus + "\\function \\infix 9 (/) (x y : Nat) : Nat => div' x ((-).p x - y)\n" +
      "\\where \\function div' (x : Nat) (y' : Nat) : Nat =>\n" +
      "\\elim y' | zero => zero | suc y'' => suc (div' x (x - suc y''))\n", 2);
  }

  @Test
  public void test34_2() {
    typeCheckClass("\\function ack (x y : Nat) : Nat\n" +
      "| zero, y => suc y\n" +
      "| suc x', zero => ack x' (suc zero)\n" +
      "| suc x', suc y' => ack (suc x') y'", 0);
  }

  @Test
  public void test36_1() {
    typeCheckClass(list + "\\function flatten {A : \\Type0} (l : List (List A)) : List A => \\elim l\n" +
      "| nil => nil\n" +
      "| (:-:) (nil) xs => flatten xs\n" +
      "| (:-:) ((:-:) y ys) xs => y :-: flatten (ys :-: xs)", 0);
  }

  @Test
  public void test36_2() {
    typeCheckClass(list + "\\function f {A : \\Type0} (l : List (List A)) : List A => \\elim l | nil => nil | (:-:) x xs => g x xs\n" +
      "\\function g {A : \\Type0} (l : List A) (ls : List (List A)) : List A => \\elim l | nil => f ls | (:-:) x xs => x :-: g xs ls", 0);
  }

  @Test
  public void test38_1() {
    typeCheckClass(list + "\\function zip1 {A : \\Type0} (l1 l2 : List A) : List A => \\elim l1\n" +
      "| nil => l2\n" +
      "| (:-:) x xs => x :-: zip2 l2 xs\n" +
      "\\function zip2 {A : \\Type0} (l1 l2 : List A) : List A => \\elim l1\n" +
      "| nil => l2\n" +
      "| (:-:) x xs => x :-: zip1 l2 xs\n", 0);
  }

  @Test
  public void test38_2() {
    typeCheckClass(list + "\\function zip-bad {A : \\Type0} (l1 l2 : List A) : List A => \\elim l1\n" +
      "| nil => l2\n" +
      "| (:-:) x xs => x :-: zip-bad l2 xs", 1);
  }

  @Test
  public void test310() {
    typeCheckClass("\\data ord | O | S (_ : ord) | Lim (_ : Nat -> ord)\n" +
      "\\function addord (x y : ord) : ord => \\elim x\n" +
      "| O => y\n" +
      "| S x' => S (addord x' y)\n" +
      "| Lim f => Lim (\\lam z => addord (f z) y)", 0);
  }

  @Test
  public void test312_2() {
    typeCheckClass("\\function h (x y : Nat) : Nat\n" +
      "| zero, zero => zero\n" +
      "| zero, suc y' => h zero y'\n" +
      "| suc x', y' => h x' y'\n" +
      "\\function f (x y : Nat) : Nat\n" +
      "| zero, _ => zero\n" +
      "| suc x', zero => zero\n" +
      "| suc x', suc y' => h (g x' (suc y')) (f (suc (suc (suc x'))) y')\n" +
      "\\function g (x y : Nat) : Nat\n" +
      "| zero, _ => zero\n" +
      "| suc x', zero => zero\n" +
      "| suc x', suc y' => h (f (suc x') (suc y')) (g x' (suc (suc y')))", 2);
  }

  @Test
  public void selfCallInType() {
    typeCheckClass(
      "\\data D Nat | con\n" +
      "\\function f (x : Nat) (y : D (f x con)) : Nat => x", 1);
  }

  @Test
  public void headerCycle() {
    typeCheckClass(
      "\\function he1 : he2 = he2 => path (\\lam _ => he2)\n" +
      "\\function he2 : he1 = he1 => path (\\lam _ => he1)\n", 1);
  }

  @Test
  public void headerNoCycle() {
    typeCheckClass(
      "\\function he1 (n : Nat) : Nat | zero => 0 | suc n => he2 n @ right\n" +
      "\\function he2 (n : Nat) : he1 n = he1 n | zero => path (\\lam _ => he1 0) | suc n => path (\\lam _ => he1 (suc n))");
  }

  @Test
  public void twoErrors() {
    typeCheckClass(
      "\\data D Nat | con\n" +
      "\\function f (x : Nat) (y : D (f x con)) : Nat => x\n" +
      "\\function g : Nat => f 0 con", 2);
  }

  @Test
  public void threeMutualRecursiveFunctions() {
    typeCheckClass(
      "\\function f (n : Nat) (x : g n (\\lam _ => 0) -> Nat) : Nat => 0\n" +
      "\\function g (n : Nat) (x : h n = h n -> Nat) : \\Set0 => Nat\n" +
      "\\function h (n : Nat) : Nat | zero => 0 | suc n => f n (\\lam _ => n)");
  }

  @Test
  public void threeMutualRecursiveFunctionsWithoutType() {
    typeCheckClass(
      "\\function f (n : Nat) (x : g n (\\lam _ => 0) -> Nat) : Nat => 0\n" +
      "\\function g (n : Nat) (x : h n = h n -> Nat) => Nat\n" +
      "\\function h (n : Nat) : Nat | zero => 0 | suc n => f n (\\lam _ => n)", 2);
  }

  @Test
  public void nonMonomialCallMatrixTest() {
    typeCheckClass(
      "\\data Int : \\Set0 | pos Nat | neg Nat \\with neg zero => pos zero\n" +
      "\\function \\infixl 6 (+$) (n m : Int) : Int => \\elim n\n" +
      "  | pos zero => m\n" +
      "  | pos (suc n) => (pos n) +$ m\n" +
      "  | neg zero => m\n" +
      "  | neg (suc n) => (neg n) +$ m\n", 0);
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
