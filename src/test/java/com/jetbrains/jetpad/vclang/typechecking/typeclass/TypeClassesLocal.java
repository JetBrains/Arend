package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class TypeClassesLocal extends TypeCheckingTestCase {
  @Test
  public void inferVar() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a");
  }

  @Test
  public void inferVar2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarRenamedView() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\function f (x : Y) (a : x.A) => B a");
  }

  @Test
  public void inferVarRenamedView2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : Y { A => A' }) (a : A') => B a");
  }

  @Test
  public void inferVarRenamedViewError() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) => B a", 1);
  }

  @Test
  public void inferVarRenamedViewError2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view Y \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X { A => A' }) (a : A') => B a", 1);
  }

  @Test
  public void inferVarDuplicate() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (x : X) (a : x.A) {y : X { A => x.A } } => B a", 1);
  }

  @Test
  public void inferVarDuplicate2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) (x : X { A => A' }) {y : X { A => A' } } (a : A') => B a", 1);
  }

  @Test
  public void inferVar3() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract B : A -> Nat\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { B }\n" +
        "\\function f (A' : \\Type0) {y : X { A => A' -> A' } } (a : A') (x : X { A => A' }) => B a");
  }

  @Test
  public void inferVarFromType() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { a }\n" +
        "\\function f (x : X) : x.A => a");
  }

  @Test
  public void inferVarDuplicateFromType() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { a }\n" +
        "\\function f (x : X) (y : X { A => x.A }) : x.A => a", 1);
  }

  @Test
  public void inferVarFromType2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { a }\n" +
        "\\function f (A' : \\Type0) (x : X { A => A' }) : A' => a");
  }

  @Test
  public void inferVarDuplicateFromType2() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { a }\n" +
        "\\function f (A' : \\Type0) (y : X { A => A' }) (x : X { A => A' }) : A' => a", 1);
  }

  @Test
  public void inferVarFromType3() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\abstract A : \\Type0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\view \\on X \\by A { a }\n" +
        "\\function f (x : X) (y : X { A => x.A -> x.A }) : x.A -> x.A => a");
  }
}
