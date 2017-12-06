package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExtensionsTest extends TypeCheckingTestCase {
  @Test
  public void fields() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "  | p : a = a'\n" +
        "}\n" +
        "\\function f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
  }

  @Test
  public void newTest() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "  | p : a = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
  }

  @Test
  public void badFieldTypeError() {
    resolveNamesModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "  | p : undefined_variable = a'\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }", 1);
  }

  @Test
  public void newError() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\function f => \\new B { A => Nat | a' => 0 }", 1);
  }

  @Test
  public void fieldEval() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\function f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b => \\new B { A => Nat | a => 1 | a' => 0 }" +
        "  \\in  (path (\\lam _ => b.a), path (\\lam _ => b.a'))");
  }

  @Test
  public void coercion() {
    typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C {\n" +
        "  | a' : A\n" +
        "}\n" +
        "\\function f (a : C) => a.a\n" +
        "\\function g : 3 = 3 => path (\\lam _ => f (\\new B { A => Nat | a' => 2 | a => 3 }))\n" +
        "\\function h (b : B { A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
  }

  @Test
  public void nameClashError() {
    resolveNamesModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class M {\n" +
        "  \\class B \\extends A {\n" +
        "    | x : Nat\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void nameClashError2() {
    resolveNamesModule(
        "\\class A {\n" +
        "  | x : Nat\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  | y : Nat\n" +
        "}\n" +
        "\\class M {\n" +
        "  \\class C \\extends B {\n" +
        "    | x : Nat -> Nat\n" +
        "  }\n" +
        "}", 1);
  }

  @Test
  public void nameClashError3() {
    resolveNamesModule(
        "\\class A {\n" +
        "  | S : \\Set0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  | s : S\n" +
        "}\n" +
        "\\class M {\n" +
        "  \\class C \\extends A {\n" +
        "    | s : S\n" +
        "  }\n" +
        "}\n" +
        "\\class D \\extends B, M.C", 1);
  }

  @Test
  public void multiple() {
    typeCheckModule(
        "\\class A {\n" +
        "  | S : \\Set0\n" +
        "}\n" +
        "\\class B \\extends A {\n" +
        "  | b : S\n" +
        "}\n" +
        "\\class C \\extends A {\n" +
        "  | c : S\n" +
        "}\n" +
        "\\class D \\extends B, C {\n" +
        "  | p : b = c\n" +
        "}\n" +
        "\\function f (d : D { S => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\function g => \\new D { S => Nat | b => 3 | c => 3 | p => path (\\lam _ => 3)}");
  }

  @Test
  public void superClassExpression() {
    typeCheckModule(
        "\\class A\n" +
        "\\class B \\extends ((\\lam x => x) A)");
  }

  @Test
  public void dynamicInheritance() {
    typeCheckModule(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.A");
  }

  @Test
  public void dynamicInheritanceUnresolved() {
    resolveNamesModule(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x => \\new X\n" +
        "\\class B \\extends x.C", 1);
  }

  @Ignore
  @Test
  public void dynamicInheritanceFieldAccess() {
    typeCheckModule(
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
    typeCheckModule(
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
    typeCheckModule(
        "\\class A {\n" +
        "  | a : Nat\n" +
        "}\n" +
        "\\class Z \\extends A { | a => 0 }\n" +
        "\\class B \\extends Z\n" +
        "\\class C \\extends Z\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void multipleInheritanceEqualImplementations() {
    typeCheckModule(
        "\\class A {\n" +
        "  | a : Nat\n" +
        "}\n" +
        "\\class B \\extends A { | a => 0 }\n" +
        "\\class C \\extends A { | a => 0 }\n" +
        "\\class D \\extends B, C\n");
  }

  @Test
  public void multipleDynamicInheritanceSameParent() {
    typeCheckModule(
        "\\class X {\n" +
        "  \\class A\n" +
        "}\n" +
        "\\function x1 => \\new X\n" +
        "\\function x2 => \\new X\n" +
        "\\class B \\extends x1.A, x2.A");
  }

  @Test
  public void multipleDynamicInheritanceDifferentParentsError() {
    typeCheckModule(
        "\\class X {\n" +
        "  | n : Nat" +
        "  \\class A\n" +
        "}\n" +
        "\\function x1 => \\new X { n => 1 }\n" +
        "\\function x2 => \\new X { n => 2 }\n" +
        "\\class B \\extends x1.A, x2.A", 1);
  }

  @Test
  public void internalInheritance() {
    typeCheckModule("\\class A { \\class B \\extends A }");
  }

  @Test
  public void universe() {
    TypeCheckModuleResult result = typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C");
    assertEquals(new Sort(1, 1), ((ClassDefinition) result.getDefinition("C")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) result.getDefinition("B")).getSort());
  }
}
