package com.jetbrains.jetpad.vclang.record;

import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.sort.Sort;
import com.jetbrains.jetpad.vclang.term.expr.sort.SortMax;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.naming.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase.typeCheckClass;
import static org.junit.Assert.assertEquals;

public class ExtensionsTest {
  @Test
  public void fields() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\function f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void newTest() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : a = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
  }

  @Test
  public void badFieldTypeError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "  \\abstract p : undefined_variable = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }", 1, 1);
  }

  @Test
  public void newError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a' => 0 }", 1);
  }

  @Test
  public void fieldEval() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b => \\new B { A => Nat | a => 1 | a' => 0 }" +
        "  \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))");
  }

  @Test
  public void coercion() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract a' : A\n" +
        "}\n" +
        "\\function f (a : A) => a.a\n" +
        "\\function g : 3 = 3 => path (\\lam _ => f (\\new B { A => Nat | a' => 2 | a => 3 }))\n" +
        "\\function h (b : B { A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
  }

  @Test
  public void nameClashError() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract x : Nat\n" +
        "}", 1, 1);
  }

  @Test
  public void nameClashError2() {
    typeCheckClass(
        "\\class A {\n" +
        "  \\abstract x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  \\abstract y : Nat\n" +
        "}\n" +
        "\\class C \\extends B {\n" +
        "  \\abstract x : Nat -> Nat\n" +
        "}", 1, 1);
  }

  @Test
  public void nameClashError3() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\class D \\extends B, C {}", 1, 1);
  }

  @Test
  public void multiple() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "}\n" +
        "\\static \\class B \\extends A {\n" +
        "  \\abstract b : A\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  \\abstract c : A\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  \\abstract p : b = c\n" +
        "}\n" +
        "\\function f (d : D { A => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\function g => \\new D { A => Nat | b => 3 | c => 3 | p => path (\\lam _ => 3)}");
  }

  @Ignore
  @Test
  public void superClassExpression() {
    typeCheckClass(
        "\\static \\class A {}\n" +
        "\\static \\class B \\extends (\\lam x => x) A {}");
  }

  @Test
  public void dynamicInheritance() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\class A {}\n" +
        "}\n" +
        "\\static \\function x => \\new X\n" +
        "\\static \\class B \\extends x.A {}");
  }

  @Ignore
  @Test
  public void dynamicInheritanceFieldAccess() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\class A {\n" +
        "    \\static \\function n : Nat => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function x => \\new X\n" +
        "\\static \\class B \\extends x.A {\n" +
        "  \\function my : Nat => n\n" +
        "}");
  }

  @Test
  public void dynamicInheritanceFieldAccessQualified() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\class A {\n" +
        "    \\static \\function n : Nat => 0\n" +
        "  }\n" +
        "}\n" +
        "\\static \\function x => \\new X\n" +
        "\\static \\class B \\extends x.A {\n" +
        "  \\function my : Nat => x.A.n\n" +
        "}");
  }

  @Test
  public void multipleInheritanceSingleImplementation() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class Z \\extends A {a => 0} {}\n" +
        "\\static \\class B \\extends Z {}\n" +
        "\\static \\class C \\extends Z {}\n" +
        "\\static \\class D \\extends B, C {}\n");
  }

  @Test
  public void multipleInheritanceEqualImplementations() {
    typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract a : Nat\n" +
        "}\n" +
        "\\static \\class B \\extends A {a => 0} {}\n" +
        "\\static \\class C \\extends A {a => 0} {}\n" +
        "\\static \\class D \\extends B, C {}\n");
  }

  @Ignore
  @Test
  public void multipleDynamicInheritanceSameParent() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\class A {}\n" +
        "}\n" +
        "\\static \\function x => \\new X\n" +
        "\\static \\class B \\extends x.A, x.A {}");
  }

  @Test
  public void multipleDynamicInheritanceDifferentParentsError() {
    typeCheckClass(
        "\\static \\class X {\n" +
        "  \\class A {}\n" +
        "}\n" +
        "\\static \\function x1 => \\new X\n" +
        "\\static \\function x2 => \\new X\n" +
        "\\static \\class B \\extends x1.A, x2.A {}", 1);
  }

  @Test
  public void internalInheritance() {
    typeCheckClass("\\class A { \\static \\class B \\extends A { } }");
  }

  @Ignore
  @Test
  public void recursiveExtendsError() {
    typeCheckClass("\\class A \\extends A {}", 1);
  }

  @Ignore
  @Test
  public void mutualRecursiveExtendsError() {
    resolveNamesClass("test",
        "\\class A \\extends B {}\n" +
        "\\class B \\extends A {}", 1);
  }

  @Test
  public void universe() {
    TypeCheckingTestCase.TypeCheckClassResult result = typeCheckClass(
        "\\static \\class A {\n" +
        "  \\abstract A : \\Set0\n" +
        "  \\abstract a : A\n" +
        "}\n" +
        "\\static \\class B \\extends A {}");
    assertEquals(new SortMax(new Sort(1,1)), ((ClassDefinition) result.getDefinition("A")).getSorts());
    assertEquals(new SortMax(new Sort(1,1)), ((ClassDefinition) result.getDefinition("B")).getSorts());
  }
}
