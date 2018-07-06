package com.jetbrains.jetpad.vclang.typechecking;

import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.typeMismatchError;

public class IntTest extends TypeCheckingTestCase {
  @Test
  public void notInt() {
    typeCheckModule(
      "\\data D\n" +
      "\\func f => D {>= 0}", 1);
  }

  @Test
  public void afterInt() {
    typeCheckModule(
      "\\data D\n" +
      "\\func f => Int {>= 0}");
  }

  @Test
  public void afterNat() {
    typeCheckModule(
      "\\data D\n" +
      "\\func f => Nat {<= 0}");
  }

  @Test
  public void comparison() {
    typeCheckModule(
      "\\func f1 : Int {>= 5} {<= 10} => 6\n" +
      "\\func g1 : Int => f1\n" +
      "\\func f2 : Int {>= 5} {<= 10} => 6\n" +
      "\\func g2 : Int {>= 1} => f2\n" +
      "\\func f3 : Int {>= 5} {<= 10} => 6\n" +
      "\\func g3 : Int {<= 15} => f3\n" +
      "\\func f4 : Int {>= 5} {<= 10} => 6\n" +
      "\\func g4 : Int {>= 1} {<= 10} => f4\n" +
      "\\func f5 : Int {>= 5} => 6\n" +
      "\\func g5 : Int {>= 1} => f5\n" +
      "\\func f6 : Int {<= 15} => 6\n" +
      "\\func g6 : Int {>= 0} {<= 10} => f6");
  }

  @Test
  public void comparisonError() {
    typeCheckModule(
      "\\func foo : Int => 6\n" +
      "\\func g : Int {>= 5} {<= 10} => foo", 1);
    assertThatErrorsAre(typeMismatchError());
    errorList.clear();

    typeCheckModule(
      "\\func f : Int {>= 5} => 6\n" +
      "\\func g : Int {>= 6} => f", 1);
    assertThatErrorsAre(typeMismatchError());
    errorList.clear();

    typeCheckModule(
      "\\func f : Int {<= 6} => 6\n" +
      "\\func g : Int {<= 5} => f", 1);
    assertThatErrorsAre(typeMismatchError());
    errorList.clear();

    typeCheckModule(
      "\\func f : Int {>= 5} {<= 10} => 6\n" +
      "\\func g : Int {>= 5} {<= 11} => f", 1);
    assertThatErrorsAre(typeMismatchError());
    errorList.clear();

    typeCheckModule(
      "\\func f : Int {>= 5} {<= 10} => 6\n" +
      "\\func g : Int {>= 6} {<= 10} => f", 1);
    assertThatErrorsAre(typeMismatchError());
    errorList.clear();
  }
}