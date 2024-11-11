package org.arend.typechecking.constructions;

import org.arend.Matchers;
import org.arend.core.context.binding.TypedBinding;
import org.arend.core.definition.Definition;
import org.arend.core.expr.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.prelude.Prelude;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.TruncatedDataError;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.arend.Matchers.*;
import static org.junit.Assert.assertEquals;

public class CaseTest extends TypeCheckingTestCase {
  @Test
  public void testCase() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func f (b : Bool) : Bool => \\case b \\with { | true => false | false => true }");
  }

  @Test
  public void testCaseReturn() {
    typeCheckModule("""
      \\data Bool | true | false
      \\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }
      \\func f (b : Bool) => \\case b \\as x \\return not (not x) = x \\with { | true => idp | false => idp }
      """);
  }

  @Test
  public void testCaseReturnError() {
    typeCheckModule("""
      \\data Bool | true | false
      \\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }
      \\func f (b : Bool) => \\case b \\return not (not b) = b \\with { | true => idp | false => idp }
      """, 2);
    assertThatErrorsAre(Matchers.error(), Matchers.error());
  }

  @Test
  public void testCaseArguments() {
    typeCheckModule("""
      \\data Bool | true | false
      \\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }
      \\data Or (A B : \\Type) | inl A | inr B
      \\func f (b : Bool) : (b = true) `Or` (b = false) => \\case b \\as x, idp : b = x \\with { | true, p => inl p | false, p => inr p }
      """);
  }

  @Test
  public void testCaseMultipleArguments() {
    typeCheckModule("""
      \\func \\infix 4 < (n m : Nat) => Nat
      \\func f1 (n k : Nat) : Nat => \\case k \\as z, n < z \\as r, idp : r = n < z \\with { | k, T, P => 0 }
      \\func f2 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < z} p \\with { | k, p, s => 0 }
      \\func f3 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < k} p \\with { | k, p, s => 0 }
      """);
  }

  @Test
  public void caseElimResolveError() {
    resolveNamesDef("""
      \\func f (x : Nat) : Nat => \\case \\elim x \\with {
        | _ => x
      }""", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void caseElim() {
    typeCheckDef("""
      \\func f (x : Nat) (p : x = 0) => \\case \\elim x, p : x = 0 \\return x = 0 \\with {
        | 0, _ => idp
        | suc _, p => p
      }
      """);
  }

  @Test
  public void caseElimSubst() {
    typeCheckDef("""
      \\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, p \\with {
        | 0, _ => idp
        | suc _, p => p
      }
      """);
  }

  @Test
  public void caseElimSubstType() {
    typeCheckDef("""
      \\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, p : x = 0 \\with {
        | 0, _ => idp
        | suc _, p => p
      }
      """);
  }

  @Test
  public void caseElimTypeError() {
    typeCheckDef("""
      \\func f (x : \\Set0) => \\case \\elim x : \\Set1 \\return Nat \\with {
        | _ => 0
      }
      """, 1);
  }

  @Test
  public void checkElimTypeTest() {
    typeCheckModule("""
      \\func test (A : \\Type) (x : A) (p : x = x) : p = p =>
        \\case \\elim x, \\elim p \\with {
          | _, _ => idp
        }
      """);
  }

  @Test
  public void checkElimTypeError() {
    typeCheckModule("""
      \\func test (A : \\Type) (x : A) (p : x = x) : p = p =>
        \\case \\elim x \\with {
          | _ => {?}
        }
      """, 2);
    assertThatErrorsAre(elimSubstError("p"), goal(2));
  }

  @Test
  public void checkElimTypeError2() {
    typeCheckModule("""
      \\func test (A : \\Type) (x : A) (p : x = x) : p = p =>
        \\case \\elim x, p \\with {
          | _, _ => {?}
        }
      """, 2);
    assertThatErrorsAre(elimSubstError("p"), goal(2));
  }

  @Test
  public void checkElimTypeError3() {
    typeCheckModule("""
      \\func test (A : \\Type) (x : A) (p : x = x) : p = p =>
        \\case \\elim p, \\elim x \\with {
          | _, _ => {?}
        }
      """, 1);
    assertThatErrorsAre(elimSubstError("p"));
  }

  @Test
  public void elimContextTest() {
    typeCheckModule("""
      \\func test (x : Nat) (p : x = x) =>
        (\\case \\elim x, p : x = x \\with {
          | _, _ => 0
        }) Nat.+ (\\case x, p : x = x \\with {
          | _, _ => 1
      })
      """);
  }

  @Test
  public void letElimTest() {
    typeCheckModule("""
      \\data Or (A B : \\Type) | inl A | inr B
      \\func test (f : Nat -> Nat) : Or (f 0 = 0) (\\Sigma (n : Nat) (f 0 = suc n)) =>
        \\let x => f 0
        \\in \\case \\elim x \\return Or (x = 0) (\\Sigma (n : Nat) (x = suc n)) \\with {
          | 0 => inl idp
          | suc n => inr (n, idp)
        }
      """);
  }

  @Test
  public void lemmaLevelTest() {
    typeCheckModule(
      "\\truncated \\data Trunc (A : \\Type) : \\Prop | in A\n" +
      "\\lemma test {A : \\Type} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p => \\case t \\with { | in a => a }");
  }

  @Test
  public void funcLevelTest() {
    typeCheckModule(
      "\\truncated \\data Trunc (A : \\Type) : \\Prop | in A\n" +
      "\\func test {A : \\Type} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p => \\case t \\with { | in a => a }", -1);
  }

  @Test
  public void propertyLevelTest() {
    typeCheckModule("""
      \\truncated \\data Trunc (A : \\Type) : \\Prop | in A
      \\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p
      \\func test : R \\cowith | field _ t => \\case t \\with { | in a => a }
      """);
  }

  @Test
  public void propertyLevelError() {
    typeCheckModule("""
      \\truncated \\data Trunc (A : \\Type) : \\Prop | in A
      \\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : A
      \\func test : R \\cowith | field _ t => \\scase t \\with { | in a => a }
      """, 1);
    assertThatErrorsAre(Matchers.typecheckingError(TruncatedDataError.class));
  }

  @Test
  public void propertyExtendsLevelTest() {
    typeCheckModule("""
      \\truncated \\data Trunc (A : \\Type) : \\Prop | in A
      \\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p
      \\record S \\extends R | field _ t => \\case t \\with { | in a => a }
      """);
  }

  @Test
  public void varSubstTest() {
    typeCheckModule("\\func test (n : Nat) => \\case n \\as n', idp {_} {n'} \\return \\Sigma (x : Nat) (n' = x) \\with { | n, p => (n,p) }");
  }

  @Test
  public void elimTypeTest() {
    TypedBinding n = new TypedBinding("n", ExpressionFactory.Nat());
    Expression type = FunCallExpression.make(Prelude.PATH_INFIX, LevelPair.SET0, Arrays.asList(ExpressionFactory.Nat(), new ReferenceExpression(n), new SmallIntegerExpression(0)));
    typeCheckExpr(Arrays.asList(n, new TypedBinding("p", type)), """
      \\case \\elim n, p \\with {
        | 0, _ => idp
        | suc _, p => p
      }
      """, type);
  }

  @Test
  public void elimTypeTest2() {
    TypedBinding n = new TypedBinding("n", ExpressionFactory.Nat());
    Expression type = FunCallExpression.make(Prelude.PATH_INFIX, LevelPair.SET0, Arrays.asList(ExpressionFactory.Nat(), new ReferenceExpression(n), new SmallIntegerExpression(0)));
    typeCheckExpr(Arrays.asList(n, new TypedBinding("p", type)), """
      \\case \\elim n, p \\return n = 0 \\with {
        | 0, _ => idp
        | suc _, p => p
      }
      """, null);
  }

  @Test
  public void elimArgTypeTest() {
    typeCheckModule("""
      \\lemma test (f : Nat -> Nat) (p : f 0 = 0) : f 0 = 0 =>
        \\case f 0 \\as x, p : x = 0 \\return x = 0 \\with {
          | _, p => p
        }
      """);
  }

  @Test
  public void argTypeTest() {
    typeCheckModule("""
      \\func test (f : Nat -> Nat) (p : f 0 = 0) : Nat =>
        \\case p : Nat \\with {
          | p => 0
        }
      """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void typeTest() {
    Definition def = typeCheckDef("\\func test (n : Nat) (x : \\case n \\with { | 0 => Nat | suc _ => Nat }) : Nat => 0");
    assertEquals(Sort.SET0, def.getParameters().getNext().getType().getSortOfType());
  }

  @Test
  public void depTest() {
  typeCheckModule("""
    \\data D (n : Nat) | con1 | con2
    \\func foo (n : Nat) (d : D n) : n = n
      | 0, con1 => idp
      | 0, con2 => idp
      | suc n, con1 => idp
      | suc n, con2 => idp
    \\lemma test (g : Nat -> Nat) (f : \\Pi (n : Nat) -> D n) : foo (g 0) (f (g 0)) = idp
      => \\case g 0 \\as x, f x \\as y \\return foo x y = idp \\with {
        | 0, con1 => idp
        | 0, con2 => idp
        | suc x, con1 => idp
        | suc x, con2 => idp
      }
    """);
  }

  // TODO: Fix this
  @Ignore
  @Test
  public void elimDepTest() {
    typeCheckModule("""
      \\func f (n : Nat) : Nat
        | 0 => 0
        | suc n => n
      \\func test (n : Nat) : Nat => \\case \\elim n, f n \\as x, idp : f n = x \\with {
        | n, e, p => 0
      }
      """);
  }

  @Test
  public void elimWithTypeTest() {
    typeCheckModule("""
      \\func Type-isSet4 {A : \\hType} {x : A} (p : x = x) : p = idp
        => \\case x \\as x_, \\elim p : x_ = x \\with {
          | x, idp => idp
        }
      """, 1);
  }
}
