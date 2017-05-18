package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.frontend.Concrete;
import com.jetbrains.jetpad.vclang.naming.NameResolverTestCase;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.GoalError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.order.BaseDependencyListener;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class TypeCheckingTestCase extends NameResolverTestCase {
  @SuppressWarnings("StaticNonFinalField")
  private static SimpleTypecheckerState PRELUDE_TYPECHECKER_STATE = null;

  private TypecheckerState state = new SimpleTypecheckerState();

  protected LocalErrorReporter localErrorReporter = new TestLocalErrorReporter(errorReporter);

  public TypeCheckingTestCase() {
    typeCheckPrelude();
  }

  private void typeCheckPrelude() {
    loadPrelude();

    if (PRELUDE_TYPECHECKER_STATE == null) {
      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      PRELUDE_TYPECHECKER_STATE = new SimpleTypecheckerState();
      new Typechecking(PRELUDE_TYPECHECKER_STATE, staticNsProvider, dynamicNsProvider, internalErrorReporter, new Prelude.UpdatePreludeReporter(PRELUDE_TYPECHECKER_STATE), new BaseDependencyListener()).typecheckModules(Collections.singletonList(prelude));
      //assertThat(internalErrorReporter.getErrorList(), is(empty()));  // does not type-check by design
    }

    state = new SimpleTypecheckerState(PRELUDE_TYPECHECKER_STATE);
  }


  CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType, int errors) {
    CheckTypeVisitor.Result result = new CheckTypeVisitor(state, staticNsProvider, dynamicNsProvider, context, localErrorReporter, null).finalCheckExpr(expression, expectedType);
    assertThat(errorList, containsErrors(errors));
    if (errors == 0) {
      assertThat(result, is(notNullValue()));
    }
    return result;
  }

  CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    return typeCheckExpr(new ArrayList<>(), expression, expectedType, errors);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(List<Binding> context, Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(context, expression, expectedType, 0);
  }

  protected CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType) {
    return typeCheckExpr(new ArrayList<>(), expression, expectedType, 0);
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
    new Typechecking(state, staticNsProvider, dynamicNsProvider, errorReporter, new TypecheckedReporter.Dummy(), new BaseDependencyListener()).typecheckDefinitions(Collections.singletonList(definition));
    assertThat(errorList, containsErrors(errors));
    return state.getTypechecked(definition);
  }

  protected Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(resolveNamesDef(text), errors);
  }

  protected Definition typeCheckDef(String text) {
    return typeCheckDef(text, 0);
  }


  private TypecheckerState typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    new Typechecking(state, staticNsProvider, dynamicNsProvider, localErrorReporter, new TypecheckedReporter.Dummy(), new BaseDependencyListener()).typecheckModules(Collections.singletonList(classDefinition));
    assertThat(errorList, containsErrors(errors));
    return state;
  }


  protected class TypeCheckClassResult {
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

  protected TypeCheckClassResult typeCheckClass(String instance, String global, int errors) {
    Concrete.ClassDefinition def = (Concrete.ClassDefinition) resolveNamesDef("\\class Test {\n" + instance + (global.isEmpty() ? "" : "\n} \\where {\n" + global) + "\n}");
    return new TypeCheckClassResult(typeCheckClass(def, errors), def);
  }

  protected TypeCheckClassResult typeCheckClass(String instance, String global) {
    return typeCheckClass(instance, global, 0);
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

  protected static Matcher<? super GeneralError> typecheckingError(final Class<? extends LocalTypeCheckingError> type) {
    return new TypeCheckingErrorMatcher() {
      @Override
      protected boolean matchesTypeCheckingError(LocalTypeCheckingError error, Description description) {
        if (type.isInstance(error)) {
          description.appendText(type.getName());
          return true;
        } else {
          description.appendText("not a " + type.getName());
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should be a " + type.getName());
      }
    };
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
