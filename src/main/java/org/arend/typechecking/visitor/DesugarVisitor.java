package org.arend.typechecking.visitor;

import org.arend.error.Error;
import org.arend.naming.reference.*;
import org.arend.naming.scope.ClassFieldImplScope;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.ArgInferenceError;
import org.arend.typechecking.error.local.DesugaringError;
import org.arend.typechecking.error.local.LocalError;
import org.arend.typechecking.error.local.WrongReferable;
import org.arend.typechecking.typecheckable.provider.ConcreteProvider;

import java.util.*;

public class DesugarVisitor extends BaseConcreteExpressionVisitor<Void> {
  private final ConcreteProvider myConcreteProvider;
  private final LocalErrorReporter myErrorReporter;

  private DesugarVisitor(ConcreteProvider concreteProvider, LocalErrorReporter errorReporter) {
    myConcreteProvider = concreteProvider;
    myErrorReporter = errorReporter;
  }

  public static void desugar(Concrete.Definition definition, ConcreteProvider concreteProvider, LocalErrorReporter errorReporter) {
    definition.accept(new DesugarVisitor(concreteProvider, errorReporter), null);
    definition.setDesugarized();
  }

  private Set<LocatedReferable> getClassFields(ClassReferable classRef) {
    Set<LocatedReferable> fields = new HashSet<>();
    new ClassFieldImplScope(classRef, false).find(ref -> {
      if (ref instanceof LocatedReferable) {
        fields.add((LocatedReferable) ref);
      }
      return false;
    });
    return fields;
  }

  private Referable checkDefinition(Concrete.Definition def) {
    if (def.enclosingClass != null) {
      Referable thisParameter = new HiddenLocalReferable("this");
      def.accept(new ClassFieldChecker(thisParameter, def.enclosingClass, myConcreteProvider, getClassFields(def.enclosingClass), null, myErrorReporter), null);
      return thisParameter;
    } else {
      return null;
    }
  }

  @Override
  public Void visitFunction(Concrete.FunctionDefinition def, Void params) {
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
    Set<LocatedReferable> fields = getClassFields(def.getData());

    Set<TCReferable> futureFields = new HashSet<>();
    for (Concrete.ClassField field : def.getFields()) {
      futureFields.add(field.getData());
    }

    // Check fields
    ClassFieldChecker classFieldChecker = new ClassFieldChecker(null, def.getData(), myConcreteProvider, fields, futureFields, myErrorReporter);
    Concrete.Expression previousType = null;
    for (int i = 0; i < def.getFields().size(); i++) {
      Concrete.ClassField classField = def.getFields().get(i);
      Concrete.Expression fieldType = classField.getResultType();
      Referable thisParameter = new HiddenLocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      if (fieldType == previousType && classField.getParameters().isEmpty()) {
        classField.getParameters().addAll(def.getFields().get(i - 1).getParameters());
        classField.setResultType(def.getFields().get(i - 1).getResultType());
        classField.setResultTypeLevel(def.getFields().get(i - 1).getResultTypeLevel());
      } else {
        previousType = classField.getParameters().isEmpty() ? fieldType : null;
        classFieldChecker.visitParameters(classField.getParameters(), null);
        classField.getParameters().add(0, new Concrete.TelescopeParameter(fieldType.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(fieldType.getData(), def.getData())));
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
    for (Concrete.ClassFieldImpl classFieldImpl : def.getImplementations()) {
      Concrete.Expression impl = classFieldImpl.implementation;
      Referable thisParameter = new HiddenLocalReferable("this");
      classFieldChecker.setThisParameter(thisParameter);
      classFieldImpl.implementation = new Concrete.LamExpression(impl.getData(), Collections.singletonList(new Concrete.TelescopeParameter(impl.getData(), false, Collections.singletonList(thisParameter), new Concrete.ReferenceExpression(impl.getData(), def.getData()))), impl.accept(classFieldChecker, null));
    }

    return null;
  }

  private Concrete.Expression visitApp(Concrete.ReferenceExpression fun, List<Concrete.Argument> arguments, Concrete.Expression expr, boolean inferTailImplicits) {
    Referable ref = fun.getReferent();
    if (!(ref instanceof ClassReferable)) {
      return expr;
    }

    // Convert class call with arguments to class extension.
    List<Concrete.ClassFieldImpl> classFieldImpls = new ArrayList<>();
    Set<FieldReferable> notImplementedFields = ClassReferable.Helper.getNotImplementedFields((ClassReferable) ref);
    Iterator<FieldReferable> it = notImplementedFields.iterator();
    for (int i = 0; i < arguments.size(); i++) {
      if (!it.hasNext()) {
        myErrorReporter.report(new DesugaringError("Too many arguments. Class '" + ref.textRepresentation() + "' " + (notImplementedFields.isEmpty() ? "does not have fields" : "has only " + ArgInferenceError.number(notImplementedFields.size(), "field")), arguments.get(i).expression));
        break;
      }

      FieldReferable fieldRef = it.next();
      boolean fieldExplicit = fieldRef.isExplicitField();
      if (fieldExplicit && !arguments.get(i).isExplicit()) {
        myErrorReporter.report(new DesugaringError("Expected an explicit argument", arguments.get(i).expression));
        while (i < arguments.size() && !arguments.get(i).isExplicit()) {
          i++;
        }
        if (i == arguments.size()) {
          break;
        }
      }

      Concrete.Expression argument = arguments.get(i).expression;
      if (fieldExplicit == arguments.get(i).isExplicit()) {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), fieldRef, argument, Collections.emptyList()));
      } else {
        classFieldImpls.add(new Concrete.ClassFieldImpl(argument.getData(), fieldRef, new Concrete.HoleExpression(argument.getData()), Collections.emptyList()));
        i--;
      }
    }

