package org.arend.typechecking.patternmatching;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.arend.Matchers.*;

public class TruncatedElimTest extends TypeCheckingTestCase {
  @Test
  public void propElim() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => idp");
  }

  @Test
  public void propElimWarn() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => idp\n" +
      "  | con2 _ => idp", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void propElimPartial() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 }\n" +
      "\\func f (x : D) : 0 = 0\n" +
      "  | con1 => idp", 1);
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
      "  | con1 => idp", 1);
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

  @Test
  public void caseTest() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) => \\case x \\return 0 = 0 \\with {\n" +
      "  | con1 => idp\n" +
      "}");
  }

  @Test
  public void caseTest2() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\data Maybe (A : \\Type) | just A | nothing\n" +
      "\\func f (x : D) => \\case x \\return (just 0 = just 0 : \\Prop) \\with {\n" +
      "  | con1 => idp\n" +
      "}");
  }

  @Test
  public void caseTestError() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\data Maybe (A : \\Type) | just A | nothing\n" +
      "\\func f (x : D) => \\case x \\return (Nat : \\Prop) \\with {\n" +
      "  | con1 => 0\n" +
      "}", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void caseTest3() {
    typeCheckModule(
      "\\data Unit | unit\n" +
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\func f (x : D) : Unit => \\case x \\with {\n" +
      "  | con1 => unit\n" +
      "}");
  }

  @Test
  public void levelTest() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\data Empty\n" +
      "\\data Bool | true | false\n" +
      "\\func E (b : Bool) : \\Set0 | true => Empty | false => Empty\n" +
      "\\func E-isProp (b : Bool) (x y : E b) : x = y \\elim b, x | true, () | false, ()\n" +
      "\\func f (b : Bool) (x : E b) (d : D) : \\level (E b) (E-isProp b) \\elim d | con1 => x");
  }

  @Test
  public void caseLevelTest() {
    typeCheckModule(
      "\\data D | con1 | con2 I { | left => con1 | right => con1 }\n" +
      "\\data Empty\n" +
      "\\data Bool | true | false\n" +
      "\\func E (b : Bool) : \\Set0 | true => Empty | false => Empty\n" +
      "\\func E-isProp (b : Bool) (x y : E b) : x = y \\elim b, x | true, () | false, ()\n" +
      "\\func f (b : Bool) (x : E b) (d : D) => \\case d \\return \\level (E b) (E-isProp b) \\with { | con1 => x }");
  }

  @Test
  public void truncatedLevelTest() {
    typeCheckModule(
      "\\truncated \\data \\infixr 2 || (A B : \\Type) : \\Prop\n" +
      "    | byLeft A\n" +
      "    | byRight B\n" +
      "\\sfunc rec {A B C : \\Type} (p : \\Pi (x y : C) -> x = y) (f : A -> C) (g : B -> C) (t : A || B) : \\level C p \\elim t\n" +
      "  | byLeft a => f a\n" +
      "  | byRight b => g b");
  }
}
