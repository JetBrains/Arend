package org.arend.typechecking.patternmatching;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.DataDefinition;
import org.arend.core.definition.Definition;
import org.arend.core.expr.DataCallExpression;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorExpressionPattern;
import org.arend.core.pattern.EmptyPattern;
import org.arend.core.pattern.ExpressionPattern;
import org.arend.core.sort.Sort;
import org.arend.core.subst.LevelPair;
import org.arend.ext.error.RedundantClauseError;
import org.arend.naming.reference.ConcreteLocatedReferable;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.arend.typechecking.TypeCheckingTestCase;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.ext.util.Pair;
import org.junit.Test;

import java.util.*;

import static org.arend.ExpressionFactory.*;
import static org.arend.Matchers.typeMismatchError;
import static org.arend.Matchers.typecheckingError;
import static org.arend.core.expr.ExpressionFactory.Interval;
import static org.arend.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class PatternTest extends TypeCheckingTestCase {
  private boolean checkPatterns(List<? extends Concrete.Pattern> patternArgs, List<? extends ExpressionPattern> patterns, Map<Referable, Binding> expected, Map<Referable, Binding> actual, boolean hasImplicit) {
    int i = 0, j = 0;
    for (; i < patternArgs.size() && j < patterns.size(); i++, j++) {
      Concrete.Pattern pattern1 = patternArgs.get(i);
      if (pattern1 instanceof Concrete.TuplePattern) {
        assertTrue(((Concrete.TuplePattern) pattern1).getPatterns().isEmpty());
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof EmptyPattern);
        for (j++; j < patterns.size(); j++) {
          assertTrue(patterns.get(j) instanceof BindingPattern);
        }
        return false;
      } else
      if (pattern1 instanceof Concrete.NamePattern) {
        Referable referable = ((Concrete.NamePattern) pattern1).getReferable();
        while (hasImplicit && patterns.get(j) instanceof BindingPattern && expected.get(referable) != ((BindingPattern) patterns.get(j)).getBinding()) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof BindingPattern);
        actual.put(referable, ((BindingPattern) patterns.get(j)).getBinding());
      } else
      if (pattern1 instanceof Concrete.ConstructorPattern conPattern1) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof ConstructorExpressionPattern);

        ConstructorExpressionPattern conPattern2 = (ConstructorExpressionPattern) patterns.get(j);
        assertNotNull(conPattern2.getDefinition());
        assertEquals(conPattern1.getConstructor(), conPattern2.getDefinition().getReferable());
        checkPatterns(conPattern1.getPatterns(), conPattern2.getSubPatterns(), expected, actual, hasImplicit);
      } else {
        throw new IllegalStateException();
      }
    }

    assertEquals(patternArgs.size(), i);
    assertEquals(patterns.size(), j);
    return true;
  }

  private void checkPatterns(List<? extends Concrete.Pattern> patternArgs, List<ExpressionPattern> patterns, Map<Referable, Binding> expected, @SuppressWarnings("SameParameterValue") boolean hasImplicit) {
    Map<Referable, Binding> actual = new HashMap<>();
    boolean withoutEmpty = checkPatterns(patternArgs, patterns, expected, actual, hasImplicit);
    assertEquals(expected, withoutEmpty ? actual : null);

    Stack<ExpressionPattern> patternStack = new Stack<>();
    for (int i = patterns.size() - 1; i >= 0; i--) {
      patternStack.push(patterns.get(i));
    }

    DependentLink last = null;
    while (!patternStack.isEmpty()) {
      ExpressionPattern pattern = patternStack.pop();
      if (pattern instanceof BindingPattern) {
        DependentLink link = ((BindingPattern) pattern).getBinding();
        if (last != null) {
          assertEquals(last.getNext(), link);
        }
        last = link;
      } else
      if (pattern instanceof ConstructorExpressionPattern) {
        for (int i = pattern.getSubPatterns().size() - 1; i >= 0; i--) {
          patternStack.push(pattern.getSubPatterns().get(i));
        }
      } else
      if (pattern instanceof EmptyPattern) {
        break;
      }
    }
  }

  @Test
  public void threeVars() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | suc n, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void nestedPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | suc (suc (suc n)), zero, suc (suc (suc (suc zero))) => n").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void incorrectType() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (m : Nat -> Nat) (k : Nat) : Nat\n" +
      "  | suc n, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Pi(Nat(), Nat())), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void incorrectDataType() {
    Group module = resolveNamesModule(
      """
        \\data D | con
        \\func f (n : Nat) (d : D) (k : Nat) : Nat
          | suc n, zero, suc k => k
        """);
    Iterator<? extends Statement> it = module.getStatements().iterator();
    TCDefReferable dataDef = (TCDefReferable) it.next().getGroup().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getGroup().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = funDef.getBody().getClauses().get(0).getPatterns();
    LocalErrorReporter localErrorReporter = new LocalErrorReporter(funDef.getData(), errorReporter);
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(PatternTypechecking.Mode.DATA, new CheckTypeVisitor(localErrorReporter, null, null), true, null, Collections.emptyList()).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, DataCallExpression.make(data, LevelPair.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n m k : Nat) : Nat\n" +
      "  | suc n m, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void interval() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, i => zero").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void intervalFail() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (i : I) : Nat\n" +
      "  | zero, left => zero").getDefinition();
    List<Concrete.Pattern> patternsArgs = fun.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(fun.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void emptyDataType() {
    Group module = resolveNamesModule(
      """
        \\data D
        \\func f (n : Nat) (d : D) (k : Nat) : Nat
          | suc n, (), k => k
        """);
    Iterator<? extends Statement> it = module.getStatements().iterator();
    TCDefReferable dataDef = (TCDefReferable) it.next().getGroup().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getGroup().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = funDef.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(funDef.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, DataCallExpression.make(data, LevelPair.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void emptyDataTypeWarning() {
    Group module = resolveNamesModule(
      """
        \\data D
        \\func f (n : Nat) (d : D) (k : Nat) : Nat
          | suc n, (), suc k => k
        """);
    Iterator<? extends Statement> it = module.getStatements().iterator();
    TCDefReferable dataDef = (TCDefReferable) it.next().getGroup().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getGroup().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = funDef.getBody().getClauses().get(0).getPatterns();
    Pair<List<ExpressionPattern>, Map<Referable, Binding>> res = new PatternTypechecking(new LocalErrorReporter(funDef.getData(), errorReporter), PatternTypechecking.Mode.DATA).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, DataCallExpression.make(data, LevelPair.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void elimBefore() {
    typeCheckDef(
      """
        \\func if (n : Nat) {A : \\Type} (a a' : A) : A \\elim n
          | zero => a
          | suc _ => a'
        """);
  }

  @Test
  public void elimAfter() {
    typeCheckDef(
      """
        \\func if {A : \\Type} (a a' : A) (n : Nat) : A \\elim n
          | zero => a
          | suc _ => a'
        """);
  }

  @Test
  public void dependentElim() {
    typeCheckModule(
      """
        \\func if {A : \\Type} (n : Nat) (a a' : A) : A \\elim n
          | zero => a
          | suc _ => a'
        \\func f (n : Nat) (x : if n Nat (Nat -> Nat)) : Nat \\elim n
          | zero => x
          | suc _ => x 0
        """);
  }

  @Test
  public void elimLess() {
    typeCheckModule(
      """
        \\data D Nat \\with | suc n => dsc
        \\func tests (n : Nat) (d : D n) : Nat \\elim n, d
          | suc n => 0
          | zero => 0
        """, 1);
  }

  @Test
  public void withoutElimLess() {
    typeCheckModule(
      """
        \\data D Nat \\with | suc n => dsc
        \\func tests (n : Nat) (d : D n) : Nat
          | suc n => 0
          | zero => 0
        """, 2);
  }

  @Test
  public void elimMore() {
    typeCheckModule(
      """
        \\func tests (n m : Nat) : Nat \\elim n
          | suc n, zero => 0
          | zero, suc m => 0
        """, 1);
  }

  @Test
  public void elimEvenMore() {
    typeCheckModule(
      """
        \\func tests (n m : Nat) : Nat \\elim n
          | suc n, zero => 0
          | zero, zero => 0
          | suc n, suc m => 0
          | zero, suc m => 0
        """, 1);
  }

  @Test
  public void withoutElimMore() {
    typeCheckModule(
      """
        \\func tests (n : Nat) : Nat
          | suc n, zero => 0
          | zero, suc m => 0
        """, 2);
  }

  @Test
  public void implicitParameter() {
    typeCheckModule(
      """
        \\func tests {n : Nat} (m : Nat) : Nat
          | {suc n}, zero => 0
          | {zero}, suc m => 0
          | {suc n}, suc m => 0
          | {zero}, zero => 0
        """);
  }

  @Test
  public void skipImplicitParameter() {
    typeCheckModule(
      """
        \\func tests {n : Nat} (m : Nat) : Nat
          | suc m => 0
          | zero => 0
        """);
  }

  @Test
  public void implicitParameterError() {
    typeCheckModule(
      """
        \\func tests {n : Nat} : Nat
          | suc n => 0
          | zero => 0
        """, 2);
  }

  @Test
  public void withThis() {
    typeCheckClass(
      """
        \\func tests (n : Nat) : Nat
          | suc n => 0
          | zero => 0
        """, "");
  }

  @Test
  public void withThisAndElim() {
    typeCheckClass(
      """
        \\func tests (n : Nat) : Nat
          | suc n => 0
          | zero => 0
        """, "");
  }

  @Test
  public void nonEliminatedAvailable() {
    typeCheckModule(
      """
        \\func tests {n : Nat} (m : Nat) : Nat \\elim m
          | suc m => m
          | zero => n
        """);
  }

  @Test
  public void explicitAvailable() {
    typeCheckModule(
      """
        \\func tests {n : Nat} (m : Nat) : Nat
          | {n}, suc m => n
          | {k}, zero => k
        """);
  }

  @Test
  public void eliminateOverridden() {
    typeCheckModule(
      """
        \\func f (n : Nat) (n : Nat) : Nat \\elim n
          | suc _ => 2
          | zero => n
        \\func g : f 1 0 = 1 => idp
        """);
  }

  @Test
  public void redundantPattern() {
    typeCheckDef(
      """
        \\func f (n : Nat) : Nat
          | _ => 0
          | zero => 1
        """, 1);
    assertThatErrorsAre(typecheckingError(RedundantClauseError.class));
  }

  @Test
  public void casePatternWrongDefinition() {
    typeCheckModule(
      "\\data D | con\n" +
      "\\func test (x : Nat) : Nat => \\case x \\with { zero => 0 | con => 1 }", 1);
  }

  @Test
  public void elimPatternWrongDefinition() {
    typeCheckModule(
      "\\data D | con\n" +
      "\\func test (x : Nat) : Nat | zero => 0 | con => 1", 1);
  }

  @Test
  public void patternWrongDefinition() {
    resolveNamesModule(
      """
        \\data Nat | zero | suc Nat
        \\data D (n m : Nat) | d
        \\data C | c (n m : Nat) (D n m)
        \\data E C \\with | E (c zero (suc zero) d) => e
        """, 1);
  }

  @Test
  public void functionPatternWrongDefinition() {
    typeCheckModule(
      "\\data D | con\n" +
      "\\func test (x : Nat) : Nat | zero => 0 | con => 1", 1);
  }

  @Test
  public void fakeNatTest() {
    typeCheckModule(
      """
        \\data Nat' | suc' Nat' | zero'
        \\data Foo | foo Nat'
        \\func ff (x : Foo) : Nat'
          | foo (suc' (suc' (suc' zero'))) => zero'
          | foo n => n
        \\func test : ff (foo (suc' (suc' zero'))) = suc' (suc' zero') => idp
        """);
  }

  @Test
  public void natTest() {
    typeCheckModule(
      """
        \\func ff (x : Nat) : Nat
          | suc zero => zero
          | n => n
        \\func test : ff (suc (suc (suc zero))) = 3 => idp
        """);
  }

  @Test
  public void typeTest() {
    typeCheckModule(
      """
        \\class A (f : Nat)
        \\data D | con A
        \\func foo (d : D) : Nat | con (a : A) => a.f
        """);
  }

  @Test
  public void typeTestError() {
    typeCheckModule(
      """
        \\class A (f : Nat)
        \\data D | con Nat
        \\func foo (n : Nat) (d : D) : Nat | n, con (a : A) => n
        """, 1);
    assertThatErrorsAre(typeMismatchError());
  }

  @Test
  public void implicitParametersAndCase() {
    typeCheckModule(
      """
        \\data D | con {n : Nat} (p : n = n)
        \\func foo (d : D) => \\case d \\as d \\return d = d \\with {
          | con p => idp
        }
        """);
  }

  @Test
  public void implicitPatternTest() {
    typeCheckModule(
      """
        \\func test {n : Nat} {x : Nat} : x = x
          | {0} => idp
          | {suc n} => idp
        """);
  }

  @Test
  public void infixPatterns() {
    typeCheckModule(
      """
        \\data List | \\infix 10 :: Nat List
        \\func test (n : List) : 0 = 0
          | n :: m => idp
        """);
  }

  @Test
  public void infixPatterns2() {
    typeCheckModule(
      """
        \\data List | \\infix 10 :: Nat List
        \\func test (r : List) : Nat
          | n :: m => n
        """);
  }

  @Test
  public void infixPatternsChain() {
    typeCheckModule(
      """
        \\data List | \\infixr 10 :: Nat List
        \\func test (r : List) : Nat
          | n :: m :: q => m
        """);
  }

  @Test
  public void definedInfixPatterns() {
    typeCheckModule(
      """
        \\data List | cons Nat List
        \\cons \\infix 10 :: (n : Nat) (m : List) => cons n m\\func test (r : List) : Nat
          | n :: m => n
        """);
  }

  @Test
  public void infixPatternsInTuple() {
    typeCheckModule(
      """
        \\data List | cons Nat List
        \\cons \\infix 10 :: (n : Nat) (m : List) => cons n m\\func test (r : \\Sigma List List) : Nat
          | (n :: m, k :: l) => n
        """);
  }

  @Test
  public void infixPatternsInLambda() {
    typeCheckModule(
      """
        \\data List | \\infixr 10 :: Nat List
        \\func test (r : List ) : List -> Nat => \\lam (n :: m) => n
        """);
  }

  @Test
  public void infixPatternsInLet() {
    typeCheckModule(
      """
        \\data List | \\infixr 10 :: Nat List
        \\func test (r : List ) : Nat => \\let (n :: m) => r \\in n\s
        """);
  }

  @Test
  public void infixPatternFromNonfix() {
    typeCheckModule(
      """
        \\data List | cons Nat List
        \\func test (r : List) : Nat
          | n `cons` m => n
        """);
  }

  @Test
  public void redundantClause() {
    typeCheckModule(
      """
        \\data List (A : \\Type) | nil | \\infix 6 :: A (List A)
        \\func indices {A : \\Type} (is : List Nat) (l : List A) : List A \\elim is, l
          | nil, _ => nil
          | :: _ _, nil => nil
          | :: 0 is, :: a l => a :: indices is l
          | :: (suc n) is, :: _ l => indices (n :: is) l
        """);
  }

  @Test
  public void redundantClause2() {
    typeCheckModule(
      """
        \\data List (A : \\Type) | nil | \\infix 6 :: (\\Sigma A A) (List A)
        \\func indices {A : \\Type} (is : List Nat) (l : List A) : List A \\elim is, l
          | nil, _ => nil
          | :: _ _, nil => nil
          | :: (0, _) is, :: a l => a :: indices is l
          | :: (suc n, _) is, :: _ l => indices ((n, 0) :: is) l
        """);
  }


  @Test
  public void qualifiedConstructor() {
    typeCheckModule(
      """
        \\module M \\where {
          \\data List | nil | \\infixr 10 ::: Nat List
          \\func f (a b : M.List) : Nat \\elim a
            | M.nil => 0
        }
        """, 1);
  }
}
