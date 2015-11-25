package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.assertEquals;

public class TypeCheckingTest {
  @Test
  public void typeCheckDefinition() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function x : Nat => zero\n" +
        "\\static \\function y : Nat => x");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckDefType() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function x : \\Type0 => Nat\n" +
        "\\static \\function y : x => zero");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckInfixDef() {
    ClassDefinition result = typeCheckClass(
        "\\static \\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\static \\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, result.getParentNamespace().getChild(result.getName()).getMembers().size());
  }

  @Test
  public void typeCheckConstructor() {
    typeCheckClass(
        "\\static \\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\static \\function idp {A : \\Type0} {a : A} => path (\\lam _ => a)\n" +
        "\\static \\function f : con {0} {0} {0} = (D 0 {0} 0).con => idp");
  }
}
