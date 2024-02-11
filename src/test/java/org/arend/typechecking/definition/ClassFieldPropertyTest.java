package org.arend.typechecking.definition;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.LevelMismatchError;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

import static org.arend.Matchers.typeMismatchError;
import static org.arend.Matchers.typecheckingError;
import static org.junit.Assert.*;

public class ClassFieldPropertyTest extends TypeCheckingTestCase {
  @Test
  public void nonPropPropertyError() {
    typeCheckModule("""
      \\class C {
        \\property p : Nat
      }
      """, 1);
    assertThatErrorsAre(typecheckingError(LevelMismatchError.class));
  }

  @Test
  public void propertyTest() {
    typeCheckModule("""
      \\class C {
        \\property p : 0 = 0
      }
      """);
  }

  @Test
  public void propertyNewEvalTest() {
    typeCheckModule("""
      \\class C {
        | p : 0 = 0
      }
      \\func foo (x : 0 = 0) : p {\\new C x} = x => idp
      """, 1);
    assertThatErrorsAre(typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void propertyFunctionEvalTest() {
    typeCheckModule("""
      \\class C {
        | p : 0 = 0
      }
      \\lemma inst : C \\cowith | p => idp
      \\func foo : p {inst} = idp => idp
      """, 1);
    assertThatErrorsAre(typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void classesTest() {
    typeCheckModule("""
      \\class A {
        | p : 0 = 0 -> 0 = 0 -> 0 = 0
      }
      \\class B \\extends A {
        | p x y => x
      }
      \\class C \\extends A {
        | p x y => y
      }
      \\class D \\extends B,C
      """);
  }

  @Test
  public void propertySetLevel() {
    typeCheckModule("""
      \\class A (X : \\Type) (Xs : \\Pi (x x' : X) (p q : x = x') -> p = q) {
        \\property f : \\level X Xs
      }
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void propertyLevel() {
    typeCheckModule("""
      \\class A {
        \\property f (A : \\Type) (p : \\Pi (x y : A) -> x = y) : \\level A p
      }
      """);
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR)), ((ClassDefinition) getDefinition("A")).getSort());
  }

  @Test
  public void propertyLevel2() {
    typeCheckModule("""
      \\class A {
        | f (A : \\Type) : \\level ((\\Pi (x y : A) -> x = y) -> A) (\\lam (f g : (\\Pi (x y : A) -> x = y) -> A) => path (\\lam i (p : \\Pi (x y : A) -> x = y) => p (f p) (g p) @ i))
      }
      """);
    assertTrue(((ClassField) getDefinition("A.f")).isProperty());
  }

  @Test
  public void fieldLevelTest() {
    typeCheckModule("""
      \\data S1 | base | loop : base = base
      \\record R {
        \\field foo (A : \\Type) (a : A) (p : \\Pi (x y : A) -> x = y) (x : S1) : A
          \\level \\lam a a' => path (\\lam i => p a a' @ i)
      }
      \\func test : R \\cowith
        | foo A a _ (base) => a
      """);
    assertFalse(((ClassField) getDefinition("R.foo")).isProperty());
  }
}
