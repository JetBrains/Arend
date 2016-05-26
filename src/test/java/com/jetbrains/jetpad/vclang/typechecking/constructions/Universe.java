package com.jetbrains.jetpad.vclang.typechecking.constructions;

import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckExpr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Universe {
  @Test
  public void universe() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Type5", null);
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(5), result.expression);
    assertEquals(ExpressionFactory.Universe(6), result.type);
    assertEquals(ExpressionFactory.Universe(6), result.expression.getType());
  }

  @Test
  public void universeExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Type5", ExpressionFactory.Universe(8));
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(5), result.expression);
    assertEquals(ExpressionFactory.Universe(6), result.type);
    assertEquals(ExpressionFactory.Universe(6), result.expression.getType());
  }

  @Test
  public void universeError() {
    typeCheckExpr("\\Type5", ExpressionFactory.Universe(5), 1);
  }

  @Test
  public void universeError2() {
    typeCheckExpr("\\Type5", ExpressionFactory.Universe(6, 100), 1);
  }

  @Test
  public void truncated() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\3-Type5", null);
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(5, 3), result.expression);
    assertEquals(ExpressionFactory.Universe(6, 4), result.type);
    assertEquals(ExpressionFactory.Universe(6, 4), result.expression.getType());
  }

  @Test
  public void truncatedExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\3-Type5", ExpressionFactory.Universe(8, 9));
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(5, 3), result.expression);
    assertEquals(ExpressionFactory.Universe(6, 4), result.type);
    assertEquals(ExpressionFactory.Universe(6, 4), result.expression.getType());
  }

  @Test
  public void truncatedError() {
    typeCheckExpr("\\3-Type5", ExpressionFactory.Universe(8, 3), 1);
  }

  @Test
  public void truncatedError2() {
    typeCheckExpr("\\3-Type5", ExpressionFactory.Universe(5, 8), 1);
  }

  @Test
  public void prop() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Prop", null);
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(3, -1), result.expression);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(0)), result.type);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(0)), result.expression.getType());
  }

  @Test
  public void propExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Prop", ExpressionFactory.Universe(8, 9));
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(3, -1), result.expression);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(0)), result.type);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(0)), result.expression.getType());
  }

  @Test
  public void propError() {
    typeCheckExpr("\\Prop", ExpressionFactory.Universe(5, -1), 1);
  }

  @Test
  public void set() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Set7", null);
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(7)), result.expression);
    assertEquals(ExpressionFactory.Universe(8, 1), result.type);
    assertEquals(ExpressionFactory.Universe(8, 1), result.expression.getType());
  }

  @Test
  public void setExpected() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\Set7", ExpressionFactory.Universe(11, 4));
    assertNotNull(result);
    assertEquals(ExpressionFactory.Universe(TypeUniverse.SetOfLevel(7)), result.expression);
    assertEquals(ExpressionFactory.Universe(8, 1), result.type);
    assertEquals(ExpressionFactory.Universe(8, 1), result.expression.getType());
  }

  @Test
  public void setError() {
    typeCheckExpr("\\Set7", ExpressionFactory.Universe(6), 1);
  }

  @Test
  public void setError2() {
    typeCheckExpr("\\Set7", ExpressionFactory.Universe(9, 0), 1);
  }
}
