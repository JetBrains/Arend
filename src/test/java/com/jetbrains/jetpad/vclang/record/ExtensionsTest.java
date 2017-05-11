package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExtensionsTest extends TypeCheckingTestCase {
  @Test
  public void fields() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "  \\field p : a = a'\n" +
        "}\n" +
        "\\function f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "  \\field p : a = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
  }

  @Test
  public void badFieldTypeError() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "  \\field p : undefined_variable = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }", 1);
  }

  @Test
  public void newError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a' => 0 }", 1);
  }

  @Test
  public void fieldEval() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "}\n" +
        "\\function f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b => \\new B { A => Nat | a => 1 | a' => 0 }" +
        "  \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))");
  }

  @Test
  public void coercion() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a' : A\n" +
        "}\n" +
        "\\function f (a : A) => a.a\n" +
        "\\function g : 3 = 3 => path (\\lam _ => f (\\new B { A => Nat | a' => 2 | a => 3 }))\n" +
        "\\function h (b : B { A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
  }

  @Test
  public void nameClashError() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field x : Nat\n" +
        "}", 1);
  }

  @Test
  public void nameClashError2() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\field x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field y : Nat\n" +
        "}\n" +
        "\\class C \\extends B {\n" +
        "  \\field x : Nat -> Nat\n" +
        "}", 1);
  }

  @Test
  public void nameClashError3() {
    resolveNamesClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class D \\extends B, C", 1);
  }

  @Test
  public void multiple() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\field b : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\field c : A\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  \\field p : b = c\n" +
        "}\n" +
        "\\function f (d : D { A => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\function g => \\new D { A => Nat | b => 3 | c => 3 | p => path (\\lam _ => 3)}");
  }

  @Test
  public void superClassExpression() {
    typeCheckClass(
        "\\class A\n" +
        "\\class B \\extends (\\lam x => x) A");
  }

  @Test
  public void dynamicInheritance() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.A");
  }

  @Ignore
  @Test
  public void dynamicInheritanceFieldAccess() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\class A \\where {\n" +
        "    \\function n : Nat => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.A {\n" +
        "  \\function my : Nat => n\n" +
        "}");
  }

  @Test
  public void dynamicInheritanceFieldAccessQualified() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\class A \\where {\n" +
        "    \\function n : Nat => 0\n" +
        "  }\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.A {\n" +
        "  \\function my : Nat => x.A.n\n" +
        "}");
  }

  @Test
  public void multipleInheritanceSingleImplementation() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class Z \\extends A {a => 0}\n" +
        "\\class B \\extends Z\n" +
        "\\class C \\extends Z\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void multipleInheritanceEqualImplementations() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\field a : Nat\n" +
        "}\n" +
        "\\class B \\extends A {a => 0}\n" +
        "\\class C \\extends A {a => 0}\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void multipleDynamicInheritanceSameParent() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.A, x.A");
  }

  @Test
  public void multipleDynamicInheritanceDifferentParentsError() {
    typeCheckClass(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x1 => \\new X\n" +
        "\\function x2 => \\new X\n" +
        "\\class B \\extends x1.A, x2.A", 1);
  }

  @Test
  public void internalInheritance() {
    typeCheckClass("\\class A { \\class B \\extends A }");
  }

  @Ignore
  @Test
  public void recursiveExtendsError() {
    typeCheckClass("\\class A \\extends A", 1);
  }

  @Ignore
  @Test
  public void mutualRecursiveExtendsError() {
    resolveNamesClass(
        "\\class A \\extends B\n" +
        "\\class B \\extends A", 1);
  }

  @Test
  public void universe() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\class A {\n" +
        "  \\field A : \\Set0\n" +
        "  \\field a : A\n" +
        "}\n" +
        "\\class B \\extends A");
    assertEquals(new Sort(1, 1), ((ClassDefinition) result.getDefinition("A")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) result.getDefinition("B")).getSort());
  }
}
