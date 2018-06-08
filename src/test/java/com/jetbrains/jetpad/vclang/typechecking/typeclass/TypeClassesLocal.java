package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesLocal extends TypeCheckingTestCase {
  @Test
  public void inferVar() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X) (a : x.A) => B a");
  }

  @Test
  public void inferVar2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarGlobalType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X { A => Nat }) => B 0");
  }

  @Test
  public void inferVarSynonym() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\class Y => X\n" +
        "\\func f (y : Y) (a : y.A) => B a");
  }

  @Test
  public void inferVarSynonym2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\class Y => X\n" +
        "\\func f (A' : \\Type0) (y : Y { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarDuplicateTele() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x y : X) (a : y.A) => B a");
  }

  @Test
  public void inferVarDuplicateTele2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A : \\Type0) (x y : X { A => A }) (a : y.A) => 0", 1);
  }

  @Test
  public void inferVarDuplicate() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (x : X) (a : x.A) {y : X { A => x.A } } => 0", 1);
  }

  @Test
  public void inferVarDuplicate2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { A => A' }) {y : X { A => A' } } (a : A') => 0", 1);
  }

  @Test
  public void inferVarDifferent() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\func f (A' : \\Type0) (x : X { A => A' }) {y : Y { A => A' } } (a : A') => B a = C a");
  }

  @Test
  public void inferVar3() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\func f (A' : \\Type0) {y : X { A => A' -> A' } } (a : A') (x : X { A => A' }) => B a");
  }

  @Test
  public void inferVarFromType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) : x.A => a");
  }

  @Test
  public void inferVarDuplicateFromType() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) (y : X { A => x.A }) : x.A => a", 1);
  }

  @Test
  public void inferVarFromType2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (x : X { A => A' }) : A' => a");
  }

  @Test
  public void inferVarDuplicateFromType2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (A' : \\Type0) (y : X { A => A' }) (x : X { A => A' }) : A' => a", 1);
  }

  @Test
  public void inferVarFromType3() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | a : A\n" +
        "}\n" +
        "\\func f (x : X) (y : X { A => x.A -> x.A }) : x.A -> x.A => a");
  }

  @Test
  public void transitiveInferLocal() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\func f {A : \\Type0} {x : X { A => A } } (a : A) => B a\n" +
        "\\func g (y : Y) (a : y.A) => f a");
  }

  @Test
  public void transitiveInferLocal2() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\func f {x : X} (a : x.A) => B a\n" +
        "\\func g (y : Y) (a : y.A) => f a");
  }

  @Test
  public void transitiveLocalDuplicate() {
    typeCheckModule(
        "\\class X (A : \\Type0) {\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\class Y => X { B => C }\n" +
        "\\func f {A : \\Type0} {x : X { A => A }} (a : A) => B a\n" +
        "\\func g {A : \\Type0} {x : X { A => A }} {y : Y { A => A }} (a : A) : f a = y.B a => path (\\lam _ => B a)");
  }
}
