package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.pattern.BindingPattern;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.EmptyPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.reference.ConcreteLocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.term.group.Group;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.util.Pair;
import org.junit.Test;

import java.util.*;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Interval;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class PatternTest extends TypeCheckingTestCase {
  private boolean checkPatterns(List<? extends Concrete.Pattern> patternArgs, List<Pattern> patterns, Map<Referable, Binding> expected, Map<Referable, Binding> actual, boolean hasImplicit) {
    int i = 0, j = 0;
    for (; i < patternArgs.size() && j < patterns.size(); i++, j++) {
      Concrete.Pattern pattern1 = patternArgs.get(i);
      if (pattern1 instanceof Concrete.EmptyPattern) {
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
      if (pattern1 instanceof Concrete.ConstructorPattern) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof ConstructorPattern);

        Concrete.ConstructorPattern conPattern1 = (Concrete.ConstructorPattern) pattern1;
        ConstructorPattern conPattern2 = (ConstructorPattern) patterns.get(j);
        assertEquals(conPattern1.getConstructor(), conPattern2.getConstructor().getReferable());
        checkPatterns(conPattern1.getPatterns(), conPattern2.getArguments(), expected, actual, hasImplicit);
      } else {
        throw new IllegalStateException();
      }
    }

    assertEquals(patternArgs.size(), i);
    assertEquals(patterns.size(), j);
    return true;
  }

  private void checkPatterns(List<? extends Concrete.Pattern> patternArgs, List<Pattern> patterns, Map<Referable, Binding> expected, @SuppressWarnings("SameParameterValue") boolean hasImplicit) {
    Map<Referable, Binding> actual = new HashMap<>();
    boolean withoutEmpty = checkPatterns(patternArgs, patterns, expected, actual, hasImplicit);
    assertEquals(expected, withoutEmpty ? actual : null);

    Stack<Pattern> patternStack = new Stack<>();
    for (int i = patterns.size() - 1; i >= 0; i--) {
      patternStack.push(patterns.get(i));
    }

    DependentLink last = null;
    while (!patternStack.isEmpty()) {
      Pattern pattern = patternStack.pop();
      if (pattern instanceof BindingPattern) {
        DependentLink link = ((BindingPattern) pattern).getBinding();
        if (last != null) {
          assertEquals(last.getNext(), link);
        }
        last = link;
      } else
      if (pattern instanceof ConstructorPattern) {
        for (int i = ((ConstructorPattern) pattern).getArguments().size() - 1; i >= 0; i--) {
          patternStack.push(((ConstructorPattern) pattern).getArguments().get(i));
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
      "\\func f (n m k : Nat)\n" +
      "  | suc n, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void nestedPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n m k : Nat)\n" +
      "  | suc (suc (suc n)), zero, suc (suc (suc (suc zero))) => n").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void incorrectType() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (m : Nat -> Nat) (k : Nat)\n" +
      "  | suc n, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Pi(Nat(), Nat())), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void incorrectDataType() {
    Group module = resolveNamesModule(
      "\\data D | con\n" +
      "\\func f (n : Nat) (d : D) (k : Nat)\n" +
      "  | suc n, zero, suc k => k");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable dataDef = it.next().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(funDef.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n m k : Nat)\n" +
      "  | suc n m, zero, suc k => k").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void interval() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (i : I)\n" +
      "  | zero, i => zero").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void intervalFail() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\func f (n : Nat) (i : I)\n" +
      "  | zero, left => zero").getDefinition();
    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) fun.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(fun.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, Interval())), fun.getBody(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void emptyDataType() {
    Group module = resolveNamesModule(
      "\\data D\n" +
      "\\func f (n : Nat) (d : D) (k : Nat)\n" +
      "  | suc n, (), k => k");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable dataDef = it.next().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(funDef.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void emptyDataTypeWarning() {
    Group module = resolveNamesModule(
      "\\data D\n" +
      "\\func f (n : Nat) (d : D) (k : Nat)\n" +
      "  | suc n, (), suc k => k");
    Iterator<? extends Group> it = module.getSubgroups().iterator();
    GlobalReferable dataDef = it.next().getReferable();
    Concrete.FunctionDefinition funDef = (Concrete.FunctionDefinition) ((ConcreteLocatedReferable) it.next().getReferable()).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Concrete.Pattern> patternsArgs = ((Concrete.ElimFunctionBody) funDef.getBody()).getClauses().get(0).getPatterns();
    Pair<List<Pattern>, Map<Referable, Binding>> res = new PatternTypechecking(new ProxyErrorReporter(funDef.getData(), errorReporter), EnumSet.of(PatternTypechecking.Flag.HAS_THIS, PatternTypechecking.Flag.CONTEXT_FREE)).typecheckPatterns(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), funDef.getBody(), false);
    assertNotNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void elimBefore() {
    typeCheckDef(
      "\\func if (n : Nat) {A : \\Type} (a a' : A) : A \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'");
  }

  @Test
  public void elimAfter() {
    typeCheckDef(
      "\\func if {A : \\Type} (a a' : A) (n : Nat) : A \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'");
  }

  @Test
  public void dependentElim() {
    typeCheckModule(
      "\\func if {A : \\Type} (n : Nat) (a a' : A) : A \\elim n\n" +
      "  | zero => a\n" +
      "  | suc _ => a'\n" +
      "\\func f (n : Nat) (x : if n Nat (Nat -> Nat)) : Nat \\elim n\n" +
      "  | zero => x\n" +
      "  | suc _ => x 0");
  }

  @Test
  public void elimLess() {
    typeCheckModule(
      "\\data D Nat \\with | suc n => dsuc\n" +
      "\\func tests (n : Nat) (d : D n) : Nat \\elim n, d\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 1);
  }

  @Test
  public void withoutElimLess() {
    typeCheckModule(
      "\\data D Nat \\with | suc n => dsuc\n" +
      "\\func tests (n : Nat) (d : D n) : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 2);
  }

  @Test
  public void elimMore() {
    typeCheckModule(
      "\\func tests (n m : Nat) : Nat \\elim n\n" +
      "  | suc n, zero => 0\n" +
      "  | zero, suc m => 0", 1);
  }

  @Test
  public void elimEvenMore() {
    typeCheckModule(
      "\\func tests (n m : Nat) : Nat \\elim n\n" +
      "  | suc n, zero => 0\n" +
      "  | zero, zero => 0\n" +
      "  | suc n, suc m => 0\n" +
      "  | zero, suc m => 0", 1);
  }

  @Test
  public void withoutElimMore() {
    typeCheckModule(
      "\\func tests (n : Nat) : Nat\n" +
        "  | suc n, zero => 0\n" +
        "  | zero, suc m => 0", 2);
  }

  @Test
  public void implicitParameter() {
    typeCheckModule(
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {suc n}, zero => 0\n" +
      "  | {zero}, suc m => 0\n" +
      "  | {suc n}, suc m => 0\n" +
      "  | {zero}, zero => 0");
  }

  @Test
  public void skipImplicitParameter() {
    typeCheckModule(
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | suc m => 0\n" +
      "  | zero => 0");
  }

  @Test
  public void implicitParameterError() {
    typeCheckModule(
      "\\func tests {n : Nat} : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", 2);
  }

  @Test
  public void withThis() {
    typeCheckClass(
      "\\func tests (n : Nat) : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", "");
  }

  @Test
  public void withThisAndElim() {
    typeCheckClass(
      "\\func tests (n : Nat) : Nat\n" +
      "  | suc n => 0\n" +
      "  | zero => 0", "");
  }

  @Test
  public void nonEliminatedAvailable() {
    typeCheckModule(
      "\\func tests {n : Nat} (m : Nat) : Nat \\elim m\n" +
      "  | suc m => m\n" +
      "  | zero => n");
  }

  @Test
  public void explicitAvailable() {
    typeCheckModule(
      "\\func tests {n : Nat} (m : Nat) : Nat\n" +
      "  | {n}, suc m => n\n" +
      "  | {k}, zero => k");
  }

  @Test
  public void eliminateOverridden() {
    typeCheckModule(
      "\\func f (n : Nat) (n : Nat) : Nat \\elim n\n" +
      "  | suc _ => 2\n" +
      "  | zero => n\n" +
      "\\func g : f 1 0 = 1 => path (\\lam _ => 1)");
  }

  @Test
  public void redundantPattern() {
    typeCheckDef(
      "\\func f (n : Nat) : Nat\n" +
      "  | _ => 0\n" +
      "  | zero => 1", 1);
  }

  @Test
  public void casePatternWrongDefinition() {
    typeCheckModule("\\func test (x : Nat) : Nat => \\case x \\with { zero => 0 | Nat => 1 }", 1);
  }

  @Test
  public void elimPatternWrongDefinition() {
    typeCheckModule("\\func test (x : Nat) : Nat | zero => 0 | Nat => 1", 1);
  }

  @Test
  public void patternWrongDefinition() {
    typeCheckModule(
      "\\data Nat | zero | suc Nat\n" +
      "\\data D (n m : Nat) | d\n" +
      "\\data C | c (n m : Nat) (D n m)\n" +
      "\\data E C \\with | E (c zero (suc zero) d) => e", 1);
  }

  @Test
  public void functionPatternWrongDefinition() {
    typeCheckModule("\\func test (x : Nat) : Nat | zero => 0 | Nat => 1", 1);
  }
}
