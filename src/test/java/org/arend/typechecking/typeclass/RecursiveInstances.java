package org.arend.typechecking.typeclass;

import org.arend.Matchers;
import org.arend.core.definition.ClassField;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.arend.ExpressionFactory.Ref;
import static org.arend.Matchers.instanceInference;
import static org.arend.core.expr.ExpressionFactory.FieldCall;
import static org.arend.core.expr.ExpressionFactory.Nat;

public class RecursiveInstances extends TypeCheckingTestCase {
  @Test
  public void instanceWithParameter() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B\n" +
      "\\instance B-inst : B\n" +
      "\\instance A-inst {b : B} : A | a => 0\n" +
      "\\func f => a", 1);
    assertThatErrorsAre(instanceInference(get("B")));
  }

  @Ignore
  @Test
  public void noRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B\n" +
      "\\instance A-inst {b : B} : A | a => 0\n" +
      "\\func f => a", 1);
    assertThatErrorsAre(instanceInference(get("B")));
  }

  @Ignore
  @Test
  public void correctRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance B-inst : B | n => 1\n" +
      "\\instance A-inst {b : B 1} : A | a => 0\n" +
      "\\func f => a");
  }

  @Ignore
  @Test
  public void wrongRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance B-inst : B 0\n" +
      "\\instance A-inst {b : B 1} : A | a => 0\n" +
      "\\func f => a", 1);
    assertThatErrorsAre(instanceInference(get("B")));
  }

  @Ignore
  @Test
  public void wrongRecursiveInstance2() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\data Data (A : \\Set)\n" +
      "\\data D\n" +
      "\\data D'\n" +
      "\\class B (X : \\Set)\n" +
      "\\instance B-inst : B (Data D)\n" +
      "\\instance A-inst {b : B (Data D')} : A | a => 0\n" +
      "\\func f => a", 1);
    assertThatErrorsAre(instanceInference(get("B")));
  }

  @Ignore
  @Test
  public void localRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance A-inst {b : B 0} : A | a => 0\n" +
      "\\func f {c : B 0} => a");
  }

  @Test
  public void recursiveLocalInstance() {
    typeCheckModule(
      "\\class A (X : \\Set) { | x : X }\n" +
      "\\data Data (X : \\Set) | con X\n" +
      "\\instance Nat-inst : A Nat | x => 0\n" +
      "\\instance Data-inst {T : \\Set} {d : A T} : A (Data T) | x => con x\n" +
      "\\func f : Data Nat => x");
  }

  @Test
  public void recursiveLocalInstance2() {
    typeCheckModule(
      "\\class A (X Y : \\Set) { | x : X }\n" +
      "\\data Data (X : \\Set) | con X\n" +
      "\\instance Nat-inst : A Nat | x => 0 | Y => Nat\n" +
      "\\instance Data-inst {a : A} : A (Data a.X) | x => con x | Y => Nat\n" +
      "\\func f : Data Nat => x");
  }

  @Test
  public void noRecursiveLocalInstance() {
    typeCheckModule(
      "\\class A (X : \\Set) { | x : X }\n" +
      "\\data Data (X : \\Set) | con X\n" +
      "\\data Nat' | nat\n" +
      "\\instance Nat-inst : A Nat' | x => nat\n" +
      "\\instance Data-inst {T : \\Set} {d : A T} : A (Data T) | x => con x\n" +
      "\\func f : Data Nat => x", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("A"), Nat()));
  }

  @Test
  public void noRecursiveLocalInstance2() {
    typeCheckModule(
      "\\class A (X Y : \\Set) { | x : X }\n" +
      "\\data Data (X : \\Set) | con X\n" +
      "\\instance Nat-inst : A Nat | x => 0 | Y => Nat\n" +
      "\\instance Data-inst {a : A} : A (Data a.Y) | x => con x | Y => Nat", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("A"), FieldCall((ClassField) getDefinition("A.Y"), Ref(getDefinition("Data-inst").getParameters()))));
  }

  @Test
  public void noRecursiveLocalInstance3() {
    typeCheckModule(
      "\\class A (X : \\Set) { | x : X }\n" +
      "\\data Data1 (X : \\Set) | con1 X\n" +
      "\\data Data2 (X : \\Set) | con2 X\n" +
      "\\instance Data1-inst {T : \\Set} {d : A T} : A (Data1 T) | x => con1 x\n" +
      "\\instance Data2-inst {T : \\Set} {d : A T} : A (Data2 T) | x => con2 x\n" +
      "\\func f : Data1 (Data2 Nat) => x", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("A"), Nat()));
  }

  @Test
  public void infRecursiveInstance() {
    typeCheckModule(
      "\\class C (X : \\Type) | foo : X -> X\n" +
      "\\instance inst (c : C Nat) : C Nat | foo => c.foo\n" +
      "\\func f => foo 3", 1);
    assertThatErrorsAre(Matchers.instanceInference(get("C"), Nat()));
  }

  @Test
  public void onlyLocalInstance() {
    typeCheckModule(
      "\\class C | foo : Nat\n" +
      "\\class D | bar : Nat\n" +
      "\\instance inst {c : C} : D | bar => c.foo\n" +
      "\\func f {c : C} => bar");
  }
}