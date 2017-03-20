package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ImplementTest extends TypeCheckingTestCase {
  @Test
  public void implement() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementUnknownError() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement b => 0\n" +
        "}", 1);
  }

  @Test
  public void implementTypeMismatchError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat -> Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}", 1);
  }

  @Test
  public void implement2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement A => Nat\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (b : B) : b.a = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implement3() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f (x : A) => x.a\n" +
        "\\function g (b : B) : f b = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementImplementedError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\class C \\extends B {\n" +
        "  \\implement a => 0\n" +
        "}", 1);
  }

  @Test
  public void implementExistingFunction() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\function a => \\Type0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => \\Type0\n" +
        "}", 1);
  }

  @Test
  public void implementNew() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement A => Nat\n" +
        "}\n" +
        "\\function f (x : A) => x.a\n" +
        "\\function g : f (\\new B { a => 0 }) = 0 => path (\\lam _ => 0)");
  }

  @Test
  public void implementNewError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\function f => \\new B { a => 1 }", 1);
  }

  @Test
  public void implementMultiple() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "  \\field b : Nat\n" +
        "  \\field c : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement b => 0\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\implement c => 0\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  \\field p : b = c\n" +
        "  \\field f : \\Pi (q : 0 = 0 -> \\Set0) -> q p -> Nat\n" +
        "}\n" +
        "\\function f => \\new D { a => 1 | p => path (\\lam _ => 0) | f => \\lam _ _ => 0 }");
  }

  @Test
  public void implementMultipleSame() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "  \\field b : Nat\n" +
        "  \\field c : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement b => a\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\implement b => a\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  \\implement a => 1\n" +
        "}\n" +
        "\\function f => \\new D { c => 2 }");
  }

  @Test
  public void implementMultipleSameError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement a => 0\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\implement a => 1\n" +
        "}\n" +
        "\\class D \\extends B, C", 1);
  }

  @Test
  public void universe() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set1\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement A => Nat\n" +
        "}");
    assertEquals(new Sort(2, 1), ((ClassDefinition) result.getDefinition("A")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) result.getDefinition("B")).getSort());
  }

  @Test
  public void universeClassExt() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set1\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\function f => A { A => Nat }");
    assertEquals(new Sort(2, 1), ((ClassDefinition) result.getDefinition("A")).getSort());
    assertEquals(new Sort(0, 0), ((FunctionDefinition) result.getDefinition("f")).getResultType().toSort());
  }

  @Test
  public void universeMultiple() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field X : \\Set1\n" +
        "  \\field Y : \\Set0\n" +
        "  \\field x : X\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement X => Nat\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        " \\implement Y => Nat\n" +
        " \\field x' : X\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        " \\implement x' => 0\n" +
        "}\n" +
        "\\function f => D { x => 1 }");
    List<DependentLink> fParams = new ArrayList<>();
    Expression fType = result.getDefinition("f").getTypeWithParams(fParams, Sort.STD);
    assertEquals(new Sort(2, 1), ((ClassDefinition) result.getDefinition("A")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) result.getDefinition("B")).getSort());
    assertEquals(new Sort(2, 1), ((ClassDefinition) result.getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) result.getDefinition("D")).getSort());
    assertEquals(ExpressionFactory.Universe(Sort.PROP), fType.fromPiParameters(fParams));
  }

  @Test
  public void classExtDep() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\function f => A { x => 0 | y => path (\\lam _ => 0) }");
  }

  @Test
  public void classImplDep() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement x => 0\n" +
        "  \\implement y => path (\\lam _ => 0)\n" +
        "}");
  }

  @Test
  public void classExtDepMissingError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\function f => A { y => path (\\lam _ => 0) }", 1);
  }

  @Test
  public void classExtDepOrder() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\function f => A { y => path (\\lam _ => 0) | x => 0 }");
  }

  @Test
  public void classImplDepMissingError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement y => path (\\lam _ => 0)\n" +
        "}", 1);
  }

  @Test
  public void classImplDepOrderError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "  \\field y : x = 0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\implement y => path (\\lam _ => 0)\n" +
        "}", 1);
  }
}
