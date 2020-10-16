package org.arend.classes;

import org.arend.core.context.binding.LevelVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.sort.Level;
import org.arend.core.sort.Sort;
import org.arend.ext.error.RedundantCoclauseError;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.FieldsImplementationError;
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
    typeCheckModule(
      "\\class A {\n" +
      "  | a : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | a => 0\n" +
      "}\n" +
      "\\func f (b : B) : b.a = 0 => idp");
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
      "\\func f (b : B) : b.a = 0 => idp");
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
      "\\func g (b : B) : f b = 0 => idp");
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
      "\\func g : f (\\new B { | a => 0 }) = 0 => idp");
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
      "\\func f => A { | x => 0 | y => idp }");
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
      "  | y => idp\n" +
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
      "\\func f => A { | y => path (\\lam _ => 0) | x => 0 }", 1);
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
    assertThatErrorsAre(cycle(get("y")));
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
    assertThatErrorsAre(cycle(get("z"), get("y")));
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
    assertThatErrorsAre(cycle(get("z"), get("y")));
  }

  @Test
  public void recursiveMutual3() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x1 : Nat\n" +
      "  | x2 : Nat\n" +
      "  | x3 : Nat\n" +
      "  | x4 : Nat\n" +
      "  | x5 : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | x1 => x2\n" +
      "  | x2 => x3\n" +
      "  | x3 => x4\n" +
      "}\n" +
      "\\class C \\extends B {\n" +
      "  | x4 => x1\n" +
      "  | x5 => 0\n" +
      "}", 1);
    assertThatErrorsAre(cycle(get("x4"), get("x1"), get("x2"), get("x3")));
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
    assertThatErrorsAre(cycle(get("y"), get("x")));
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

  @Test
  public void recursiveType() {
    typeCheckModule(
      "\\class A {\n" +
      "  | x : Nat\n" +
      "  | y : Nat\n" +
      "}\n" +
      "\\class B \\extends A {\n" +
      "  | p : x = x\n" +
      "  | x => \\let p' => p \\in 0\n" +
      "  | y => 0\n" +
      "}", 1);
    assertThatErrorsAre(cycle(get("x"), get("p")));
  }

  @Test
  public void recursiveFunction() {
    typeCheckModule(
      "\\class A (X : \\hType) {\n" +
      "  | x : X\n" +
      "}\n" +
      "\\func f (a : A Nat) => a.x\n" +
      "\\class B \\extends A {\n" +
      "  | X => Nat\n" +
      "  | x => f \\this\n" +
      "}", 1);
    assertThatErrorsAre(cycle(get("x")));
  }

  @Test
  public void orderTest() {
    typeCheckModule(
      "\\class A (TA : \\Set) | ta : TA\n" +
      "\\class B (TB : \\Set) | tb : TB\n" +
      "\\func f (T : A) (t : T.TA) => Nat\n" +
      "\\class C (TC : \\Set) \\extends A, B\n" +
      "  | TA => TC\n" +
      "  | TB => TC\n" +
      "  | c : f \\this tb");
  }

  @Test
  public void functionNewTest() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S => R 0\n" +
      "\\func rrr => \\new S");
  }

  @Test
  public void functionNewError() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S => R\n" +
      "\\func rrr => \\new S", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewAppTest() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr => \\new T 0");
  }

  @Test
  public void functionNewAppError() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr => \\new T 0", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewImplTest() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func T => S 0\n" +
      "\\func rrr => \\new T { | q => 1 }");
  }

  @Test
  public void functionNewImplError() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T => S 0\n" +
      "\\func rrr => \\new T { | q => 1 }", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionNewImplAppTest() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr => \\new T 0 { | q => 1 }");
  }

  @Test
  public void functionNewImplAppError() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr => \\new T 0 { | q => 1 }", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceTest() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S => R 0\n" +
      "\\func rrr : S \\cowith");
  }

  @Test
  public void functionInstanceError() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S => R\n" +
      "\\func rrr : S \\cowith", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceArgTest() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func rrr : S 0 \\cowith");
  }

  @Test
  public void functionInstanceArgError() {
    typeCheckModule(
      "\\record R (n : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func rrr : S 0 \\cowith", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceImplTest() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func T => S 0\n" +
      "\\func rrr : T \\cowith\n" +
      "  | q => 1");
  }

  @Test
  public void functionInstanceImplError() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T => S 0\n" +
      "\\func rrr : T \\cowith\n" +
      "  | q => 1", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void functionInstanceImplArgTest() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R n\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr : T 0 \\cowith\n" +
      "  | q => 1");
  }

  @Test
  public void functionInstanceImplArgError() {
    typeCheckModule(
      "\\record R (n q : Nat)\n" +
      "\\func S (n : Nat) => R\n" +
      "\\func T (n : Nat) => S n\n" +
      "\\func rrr : T 0 \\cowith\n" +
      "  | q => 1", 1);
    assertThatErrorsAre(fieldsImplementation(false, Collections.singletonList(get("R.n"))));
  }

  @Test
  public void repeatedImpl() {
    typeCheckModule(
      "\\class C\n" +
      "  | nn : Nat\n" +
      "\\func f : C \\cowith\n" +
      "  | nn => 0\n" +
      "  | nn => 0", 1);
    assertThatErrorsAre(fieldsImplementation(true, Collections.singletonList(get("C.nn"))));
  }

  @Test
  public void implicitArgumentTest() {
    typeCheckModule(
      "\\class C | nn : Nat\n" +
      "\\instance I : C 0\n" +
      "\\class D {c : C}\n" +
      "\\func f : D \\cowith");
  }

  @Test
  public void implicitArgumentTest2() {
    typeCheckModule(
      "\\class C\n" +
      "  | nn : Nat\n" +
      "\\instance I : C 0\n" +
      "\\class D {c : C}\n" +
      "\\func f : D \\cowith\n" +
      "  | c => \\new C 0");
  }

  @Test
  public void implicitArgumentError() {
    typeCheckModule(
      "\\class C\n" +
      "  | nn : Nat\n" +
      "\\instance I : C 0\n" +
      "\\class D {c : C}\n" +
      "\\func f : D \\cowith\n" +
      "  | c => \\new C 1", 1);
  }
}
