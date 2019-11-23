package org.arend.typechecking.constructions;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class IntTest extends TypeCheckingTestCase  {
  @Test
  public void numeral() {
    typeCheckModule("\\func f : -1 = neg 1 => idp");
  }

  @Test
  public void positivePattern() {
    typeCheckModule(
      "\\func f (x : Int) : Nat\n" +
      "  | 1 => 7\n" +
      "  | 0 => 10\n" +
      "  | neg _ => 10\n" +
      "  | pos n => n");
  }

  @Test
  public void conditionError() {
    typeCheckModule(
      "\\func f (x : Int) : Nat\n" +
      "  | 0 => 7\n" +
      "  | neg _ => 10\n" +
      "  | pos n => 10", 1);
  }

  @Test
  public void negativePattern() {
    typeCheckModule(
      "\\func f (x : Int) : Nat\n" +
      "  | -1 => 7\n" +
      "  | neg n => n\n" +
      "  | pos n => n\n" +
      "\\func test : f -1 = 7 => idp\n" +
      "\\func test2 : f -2 = 2 => idp");
  }
}
