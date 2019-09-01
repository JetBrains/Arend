package org.arend.typechecking;

import org.junit.Test;

public class InfixPostfixTest extends TypeCheckingTestCase {
  @Test
  public void infixTest() {
    typeCheckModule(
      "\\func test1 : suc 4 Nat.`div` suc 1 = 2 => path (\\lam _ => 2)\n" +
      "\\func test2 : suc 4 Nat.`+` suc 1 = 7 => path (\\lam _ => 7)\n" +
      "\\open Nat\n" +
      "\\func test3 : suc 4 `div` suc 1 = 2 => path (\\lam _ => 2)\n" +
      "\\func test4 : suc 4 `+` suc 1 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void infixRightSectionTest() {
    typeCheckModule(
      "\\func test1 : (Nat.`div` suc 1) 5 = 2 => path (\\lam _ => 2)\n" +
      "\\func test2 : (Nat.`+` suc 1) 5 = 7 => path (\\lam _ => 7)\n" +
      "\\open Nat\n" +
      "\\func test3 : (`div` suc 1) 5 = 2 => path (\\lam _ => 2)\n" +
      "\\func test4 : (`+` suc 1) 5 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void postfixTest() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)\n" +
      "}\n" +
      "\\func test1 : 3 Test.`f 1 2 = 9 => path (\\lam _ => 9)\n" +
      "\\func test2 : suc 4 Nat.`+ suc 1 = 7 => path (\\lam _ => 7)\n" +
      "\\open Test\n" +
      "\\open Nat\n" +
      "\\func test3 : 3 `f 1 2 = 9 => path (\\lam _ => 9)\n" +
      "\\func test4 : suc 4 `+ suc 1 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void postfixRightSectionTest() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)\n" +
      "}\n" +
      "\\func test1 : (Test.`f 1 2) 3 = 9 => path (\\lam _ => 9)\n" +
      "\\func test2 : (Nat.`+ suc 1) 5 = 7 => path (\\lam _ => 7)\n" +
      "\\open Test\n" +
      "\\open Nat\n" +
      "\\func test3 : (`f 1 2) 3 = 9 => path (\\lam _ => 9)\n" +
      "\\func test4 : (`+ suc 1) 5 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void mixedInfixRightSectionTest() {
    typeCheckModule(
      "\\func test1 : (Nat.`+` suc 1 * 3) 4 = 10 => path (\\lam _ => 10)\n" +
      "\\func test2 : (Nat.`*` suc 1 + 3) 4 = 11 => path (\\lam _ => 11)\n" +
      "\\open Nat\n" +
      "\\func test3 : (`+` suc 1 * 3) 4 = 10 => path (\\lam _ => 10)\n" +
      "\\func test4 : (`*` suc 1 + 3) 4 = 11 => path (\\lam _ => 11)");
  }

  @Test
  public void mixedPostfixRightSectionTest() {
    typeCheckModule(
      "\\func test1 : (Nat.`+ suc 1 * 2) 5 = 9 => path (\\lam _ => 9)\n" +
      "\\open Nat\n" +
      "\\func test2 : (`+ suc 1 * 2) 5 = 9 => path (\\lam _ => 9)");
  }

  @Test
  public void mixedPostfixRightSectionTest2() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (x y : Nat) (g : Nat -> Nat -> Nat) => x Nat.* (g y 1)\n" +
      "}\n" +
      "\\func test1 : (Test.`f 3 (Nat.+)) 2 = 8 => path (\\lam _ => 8)\n" +
      "\\func test2 : 2 Test.`f 3 (Nat.+) = 8 => path (\\lam _ => 8)\n" +
      "\\open Test\n" +
      "\\func test3 : (`f 3 (Nat.+)) 2 = 8 => path (\\lam _ => 8)\n" +
      "\\func test4 : 2 `f 3 (Nat.+) = 8 => path (\\lam _ => 8)");
  }

  @Test
  public void mixedPostfixRightSectionTest3() {
    typeCheckModule(
      "\\func test1 : (Nat.`+ suc 1 * 3) 4 = 10 => path (\\lam _ => 10)\n" +
      "\\func test2 : (Nat.`* suc 1 + 3) 4 = 11 => path (\\lam _ => 11)\n" +
      "\\open Nat\n" +
      "\\func test3 : (`+ suc 1 * 3) 4 = 10 => path (\\lam _ => 10)\n" +
      "\\func test4 : (`* suc 1 + 3) 4 = 11 => path (\\lam _ => 11)");
  }

