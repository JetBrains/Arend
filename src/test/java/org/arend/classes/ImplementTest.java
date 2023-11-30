package org.arend.classes;

import org.arend.Matchers;
import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.IncorrectImplementationError;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.arend.ExpressionFactory.Universe;
import static org.arend.ExpressionFactory.fromPiParameters;
import static org.arend.Matchers.*;
import static org.junit.Assert.assertEquals;

public class ImplementTest extends TypeCheckingTestCase {
  @Test
  public void implement() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      \\func f (b : B) : b.a = 0 => idp
      """);
  }

  @Test
  public void implementUnknownError() {
    resolveNamesModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | b => 0
      }
      """, 1);
  }

  @Test
  public void implementTypeMismatchError() {
    typeCheckModule("""
      \\class A {
        | a : Nat -> Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      """, 1);
  }

  @Test
  public void implement2() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | A => Nat
        | a => 0
      }
      \\func f (b : B) : b.a = 0 => idp
      """);
  }

  @Test
  public void implement3() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      \\func f (x : A) => x.a
      \\func g (b : B) : f b = 0 => idp
      """);
  }

  @Test
  public void implementImplementedError() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      \\class C \\extends B {
        | a => 0
      }
      """, 1);
  }

  @Test
  public void implementExistingFunction() {
    typeCheckModule("""
      \\class A {
        \\func a => \\Type0
      }
      \\class B \\extends A {
        | a => \\Type0
      }
      """, 1);
  }

  @Test
  public void implementNew() {
    typeCheckModule("""
      \\class C {
        | A : \\Set0
        | a : A
      }
      \\class B \\extends C {
        | A => Nat
      }
      \\func f (x : C) => x.a
      \\func g : f (\\new B { | a => 0 }) = 0 => idp
      """);
  }

  @Test
  public void implementNewError() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      \\lemma f => \\new B { | a => 1 }
      """, 1);
  }

  @Test
  public void implementMultiple() {
    typeCheckModule("""
      \\class A {
        | a : Nat
        | b : Nat
        | c : Nat
      }
      \\class B \\extends A {
        | b => 0
      }
      \\class C \\extends A {
        | c => 0
      }
      \\class D \\extends B, C {
        | p : b = c
        | f : \\Pi (q : 0 = 0 -> \\Set0) -> q p -> Nat
      }
      \\func g => \\new D { | a => 1 | p => path (\\lam _ => 0) | f => \\lam _ _ => 0 }
      """);
  }

  @Test
  public void implementMultipleSame() {
    typeCheckModule("""
      \\class A {
        | a : Nat
        | b : Nat
        | c : Nat
      }
      \\class B \\extends A {
        | b => a
      }
      \\class C \\extends A {
        | b => a
      }
      \\class D \\extends B, C {
        | a => 1
      }
      \\func f => \\new D { | c => 2 }
      """);
  }

  @Test
  public void implementMultipleSameError() {
    typeCheckModule("""
      \\class A {
        | a : Nat
      }
      \\class B \\extends A {
        | a => 0
      }
      \\class C \\extends A {
        | a => 1
      }
      \\class D \\extends B, C
      """, 1);
  }

  @Test
  public void universe() {
    typeCheckModule("""
      \\class C {
        | A : \\Set1
        | a : A
      }
      \\class B \\extends C {
        | A => Nat
      }
      """);
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) getDefinition("B")).getSort());
  }

  @Test
  public void universeClassExt() {
    typeCheckModule("""
      \\class C {
        | A : \\Type
        | a : A
      }
      \\func f => C { | A => Nat }
      """);
    assertEquals(new Sort(new Level(LevelVariable.PVAR, 1), new Level(LevelVariable.HVAR, 1)), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((FunctionDefinition) getDefinition("f")).getResultType().toSort());
  }

  @Test
  public void universeMultiple() {
    typeCheckModule("""
      \\class A {
        | X : \\Set1
        | Y : \\Set0
        | x : X
      }
      \\class B \\extends A {
        | X => Nat
      }
      \\class C \\extends A {
       | Y => Nat
       | x' : X
      }
      \\class D \\extends B, C {
       | x' => 0
      }
      \\func f => D { | x => 1 }
      """);
    List<DependentLink> fParams = new ArrayList<>();
    Expression fType = getDefinition("f").getTypeWithParams(fParams, LevelPair.STD);
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("A")).getSort());
    assertEquals(new Sort(1, 1), ((ClassDefinition) getDefinition("B")).getSort());
    assertEquals(new Sort(2, 1), ((ClassDefinition) getDefinition("C")).getSort());
    assertEquals(new Sort(0, 0), ((ClassDefinition) getDefinition("D")).getSort());
    assertEquals(Universe(Sort.PROP), fromPiParameters(fType, fParams));
  }

  @Test
  public void classExtDep() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : x = 0
      }
      \\func f => A { | x => 0 | y => idp }
      """);
  }

  @Test
  public void classImplDep() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : x = 0
      }
      \\class B \\extends A {
        | x => 0
        | y => idp
      }
      """);
  }

  @Test
  public void classExtDepMissingError() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : x = 0
      }
      \\func f => A { | y => path (\\lam _ => 0) }
      """, 1);
  }

  @Test
  public void classExtDepOrder() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : x = 0
      }
      \\func f => A { | y => path (\\lam _ => 0) | x => 0 }
      """, 1);
  }

  @Test
  public void classImplDepMissingError() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : x = 0
      }
      \\class B \\extends A {
        | y => path (\\lam _ => 0)
      }
      """, 1);
  }

  @Test
  public void recursivePrevious() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
      }
      \\class B \\extends A {
        | y => x
      }
      \\func test (b : B { | x => 3 }) : b.y = b.x => path (\\lam _ => 3)
      """);
  }

  @Test
  public void recursiveSelf() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
      }
      \\class B \\extends A {
        | y => y
      }
      """, 1);
    assertThatErrorsAre(cycle(get("y")));
  }

  @Test
  public void recursiveNext() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
      }
      \\class B \\extends A {
        | y => z
      }
      """);
  }

  @Test
  public void recursiveMutual() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
      }
      \\class B \\extends A {
        | y => z
      }
      \\class C \\extends B {
        | z => y
      }
      """, 1);
    assertThatErrorsAre(cycle(get("z"), get("y")));
  }

  @Test
  public void recursiveMutual2() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
      }
      \\class B \\extends A {
        | y => z
      }
      \\class C \\extends A {
        | z => y
      }
      \\class D \\extends B, C
      """, 1);
    assertThatErrorsAre(cycle(get("z"), get("y")));
  }

  @Test
  public void recursiveMutual3() {
    typeCheckModule("""
      \\class A {
        | x1 : Nat
        | x2 : Nat
        | x3 : Nat
        | x4 : Nat
        | x5 : Nat
      }
      \\class B \\extends A {
        | x1 => x2
        | x2 => x3
        | x3 => x4
      }
      \\class C \\extends B {
        | x4 => x1
        | x5 => 0
      }
      """, 1);
    assertThatErrorsAre(cycle(get("x4"), get("x1"), get("x2"), get("x3")));
  }

  @Test
  public void recursiveEmpty() {
    typeCheckModule("""
      \\data Empty
      \\class A {
        | x : Empty
        | y : Empty
      }
      \\class B \\extends A {
        | x => y
      }
      \\class C \\extends A {
        | y => x
      }
      \\class D \\extends B, C
      """, 1);
    assertThatErrorsAre(cycle(get("y"), get("x")));
  }

  @Test
  public void recursiveNextImplemented() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
        | z : Nat
        | z => 7}
      \\class B \\extends A {
        | y => z
      }
      \\func test (b : B) : b.y = b.z => path (\\lam _ => 7)
      """);
  }

  @Test
  public void recursiveType() {
    typeCheckModule("""
      \\class A {
        | x : Nat
        | y : Nat
      }
      \\class B \\extends A {
        | p : x = x
        | x => \\let p' => p \\in 0
        | y => 0
      }
      """, 1);
    assertThatErrorsAre(cycle(get("x"), get("p")));
  }

  @Test
  public void recursiveFunction() {
    typeCheckModule("""
      \\class A (X : \\hType) {
        | x : X
      }
      \\func f (a : A Nat) => a.x
      \\class B \\extends A {
        | X => Nat
        | x => f \\this
      }
      """, 1);
    assertThatErrorsAre(cycle(get("x")));
  }

  @Test
  public void orderTest() {
    typeCheckModule("""
      \\class A (TA : \\Set) | ta : TA
      \\class B (TB : \\Set) | tb : TB
      \\func f (T : A) (t : T.TA) => Nat
      \\class C (TC : \\Set) \\extends A, B
        | TA => TC
        | TB => TC
        | c : f \\this tb
      """);
  }

  @Test
  public void functionNewTest() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S => R 0
      \\func rrr => \\new S
      """);
  }

  @Test
  public void functionNewError() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S => R
      \\func rrr => \\new S
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewAppTest() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S (n : Nat) => R n
      \\func T (n : Nat) => S n
      \\func rrr => \\new T 0
      """);
  }

  @Test
  public void functionNewAppError() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S (n : Nat) => R
      \\func T (n : Nat) => S n
      \\func rrr => \\new T 0
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewImplTest() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R n
      \\func T => S 0
      \\func rrr => \\new T { | q => 1 }
      """);
  }

  @Test
  public void functionNewImplError() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R
      \\func T => S 0
      \\func rrr => \\new T { | q => 1 }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewImplAppTest() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R n
      \\func T (n : Nat) => S n
      \\func rrr => \\new T 0 { | q => 1 }
      """);
  }

  @Test
  public void functionNewImplAppError() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R
      \\func T (n : Nat) => S n
      \\func rrr => \\new T 0 { | q => 1 }
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceTest() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S => R 0
      \\func rrr : S \\cowith
      """);
  }

  @Test
  public void functionInstanceError() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S => R
      \\func rrr : S \\cowith
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceArgTest() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S (n : Nat) => R n
      \\func rrr : S 0 \\cowith
      """);
  }

  @Test
  public void functionInstanceArgError() {
    typeCheckModule("""
      \\record R (n : Nat)
      \\func S (n : Nat) => R
      \\func rrr : S 0 \\cowith
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceImplTest() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R n
      \\func T => S 0
      \\func rrr : T \\cowith
        | q => 1
      """);
  }

  @Test
  public void functionInstanceImplError() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R
      \\func T => S 0
      \\func rrr : T \\cowith
        | q => 1
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceImplArgTest() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R n
      \\func T (n : Nat) => S n
      \\func rrr : T 0 \\cowith
        | q => 1
      """);
  }

  @Test
  public void functionInstanceImplArgError() {
    typeCheckModule("""
      \\record R (n q : Nat)
      \\func S (n : Nat) => R
      \\func T (n : Nat) => S n
      \\func rrr : T 0 \\cowith
        | q => 1
      """, 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void repeatedImpl() {
    typeCheckModule("""
      \\class C
        | nn : Nat
      \\func f : C \\cowith
        | nn => 0
        | nn => 0
      """, 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("C.nn"))));
  }

  @Test
  public void implicitArgumentTest() {
    typeCheckModule("""
      \\class C | nn : Nat
      \\instance I : C 0
      \\class D {c : C}
      \\func f : D \\cowith
      """);
  }

  @Test
  public void implicitArgumentTest2() {
    typeCheckModule("""
      \\class C
        | nn : Nat
      \\instance I : C 0
      \\class D {c : C}
      \\func f : D \\cowith
        | c => \\new C 1
      \\func test : f.c.nn = 1 => idp
      """);
  }

  @Test
  public void implicitArgumentTest3() {
    typeCheckModule("""
      \\class C
        | nn : Nat
      \\instance I : C 0
      \\class D {c : C}
      \\func f : D {\\new C 1} \\cowith
      \\func test : f.c.nn = 1 => idp
      """);
  }

  @Ignore
  @Test
  public void propertyImpl() {
    typeCheckModule(
      "\\record C (y x : Nat) (p : x = 0)\n" +
      "\\func test : (C { | x => 0 | p => idp }) = (C { | x => 0 }) => idp");
  }

  @Test
  public void fieldImplTest() {
    typeCheckModule("""
      \\record R
        | x : Nat
      \\record S
        | x => 0
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(IncorrectImplementationError.class));
  }

  @Test
  public void fieldImplTest2() {
    typeCheckModule("""
      \\record R
        | x : Nat
      \\record S (y : Nat)
      \\func test => S { | x => 0 }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(IncorrectImplementationError.class));
  }

  @Test
  public void fieldImplTest3() {
    typeCheckModule("""
      \\record R
        | x : Nat
      \\record S {
        \\override x : Nat
      }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(IncorrectImplementationError.class));
  }

  @Test
  public void fieldImplTest4() {
    typeCheckModule("""
      \\record R
        | x : Nat
      \\record S {
        \\default x => 0
      }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(IncorrectImplementationError.class));
  }
}
