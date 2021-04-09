package org.arend.typechecking.constructions;

import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.ClassCallExpression;
import org.arend.core.sort.Sort;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ArrayTest extends TypeCheckingTestCase {
  @Test
  public void classExt() {
    typeCheckModule(
      "\\func test1 => \\new Array Nat 1 (\\lam _ => 3)\n" +
      "\\func test2 : Array Nat \\cowith\n" +
      "  | len => 3\n" +
      "  | at _ => 1");
    Sort sort = Sort.STD.max(Sort.SET0);
    assertFalse(((ClassCallExpression) ((FunctionDefinition) getDefinition("test1")).getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) ((FunctionDefinition) getDefinition("test1")).getResultType()).getSort());
    assertFalse(((ClassCallExpression) ((FunctionDefinition) getDefinition("test2")).getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) ((FunctionDefinition) getDefinition("test2")).getResultType()).getSort());
    assertFalse(((ClassCallExpression) Prelude.EMPTY_ARRAY.getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) Prelude.EMPTY_ARRAY.getResultType()).getSort());
    assertFalse(((ClassCallExpression) Prelude.ARRAY_CONS.getResultType()).isImplemented(Prelude.ARRAY_AT));
    assertEquals(sort, ((ClassCallExpression) Prelude.ARRAY_CONS.getResultType()).getSort());
  }

  @Test
  public void indexTest() {
    typeCheckModule(
      "\\open Array\n" +
      "\\func array : Array Nat 2 => 14 cons 22 cons empty\n" +
      "\\lemma test1 : array.at 0 = 14 => idp\n" +
      "\\lemma test2 : array.at 1 = 22 => idp\n" +
      "\\lemma test3 : array !! 0 = 14 => idp\n" +
      "\\lemma test4 : array !! 1 = 22 => idp\n" +
      "\\func test5 (a : Array) (i : Fin a.len) : a.at i = a !! i => idp");
  }

  @Test
  public void newConsTest() {
    typeCheckModule(
      "\\open Array\n" +
      "\\lemma test1 : (\\new Array Nat 2 (\\case __ \\with { | 0 => 5 | 1 => 7 })) = 5 cons 7 cons empty => idp\n" +
      "\\lemma test2 : (\\new Array Nat 3 (\\case __ \\with { | 0 => 5 | suc i => \\case i \\with { | 0 => 7 | 1 => 12 } })) = (\\new Array Nat 3 (\\case __ \\with { | 0 => 5 | 1 => 7 | 2 => 12 })) => idp");
  }
}
