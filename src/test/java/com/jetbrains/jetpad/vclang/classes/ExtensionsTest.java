package com.jetbrains.jetpad.vclang.classes;

import com.jetbrains.jetpad.vclang.core.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.group.ChildGroup;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.typechecking.Matchers.error;
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
        "\\func f (b : B) : \\Sigma (x : b.A) (x = b.a') => (b.a, b.p)");
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
        "\\func f => \\new B { A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }");
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
        "\\func f => \\new B { A => Nat | a' => 0 }", 1);
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
        "\\func f : \\Sigma (1 = 1) (0 = 0) =>\n" +
        "  \\let b : B => \\new B { A => Nat | a => 1 | a' => 0 }\n" +
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
        "\\func f (a : C) => a.a\n" +
        "\\func g : 3 = 3 => path (\\lam _ => f (\\new B { A => Nat | a' => 2 | a => 3 }))\n" +
        "\\func h (b : B { A => Nat | a => 5 }) : 5 = 5 => path (\\lam _ => b.a)");
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
        "\\func f (d : D { S => Nat | c => 4 | b => 6 }) : 6 = 4 => d.p\n" +
        "\\func g => \\new D { S => Nat | b => 3 | c => 3 | p => path (\\lam _ => 3)}");
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
  public void internalInheritance() {
    typeCheckModule("\\class A { \\class B \\extends A }");
  }

  @Test
  public void universe() {
    ChildGroup result = typeCheckModule(
        "\\class C {\n" +
        "  | A : \\Set0\n" +
        "  | a : A\n" +
        "}\n" +
        "\\class B \\extends C");
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition(result, "C")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition(result, "B")).getSort());
  }

  @Test
  public void recursiveExtendsError() {
    typeCheckModule("\\class A \\extends A", 1);
    assertThatErrorsAre(error());
  }

  @Test
  public void mutualRecursiveExtendsError() {
    typeCheckModule(
      "\\class A \\extends B\n" +
      "\\class B \\extends A", 1);
    assertThatErrorsAre(error());
  }
}
