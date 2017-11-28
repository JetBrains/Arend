package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesLocal extends TypeCheckingTestCase {
  @Test
  public void inferVar() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (x : X') (a : x.A) => B a");
  }

  @Test
  public void inferVar2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X' { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarGlobalType() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (x : X' { A => Nat }) => B 0");
  }

  @Test
  public void inferVarRenamedView() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (x : Y) (a : x.A) => B a");
  }

  @Test
  public void inferVarRenamedView2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : Y { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarRenamedViewError() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a", 1);
  }

  @Test
  public void inferVarRenamedViewError2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X { A => A' }) (a : A') => B a", 1);
  }

  @Test
  public void inferVarDuplicateTele() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (x y : X') (a : y.A) => B a");
  }

  @Test
  public void inferVarDuplicateTele2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (A : \\Type0) (x y : X' { A => A }) (a : y.A) => 0", 1);
  }

  @Test
  public void inferVarDuplicate() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (x : X') (a : x.A) {y : X' { A => x.A } } => 0", 1);
  }

  @Test
  public void inferVarDuplicate2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X' { A => A' }) {y : X' { A => A' } } (a : A') => 0", 1);
  }

  @Test
  public void inferVarDifferent() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\view Y \\on X \\by A { B => C }\n" +
        "\\function f (A' : \\Type0) (x : X' { A => A' }) {y : Y { A => A' } } (a : A') => B a = C a");
  }

  @Test
  public void inferVar3() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> Nat\n" +
        "}\n" +
        "\\view X' \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) {y : X' { A => A' -> A' } } (a : A') (x : X' { A => A' }) => B a");
  }

  @Test
  public void inferVarFromType() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\view X' \\on X \\by A { a }\n" +
        "\\function f (x : X') : x.A => a");
  }

  @Test
  public void inferVarDuplicateFromType() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\view X' \\on X \\by A { a }\n" +
        "\\function f (x : X') (y : X' { A => x.A }) : x.A => a", 1);
  }

  @Test
  public void inferVarFromType2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\view X' \\on X \\by A { a }\n" +
        "\\function f (A' : \\Type0) (x : X' { A => A' }) : A' => a");
  }

  @Test
  public void inferVarDuplicateFromType2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\view X' \\on X \\by A { a }\n" +
        "\\function f (A' : \\Type0) (y : X' { A => A' }) (x : X' { A => A' }) : A' => a", 1);
  }

  @Test
  public void inferVarFromType3() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\view X' \\on X \\by A { a }\n" +
        "\\function f (x : X') (y : X' { A => x.A -> x.A }) : x.A -> x.A => a");
  }

  @Test
  public void transitiveInferLocal() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A } } (a : A) => B a\n" +
        "\\function g (z : Z) (a : z.A) => f a");
  }

  @Test
  public void transitiveInferLocal2() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\function f {y : Y} (a : y.A) => B a\n" +
        "\\function g (z : Z) (a : z.A) => f a");
  }

  @Test
  public void transitiveLocalDuplicate() {
    typeCheckModule(
        "\\class X {\n" +
        "  | A : \\Type0\n" +
        "  | B : A -> \\Type0\n" +
        "}\n" +
        "\\view Y \\on X \\by A { B }\n" +
        "\\view Z \\on X \\by A { B => C }\n" +
        "\\function f {A : \\Type0} {y : Y { A => A }} (a : A) => B a\n" +
        "\\function g {A : \\Type0} {y : Y { A => A }} {z : Z { A => A }} (a : A) : f a = y.B a => path (\\lam _ => B a)");
  }
}
