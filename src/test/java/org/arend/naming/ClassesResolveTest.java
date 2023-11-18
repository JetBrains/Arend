package org.arend.naming;

import org.junit.Test;

import static org.arend.Matchers.notInScope;
import static org.arend.Matchers.warning;

public class ClassesResolveTest extends NameResolverTestCase {
  @Test
  public void unknownExtTestError() {
    resolveNamesModule(
      "\\class Point { | x : Nat | y : Nat }\n" +
      "\\func C => Point { | x => 0 | z => 0 | y => 0 }", 1);
  }

  @Test
  public void parentCallTest() {
    resolveNamesModule("""
      \\class M {
        \\class A {
          | c : Nat -> Nat -> Nat
          | f : Nat -> Nat
        }
      }
      \\func B => M.A {
        | f => \\lam n => c n n
      }
      """, 1);
  }

  @Test
  public void mutualRecursionTestError() {
    resolveNamesModule("""
      \\class M {
        \\class Point {
          | x : Nat
          | y : Nat
        }
      }
      \\func test => M.Point {
        | x => y
        | y => x
      }
      """, 2);
  }

  @Test
  public void splitClassTestError() {
    resolveNamesModule("""
      \\class A \\where {
        \\func x => 0
      }
      \\class A \\where {
        \\func y => 0
      }
      """, 1);
  }

  @Test
  public void resolveIncorrect() {
    resolveNamesModule("""
      \\class C { | A : \\Set }
      \\class D { | B : \\Set }
      \\func f => \\new D { | A => \\Prop }
      """);
  }

  @Test
  public void resolveParameterSuper() {
    resolveNamesModule(
      "\\class C (A : \\Set)\n" +
      "\\class D \\extends C { | a : A }");
  }

  @Test
  public void resolveFieldSuper() {
    resolveNamesModule(
      "\\class C { | A : \\Set }\n" +
      "\\class D \\extends C { | a : A }");
  }

  @Test
  public void resolveFieldMultipleSuper() {
    resolveNamesModule("""
      \\class C1 { | A : \\Set }
      \\class C2 { | B : \\Set }
      \\class D \\extends C1, C2 { | a : A | b : B }
      """);
  }

  @Test
  public void resolveSuperImplement() {
    resolveNamesModule("""
      \\class A { | x : Nat }
      \\class B \\extends A { | y : Nat }
      \\class C \\extends B { | A => \\new A { | x => 0 } }
      """);
  }

  @Test
  public void resolveNotSuperImplement() {
    resolveNamesModule("""
      \\class A { | x : Nat }
      \\class B { | y : Nat }
      \\class C \\extends B { | A => \\new A { | x => 0 } }
      """);
  }

  @Test
  public void clashingNamesSuper() {
    resolveNamesModule("""
      \\class X \\where { \\class C1 { | A : \\Set } }
      \\class Y \\where { \\class C2 { | A : \\Set } }
      \\class D \\extends X.C1, Y.C2 { | a : X.C1.A | b : Y.C2.A }
      """);
  }

  @Test
  public void clashingNamesSuper2() {
    resolveNamesModule("""
      \\class X \\where { \\class C1 { | A : \\Set } }
      \\class Y \\where { \\class C2 { | A : \\Set } }
      \\class D \\extends X.C1, Y.C2 { | a : A }
      """);
  }

  @Test
  public void clashingNamesSuperImplement() {
    resolveNamesModule("""
      \\class X \\where { \\class C1 { | A : \\Set } }
      \\class Y \\where { \\class C2 { | A : \\Set } }
      \\class D \\extends X.C1, Y.C2 { | C1.A => \\Prop | C2.A => \\Prop -> \\Prop }
      """);
  }

  @Test
  public void clashingNamesSuperImplement2() {
    resolveNamesModule("""
      \\class X \\where { \\class C1 { | A : \\Set } }
      \\class Y \\where { \\class C2 { | A : \\Set } }
      \\class D \\extends X.C1, Y.C2 { | A => \\Prop }
      """);
  }

  @Test
  public void clashingNamesSuperCurrent() {
    resolveNamesModule(
      "\\class X \\where { \\class C1 { | A : \\Set } \\class C2 \\extends C1 }\n" +
      "\\class D \\extends X.C2 { | A : \\Prop }", 1);
    assertThatErrorsAre(warning());
  }

  @Test
  public void clashingNamesComplex() {
    resolveNamesModule("""
      \\class A {
        | S : \\Set0
      }
      \\class B \\extends A {
        | s : S
      }
      \\class M {
        \\class C \\extends A {
          | s : S
        }
      }
      \\class D \\extends B, M.C
      """);
  }

  @Test
  public void badFieldTypeError() {
    resolveNamesModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | a' : A
        | p : undefined_variable = a'
      }
      \\func f => \\new B { | A => Nat | a => 0 | a' => 0 | p => path (\\lam _ => 0) }
      """, 1);
  }

  @Test
  public void dynamicInheritanceUnresolved() {
    resolveNamesModule("""
      \\class X {
        \\class A
      }
      \\func x : X => \\new X
      \\class B \\extends x.C
      """, 1);
  }

  @Test
  public void wrongInheritance() {
    resolveNamesModule("""
      \\class X
      \\func Y => X
      \\class Z \\extends Y
      """, 1);
  }

  @Test
  public void instanceLocalClassReference() {
    resolveNamesModule("""
      \\class C {
        | x : Nat
      }
      \\instance D-X {c : C} : c.x
      """, 1);
  }

  @Test
  public void instanceRecord() {
    resolveNamesModule("""
      \\record X (A : \\Type0) {
        | B : A -> \\Type0
      }
      \\data D
      \\instance D-X : X | A => D | B => \\lam n => D
      """, 1);
  }

  @Test
  public void openTest() {
    resolveNamesModule("""
      \\class A (f : Nat)
      \\open B(g,h)
      \\class B (g : Nat) \\extends A {
        \\func h : Nat => 0
      }
      \\func test => g Nat.+ h
      """);
  }

  @Test
  public void openTest2() {
    resolveNamesModule("""
      \\class A (f : Nat)
      \\open B
      \\class B (g : Nat) \\extends A {
        \\func h : Nat => 0
      }
      \\func test => g Nat.+ h
      """);
  }

  @Test
  public void openTestError() {
    resolveNamesModule("""
      \\class A (f : Nat)
      \\class B \\extends A
      \\open B(f)
      """, 1);
    assertThatErrorsAre(notInScope("f"));
  }

  @Test
  public void resolveSuper() {
    resolveNamesModule(
      "\\record A \\extends B\n" +
      "  \\where \\record B");
  }

  @Test
  public void implTest() {
    resolveNamesModule("""
      \\record R \\where
        \\record D (x : Nat)
      \\record S \\extends R.D
      \\func test (d : R.D) : S \\cowith
        | R.D => d
      """);
  }

  @Test
  public void implTest2() {
    resolveNamesModule("""
      \\record R \\where
        \\record D (x : Nat)
      \\record S \\extends R, R.D
      \\func test (d : R.D) : S \\cowith
        | R.D => d
      """);
  }

  @Test
  public void metaTest() {
    resolveNamesModule("""
      \\func test (s : S) => s.r.x
      \\record R (x : Nat)
      \\meta R' => R
      \\record S (r : R')
      """);
  }
}
