package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.Universe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Universe extends TypeCheckingTestCase {
  @Test
  public void universe() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\oo-Type5", null);
    assertNotNull(result);
    assertEquals(Universe(5), result.expression);
    assertEquals(Universe(6), result.type);
    assertEquals(Universe(6), result.expression.getType());
  }

  @Test
  public void universeExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\oo-Type5", Universe(8));
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
    CheckTypeVisitor.Result result = typeCheckExpr("\\3-Type5", null);
    assertNotNull(result);
    assertEquals(Universe(5, 3), result.expression);
    assertEquals(Universe(6, 4), result.type);
    assertEquals(Universe(6, 4), result.expression.getType());
  }

  @Test
  public void truncatedExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\3-Type5", Universe(8, 9));
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
    CheckTypeVisitor.Result result = typeCheckExpr("\\Prop", null);
    assertNotNull(result);
    assertEquals(Universe(3, -1), result.expression);
    assertEquals(Universe(Sort.SET0), result.type);
    assertEquals(Universe(Sort.SET0), result.expression.getType());
  }

  @Test
  public void propExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Prop", Universe(8, 9));
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
    CheckTypeVisitor.Result result = typeCheckExpr("\\Set7", null);
    assertNotNull(result);
    assertEquals(Universe(Sort.SetOfLevel(7)), result.expression);
    assertEquals(Universe(8, 1), result.type);
    assertEquals(Universe(8, 1), result.expression.getType());
  }

  @Test
  public void setExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Set7", Universe(11, 4));
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
    typeCheckModule("\\data D : \\Prop | d1 | d2 I { _ => d1 }", 1);
  }

  @Test
  public void guessDataUniverseAsSet2() {
    typeCheckModule("\\data D : \\Set0 | d1 | d2 I { _ => d1 }");
  }

  @Test
  public void dataUniverseIsNotSet() {
    typeCheckModule(
      "\\data C | c1 | c2 | c3 I\n" +
      "  { | left => c1 | right => c2 }\n" +
      "\\data D : \\Set0 | d1 | d2 C { c1 => d1 }", 1);
  }
}
