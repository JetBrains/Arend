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
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
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
      def.accept(new ClassFieldChecker(thisParameter, def.getData(), def.enclosingClass, superClasses, fields, null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  private Concrete.Expression makeThisClassCall(Object data, Referable classRef) {
    return Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classRef), new Concrete.Coclauses(data, Collections.emptyList()));
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    // Process expressions
    super.visitFunction(def, null);

    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(def.getData(), def.enclosingClass)));
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
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(def.getData(), def.enclosingClass)));
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
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getData(), def.getData(), superClasses, fields, futureFields, myErrorReporter);
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
        classField.getParameters().add(0, new Concrete.TelescopeParameter(classField.getParameters().isEmpty() ? fieldType.getData() : classField.getParameters().get(0).getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(fieldType.getData(), def.getData())));
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
        ((Concrete.ClassFieldImpl) element).implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
      } else if (element instanceof Concrete.OverriddenField) {
        Concrete.OverriddenField field = (Concrete.OverriddenField) element;
        Referable thisParameter = new HiddenLocalReferable("this");
        classFieldChecker.setThisParameter(thisParameter);
        classFieldChecker.visitParameters(field.getParameters(), null);
        field.getParameters().add(0, new Concrete.TelescopeParameter(field.getResultType().getData(), false, Collections.singletonList(thisParameter), makeThisClassCall(field.getResultType().getData(), def.getData())));
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
    Concrete.Pattern newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.ZERO.getReferable(), Collections.emptyList(), n == 0 ? pattern.getAsReferables() : Collections.emptyList());
    boolean isNegative = n < 0;
    n = BaseDefinitionTypechecker.checkNumberInPattern(n, errorReporter, pattern);
    for (int j = 0; j < n; j++) {
      newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.SUC.getReferable(), Collections.singletonList(newPattern), !isNegative && j == n - 1 ? pattern.getAsReferables() : Collections.emptyList());
    }
    if (isNegative) {
      newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.NEG.getReferable(), Collections.singletonList(newPattern), pattern.getAsReferables());
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
}
