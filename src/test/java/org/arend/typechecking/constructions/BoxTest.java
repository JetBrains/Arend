package org.arend.typechecking.constructions;

import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

public class BoxTest extends TypeCheckingTestCase {
  @Test
  public void compareTest() {
    typeCheckDef("\\func test {A : \\Prop} (a a' : A) : (\\box a) = \\box a' => idp");
  }

  @Test
  public void notPropTest() {
    typeCheckDef("\\func test {A : \\Set} (a : A) => \\box a", 1);
  }

  @Test
  public void functionTest() {
    typeCheckModule(
      "\\sfunc foo {A : \\Prop} (\\property a : A) => 0\n" +
      "\\func test {A : \\Prop} (a a' : A) : foo a = foo a' => idp");
  }

  @Test
  public void dataTest() {
    typeCheckModule(
      "\\data D {A : \\Prop} (\\property a : A) | con\n" +
      "\\func test {A : \\Prop} (a a' : A) : D a = D a' => idp");
  }

  @Test
  public void conTest() {
    typeCheckModule(
      "\\data D {A : \\Prop} | con (\\property a : A)\n" +
      "\\func test {A : \\Prop} (a a' : A) : con a = con a' => idp");
  }

  @Test
  public void patternTest() {
    typeCheckModule(
      "\\data D (A : \\Prop) | con (\\property a : A)\n" +
      "\\func test {A : \\Prop} (a : A) (d : D A) : d = con a \\elim d\n" +
      "  | con a' => idp");
  }

  @Test
  public void notPropPropertyTest() {
    typeCheckDef("\\func foo {A : \\Set} (\\property a : A) => 0", 1);
  }
}
