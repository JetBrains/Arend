package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.typechecking.Matchers;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class CoerceTest extends TypeCheckingTestCase {
  @Test
  public void doCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C) : \\Set => c\n" +
      "\\func g (n : f (\\new C { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void coerceType() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C { A => Nat }) : c => 1\n" +
      "\\func g : f (\\new C { | A => Nat | a => 0 }) = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void doNotCoerce() {
    typeCheckModule(
      "\\class C (A : \\Set) {\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f (c : C) : Nat => c", 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void doNotCoerceExtendsExplicit() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "\\class D (B : \\Set) \\extends C", 1);
    assertThatErrorsAre(Matchers.classCoerceError());
  }

  @Test
  public void coerceExtendsImplicit() {
    typeCheckModule(
      "\\class C (A : \\Set)\n" +
      "\\class D \\extends C { | a : A }\n" +
      "\\func f (d : D) : \\Set => d\n" +
      "\\func g (n : f (\\new D { | A => Nat | a => 0 })) => suc n");
  }

  @Test
  public void doNotCoerceExtendsMultiple() {
    typeCheckModule(
      "\\class C1 (A : Nat)\n" +
      "\\class C2 (B : \\Set)\n" +
      "\\class D \\extends C1, C2", 1);
    assertThatErrorsAre(Matchers.classCoerceError());
  }

  @Test
  public void coerceExtendsMultipleExplicit() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\Set)\n" +
      "\\class D (n : Nat) \\extends C2 \\Prop, C1 n { | b : B }\n" +
      "\\func f (d : D) : d => b d\n" +
      "\\func g (d : D) : Nat => n d");
  }

  @Test
  public void coerceExtendsMultipleImplicit() {
    typeCheckModule(
      "\\class C1 (n : Nat)\n" +
      "\\class C2 (B : \\Set)\n" +
      "\\class D \\extends C2 \\Prop, C1 { | b : B }\n" +
      "\\func f (d : D) : d => b d\n" +
      "\\func g (d : D) : Nat => d");
  }
}