  @Test
  public void mixedPostfixRightSectionTest4() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)\n" +
      "}\n" +
      "\\func test1 : (Test.`f 1 2 Nat.+ 3) 4 = 15 => path (\\lam _ => 15)\n" +
      "\\func test2 : 4 Test.`f 1 2 Nat.+ 3 = 15 => path (\\lam _ => 15)\n" +
      "\\open Test\n" +
      "\\func test3 : (`f 1 2 Nat.+ 3) 4 = 15 => path (\\lam _ => 15)\n" +
      "\\func test4 : 4 Test.`f 1 2 Nat.+ 3 = 15 => path (\\lam _ => 15)");
  }

  @Test
  public void mixedPostfixRightSectionTest5() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (x y : Nat) => x Nat.* y\n" +
      "}\n" +
      "\\func test1 : (Test.`f 2 Nat.+) 3 1 = 7 => path (\\lam _ => 7)\n" +
      "\\func test2 : (3 Test.`f 2 Nat.+) 1 = 7 => path (\\lam _ => 7)\n" +
      "\\open Test\n" +
      "\\func test3 : (`f 2 Nat.+) 3 1 = 7 => path (\\lam _ => 7)\n" +
      "\\func test4 : (3 `f 2 Nat.+) 1 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void mixedPostfixRightSectionTest6() {
    typeCheckModule(
      "\\module Test \\where {\n" +
      "  \\func f (y : Nat) => 2 Nat.* y\n" +
      "}\n" +
      "\\func test1 : (Test.`f Nat.+) 3 1 = 7 => path (\\lam _ => 7)\n" +
      "\\func test2 : (3 Test.`f Nat.+) 1 = 7 => path (\\lam _ => 7)\n" +
      "\\open Test\n" +
      "\\func test3 : (`f Nat.+) 3 1 = 7 => path (\\lam _ => 7)\n" +
      "\\func test4 : (3 `f Nat.+) 1 = 7 => path (\\lam _ => 7)");
  }

  @Test
  public void postfixInfixRightSectionTest() {
    resolveNamesModule(
      "\\module Test \\where {\n" +
      "  \\func f (x : Nat) (g : Nat -> Nat -> Nat) => x Nat.* (g x x)\n" +
      "}\n" +
      "\\func test1 : (Test.`f (Nat.+)) 3 = 18 => path (\\lam _ => 18)\n" +
      "\\func test2 : 3 Test.`f (Nat.+) = 18 => path (\\lam _ => 18)\n" +
      "\\open Test\n" +
      "\\func test3 : (`f (Nat.+)) 3 = 18 => path (\\lam _ => 18)\n" +
      "\\func test4 : (`f (Nat.+)) 3 = 18 => path (\\lam _ => 18)");
  }

  @Test
  public void infixLongNameSingleTest() {
    resolveNamesDef("\\func test => Nat.`div`", 1);
  }

  @Test
  public void postfixLongNameSingleTest() {
    resolveNamesDef("\\func test => Nat.`div", 1);
  }

  @Test
  public void infixLongNameProjTest() {
    resolveNamesModule(
      "\\module Test \\where {\n" +
      "  \\func pair => (0,1)\n" +
      "}\n" +
      "\\func test => Test.`pair`.1", 1);
  }

  @Test
  public void postfixLongNameProjTest() {
    resolveNamesModule(
      "\\module Test \\where {\n" +
      "  \\func pair => (0,1)\n" +
      "}\n" +
      "\\func test => Test.`pair.1", 1);
  }

  @Test
  public void mixedPostfixTest() {
    typeCheckModule(
      "\\func \\fix 6 # (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n `#))\n" +
      "\\func \\infix 5 $ (n m : Nat) : Nat \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\func f : (1 $ 1 `#) = 3 => path (\\lam _ => 3)");
  }

  @Test
  public void mixedPostfixTest2() {
    typeCheckModule(
      "\\func \\infix 4 d (n : Nat) : Nat\n" +
      "  | zero => zero\n" +
      "  | suc n => suc (suc (n `d))\n" +
      "\\func \\infix 5 $ (n m : Nat) : Nat \\elim m\n" +
      "  | zero => n\n" +
      "  | suc m => suc (n $ m)\n" +
      "\\func f : (1 $ 1 `d) = 4 => path (\\lam _ => 4)");
  }

  @Test
  public void classTest() {
    typeCheckModule(
      "\\record R\n" +
      "  | \\infix 5 + (x y : Nat) : Nat\n" +
      "  | \\fix 5 * (x y : Nat) : Nat\n" +
      "\\func idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
      "\\func test0 (r : R) => + {r} 0 1\n" /* +
      "\\func test1 (r : R) : (r.+ 0 1) = (+ {r} 0 1) => idp\n" +
      "\\func test2 (r : R) : (0 r.+ 1) = (+ {r} 0 1) => idp\n" +
      "\\func test3 (r : R) : (0 r.`+` 1) = (+ {r} 0 1) => idp\n" +
      "\\func test4 (r : R) : (0 r.`+ 1) = (+ {r} 0 1) => idp\n" +
      "\\func test5 (r : R) : (r.* 0 1) = (* {r} 0 1) => idp\n" +
      "\\func test6 (r : R) : (0 r.* 1) = (* {r} 0 1) => idp\n" +
      "\\func test7 (r : R) : (0 r.`*` 1) = (* {r} 0 1) => idp\n" +
      "\\func test8 (r : R) : (0 r.`* 1) = (* {r} 0 1) => idp" */);
  }
}
