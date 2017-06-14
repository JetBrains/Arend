package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.BindingPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.TypeCheckingTestCase;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.util.Pair;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.Pi;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.param;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.params;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Interval;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.*;

public class PatternTest extends TypeCheckingTestCase {
  private boolean checkPatterns(List<? extends Abstract.Pattern> patternArgs, List<Pattern> patterns, Map<Abstract.ReferableSourceNode, Binding> expected, Map<Abstract.ReferableSourceNode, Binding> actual, boolean hasImplicit) {
    int i = 0, j = 0;
    for (; i < patternArgs.size() && j < patterns.size(); i++, j++) {
      Abstract.Pattern pattern1 = patternArgs.get(i);
      if (pattern1 instanceof Abstract.EmptyPattern) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof com.jetbrains.jetpad.vclang.core.elimtree.EmptyPattern);
        for (j++; j < patterns.size(); j++) {
          assertTrue(patterns.get(j) instanceof BindingPattern);
        }
        return false;
      } else
      if (pattern1 instanceof Abstract.NamePattern) {
        Abstract.ReferableSourceNode referable = ((Abstract.NamePattern) pattern1).getReferent();
        while (hasImplicit && patterns.get(j) instanceof BindingPattern && expected.get(referable) != ((BindingPattern) patterns.get(j)).getBinding()) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof BindingPattern);
        actual.put(referable, ((BindingPattern) patterns.get(j)).getBinding());
      } else
      if (pattern1 instanceof Abstract.ConstructorPattern) {
        while (hasImplicit && patterns.get(j) instanceof BindingPattern) {
          j++;
        }
        assertTrue(patterns.get(j) instanceof ConstructorPattern);

        Abstract.ConstructorPattern conPattern1 = (Abstract.ConstructorPattern) pattern1;
        ConstructorPattern conPattern2 = (ConstructorPattern) patterns.get(j);
        assertEquals(conPattern1.getConstructor(), conPattern2.getConstructor().getAbstractDefinition());
        checkPatterns(conPattern1.getArguments(), conPattern2.getPatterns(), expected, actual, hasImplicit);
      } else {
        throw new IllegalStateException();
      }
    }

    assertEquals(patternArgs.size(), i);
    assertEquals(patterns.size(), j);
    return true;
  }

  private void checkPatterns(List<? extends Abstract.Pattern> patternArgs, List<Pattern> patterns, Map<Abstract.ReferableSourceNode, Binding> expected, boolean hasImplicit) {
    Map<Abstract.ReferableSourceNode, Binding> actual = new HashMap<>();
    boolean withoutEmpty = checkPatterns(patternArgs, patterns, expected, actual, hasImplicit);
    assertEquals(expected, withoutEmpty ? actual : null);
  }

  @Test
  public void threeVars() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n m k : Nat) <= \\elim n, m, k\n" +
      "  | suc n, zero, suc k => k");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void nestedPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n m k : Nat) <= \\elim n, m, k\n" +
      "  | suc (suc (suc n)), zero, suc (suc (suc (suc zero))) => n");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void incorrectType() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n : Nat) (m : Nat -> Nat) (k : Nat) <= \\elim n, m, k\n" +
      "  | suc n, zero, suc k => k");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Pi(Nat(), Nat())), param(null, Nat())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void incorrectDataType() {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\data D | con\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) <= \\elim n, d, k\n" +
      "  | suc n, zero, suc k => k");
    Abstract.DataDefinition dataDef = (Abstract.DataDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(0)).getDefinition();
    Abstract.FunctionDefinition funDef = (Abstract.FunctionDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) funDef.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), new ProxyErrorReporter(funDef, errorReporter), funDef.getTerm(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void tooManyPatterns() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n m k : Nat) <= \\elim n, m, k\n" +
      "  | suc n m, zero, suc k => k");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Nat()), param(null, Nat())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void interval() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n : Nat) (i : I) <= \\elim n, i\n" +
      "  | zero, i => zero");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Interval())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void intervalFail() {
    Concrete.FunctionDefinition fun = (Concrete.FunctionDefinition) resolveNamesDef(
      "\\function f (n : Nat) (i : I) <= \\elim n, i\n" +
      "  | zero, left => zero");
    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) fun.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, Interval())), new ProxyErrorReporter(fun, errorReporter), fun.getTerm(), false);
    assertNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
  }

  @Test
  public void emptyDataType() {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\data D\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) <= \\elim n, d, k\n" +
      "  | suc n, (), k => k");
    Abstract.DataDefinition dataDef = (Abstract.DataDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(0)).getDefinition();
    Abstract.FunctionDefinition funDef = (Abstract.FunctionDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) funDef.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), new ProxyErrorReporter(funDef, errorReporter), funDef.getTerm(), false);
    assertNotNull(res);
    assertEquals(0, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }

  @Test
  public void emptyDataTypeWarning() {
    Concrete.ClassDefinition classDef = resolveNamesClass(
      "\\data D\n" +
      "\\function f (n : Nat) (d : D) (k : Nat) <= \\elim n, d, k\n" +
      "  | suc n, (), suc k => k");
    Abstract.DataDefinition dataDef = (Abstract.DataDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(0)).getDefinition();
    Abstract.FunctionDefinition funDef = (Abstract.FunctionDefinition) ((Abstract.DefineStatement) classDef.getGlobalStatements().get(1)).getDefinition();
    DataDefinition data = new DataDefinition(dataDef);
    data.setParameters(EmptyDependentLink.getInstance());
    data.setSort(Sort.STD);
    data.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);

    List<Abstract.Pattern> patternsArgs = ((Abstract.ElimExpression) funDef.getTerm()).getClauses().get(0).getPatterns().stream().map(pattern -> (Concrete.Pattern) pattern).collect(Collectors.toList());
    Pair<List<Pattern>, Map<Abstract.ReferableSourceNode, Binding>> res = TypecheckPattern.typecheckPatternArguments(patternsArgs, params(param(null, Nat()), param(null, new DataCallExpression(data, Sort.STD, Collections.emptyList())), param(null, Nat())), new ProxyErrorReporter(funDef, errorReporter), funDef.getTerm(), false);
    assertNotNull(res);
    assertEquals(1, errorReporter.getErrorList().size());
    checkPatterns(patternsArgs, res.proj1, res.proj2, false);
  }
}
