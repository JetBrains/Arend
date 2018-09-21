package org.arend.typechecking.typeclass;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.Collections;

import static org.arend.typechecking.Matchers.fieldsImplementation;

public class ClassSynonymInstances extends TypeCheckingTestCase {
  @Test
  public void inferInstanceSynonym() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-X : Y | A => Nat | C => \\lam n => Nat\n" +
      "\\func f => C 0");
  }

  @Test
  public void multipleInstances() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\func f : 1 = C 2 => path (\\lam _ => 1)");
  }

  @Test
  public void multipleInstances2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\func f : 1 = C 2 => path (\\lam _ => 1)");
  }

  @Test
  public void multipleInstances3() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\func f : 0 = B 2 => path (\\lam _ => 0)");
  }

  @Test
  public void multipleInstances4() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\func f : 0 = B 2 => path (\\lam _ => 0)");
  }

  @Test
  public void transitiveMultipleInstances() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\func f {A : \\Type0} {x : Y { | A => A } } (a : A) => C a\n" +
      "\\func g : 1 = f 2 => path (\\lam _ => 1)");
  }

  @Test
  public void transitiveMultipleInstances2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\instance Nat-Y : Y | A => Nat | C => \\lam n => 1\n" +
      "\\instance Nat-X : X | A => Nat | B => \\lam n => 0\n" +
      "\\func f {A : \\Type0} {x : Y { | A => A } } (a : A) => C a\n" +
      "\\func g : 0 = f 2 => path (\\lam _ => 0)");
  }

  @Test
  public void inferVarSynonym() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f (y : Y) (a : y.A) => C a");
  }

  @Test
  public void inferVarSynonym2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f (A' : \\Type0) (y : Y { | A => A' }) (a : A') => C a");
  }

  @Test
  public void inferVarDifferent() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> Nat\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f (A' : \\Type0) (x : X { | A => A' }) {y : Y { | A => A' } } (a : A') => B a = C a");
  }

  @Test
  public void transitiveInferLocal() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f {A : \\Type0} {x : X { | A => A } } (a : A) => B a\n" +
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
      "\\func f {A : \\Type0} {x : X { | A => A }} (a : A) => B a\n" +
      "\\func g {A : \\Type0} {x : X { | A => A }} {y : Y { | A => A }} (a : A) : f a = f a => path (\\lam _ => C a)");
  }

  @Test
  public void transitiveLocalDuplicate2() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class Y => X { B => C }\n" +
      "\\func f {A : \\Type0} {x : X { | A => A }} (a : A) => B a\n" +
      "\\func g {A : \\Type0} {y : Y { | A => A }} {x : X { | A => A }} (a : A) : f a = f a => path (\\lam _ => B a)");
  }

  @Test
  public void classSynonymExt() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class X' => X { B => C }\n" +
      "\\func f => \\new X  { | A => Nat | B => \\lam _ => Nat }\n" +
      "\\func g => \\new X' { | A => Nat | C => \\lam _ => Nat }\n" +
      "\\func p : f = g => path (\\lam _ => f)");
  }

  @Test
  public void notImplementedField() {
    typeCheckModule(
      "\\class X (A : \\Type0) {\n" +
      "  | B : A -> \\Type0\n" +
      "}\n" +
      "\\class X' => X\n" +
      "\\instance x : X' | A => Nat", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(getDefinition("X.B").getReferable())));
  }
}
