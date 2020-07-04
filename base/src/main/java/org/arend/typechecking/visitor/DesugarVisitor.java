package org.arend.typechecking.visitor;

import org.arend.core.definition.ClassDefinition;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Definition;
import org.arend.core.expr.ClassCallExpression;
import org.arend.ext.core.definition.CoreClassDefinition;
import org.arend.ext.error.ArgumentExplicitnessError;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.LocalError;
import org.arend.ext.error.TypecheckingError;
import org.arend.naming.reference.*;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.local.CertainTypecheckingError;
import org.arend.typechecking.error.local.WrongReferable;
import org.arend.typechecking.error.local.inference.ArgInferenceError;

import java.util.*;

public class DesugarVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final TypecheckerState myTypecheckerState;
  private final ErrorReporter myErrorReporter;

  private DesugarVisitor(TypecheckerState typecheckerState, ErrorReporter errorReporter) {
    myTypecheckerState = typecheckerState;
    myErrorReporter = errorReporter;
  }

  public static void desugar(Concrete.Definition definition, TypecheckerState typecheckerState, ErrorReporter errorReporter) {
    definition.accept(new DesugarVisitor(typecheckerState, errorReporter), null);
    definition.setDesugarized();
  }

  public static Concrete.Expression desugar(Concrete.Expression expression, TypecheckerState typecheckerState, ErrorReporter errorReporter) {
    return expression.accept(new DesugarVisitor(typecheckerState, errorReporter), null);
  }

  private void getFields(TCReferable ref, Set<TCReferable> result) {
    Definition def = myTypecheckerState.getTypechecked(ref);
    if (def instanceof ClassDefinition) {
      for (ClassField field : ((ClassDefinition) def).getFields()) {
        result.add(field.getReferable());
      }
    }
  }

  private Referable checkDefinition(Concrete.Definition def) {
    if (def.enclosingClass != null) {
      Set<TCReferable> fields = new HashSet<>();
      getFields(def.enclosingClass, fields);
      Definition enclosingClass = myTypecheckerState.getTypechecked(def.enclosingClass);
      List<CoreClassDefinition> superClasses = enclosingClass instanceof ClassDefinition ? Collections.singletonList((CoreClassDefinition) enclosingClass) : Collections.emptyList();

      Referable thisParameter = new HiddenLocalReferable("this");
      def.accept(new ClassFieldChecker(thisParameter, def.getData(), def.enclosingClass, superClasses, myTypecheckerState, fields, null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  @Override
  public Void visitFunction(Concrete.BaseFunctionDefinition def, Void params) {
    // Process expressions
    super.visitFunction(def, null);

    // Add this parameter
    Referable thisParameter = checkDefinition(def);
    if (thisParameter != null) {
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
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
      def.getParameters().add(0, new Concrete.TelescopeParameter(def.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(def.getData(), def.enclosingClass)));
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
    Set<TCReferable> fields = new HashSet<>();
    for (Concrete.ReferenceExpression superClass : def.getSuperClasses()) {
      if (superClass.getReferent() instanceof TCReferable) {
        getFields((TCReferable) superClass.getReferent(), fields);
      }
    }

    List<Concrete.ClassField> classFields = new ArrayList<>();
    for (Concrete.ClassElement element : def.getElements()) {
      if (element instanceof Concrete.ClassField) {
        classFields.add((Concrete.ClassField) element);
        fields.add(((Concrete.ClassField) element).getData());
      }
    }

    Set<TCReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : classFields) {
      futureFields.add(field.getData());
    }

    List<CoreClassDefinition> superClasses = new ArrayList<>();
    for (Concrete.ReferenceExpression superClassRef : def.getSuperClasses()) {
      if (superClassRef.getReferent() instanceof TCReferable) {
        Definition superClass = myTypecheckerState.getTypechecked((TCReferable) superClassRef.getReferent());
        if (superClass instanceof ClassDefinition) {
          superClasses.add((ClassDefinition) superClass);
        }
      }
    }

    // Check fields
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getData(), def.getData(), superClasses, myTypecheckerState, fields, futureFields, myErrorReporter);
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
        classField.getParameters().add(0, new Concrete.TelescopeParameter(classField.getParameters().isEmpty() ? fieldType.getData() : classField.getParameters().get(0).getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData())));
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
      if (element instanceof Concrete.ClassFieldImpl) {
        Concrete.Expression impl = ((Concrete.ClassFieldImpl) element).implementation;
        Referable thisParameter = new HiddenLocalReferable("this");
        classFieldChecker.setThisParameter(thisParameter);
        ((Concrete.ClassFieldImpl) element).implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
      } else if (element instanceof Concrete.OverriddenField) {
        Concrete.OverriddenField field = (Concrete.OverriddenField) element;
        Referable thisParameter = new HiddenLocalReferable("this");
        classFieldChecker.setThisParameter(thisParameter);
        classFieldChecker.visitParameters(field.getParameters(), null);
        field.getParameters().add(0, new Concrete.TelescopeParameter(field.getResultType().getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(field.getResultType().getData(), def.getData())));
        field.setResultType(field.getResultType().accept(classFieldChecker, null));
        if (field.getResultTypeLevel() != null) {
          field.setResultTypeLevel(field.getResultTypeLevel().accept(classFieldChecker, null));
        }
      }
    }

    return null;
  }

  private void getNotImplementedFields(ClassDefinition origClassDef, ClassDefinition classDef, List<ClassField> result, Set<ClassDefinition> visited) {
    if (!visited.add(classDef)) {
      return;
    }

    for (ClassDefinition superClass : classDef.getSuperClasses()) {
      getNotImplementedFields(origClassDef, superClass, result, visited);
    }
    for (ClassField field : classDef.getPersonalFields()) {
      if (!origClassDef.isImplemented(field)) {
        result.add(field);
      }
    }
  }

  private Concrete.Expression visitApp(Concrete.ReferenceExpression fun, List<Concrete.Argument> arguments, Concrete.Expression expr, boolean inferTailImplicits) {
    Referable ref = fun.getReferent();
    if (!(ref instanceof TCReferable)) {
      return expr;
    }
    Definition def = myTypecheckerState.getTypechecked((TCReferable) ref);
    if (!(def instanceof ClassDefinition)) {
      return expr;
    }
    ClassDefinition classDef = (ClassDefinition) def;

    // Convert class call with arguments to class extension.
    List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>();
    List<ClassField> notImplementedFields = new ArrayList<>();
    getNotImplementedFields(classDef, classDef, notImplementedFields, new HashSet<>());
    int j = 0;
    for (int i = 0; i < arguments.size(); i++, j++) {
      if (j >= notImplementedFields.size()) {
        myErrorReporter.report(new TypecheckingError("Too many arguments. Class '" + ref.textRepresentation() + "' " + (notImplementedFields.isEmpty() ? "does not have fields" : "has only " + ArgInferenceError.number(notImplementedFields.size(), "field")), arguments.get(i).expression));
        break;
      }

      ClassField field = notImplementedFields.get(j);
      boolean fieldExplicit = field.getReferable().isExplicitField();
      if (fieldExplicit && !arguments.get(i).isExplicit()) {
        myErrorReporter.report(new ArgumentExplicitnessError(true, arguments.get(i).expression));
        while (i < arguments.size() && !arguments.get(i).isExplicit()) {
          i++;
        }
        if (i == arguments.size()) {
          break;
        }
      }

      Concrete.Expression argument = arguments.get(i).expression;
      if (fieldExplicit == arguments.get(i).isExplicit()) {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), field.getReferable(), argument, Collections.emptyList()));
      } else {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), field.getReferable(), new Concrete.HoleExpression(argument.getData()), Collections.emptyList()));
        i--;
      }
    }

    if (inferTailImplicits) {
      for (; j < notImplementedFields.size(); j++) {
        ClassField field = notImplementedFields.get(j);
        if (field.getReferable().isExplicitField() || !field.getReferable().isParameterField()) {
          break;
        }
        if (!(field.getResultType() instanceof ClassCallExpression) || ((ClassCallExpression) field.getResultType()).getDefinition().isRecord()) {
          break;
        }

        Object data = arguments.isEmpty() ? fun.getData() : arguments.get(arguments.size() - 1).getExpression().getData();
        classFieldImpls.add(new Concrete.ClassFieldImpl(data, field.getReferable(), new Concrete.HoleExpression(data), Collections.emptyList()));
      }
    }

    return classFieldImpls.isEmpty() ? fun : Concrete.ClassExtExpression.make(expr.getData(), fun, classFieldImpls);
  }

  @Override
  public Concrete.Expression visitApplyHole(Concrete.ApplyHoleExpression expr, Void params) {
    return expr;
  }

  @Override
  public Concrete.Expression visitReference(Concrete.ReferenceExpression expr, Void params) {
    return visitApp(expr, Collections.emptyList(), expr, true);
  }

  @Override
  public Concrete.Expression visitApp(Concrete.AppExpression expr, Void params) {
    if (!(expr.getFunction() instanceof Concrete.ReferenceExpression)) {
      return super.visitApp(expr, null);
    }

    for (Concrete.Argument argument : expr.getArguments()) {
      argument.expression = argument.expression.accept(this, null);
    }
    return visitApp((Concrete.ReferenceExpression) expr.getFunction(), expr.getArguments(), expr, true);
  }

  @Override
  public Concrete.Expression visitClassExt(Concrete.ClassExtExpression expr, Void params) {
    Concrete.Expression classExpr = expr.getBaseClassExpression();
    if (classExpr instanceof Concrete.ReferenceExpression) {
      visitClassElements(expr.getStatements(), null);
      return expr;
    }
    if (classExpr instanceof Concrete.AppExpression) {
      Concrete.AppExpression appExpr = (Concrete.AppExpression) classExpr;
      if (appExpr.getFunction() instanceof Concrete.ReferenceExpression) {
        for (Concrete.Argument argument : appExpr.getArguments()) {
          argument.expression = argument.expression.accept(this, null);
        }
        expr.setBaseClassExpression(visitApp((Concrete.ReferenceExpression) appExpr.getFunction(), appExpr.getArguments(), appExpr, false));
      } else {
        expr.setBaseClassExpression(super.visitApp(appExpr, null));
      }
      visitClassElements(expr.getStatements(), null);
      return expr;
    }
    return super.visitClassExt(expr, params);
  }

  private void visitPatterns(List<Concrete.Pattern> patterns) {
    for (int i = 0; i < patterns.size(); i++) {
      Concrete.Pattern pattern = patterns.get(i);
      if (pattern instanceof Concrete.TuplePattern) {
        visitPatterns(((Concrete.TuplePattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.ConstructorPattern) {
        visitPatterns(((Concrete.ConstructorPattern) pattern).getPatterns());
      } else if (pattern instanceof Concrete.NumberPattern) {
        int n = ((Concrete.NumberPattern) pattern).getNumber();
        Concrete.Pattern newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.ZERO.getReferable(), Collections.emptyList(), n == 0 ? pattern.getAsReferables() : Collections.emptyList());
        boolean isNegative = n < 0;
        n = BaseDefinitionTypechecker.checkNumberInPattern(n, myErrorReporter, pattern);
        for (int j = 0; j < n; j++) {
          newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.SUC.getReferable(), Collections.singletonList(newPattern), !isNegative && j == n - 1 ? pattern.getAsReferables() : Collections.emptyList());
        }
        if (isNegative) {
          newPattern = new Concrete.ConstructorPattern(pattern.getData(), true, Prelude.NEG.getReferable(), Collections.singletonList(newPattern), pattern.getAsReferables());
        }
        if (!pattern.isExplicit()) {
          newPattern.setExplicit(false);
        }
        patterns.set(i, newPattern);
      }
    }
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
        if (classFieldImpl.subClassFieldImpls.isEmpty()) {
          myErrorReporter.report(new CertainTypecheckingError(CertainTypecheckingError.Kind.REDUNDANT_COCLAUSE, classFieldImpl));
        }
        for (Concrete.ClassFieldImpl subClassFieldImpl : classFieldImpl.subClassFieldImpls) {
          visitClassFieldImpl(subClassFieldImpl, result);
        }
      } else if (classFieldImpl.classRef != null) {
        visitClassElements(classFieldImpl.subClassFieldImpls, null);
        Object data = classFieldImpl.getData();
        classFieldImpl.implementation = new Concrete.NewExpression(data, Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classFieldImpl.classRef), new ArrayList<>(classFieldImpl.subClassFieldImpls)));
        classFieldImpl.subClassFieldImpls.clear();
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
