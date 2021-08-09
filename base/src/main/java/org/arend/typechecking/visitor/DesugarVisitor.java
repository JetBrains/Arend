package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.LocalError;
import org.arend.ext.error.RedundantCoclauseError;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.FunctionKind;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.concrete.LocalFreeReferableVisitor;
import org.arend.typechecking.error.local.WrongReferable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DesugarVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final ErrorReporter myErrorReporter;

  private DesugarVisitor(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  public static void desugar(Concrete.ResolvableDefinition definition, ErrorReporter errorReporter) {
    definition.accept(new DesugarVisitor(errorReporter), null);
    definition.setDesugarized();
  }

  public static Concrete.Expression desugar(Concrete.Expression expression, ErrorReporter errorReporter) {
    return expression.accept(new DesugarVisitor(errorReporter), null);
  }

  private void getFields(TCDefReferable ref, Set<TCDefReferable> result) {
    Definition def = ref.getTypechecked();
    if (def instanceof ClassDefinition) {
      for (ClassField field : ((ClassDefinition) def).getFields()) {
        result.add(field.getReferable());
      }
    }
  }

  private Referable checkDefinition(Concrete.Definition def) {
    if (def.enclosingClass != null) {
      Set<TCDefReferable> fields = new HashSet<>();
      getFields(def.enclosingClass, fields);
      Definition enclosingClass = def.enclosingClass.getTypechecked();
      List<CoreClassDefinition> superClasses = enclosingClass instanceof ClassDefinition ? Collections.singletonList((CoreClassDefinition) enclosingClass) : Collections.emptyList();

      Referable thisParameter = new HiddenLocalReferable("this");
      def.accept(new ClassFieldChecker(thisParameter, def.getRecursiveDefinitions(), def.enclosingClass, superClasses, fields, null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  private static List<Concrete.LevelExpression> makeIdLevels(Object data, Concrete.LevelParameters parameters) {
    if (parameters == null) return null;
    List<Concrete.LevelExpression> result = new ArrayList<>(parameters.referables.size());
    for (LevelReferable ref : parameters.referables) {
      result.add(new Concrete.IdLevelExpression(data, ref));
    }
    return result;
  }

  private static Concrete.Expression makeThisClassCall(Object data, Referable classRef, Concrete.Definition def) {
    return Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classRef, def != null && def.pOriginalDef == classRef ? makeIdLevels(data, def.getPLevelParameters()) : null, def != null && def.hOriginalDef == classRef ? makeIdLevels(data, def.getHLevelParameters()) : null), new Concrete.Coclauses(data, Collections.emptyList()));
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    // Process expressions
    super.visitFunction(def, null);

    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      if (def instanceof Concrete.CoClauseFunctionDefinition && def.getKind() == FunctionKind.FUNC_COCLAUSE) {
        ((Concrete.CoClauseFunctionDefinition) def).setNumberOfExternalParameters(((Concrete.CoClauseFunctionDefinition) def).getNumberOfExternalParameters() + 1);
      }
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(def.getData(), def.enclosingClass, def)));
      if (def.getBody().getEliminatedReferences().isEmpty()) {
        for (Concrete.FunctionClause clause : def.getBody().getClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter, null));
        }
      }
    }

    return null;
  }

  @Override
  public Void visitData(Concrete.DataDefinition def, Void params) {
    // Process expressions
    super.visitData(def, null);

    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(def.getData(), def.enclosingClass, def)));
      if (def.getEliminatedReferences() != null && def.getEliminatedReferences().isEmpty()) {
        for (Concrete.ConstructorClause clause : def.getConstructorClauses()) {
          clause.getPatterns().add(0, new Concrete.NamePattern(clause.getData(), false, thisParameter, null));
        }
      }
    }

    return null;
  }

  @Override
  public Void visitClass(Concrete.ClassDefinition def, Void params) {
    Set<TCDefReferable> fields = new HashSet<>();
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      if (superClass.getReferent() instanceof TCDefReferable) {
        getFields((TCDefReferable) superClass.getReferent(), fields);
      }
    }

    List<Concrete.ClassField> classFields = new ArrayList<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        classFields.add((Concrete.ClassField) element);
        fields.add(((Concrete.ClassField) element).getData());
      }
    }

    Set<TCDefReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : classFields) {
      futureFields.add(field.getData());
    }

    List<CoreClassDefinition> superClasses = new ArrayList<>();
    for (Concrete.ReferenceExpression superClassRef : def.getSuperClasses()) {
      if (superClassRef.getReferent() instanceof TCDefReferable) {
        Definition superClass = ((TCDefReferable) superClassRef.getReferent()).getTypechecked();
        if (superClass instanceof ClassDefinition) {
          superClasses.add((ClassDefinition) superClass);
        }
      }
    }

    // Check fields
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getRecursiveDefinitions(), def.getData(), superClasses, fields, futureFields, myErrorReporter);
    Concrete.Expression previousType = null;
    for (int i = 0; i < classFields.size(); i++) {
      Concrete.ClassField classField = classFields.get(i);
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new HiddenLocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      if (fieldType == previousType && classField.getParameters().isEmpty()) {
        classField.getParameters().addAll(classFields.get(i - 1).getParameters());
        classField.setResultType(classFields.get(i - 1).getResultType());
        classField.setResultTypeLevel(classFields.get(i - 1).getResultTypeLevel());
      } else {
        previousType = classField.getParameters().isEmpty() ? fieldType : null;
        classFieldChecker.visitParameters(classField.getParameters(), null);
        classField.getParameters().add(0, new Concrete.TelescopeParameter(classField.getParameters().isEmpty() ? fieldType.getData() : classField.getParameters().get(0).getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(fieldType.getData(), def.getData(), null)));
        classField.setResultType(fieldType.accept(classFieldChecker, null));
        if (classField.getResultTypeLevel() != null) {
          classField.setResultTypeLevel(classField.getResultTypeLevel().accept(classFieldChecker, null));
        }
      }
      futureFields.remove(classField.getData());
    }

    // Process expressions
    super.visitClass(def, null);

    // Check implementations
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassFieldImpl && !(element instanceof Concrete.CoClauseFunctionReference)) {
        Concrete.Expression impl = ((Concrete.ClassFieldImpl) element).implementation;
        Referable thisParameter = new HiddenLocalReferable("this");
        classFieldChecker.setThisParameter(thisParameter);
        ((Concrete.ClassFieldImpl) element).implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(impl.getData(), def.getData(), null))), impl.accept(classFieldChecker, null));
      } else if (element instanceof Concrete.OverriddenField) {
        Concrete.OverriddenField field = (Concrete.OverriddenField) element;
        Referable thisParameter = new HiddenLocalReferable("this");
        classFieldChecker.setThisParameter(thisParameter);
        classFieldChecker.visitParameters(field.getParameters(), null);
        field.getParameters().add(0, new Concrete.TelescopeParameter(field.getResultType().getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(field.getResultType().getData(), def.getData(), null)));
        field.setResultType(field.getResultType().accept(classFieldChecker, null));
        if (field.getResultTypeLevel() != null) {
          field.setResultTypeLevel(field.getResultTypeLevel().accept(classFieldChecker, null));
        }
      }
    }

    return null;
  }

  private void visitPatterns(List<Concrete.Pattern> patterns) {
    for (var pattern : patterns) {
      if (pattern instanceof Concrete.TuplePattern) {
        visitPatterns(((Concrete.TuplePattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.ConstructorPattern) {
        visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns());
      }
    }
  }

  public static @NotNull Concrete.Pattern desugarNumberPattern(@NotNull Concrete.NumberPattern pattern, @NotNull ErrorReporter errorReporter) {
    int n = pattern.getNumber();
    Concrete.Pattern newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.ZERO.getReferable(), Collections.emptyList(), n == 0 ? pattern.getAsReferable() : null);
    boolean isNegative = n < 0;
    n = BaseDefinitionTypechecker.checkNumberInPattern(n, errorReporter, pattern);
    for (int j = 0; j < n; j++) {
      newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.SUC.getReferable(), Collections.singletonList(newPattern), !isNegative && j == n - 1 ? pattern.getAsReferable() : null);
    }
    if (isNegative) {
      newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.NEG.getReferable(), Collections.singletonList(newPattern), pattern.getAsReferable());
    }
    if (!pattern.isExplicit()) {
      newPattern.setExplicit(false);
    }
    return newPattern;
  }

  @Override
  protected void visitClause(Concrete.Clause clause, Void params) {
    if (clause.getPatterns() != null) {
      visitPatterns(clause.getPatterns());
    }
    super.visitClause(clause, null);
  }

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, List<? super Concrete.ClassFieldImpl> result) {
    if (classFieldImpl.implementation != null) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
      result.add(classFieldImpl);
    } else {
      boolean ok = true;
      if (classFieldImpl.getImplementedField() instanceof GlobalReferable && ((GlobalReferable) classFieldImpl.getImplementedField()).getKind() == GlobalReferable.Kind.CLASS) {
        if (classFieldImpl.getSubCoclauseList().isEmpty()) {
          myErrorReporter.report(new RedundantCoclauseError(classFieldImpl));
        }
        for (Concrete.ClassFieldImpl subClassFieldImpl : classFieldImpl.getSubCoclauseList()) {
          visitClassFieldImpl(subClassFieldImpl, result);
        }
      } else if (classFieldImpl.classRef != null) {
        visitClassElements(classFieldImpl.getSubCoclauseList(), null);
        Object data = classFieldImpl.getData();
        classFieldImpl.implementation = new Concrete.NewExpression(data, Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classFieldImpl.classRef), new Concrete.Coclauses(data, new ArrayList<>(classFieldImpl.getSubCoclauseList()))));
        if (classFieldImpl.getSubCoclauses() != null) {
          classFieldImpl.getSubCoclauseList().clear();
        }
        result.add(classFieldImpl);
      } else {
        ok = classFieldImpl.getImplementedField() instanceof ErrorReference || classFieldImpl.getImplementedField() instanceof UnresolvedReference;
      }

      if (!ok) {
        LocalError error = new WrongReferable("Expected either a class or a field which has a class as its type", classFieldImpl.getImplementedField(), classFieldImpl);
        myErrorReporter.report(error);
        classFieldImpl.implementation = new Concrete.ErrorHoleExpression(classFieldImpl.getData(), error);
        result.add(classFieldImpl);
      }
    }
  }

  @Override
  protected <T extends Concrete.ClassElement> void visitClassElements(List<T> elements, Void params) {
    if (elements.isEmpty()) {
      return;
    }

    List<T> originalElements = new ArrayList<>(elements);
    elements.clear();
    for (T element : originalElements) {
      if (element instanceof Concrete.ClassFieldImpl) {
        //noinspection unchecked
        visitClassFieldImpl((Concrete.ClassFieldImpl) element, (List<Concrete.ClassFieldImpl>) elements);
      } else {
        visitClassElement(element, null);
        elements.add(element);
      }
    }
  }

  private static boolean hasIdp(List<? extends Concrete.Pattern> patterns) {
    for (Concrete.Pattern pattern : patterns) {
      if (pattern != null && (pattern instanceof Concrete.ConstructorPattern && ((Concrete.ConstructorPattern) pattern).getConstructor() == Prelude.IDP.getRef() || hasIdp(pattern.getPatterns()))) {
        return true;
      }
    }
    return false;
  }

  private static boolean onlyTuples(List<? extends Concrete.Pattern> patterns) {
    for (Concrete.Pattern pattern : patterns) {
      if (pattern != null && (!(pattern instanceof Concrete.NamePattern) && !(pattern instanceof Concrete.TuplePattern) || !onlyTuples(pattern.getPatterns()))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Concrete.Expression visitLam(Concrete.LamExpression expr, Void params) {
    if (!(expr instanceof Concrete.PatternLamExpression)) {
      return super.visitLam(expr, params);
    }

    int i = 0;
    int j = 0;
    List<Concrete.Parameter> newParams = new ArrayList<>();
    Concrete.Expression body = expr.body.accept(this, null);
    if (onlyTuples(((Concrete.PatternLamExpression) expr).getPatterns())) {
      List<Concrete.LetClause> clauses = new ArrayList<>();
      for (Concrete.Pattern pattern : ((Concrete.PatternLamExpression) expr).getPatterns()) {
        if (pattern == null) {
          Concrete.Parameter param = expr.getParameters().get(i++);
          visitParameter(param, null);
          newParams.add(param);
          continue;
        }

        Referable ref = pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).getRef() : null;
        if (ref == null && pattern.getAsReferable() != null) ref = pattern.getAsReferable().referable;
        if (ref == null) ref = new LocalReferable("p" + j++);
        Concrete.Expression type = pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).type : pattern.getAsReferable() != null ? pattern.getAsReferable().type : null;
        newParams.add(type != null ? new Concrete.TelescopeParameter(pattern.getData(), pattern.isExplicit(), Collections.singletonList(ref), type.accept(this, null)) : new Concrete.NameParameter(pattern.getData(), pattern.isExplicit(), ref));
        pattern.setExplicit(true);
        clauses.add(new Concrete.LetClause(pattern, null, new Concrete.ReferenceExpression(pattern.getData(), ref)));
      }
      return new Concrete.LamExpression(expr.getData(), newParams, new Concrete.LetExpression(expr.getData(), false, false, clauses, body));
    }

    boolean genLambda = !hasIdp(((Concrete.PatternLamExpression) expr).getPatterns());

    List<Concrete.Pattern> newPatterns = new ArrayList<>();
    List<Concrete.CaseArgument> caseArgs = new ArrayList<>();
    for (Concrete.Pattern pat : ((Concrete.PatternLamExpression) expr).getPatterns()) {
      List<Concrete.Pattern> patterns;
      if (pat == null) {
        Concrete.Parameter param = expr.getParameters().get(i++);
        if (genLambda && caseArgs.isEmpty()) {
          visitParameter(param, null);
          newParams.add(param);
          continue;
        }
        patterns = new ArrayList<>();
        for (Referable referable : param.getReferableList()) {
          patterns.add(new Concrete.NamePattern(param.getData(), param.isExplicit(), referable, param.getType()));
        }
      } else {
        patterns = Collections.singletonList(pat);
      }

      for (Concrete.Pattern pattern : patterns) {
        Referable ref = pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).getRef() : null;
        if (ref == null && pattern.getAsReferable() != null) ref = pattern.getAsReferable().referable;
        if (ref == null) ref = new LocalReferable("p" + j++);
        Concrete.Expression type = pattern instanceof Concrete.NamePattern ? ((Concrete.NamePattern) pattern).type : pattern.getAsReferable() != null ? pattern.getAsReferable().type : null;
        newParams.add(type != null ? new Concrete.TelescopeParameter(pattern.getData(), pattern.isExplicit(), Collections.singletonList(ref), type.accept(this, null)) : new Concrete.NameParameter(pattern.getData(), pattern.isExplicit(), ref));
        caseArgs.add(new Concrete.CaseArgument(new Concrete.ReferenceExpression(pattern.getData(), ref), null));
        pattern.setExplicit(true);
        newPatterns.add(pattern);
      }
    }

    return caseArgs.isEmpty() ? new Concrete.LamExpression(expr.getData(), newParams, body) : new Concrete.LamExpression(expr.getData(), newParams, new Concrete.CaseExpression(expr.getData(), false, caseArgs, null, null, Collections.singletonList(new Concrete.FunctionClause(expr.getData(), newPatterns, body instanceof Concrete.IncompleteExpression ? null : body))));
  }

  private static boolean isTuplePattern(Concrete.Pattern pattern) {
    if (!(pattern instanceof Concrete.NamePattern || pattern instanceof Concrete.TuplePattern)) {
      return false;
    }
    if (pattern instanceof Concrete.TuplePattern && pattern.getPatterns().isEmpty()) {
      return false;
    }
    for (Concrete.Pattern subpattern : pattern.getPatterns()) {
      if (!isTuplePattern(subpattern)) {
        return false;
      }
    }
    return true;
  }

  private static void collectRefs(Concrete.Pattern pattern, Set<Referable> refs) {
    if (pattern instanceof Concrete.NamePattern) {
      Referable ref = ((Concrete.NamePattern) pattern).getRef();
      if (ref != null) refs.add(ref);
    }
    for (Concrete.Pattern subpattern : pattern.getPatterns()) {
      collectRefs(subpattern, refs);
    }
  }

  private Concrete.Expression desugarLet(Object data, boolean isHave, boolean isStrict, List<Concrete.LetClause> clauses, Concrete.Expression body) {
    for (int i = 0; i < clauses.size(); i++) {
      Concrete.LetClause clause = clauses.get(i);
      if (isTuplePattern(clause.getPattern())) {
        continue;
      }

      Set<Referable> asRefs = new HashSet<>();
      Set<Referable> refs = new HashSet<>();
      collectRefs(clause.getPattern(), refs);
      if (clause.getPattern().getAsReferable() != null) {
        Referable ref = clause.getPattern().getAsReferable().referable;
        if (ref != null) asRefs.add(ref);
      }
      int j = i + 1;
      for (; j < clauses.size(); j++) {
        Concrete.LetClause curClause = clauses.get(j);
        if (isTuplePattern(curClause.getPattern())) {
          if (asRefs.isEmpty() || curClause.resultType == null) break;
          LocalFreeReferableVisitor visitor = new LocalFreeReferableVisitor(asRefs);
          curClause.resultType.accept(visitor, null);
          if (visitor.getFound() == null) break;
        }
        if (!refs.isEmpty()) {
          LocalFreeReferableVisitor visitor = new LocalFreeReferableVisitor(refs);
          curClause.term.accept(visitor, null);
          if (visitor.getFound() != null) break;
        }
        collectRefs(curClause.getPattern(), refs);
        if (curClause.getPattern().getAsReferable() != null) {
          Referable ref = curClause.getPattern().getAsReferable().referable;
          if (ref != null) asRefs.add(ref);
        }
      }
      Concrete.Expression newBody = j < clauses.size() ? desugarLet(data, isHave, isStrict, clauses.subList(j, clauses.size()), body) : body;
      List<Concrete.CaseArgument> caseArgs = new ArrayList<>();
      List<Concrete.Pattern> patterns = new ArrayList<>();
      for (int k = i; k < j; k++) {
        Concrete.LetClause curClause = clauses.get(k);
        boolean isElim = curClause.term instanceof Concrete.ReferenceExpression && curClause.getPattern().getAsReferable() == null;
        if (isElim) {
          LocalFreeReferableVisitor visitor = new LocalFreeReferableVisitor(Collections.singleton(((Concrete.ReferenceExpression) curClause.term).getReferent()));
          for (int m = k + 1; m < j; m++) {
            if (clauses.get(m).resultType != null) {
              clauses.get(m).resultType.accept(visitor, null);
              if (visitor.getFound() != null) {
                isElim = false;
                break;
              }
            }
          }
        }
        caseArgs.add(isElim ? new Concrete.CaseArgument((Concrete.ReferenceExpression) curClause.term, curClause.resultType) : new Concrete.CaseArgument(curClause.term, curClause.getPattern().getAsReferable() == null ? null : curClause.getPattern().getAsReferable().referable, curClause.resultType));
        patterns.add(curClause.getPattern());
      }
      newBody = new Concrete.CaseExpression(data, false, caseArgs, null, null, Collections.singletonList(new Concrete.FunctionClause(data, patterns, newBody instanceof Concrete.IncompleteExpression ? null : newBody)));
      return i > 0 ? new Concrete.LetExpression(data, isHave, isStrict, clauses.subList(0, i), newBody) : newBody;
    }
    return new Concrete.LetExpression(data, isHave, isStrict, clauses, body);
  }

  @Override
  public Concrete.Expression visitLet(Concrete.LetExpression expr, Void params) {
    for (Concrete.LetClause clause : expr.getClauses()) {
      visitLetClause(clause, null);
    }
    return desugarLet(expr.getData(), expr.isHave(), expr.isStrict(), expr.getClauses(), expr.getExpression().accept(this, null));
  }
}
