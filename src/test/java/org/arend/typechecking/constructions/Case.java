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
import org.junit.Test;

import java.util.Arrays;

import static org.arend.Matchers.*;
import static org.junit.Assert.assertEquals;

public class Case extends TypeCheckingTestCase {
  @Test
  public void testCase() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func f (b : Bool) : Bool => \\case b \\with { | true => false | false => true }");
  }

  @Test
  public void testCaseReturn() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\func f (b : Bool) => \\case b \\as x \\return not (not x) = x \\with { | true => idp | false => idp }");
  }

  @Test
  public void testCaseReturnError() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\func f (b : Bool) => \\case b \\return not (not b) = b \\with { | true => idp | false => idp }", 2);
    assertThatErrorsAre(Matchers.error(), Matchers.error());
  }

  @Test
  public void testCaseArguments() {
    typeCheckModule(
      "\\data Bool | true | false\n" +
      "\\func not (b : Bool) => \\case b \\return Bool \\with { | true => false | false => true }\n" +
      "\\data Or (A B : \\Type) | inl A | inr B\n" +
      "\\func f (b : Bool) : (b = true) `Or` (b = false) => \\case b \\as x, idp : b = x \\with { | true, p => inl p | false, p => inr p }");
  }

  @Test
  public void testCaseMultipleArguments() {
    typeCheckModule(
      "\\func \\infix 4 < (n m : Nat) => Nat\n" +
      "\\func f1 (n k : Nat) : Nat => \\case k \\as z, n < z \\as r, idp : r = n < z \\with { | k, T, P => 0 }\n" +
      "\\func f2 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < z} p \\with { | k, p, s => 0 }\n" +
      "\\func f3 (n k : Nat) (p : n < k) : Nat => \\case k \\as z, p \\as r : n < z, idp : r = {n < k} p \\with { | k, p, s => 0 }");
  }

  @Test
  public void caseElimResolveError() {
    resolveNamesDef(
      "\\func f (x : Nat) : Nat => \\case \\elim x \\with {\n" +
      "  | _ => x\n" +
      "}", 1);
    assertThatErrorsAre(notInScope("x"));
  }

  @Test
  public void caseElim() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) => \\case \\elim x, p : x = 0 \\return x = 0 \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimSubst() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, p \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimSubstType() {
    typeCheckDef(
      "\\func f (x : Nat) (p : x = 0) : x = 0 => \\case \\elim x, \\elim p : x = 0 \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}");
  }

  @Test
  public void caseElimTypeError() {
    typeCheckDef(
      "\\func f (x : \\Set0) => \\case \\elim x : \\Set1 \\return Nat \\with {\n" +
      "  | _ => 0\n" +
      "}", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void checkElimTypeTest() {
    typeCheckModule(
      "\\func test (A : \\Type) (x : A) (p : x = x) : p = p =>\n" +
      "  \\case \\elim x, \\elim p \\with {\n" +
      "    | _, _ => idp\n" +
      "  }");
  }

  @Test
  public void checkElimTypeError() {
    typeCheckModule(
      "\\func test (A : \\Type) (x : A) (p : x = x) : p = p =>\n" +
      "  \\case \\elim x \\with {\n" +
      "    | _ => {?}\n" +
      "  }", 2);
    assertThatErrorsAre(elimSubstError("p"), goal(2));
  }

  @Test
  public void checkElimTypeError2() {
    typeCheckModule(
      "\\func test (A : \\Type) (x : A) (p : x = x) : p = p =>\n" +
      "  \\case \\elim x, p \\with {\n" +
      "    | _, _ => {?}\n" +
      "  }", 2);
    assertThatErrorsAre(elimSubstError("p"), goal(2));
  }

  @Test
  public void checkElimTypeError3() {
    typeCheckModule(
      "\\func test (A : \\Type) (x : A) (p : x = x) : p = p =>\n" +
      "  \\case \\elim p, \\elim x \\with {\n" +
      "    | _, _ => {?}\n" +
      "  }", 1);
    assertThatErrorsAre(elimSubstError("p"));
  }

  @Test
  public void elimContextTest() {
    typeCheckModule(
      "\\func test (x : Nat) (p : x = x) =>\n" +
      "  (\\case \\elim x, p : x = x \\with {\n" +
      "    | _, _ => 0\n" +
      "  }) Nat.+ (\\case x, p : x = x \\with {\n" +
      "    | _, _ => 1\n" +
      "})");
  }

  @Test
  public void letElimTest() {
    typeCheckModule(
      "\\data Or (A B : \\Type) | inl A | inr B\n" +
      "\\func test (f : Nat -> Nat) : Or (f 0 = 0) (\\Sigma (n : Nat) (f 0 = suc n)) =>\n" +
      "  \\let x => f 0\n" +
      "  \\in \\case \\elim x \\return Or (x = 0) (\\Sigma (n : Nat) (x = suc n)) \\with {\n" +
      "    | 0 => inl idp\n" +
      "    | suc n => inr (n, idp)\n" +
      "  }");
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
    typeCheckModule(
      "\\truncated \\data Trunc (A : \\Type) : \\Prop | in A\n" +
      "\\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p\n" +
      "\\func test : R \\cowith | field _ t => \\case t \\with { | in a => a }");
  }

  @Test
  public void propertyLevelError() {
    typeCheckModule(
      "\\truncated \\data Trunc (A : \\Type) : \\Prop | in A\n" +
      "\\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : A\n" +
      "\\func test : R \\cowith | field _ t => \\scase t \\with { | in a => a }", 1);
    assertThatErrorsAre(Matchers.typecheckingError(TruncatedDataError.class));
  }

  @Test
  public void propertyExtendsLevelTest() {
    typeCheckModule(
      "\\truncated \\data Trunc (A : \\Type) : \\Prop | in A\n" +
      "\\record R | field {A : \\Set} (p : \\Pi (a a' : A) -> a = a') (t : Trunc A) : \\level A p\n" +
      "\\record S \\extends R | field _ t => \\case t \\with { | in a => a }");
  }

  @Test
  public void varSubstTest() {
    typeCheckModule("\\func test (n : Nat) => \\case n \\as n', idp {_} {n'} \\return \\Sigma (x : Nat) (n' = x) \\with { | n, p => (n,p) }");
  }

  @Test
  public void elimTypeTest() {
    TypedBinding n = new TypedBinding("n", ExpressionFactory.Nat());
    Expression type = FunCallExpression.make(Prelude.PATH_INFIX, LevelPair.SET0, Arrays.asList(ExpressionFactory.Nat(), new ReferenceExpression(n), new SmallIntegerExpression(0)));
    typeCheckExpr(Arrays.asList(n, new TypedBinding("p", type)),
      "\\case \\elim n, p \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}", type);
  }

  @Test
  public void elimTypeTest2() {
    TypedBinding n = new TypedBinding("n", ExpressionFactory.Nat());
    Expression type = FunCallExpression.make(Prelude.PATH_INFIX, LevelPair.SET0, Arrays.asList(ExpressionFactory.Nat(), new ReferenceExpression(n), new SmallIntegerExpression(0)));
    typeCheckExpr(Arrays.asList(n, new TypedBinding("p", type)),
      "\\case \\elim n, p \\return n = 0 \\with {\n" +
      "  | 0, _ => idp\n" +
      "  | suc _, p => p\n" +
      "}", null);
  }

  @Test
  public void elimArgTypeTest() {
    typeCheckModule(
      "\\lemma test (f : Nat -> Nat) (p : f 0 = 0) : f 0 = 0 =>\n" +
      "  \\case f 0 \\as x, \\elim p : x = 0 \\return x = 0 \\with {\n" +
      "    | _, p => p\n" +
      "  }");
  }

  @Test
  public void argTypeTest() {
    typeCheckModule(
      "\\func test (f : Nat -> Nat) (p : f 0 = 0) : Nat =>\n" +
      "  \\case p : Nat \\with {\n" +
      "    | p => 0\n" +
      "  }", 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void typeTest() {
    Definition def = typeCheckDef("\\func test (n : Nat) (x : \\case n \\with { | 0 => Nat | suc _ => Nat }) : Nat => 0");
    assertEquals(Sort.SET0, def.getParameters().getNext().getType().getSortOfType());
  }
}
