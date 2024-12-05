package org.arend.typechecking;

import org.junit.Test;

public class InfixPostfixTest extends TypeCheckingTestCase {
  @Test
  public void infixTest() {
    typeCheckModule("""
      \\func test1 : suc 4 Nat.`div` suc 1 = 2 => idp
      \\func test2 : suc 4 Nat.`+` suc 1 = 7 => idp
      \\open Nat
      \\func test3 : suc 4 `div` suc 1 = 2 => idp
      \\func test4 : suc 4 `+` suc 1 = 7 => idp
      """);
  }

  @Test
  public void infixRightSectionTest() {
    typeCheckModule("""
      \\func test1 : (Nat.`div` suc 1) 5 = 2 => idp
      \\func test2 : (Nat.`+` suc 1) 5 = 7 => idp
      \\open Nat
      \\func test3 : (`div` suc 1) 5 = 2 => idp
      \\func test4 : (`+` suc 1) 5 = 7 => idp
      """);
  }

  @Test
  public void postfixTest() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)
      }
      \\func test1 : 3 Test.`f 1 2 = 9 => idp
      \\func test2 : suc 4 Nat.`+ suc 1 = 7 => idp
      \\open Test
      \\open Nat
      \\func test3 : 3 `f 1 2 = 9 => idp
      \\func test4 : suc 4 `+ suc 1 = 7 => idp
      """);
  }

  @Test
  public void postfixRightSectionTest() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)
      }
      \\func test1 : (Test.`f 1 2) 3 = 9 => idp
      \\func test2 : (Nat.`+ suc 1) 5 = 7 => idp
      \\open Test
      \\open Nat
      \\func test3 : (`f 1 2) 3 = 9 => idp
      \\func test4 : (`+ suc 1) 5 = 7 => idp
      """);
  }

  @Test
  public void mixedInfixRightSectionTest() {
    typeCheckModule("""
      \\func test1 : (Nat.`+` suc 1 * 3) 4 = 10 => idp
      \\func test2 : (Nat.`*` suc 1 + 3) 4 = 11 => idp
      \\open Nat
      \\func test3 : (`+` suc 1 * 3) 4 = 10 => idp
      \\func test4 : (`*` suc 1 + 3) 4 = 11 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest() {
    typeCheckModule("""
      \\func test1 : (Nat.`+ suc 1 * 2) 5 = 9 => idp
      \\open Nat
      \\func test2 : (`+ suc 1 * 2) 5 = 9 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest2() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (x y : Nat) (g : Nat -> Nat -> Nat) => x Nat.* (g y 1)
      }
      \\func test1 : (Test.`f 3 (Nat.+)) 2 = 8 => idp
      \\func test2 : 2 Test.`f 3 (Nat.+) = 8 => idp
      \\open Test
      \\func test3 : (`f 3 (Nat.+)) 2 = 8 => idp
      \\func test4 : 2 `f 3 (Nat.+) = 8 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest3() {
    typeCheckModule("""
      \\func test1 : (Nat.`+ suc 1 * 3) 4 = 10 => idp
      \\func test2 : (Nat.`* suc 1 + 3) 4 = 11 => idp
      \\open Nat
      \\func test3 : (`+ suc 1 * 3) 4 = 10 => idp
      \\func test4 : (`* suc 1 + 3) 4 = 11 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest4() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (x y z : Nat) => x Nat.* (y Nat.+ z)
      }
      \\func test1 : (Test.`f 1 2 Nat.+ 3) 4 = 15 => idp
      \\func test2 : 4 Test.`f 1 2 Nat.+ 3 = 15 => idp
      \\open Test
      \\func test3 : (`f 1 2 Nat.+ 3) 4 = 15 => idp
      \\func test4 : 4 Test.`f 1 2 Nat.+ 3 = 15 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest5() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (x y : Nat) => x Nat.* y
      }
      \\func test1 : (Test.`f 2 Nat.+) 3 1 = 7 => idp
      \\func test2 : (3 Test.`f 2 Nat.+) 1 = 7 => idp
      \\open Test
      \\func test3 : (`f 2 Nat.+) 3 1 = 7 => idp
      \\func test4 : (3 `f 2 Nat.+) 1 = 7 => idp
      """);
  }

  @Test
  public void mixedPostfixRightSectionTest6() {
    typeCheckModule("""
      \\module Test \\where {
        \\func f (y : Nat) => 2 Nat.* y
      }
      \\func test1 : (Test.`f Nat.+) 3 1 = 7 => idp
      \\func test2 : (3 Test.`f Nat.+) 1 = 7 => idp
      \\open Test
      \\func test3 : (`f Nat.+) 3 1 = 7 => idp
      \\func test4 : (3 `f Nat.+) 1 = 7 => idp
      """);
  }

  @Test
  public void postfixInfixRightSectionTest() {
    resolveNamesModule("""
      \\module Test \\where {
        \\func f (x : Nat) (g : Nat -> Nat -> Nat) => x Nat.* (g x x)
      }
      \\func test1 : (Test.`f (Nat.+)) 3 = 18 => idp
      \\func test2 : 3 Test.`f (Nat.+) = 18 => idp
      \\open Test
      \\func test3 : (`f (Nat.+)) 3 = 18 => idp
      \\func test4 : (`f (Nat.+)) 3 = 18 => idp
      """);
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
    resolveNamesModule("""
      \\module Test \\where {
        \\func pair => (0,1)
      }
      \\func test => Test.`pair`.1
      """, 1);
  }

  @Test
  public void postfixLongNameProjTest() {
    resolveNamesModule("""
      \\module Test \\where {
        \\func pair => (0,1)
      }
      \\func test => Test.`pair.1
      """, 1);
  }

  @Test
  public void mixedPostfixTest() {
    typeCheckModule("""
      \\func \\fix 6 # (n : Nat) : Nat
        | zero => zero
        | suc n => suc (suc (n `#))
      \\func \\infix 5 $ (n m : Nat) : Nat \\elim m
        | zero => n
        | suc m => suc (n $ m)
      \\func f : (1 $ 1 `#) = 3 => idp
      """);
  }

  @Test
  public void mixedPostfixTest2() {
    typeCheckModule("""
      \\func \\infix 4 d (n : Nat) : Nat
        | zero => zero
        | suc n => suc (suc (n `d))
      \\func \\infix 5 $ (n m : Nat) : Nat \\elim m
        | zero => n
        | suc m => suc (n $ m)
      \\func f : (1 $ 1 `d) = 4 => idp
      """);
  }

  @Test
  public void classTest() {
    typeCheckModule("""
        \\record R
          | \\infix 5 + (x y : Nat) : Nat
          | \\fix 5 * (x y : Nat) : Nat
        \\func test0 (r : R) => + {r} 0 1
      """ /* +
      "\\func test1 (r : R) : (r.+ 0 1) = (+ {r} 0 1) => idp\n" +
      "\\func test2 (r : R) : (0 r.+ 1) = (+ {r} 0 1) => idp\n" +
      "\\func test3 (r : R) : (0 r.`+` 1) = (+ {r} 0 1) => idp\n" +
      "\\func test4 (r : R) : (0 r.`+ 1) = (+ {r} 0 1) => idp\n" +
      "\\func test5 (r : R) : (r.* 0 1) = (* {r} 0 1) => idp\n" +
      "\\func test6 (r : R) : (0 r.* 1) = (* {r} 0 1) => idp\n" +
      "\\func test7 (r : R) : (0 r.`*` 1) = (* {r} 0 1) => idp\n" +
      "\\func test8 (r : R) : (0 r.`* 1) = (* {r} 0 1) => idp" */);
  }

  @Test
  public void dynamicTest() {
    typeCheckModule("""
      \\record R (field2 : Nat -> Nat)
        | field : Nat -> Nat
      \\func test (r : R) => 0 r.`field
      \\func test2 (r : R) => 0 r.`field2
      """);
  }
}
