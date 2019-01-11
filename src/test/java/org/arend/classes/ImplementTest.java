package org.arend.classes;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.arend.ExpressionFactory.Universe;
import static org.arend.ExpressionFactory.fromPiParameters;
import static org.junit.Assert.assertEquals;

public class ImplementTest extends TypeCheckingTestCase {
  @Test
  public void implement() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\func f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementUnknownError() {
    resolveNamesModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | b => 0\n" +
      "}", 1);
  }

  @Test
  public void implementTypeMismatchError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat -> Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}", 1);
  }

  @Test
  public void implement2() {
    typeCheckModule(
      "\\class C {\n" +
      "  | A : \\Set0\n" +
      "  | a : A\n" +
      "}\n" +
      "\\class B \\extends C {\n" +
      "  | A => Nat\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\func f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implement3() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\func f (x : A) => x.a\n" +
      "\\func g (b : B) : f b = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementImplementedError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\class C \\extends B {\n" +
      "  | a => 0\n" +
      "}", 1);
  }

  @Test
  public void implementExistingFunction() {
    resolveNamesModule(
      "\\class A {\n" +
      "  \\func a => \\Type0\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => \\Type0\n" +
      "}", 1);
  }

  @Test
  public void implementNew() {
    typeCheckModule(
      "\\class C {\n" +
      "  | A : \\Set0\n" +
      "  | a : A\n" +
      "}\n" +
      "\\class B \\extends C {\n" +
      "  | A => Nat\n" +
      "}\n" +
      "\\func f (x : C) => x.a\n" +
      "\\func g : f (\\new B { | a => 0 }) = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementNewError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\func f => \\new B { | a => 1 }", 1);
  }

  @Test
  public void implementMultiple() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "  | b : Nat\n" +
      "  | c : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | b => 0\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | c => 0\n" +
      "}\n" +
      "\\class D \\extends B, C {\n" +
      "  | p : b = c\n" +
      "  | f : \\Pi (q : 0 = 0 -> \\Set0) -> q p -> Nat\n" +
      "}\n" +
      "\\func g => \\new D { | a => 1 | p => path (\\lam _ => 0) | f => \\lam _ _ => 0 }");
  }

  @Test
  public void implementMultipleSame() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "  | b : Nat\n" +
      "  | c : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | b => a\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | b => a\n" +
      "}\n" +
      "\\class D \\extends B, C {\n" +
      "  | a => 1\n" +
      "}\n" +
      "\\func f => \\new D { | c => 2 }");
  }

  @Test
  public void implementMultipleSameError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | a => 1\n" +
      "}\n" +
      "\\class D \\extends B, C", 1);
  }

  @Test
  public void universe() {
    typeCheckModule(
      "\\class C {\n" +
      "  | A : \\Set1\n" +
      "  | a : A\n" +
      "}\n" +
      "\\class B \\extends C {\n" +
      "  | A => Nat\n" +
      "}");
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) getDefinition("B")).getSort());
  }

  @Test
  public void universeClassExt() {
    typeCheckModule(
      "\\class C {\n" +
      "  | A : \\Type\n" +
      "  | a : A\n" +
      "}\n" +
      "\\func f => C { | A => Nat }");
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((FunctionDefinition) getDefinition("f")).getResultType().toSort());
  }

  @Test
  public void universeMultiple() {
    typeCheckModule(
      "\\class A {\n" +
      "  | X : \\Set1\n" +
      "  | Y : \\Set0\n" +
      "  | x : X\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | X => Nat\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      " | Y => Nat\n" +
      " | x' : X\n" +
      "}\n" +
      "\\class D \\extends B, C {\n" +
      " | x' => 0\n" +
      "}\n" +
      "\\func f => D { | x => 1 }");
    List<DependentLink> fParams = new ArrayList<>();
    Expression fType = getDefinition("f").getTypeWithParams(fParams, Sort.STD);
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("A")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition("B")).getSort());
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) getDefinition("D")).getSort());
    assertEquals(Universe(Sort.PROP), fromPiParameters(fType, fParams));
  }

  @Test
  public void classExtDep() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "}\n" +
      "\\func f => A { | x => 0 | y => path (\\lam _ => 0) }");
  }

  @Test
  public void classImplDep() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | x => 0\n" +
      "  | y => path (\\lam _ => 0)\n" +
      "}");
  }

  @Test
  public void classExtDepMissingError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "}\n" +
      "\\func f => A { | y => path (\\lam _ => 0) }", 1);
  }

  @Test
  public void classExtDepOrder() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "}\n" +
      "\\func f => A { | y => path (\\lam _ => 0) | x => 0 }");
  }

  @Test
  public void classImplDepMissingError() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : x = 0\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => path (\\lam _ => 0)\n" +
      "}", 1);
  }

  @Test
  public void recursivePrevious() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => x\n" +
      "}\n" +
      "\\func test (b : B { | x => 3 }) : b.y = b.x => path (\\lam _ => 3)");
  }

  @Test
  public void recursiveSelf() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => y\n" +
      "}", 1);
  }

  @Test
  public void recursiveNext() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => z\n" +
      "}");
  }

  @Test
  public void recursiveMutual() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => z\n" +
      "}\n" +
      "\\class C \\extends B {\n" +
      "  | z => y\n" +
      "}", 1);
  }

  @Test
  public void recursiveMutual2() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => z\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | z => y\n" +
      "}\n" +
      "\\class D \\extends B, C", 1);
  }

  @Test
  public void recursiveEmpty() {
    typeCheckModule(
      "\\data Empty\n" +
      "\\class A {\n" +
      "  | x : Empty\n" +
      "  | y : Empty\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | x => y\n" +
      "}\n" +
      "\\class C \\extends A {\n" +
      "  | y => x\n" +
      "}\n" +
      "\\class D \\extends B, C", 1);
  }

  @Test
  public void recursiveNextImplemented() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "  | z : Nat\n" +
      "  | z => 7" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | y => z\n" +
      "}\n" +
      "\\func test (b : B) : b.y = b.z => path (\\lam _ => 7)");
  }
}
