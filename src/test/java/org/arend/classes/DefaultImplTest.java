package org.arend.classes;

import org.arend.Matchers;
import org.arend.core.definition.ClassDefinition;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.FieldCycleError;
import org.arend.typechecking.error.local.NotEqualExpressionsError;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DefaultImplTest extends TypeCheckingTestCase {
  @Test
  public void defaultTest() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : x = 0
      \\record D \\extends C {
        \\default x => 0
      }
      \\func f => \\new D { | y => idp }
      \\func g : D \\cowith
        | y => idp
      """);
  }

  @Test
  public void redefineTest() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : x = 1
      \\record D \\extends C {
        \\default x => 0
      }
      \\func f => \\new D { | x => 1 | y => idp }
      \\func g : D \\cowith
        | x => 1
        | y => idp
      \\record E \\extends D
        | x => 1
        | y => idp
      """);
  }

  @Test
  public void redefineDefaultTest() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : x = 1
      \\record D \\extends C {
        \\default x => 0
      }
      \\record E \\extends D {
        \\default x => 1
      }
      \\func f => \\new E { | y => idp }
      \\func g : E \\cowith
        | y => idp
      """);
  }

  @Test
  public void defaultAssumptionError() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : x = 0
      \\record D \\extends C {
        \\default x => 0
        \\default y => idp
      }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void defaultFunction() {
    typeCheckModule("""
      \\record C (k : Nat)
        | f (n : Nat) : n = k -> Nat
      \\record D \\extends C {
        \\default f (n : Nat) (p : n = k) : Nat \\elim n {
          | 0 => 0
          | suc n => n
        }
      }
      \\func d : D 3 \\cowith
      \\func test : d.f 3 idp = 2 => idp
      """);
  }

  @Test
  public void renameDefault() {
    typeCheckModule("""
      \\record C {
        | f : Nat
        \\default f \\as f' => 0
      }
      \\func c : C \\cowith
      \\func test (c' : C) : c.f = C.f' {c'} => idp
      """);
  }

  @Test
  public void renameDefaultError() {
    resolveNamesModule("""
      \\record C {
        | f : Nat -> Nat
        \\default f n \\with {
          | 0 => 0
          | suc n => n
        }
      }
      """, 1);
  }

  @Test
  public void sameName() {
    typeCheckModule("""
      \\record C {
        | f : Nat
        \\default f => 0
      }
      \\func g : C \\cowith""");
  }

  @Test
  public void defaultDependency() {
    typeCheckModule("""
      \\record C {
        | f : Nat -> Nat
        | g (n : Nat) : f (suc n) = n
        \\default f \\as f' (n : Nat) : Nat \\elim n {
          | 0 => 0
          | suc n => n
        }
        \\default g \\as g' n : f' (suc n) = n => idp
      }
      \\func test : C \\cowith
      """);
  }

  @Test
  public void defaultDependencyError() {
    typeCheckModule("""
      \\record C {
        | f : Nat -> Nat
        | g (n : Nat) : f (suc n) = n
        \\default f \\as f' (n : Nat) : Nat \\elim n {
          | 0 => 0
          | suc n => n
        }
        \\default g \\as g' n => idp
      }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(NotEqualExpressionsError.class));
  }

  @Test
  public void defaultDependencyError2() {
    typeCheckModule("""
      \\record C {
        | f : Nat -> Nat
        | g (n : Nat) : f (suc n) = n
        \\default f \\as f' (n : Nat) : Nat \\elim n {
          | 0 => 0
          | suc n => n
        }
        \\default g \\as g' n : f' (suc n) = n => idp
      }
      \\func test : C \\cowith
        | f n => n
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("C.g"))));
  }

  @Test
  public void defaultDependencyError3() {
    typeCheckModule("""
      \\record C {
        | f : Nat -> Nat
        | g (n : Nat) : f (suc n) = n
        \\default f \\as f' (n : Nat) : Nat \\elim n {
          | 0 => 0
          | suc n => n
        }
        \\default g \\as g' n : f' (suc n) = n => idp
      }
      \\record D \\extends C
        | f n => n
      \\func test : D \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("C.g"))));
  }

  @Test
  public void defaultDependency4() {
    typeCheckModule("""
      \\record C
        | const : Nat
        | path : const = 0
      \\record D \\extends C {
        \\default const \\as const' => 0
        \\default path : const' = 0 => idp
      }
      \\record E \\extends C
        | const => 1
      \\record F \\extends D, E
      \\func test : F \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("C.path"))));
  }

  @Test
  public void defaultDependency5() {
    typeCheckModule("""
      \\record C
        | const : Nat
        | path : const = 0
      \\record D \\extends C {
        \\default const \\as const' => 0
        \\default path : const' = 0 => idp
      }
      \\record E \\extends C
        | const => 1
      \\record F \\extends E, D
      \\func test : F \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Collections.singletonList(get("C.path"))));
  }

  @Test
  public void fieldTypeMismatch() {
    typeCheckModule("""
      \\record C
        | f : Int -> Int
      \\record D \\extends C {
        \\default f (x : Nat) : Int \\with {
          | 0 => pos 0

          | suc n => pos n

        }
      }
      """, 1);
    assertThatErrorsAre(Matchers.typeMismatchError());
  }

  @Test
  public void infixTest() {
    typeCheckModule("""
      \\record C
        | \\infix 4 # : Nat -> Nat -> Nat
        | f (x y : Nat) : x # y = x
      \\record D \\extends C {
        \\default # \\as #Impl x y => x
        \\default f x y : x #Impl y = x => idp
      }
      """);
  }

  @Test
  public void resolveTest() {
    typeCheckModule("""
      \\record C
        | \\infix 4 # : Nat -> Nat -> Nat
        | f (x y : Nat) : x # y = x
      \\record D \\extends C {
        \\default # \\as # x y => x
        \\default f x y : x # y = x => idp
      }
      """);
  }

  @Test
  public void resolveTest2() {
    typeCheckModule("""
      \\record C
        | \\infix 4 # : Nat -> Nat -> Nat
        | f (x y : Nat) : x # y = x
      \\record D \\extends C {
        | g (x y : Nat) : x # y = x
        \\default # x y : Nat => x
        \\default f x y : x # y = x => idp
      }
      """);
  }

  @Test
  public void mutualRecursion() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : Nat
      \\record D \\extends C {
        \\default x => y
        \\default y => x
      }
      \\func d1 : D \\cowith
        | x => 0
      \\func d2 : D \\cowith
        | y => 0
      \\func test : \\Sigma (d1.y = 0) (d2.x = 0) => (idp, idp)
      """);
  }

  @Test
  public void mutualRecursionError() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : Nat
      \\record D \\extends C {
        \\default x => y
        \\default y => x
      }
      \\func test : D \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Arrays.asList(get("C.x"), get("C.y"))));
  }

  @Test
  public void mutualRecursionError2() {
    typeCheckModule("""
      \\record C
        | x : Nat
        | y : Nat
      \\record D \\extends C {
        \\default x \\as xImpl => y
        \\default y \\as yImpl => x
      }
      \\func test : D \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Arrays.asList(get("C.x"), get("C.y"))));
  }

  @Test
  public void mutualRecursionError3() {
    typeCheckModule("""
      \\record C
        | x : 0 = 1
        | y : 0 = 1
      \\record D \\extends C {
        \\default x \\as xImpl => y
        \\default y \\as yImpl => x
      }
      \\func test : D \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.fieldsImplementation(false, Arrays.asList(get("C.x"), get("C.y"))));
  }

  @Test
  public void implDefaultRecursion() {
    typeCheckModule("""
      \\record R (x y : Nat) {
        \\default y => x
        | x => y
      }
      \\func test : R \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(FieldCycleError.class));
  }

  @Test
  public void implDefaultRecursion2() {
    typeCheckModule("""
      \\record R (x y : Nat)
      \\record A \\extends R {
        \\default y => x
      }
      \\record B \\extends R
        | x => y
      \\record C \\extends A, B
      \\func test : C \\cowith
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(FieldCycleError.class));
  }

  @Test
  public void superDependencies() {
    typeCheckModule("""
      \\record B (b : Nat)
      \\record R (r : Nat) \\extends B {
        \\default b => r
      }
      \\record S (s : Nat) \\extends B {
        \\default b => s
      }
      \\record T \\extends R, S
      """);
    assertEquals(1, ((ClassDefinition) getDefinition("T")).getDefaultImplDependencies().values().iterator().next().size());
  }
}