    if (inferTailImplicits) {
      while (it.hasNext()) {
        FieldReferable fieldRef = it.next();
        if (fieldRef.isExplicitField() || !fieldRef.isParameterField()) {
          break;
        }
        ClassReferable classRef = fieldRef.getTypeClassReference();
        if (classRef == null || classRef.isRecord()) {
          break;
        }

        Object data = arguments.isEmpty() ? fun.getData() : arguments.get(arguments.size() - 1).getExpression().getData();
        classFieldImpls.add(new Concrete.ClassFieldImpl(data, fieldRef, new Concrete.HoleExpression(data), Collections.emptyList()));
      }
    }

    return classFieldImpls.isEmpty() ? fun : Concrete.ClassExtExpression.make(expr.getData(), fun, classFieldImpls);
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
      visitClassFieldImpls(expr.getStatements(), null);
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
      visitClassFieldImpls(expr.getStatements(), null);
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
        if (isNegative) {
          n = -n;
        }
        if (n > Concrete.NumberPattern.MAX_VALUE) {
          n = Concrete.NumberPattern.MAX_VALUE;
        }
        if (n == Concrete.NumberPattern.MAX_VALUE) {
          myErrorReporter.report(new DesugaringError("Value too big", pattern));
        }
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

  private void visitClassFieldImpl(Concrete.ClassFieldImpl classFieldImpl, List<Concrete.ClassFieldImpl> result) {
    if (classFieldImpl.implementation != null) {
      classFieldImpl.implementation = classFieldImpl.implementation.accept(this, null);
      result.add(classFieldImpl);
    } else {
      boolean ok = true;
      if (classFieldImpl.getImplementedField() instanceof ClassReferable) {
        if (classFieldImpl.subClassFieldImpls.isEmpty()) {
          myErrorReporter.report(new DesugaringError(Error.Level.WEAK_WARNING, DesugaringError.Kind.REDUNDANT_COCLAUSE, classFieldImpl));
        }
        for (Concrete.ClassFieldImpl subClassFieldImpl : classFieldImpl.subClassFieldImpls) {
          visitClassFieldImpl(subClassFieldImpl, result);
        }
      } else if (classFieldImpl.getImplementedField() instanceof TypedReferable) {
        ClassReferable classRef = ((TypedReferable) classFieldImpl.getImplementedField()).getTypeClassReference();
        if (classRef != null) {
          visitClassFieldImpls(classFieldImpl.subClassFieldImpls, null);
          Object data = classFieldImpl.getData();
          classFieldImpl.implementation = new Concrete.NewExpression(data, Concrete.ClassExtExpression.make(data, new Concrete.ReferenceExpression(data, classRef), new ArrayList<>(classFieldImpl.subClassFieldImpls)));
          classFieldImpl.subClassFieldImpls.clear();
          result.add(classFieldImpl);
        } else {
          ok = false;
        }
      } else {
        ok = classFieldImpl.getImplementedField() instanceof ErrorReference || classFieldImpl.getImplementedField() instanceof UnresolvedReference;
      }

      if (!ok) {
        LocalError error = new WrongReferable("Expected either a class or a field which has a class as its type", classFieldImpl.getImplementedField(), false, classFieldImpl);
        myErrorReporter.report(error);
        classFieldImpl.implementation = new Concrete.ErrorHoleExpression(classFieldImpl.getData(), error);
        result.add(classFieldImpl);
      }
    }
  }

  @Override
  protected void visitClassFieldImpls(List<Concrete.ClassFieldImpl> classFieldImpls, Void params) {
    if (classFieldImpls.isEmpty()) {
      return;
    }
    List<Concrete.ClassFieldImpl> originalClassFieldImpls = new ArrayList<>(classFieldImpls);
    classFieldImpls.clear();
    for (Concrete.ClassFieldImpl classFieldImpl : originalClassFieldImpls) {
      visitClassFieldImpl(classFieldImpl, classFieldImpls);
    }
  }
}
