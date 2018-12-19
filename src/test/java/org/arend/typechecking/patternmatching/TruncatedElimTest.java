package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.typechecking.Matchers.*;

public class TruncatedElimTest extends TypeCheckingTestCase {
  @Test
  public void propElim() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => path (\\lam _ => 0)");
  }

  @Test
  public void propElimWarn() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => path (\\lam _ => 0)\n" +
      "  | con2 _ => path (\\lam _ => 0)", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void propElimPartial() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => path (\\lam _ => 0)", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void propElimNotProp() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : Nat\n" +
      "  | con1 => 0", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void setElim() {
    typeCheckModule(
      "\\data D | con1 | con2 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 | _, right => con1 }\n" +
      "\\func f (x : D) : Nat\n" +
      "  | con1 => 0");
  }

  @Test
  public void setElimPartial() {
    typeCheckModule(
      "\\data D | con1 | con2 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 }\n" +
      "\\func f (x : D) : Nat\n" +
      "  | con1 => 0", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void setElimNotSet() {
    typeCheckModule(
      "\\data D | con1 | con2 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 | _, right => con1 }\n" +
      "\\func f (x : D) : x = x\n" +
      "  | con1 => path (\\lam _ => con1)", 1);
    assertThatErrorsAre(missingClauses(1));
  }

  @Test
  public void multiplePatternsSet() {
    typeCheckModule(
      "\\data D\n" +
      "  | con1\n" +
      "  | con2 I { | left => con1 | right => con1 }\n" +
      "  | con3 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 | _, right => con1 }\n" +
      "\\func f (x y : D) : Nat\n" +
      "  | con1, con1 => 0\n" +
      "  | con2 _, con1 => 0\n" +
      "  | con1, con2 _ => 0");
  }

  @Test
  public void multiplePatterns1Type() {
    typeCheckModule(
      "\\data D\n" +
      "  | con1\n" +
      "  | con2 I { | left => con1 | right => con1 }\n" +
      "  | con3 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 | _, right => con1 }\n" +
      "\\func f (x y : D) : \\Set\n" +
      "  | con1, con1 => Nat\n" +
      "  | con2 _, con1 => Nat\n" +
      "  | con1, con2 _ => Nat\n" +
      "  | con1, con3 _ _ => Nat\n" +
      "  | con3 _ _, con1 => Nat\n" +
      "  | con2 _, con2 _ => Nat");
  }

  @Test
  public void multiplePatterns1TypeError() {
    typeCheckModule(
      "\\data D\n" +
      "  | con1\n" +
      "  | con2 I { | left => con1 | right => con1 }\n" +
      "  | con3 I I { | left, _ => con1 | right, _ => con1 | _, left => con1 | _, right => con1 }\n" +
      "\\func f (x y : D) : \\Set\n" +
      "  | con1, con1 => Nat\n" +
      "  | con2 _, con1 => Nat\n" +
      "  | con1, con2 _ => Nat\n" +
      "  | con1, con3 _ _ => Nat\n" +
      "  | con3 _ _, con1 => Nat", 1);
    assertThatErrorsAre(missingClauses(1));
  }
}
