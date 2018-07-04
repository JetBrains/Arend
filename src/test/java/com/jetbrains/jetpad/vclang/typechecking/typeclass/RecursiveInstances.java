package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class RecursiveInstances extends TypeCheckingTestCase {
  @Test
  public void instanceWithExplicitParams() {
    resolveNamesModule(
      "\\class A\n" +
      "\\class B\n" +
      "\\instance a (b : B) : A", 1);
  }

  @Test
  public void instanceWithWrongParameter() {
    resolveNamesModule(
      "\\class A\n" +
      "\\instance a {n : Nat} : A", 1);
  }

  @Test
  public void instanceWithRecordParameter() {
    resolveNamesModule(
      "\\class A\n" +
      "\\record B\n" +
      "\\instance a {b : B} : A", 1);
  }

  @Test
  public void instanceWithParameter() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B\n" +
      "\\instance B-inst : B\n" +
      "\\instance A-inst {b : B} : A | a => 0\n" +
      "\\func f => a");
  }

  @Test
  public void noRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B\n" +
      "\\instance A-inst {b : B} : A | a => 0\n" +
      "\\func f => a", 1);
  }

  @Test
  public void correctRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance B-inst : B | n => 1\n" +
      "\\instance A-inst {b : B 1} : A | a => 0\n" +
      "\\func f => a");
  }

  @Test
  public void wrongRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance B-inst : B 0\n" +
      "\\instance A-inst {b : B 1} : A | a => 0\n" +
      "\\func f => a", 1);
  }

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
  }

  @Test
  public void localRecursiveInstance() {
    typeCheckModule(
      "\\class A { | a : Nat }\n" +
      "\\class B (n : Nat)\n" +
      "\\instance A-inst {b : B 0} : A | a => 0\n" +
      "\\func f {c : B 0} => a");
  }
}
