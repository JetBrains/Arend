package org.arend.typechecking.constructions;

import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.result.TypecheckingResult;
import org.junit.Test;

import static org.arend.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Universe extends TypeCheckingTestCase {
  @Test
  public void universe() {
    TypecheckingResult result = typeCheckExpr("\\oo-Type5", null);
    assertNotNull(result);
    assertEquals(Universe(5), result.expression);
    assertEquals(Universe(6), result.type);
    assertEquals(Universe(6), result.expression.getType());
  }

  @Test
  public void universeExpected() {
    TypecheckingResult result = typeCheckExpr("\\oo-Type5", Universe(8));
    assertNotNull(result);
    assertEquals(Universe(5), result.expression);
    assertEquals(Universe(6), result.expression.getType());
  }

  @Test
  public void universeError() {
    typeCheckExpr("\\oo-Type5", Universe(5), 1);
  }

  @Test
  public void universeError2() {
    typeCheckExpr("\\oo-Type5", Universe(6, 100), 1);
  }

  @Test
  public void truncated() {
    TypecheckingResult result = typeCheckExpr("\\3-Type5", null);
    assertNotNull(result);
    assertEquals(Universe(5, 3), result.expression);
    assertEquals(Universe(6, 4), result.type);
    assertEquals(Universe(6, 4), result.expression.getType());
  }

  @Test
  public void truncatedExpected() {
    TypecheckingResult result = typeCheckExpr("\\3-Type5", Universe(8, 9));
    assertNotNull(result);
    assertEquals(Universe(5, 3), result.expression);
    assertEquals(Universe(6, 4), result.expression.getType());
  }

  @Test
  public void truncatedError() {
    typeCheckExpr("\\3-Type5", Universe(8, 3), 1);
  }

  @Test
  public void truncatedError2() {
    typeCheckExpr("\\3-Type5", Universe(5, 8), 1);
  }

  @Test
  public void prop() {
    TypecheckingResult result = typeCheckExpr("\\Prop", null);
    assertNotNull(result);
    assertEquals(Universe(3, -1), result.expression);
    assertEquals(Universe(Sort.SET0), result.type);
    assertEquals(Universe(Sort.SET0), result.expression.getType());
  }

  @Test
  public void propExpected() {
    TypecheckingResult result = typeCheckExpr("\\Prop", Universe(8, 9));
    assertNotNull(result);
    assertEquals(Universe(3, -1), result.expression);
    assertEquals(Universe(Sort.SET0), result.expression.getType());
  }

  @Test
  public void propError() {
    typeCheckExpr("\\Prop", Universe(5, -1), 1);
  }

  @Test
  public void set() {
    TypecheckingResult result = typeCheckExpr("\\Set7", null);
    assertNotNull(result);
    assertEquals(Universe(Sort.SetOfLevel(7)), result.expression);
    assertEquals(Universe(8, 1), result.type);
    assertEquals(Universe(8, 1), result.expression.getType());
  }

  @Test
  public void setExpected() {
    TypecheckingResult result = typeCheckExpr("\\Set7", Universe(11, 4));
    assertNotNull(result);
    assertEquals(Universe(Sort.SetOfLevel(7)), result.expression);
    assertEquals(Universe(8, 1), result.expression.getType());
  }

  @Test
  public void setError() {
    typeCheckExpr("\\Set7", Universe(6), 1);
  }

  @Test
  public void setError2() {
    typeCheckExpr("\\Set7", Universe(9, 0), 1);
  }

  @Test
  public void guessDataUniverseAsSet() {
    typeCheckModule("\\data D (A : \\Prop) : \\Prop | d1 | d2 A { _ => d1 }", 1);
  }

  @Test
  public void guessDataUniverseAsSet2() {
    typeCheckModule("\\data D (A : \\Prop) : \\Set0 | d1 | d2 A { _ => d1 }");
  }

  @Test
  public void dataUniverseIsNotSet() {
    typeCheckModule(
      "\\data C | c1 | c2 | c3 I\n" +
      "  { | left => c1 | right => c2 }\n" +
      "\\data D : \\Set0 | d1 | d2 C { c1 => d1 }", 1);
  }
}
