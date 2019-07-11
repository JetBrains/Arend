package org.arend.typechecking.visitor;

import org.arend.core.context.binding.Binding;
import org.arend.core.definition.*;
import org.arend.core.expr.*;
import org.arend.core.expr.type.ExpectedType;
import org.arend.core.expr.type.Type;
import org.arend.naming.reference.*;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.LocalErrorReporter;
import org.arend.typechecking.error.local.FieldsImplementationError;
import org.arend.typechecking.error.local.TypecheckingError;
import org.arend.typechecking.error.local.WrongReferable;

import javax.annotation.Nullable;
import java.util.*;

public abstract class BaseTypechecker {
  protected LocalErrorReporter errorReporter;

  protected abstract CheckTypeVisitor.Result finalCheckExpr(Concrete.Expression expr, ExpectedType expectedType, boolean returnExpectedType);

  protected abstract CheckTypeVisitor.Result finalize(CheckTypeVisitor.Result result, Expression expectedType, Concrete.SourceNode sourceNode);

  protected abstract Type checkType(Concrete.Expression expr, ExpectedType expectedType, boolean isFinal);

  public abstract void addBinding(@Nullable Referable referable, Binding binding);

  protected abstract Definition getTypechecked(TCReferable referable);

  protected abstract boolean isDumb();

  protected abstract CheckTypeVisitor.Result typecheckClassExt(List<? extends Concrete.ClassFieldImpl> classFieldImpls, ExpectedType expectedType, Expression implExpr, ClassCallExpression classCallExpr, Set<ClassField> pseudoImplemented, Concrete.Expression expr);

  protected boolean checkAllImplemented(ClassCallExpression classCall, Set<ClassField> pseudoImplemented, Concrete.SourceNode sourceNode) {
    int notImplemented = classCall.getDefinition().getNumberOfNotImplementedFields() - classCall.getImplementedHere().size();
    if (notImplemented == 0) {
      return true;
    } else {
      List<GlobalReferable> fields = new ArrayList<>(notImplemented);
      for (ClassField field : classCall.getDefinition().getFields()) {
        if (!classCall.isImplemented(field) && !pseudoImplemented.contains(field)) {
          fields.add(field.getReferable());
        }
      }
      if (!fields.isEmpty()) {
        errorReporter.report(new FieldsImplementationError(false, fields, sourceNode));
      }
      return false;
    }
  }

  protected Definition referableToDefinition(Referable referable, Concrete.SourceNode sourceNode) {
    if (referable == null || referable instanceof ErrorReference) {
      return null;
    }

    Definition definition = referable instanceof TCReferable ? getTypechecked((TCReferable) referable) : null;
    if (definition == null && sourceNode != null) {
      errorReporter.report(new TypecheckingError("Internal error: definition '" + referable.textRepresentation() + "' was not typechecked", sourceNode));
    }
    return definition;
  }

  public <T extends Definition> T referableToDefinition(Referable referable, Class<T> clazz, String errorMsg, Concrete.SourceNode sourceNode) {
    Definition definition = referableToDefinition(referable, sourceNode);
    if (definition == null) {
      return null;
    }
    if (clazz.isInstance(definition)) {
      return clazz.cast(definition);
    }

    if (sourceNode != null) {
      errorReporter.report(new WrongReferable(errorMsg, referable, sourceNode));
    }
    return null;
  }

  public ClassField referableToClassField(Referable referable, Concrete.SourceNode sourceNode) {
    return referableToDefinition(referable, ClassField.class, "Expected a class field", sourceNode);
  }
}
