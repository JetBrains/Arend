package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ProxyErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.order.TypecheckingOrdering;
import com.jetbrains.jetpad.vclang.typechecking.visitor.DefinitionCheckTypeVisitor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TypeCheckingTestCase extends NameResolverTestCase {
  @SuppressWarnings("StaticNonFinalField")
  private static TypecheckerState PRELUDE_TYPECHECKER_STATE = null;

  private TypecheckerState state = new TypecheckerState();

  protected LocalErrorReporter localErrorReporter = new ProxyErrorReporter(null, errorReporter);

  public TypeCheckingTestCase() {
    typeCheckPrelude();
  }

  private void typeCheckPrelude() {
    loadPrelude();

    if (PRELUDE_TYPECHECKER_STATE == null) {
      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      PRELUDE_TYPECHECKER_STATE = new TypecheckerState();
      assertTrue(TypecheckingOrdering.typecheck(PRELUDE_TYPECHECKER_STATE, Collections.singletonList(prelude), internalErrorReporter, true));
      //assertThat(internalErrorReporter.getErrorList(), is(empty()));  // does not type-check by design
    }

    state = new TypecheckerState(PRELUDE_TYPECHECKER_STATE);
  }


  CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor.Result result = new CheckTypeVisitor.Builder(state, context, localErrorReporter).build().checkType(expression, expectedType);
    assertThat(errorList, containsErrors(errors));
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }
    return result;
  }

  CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(context, expression, expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(new ArrayList<Binding>(), expression, expectedType, 0);
  }


  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType, int errors) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, String text, Expression expectedType) {
    return typeCheckExpr(context, resolveNamesExpr(context, text), expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(resolveNamesExpr(text), expectedType, 0);
  }


  private Definition typeCheckDef(Concrete.Definition definition, int errors) {
    DefinitionCheckTypeVisitor visitor = new DefinitionCheckTypeVisitor(state, localErrorReporter);
    Definition result = definition.accept(visitor, null);
    assertThat(errorList, containsErrors(errors));
    return result;
  }

  protected Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  protected Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }


  private TypecheckerState typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    TypecheckingOrdering.typecheck(state, classDefinition, localErrorReporter);
    assertThat(errorList, containsErrors(errors));
    return state;
  }


  protected static class TypeCheckClassResult {
    public final TypecheckerState typecheckerState;
    public final Concrete.ClassDefinition classDefinition;

    public TypeCheckClassResult(TypecheckerState typecheckerState, Concrete.ClassDefinition classDefinition) {
      this.typecheckerState = typecheckerState;
      this.classDefinition = classDefinition;
    }

    public Definition getDefinition(String path) {
      Abstract.Definition ref = get(classDefinition, path);
      return ref != null ? typecheckerState.getTypechecked(ref) : null;
    }
  }

  protected TypeCheckClassResult typeCheckClass(Concrete.ClassDefinition classDefinition) {
    TypecheckerState state = typeCheckClass(classDefinition, 0);
    return new TypeCheckClassResult(state, classDefinition);
  }

  protected TypeCheckClassResult typeCheckClass(String text, int nameErrors, int tcErrors) {
    Concrete.ClassDefinition classDefinition = resolveNamesClass(text, nameErrors);
    TypecheckerState state = typeCheckClass(classDefinition, nameErrors + tcErrors);
    return new TypeCheckClassResult(state, classDefinition);
  }

  protected TypeCheckClassResult typeCheckClass(String text, int tcErrors) {
    return typeCheckClass(text, 0, tcErrors);
  }

  protected TypeCheckClassResult typeCheckClass(String text) {
    return typeCheckClass(text, 0, 0);
  }


  protected abstract static class TypeCheckingErrorMatcher extends TypeSafeDiagnosingMatcher<GeneralError> {
    @Override
    protected boolean matchesSafely(GeneralError generalError, Description description) {
      if (generalError instanceof TypeCheckingError) {
        description.appendText("TC error ");
        return matchesTypeCheckingError(((TypeCheckingError) generalError).localError, description);
      } else {
        description.appendText("not a TC error");
        return false;
      }
    }

    protected abstract boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description);
  }

  protected static Matcher<? super GeneralError> typeMismatchError() {
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
          if (error instanceof TypeMismatchError) {
            description.appendText("type mismatch");
            return true;
          } else {
            description.appendText("not a type mismatch");
            return false;
          }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a type mismatch");
      }
    };
  }

  protected static Matcher<GeneralError> goal(final int expectedSize) {
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
        if (error instanceof GoalError) {
          description.appendText("goal with ");
          int size = ((GoalError) error).context.size();
          if (size == 0) {
            description.appendText("empty context");
          } else {
            description.appendText("context of size ").appendValue(size);
          }
          return size == expectedSize;
        } else {
          description.appendText("not a goal");
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a goal with ");
        if (expectedSize == 0) {
          description.appendText("empty context");
        } else {
          description.appendText("context of size ").appendValue(expectedSize);
        }
      }
    };
  }
}
