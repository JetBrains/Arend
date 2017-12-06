package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class CoerceTest extends TypeCheckingTestCase {
  @Test
  public void doCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\function f (c : C) : \\Set => c\n" +
      "\\function g (n : f (\\new C { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void doNotCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\function f (c : C) : Nat => c", 1);
  }

  @Test
  public void coerceSuper() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "\\class D \\extends C { | a : A }\n" +
      "\\function f (d : D) : \\Set => d\n" +
      "\\function g (n : f (\\new D { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void doNotCoerceSuperMultiple() {
    typeCheckModule(
      "\\class C (A : Nat)\n" +
      "\\class C2 (B : \\Set)\n" +
      "\\class D \\extends C, C2 { | b : B }\n" +
      "\\function f (d : D) : \\Set => d");
  }
}
