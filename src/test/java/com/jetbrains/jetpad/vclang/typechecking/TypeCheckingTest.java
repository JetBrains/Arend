package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckDef;
import static org.junit.Assert.assertEquals;

public class TypeCheckingTest {
  @Test
  public void typeCheckDefinition() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function x : Nat => zero\n" +
        "\\static \\function y : Nat => x");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckDefType() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function x : \\Type0 => Nat\n" +
        "\\static \\function y : x => zero");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckInfixDef() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\static \\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckConstructor1() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor1d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void typeCheckConstructor2d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckClass(
        "\\static \\data Either (A B : \\Type0) | inl A | inr B\n" +
        "\\static \\function fun {A B : \\Type0} (e : Either A B) : \\Set0 <= \\elim e\n" +
        "  | inl _ => Nat\n" +
        "  | inr _ => Nat\n" +
        "\\static \\function test : fun (inl {Nat} {Nat} 0) => 0");
  }

  @Test
  public void testPMap1() {
    typeCheckDef("\\function pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a == a') : (f a == f a') => path1 (\\lam i => f (p @@ i))");
  }

  @Test
  public void testPMap1Mix() {
    typeCheckDef("\\function pmap {A : \\Type1} {B : \\Type0} {a a' : A} (f : A -> B) (p : a == a') : (f a = f a') => path (\\lam i => f (p @@ i))");
  }

   @Test
  public void testPMap1Error() {
    typeCheckDef("\\function pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a == a') : (f a = f a') => path1 (\\lam i => f (p @@ i))", 1);
  }

  @Test
  public void testTransport1() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a == a') (b : B a) : B a' =>\n" +
        "coe1 (\\lam i => B (p @@ i)) b right");
  }

  @Test
  public void testTransport1Error() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a == a') (b : B a) : B a' =>\n" +
        "coe (\\lam i => B (p @ i)) b right", 1);
  }
}
