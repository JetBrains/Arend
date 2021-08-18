package org.arend.term.concrete;

import org.arend.ext.concrete.ConcreteSourceNode;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.concrete.expr.ConcreteCoclause;
import org.arend.ext.concrete.expr.ConcreteCoclauses;
import org.arend.ext.concrete.expr.ConcreteExpression;
import org.arend.ext.error.ErrorReporter;
import org.arend.ext.error.TypecheckingError;
import org.arend.prelude.Prelude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayMetaDefinition extends DefinableMetaDefinition {
  public ArrayMetaDefinition(List<Concrete.NameParameter> params, Concrete.Expression body) {
    super(Prelude.ARRAY, Collections.emptyList(), Collections.emptyList(), params, body);
  }

  @Override
  protected boolean checkArguments(@NotNull List<? extends ConcreteArgument> arguments, @Nullable ConcreteCoclauses coclauses, @Nullable ErrorReporter errorReporter, @Nullable ConcreteSourceNode marker) {
    if (arguments.isEmpty()) {
      boolean ok = false;
      if (coclauses != null) {
        for (ConcreteCoclause coclause : coclauses.getCoclauseList()) {
          if (coclause.getImplementedRef() == Prelude.ARRAY_ELEMENTS_TYPE.getRef()) {
            ok = true;
            break;
          }
        }
      }
      if (!ok) {
        if (errorReporter != null) errorReporter.report(new TypecheckingError("Expected at least 1 argument", marker));
        return false;
      }
    }
    if (arguments.size() > 3) {
      if (errorReporter != null) errorReporter.report(new TypecheckingError("Expected at most 3 arguments", arguments.get(3).getExpression()));
      return false;
    }
    return true;
  }

  private Concrete.Expression makeLam(Concrete.Expression arg) {
    return new Concrete.LamExpression(arg.getData(), Collections.singletonList(new Concrete.NameParameter(arg.getData(), true, null)), arg);
  }

  @Override
  protected @Nullable ConcreteExpression getConcreteRepresentation(@Nullable Object data, @Nullable List<Concrete.LevelExpression> pLevels, @Nullable List<Concrete.LevelExpression> hLevels, @NotNull List<? extends ConcreteArgument> arguments, @Nullable ConcreteCoclauses coclauses) {
    Concrete.ReferenceExpression fun = new Concrete.ReferenceExpression(data, Prelude.DEP_ARRAY.getReferable(), pLevels, hLevels);

    for (ConcreteArgument argument : arguments) {
      if (!(argument instanceof Concrete.Argument)) {
        throw new IllegalStateException();
      }
    }

    if (arguments.size() == 1 || coclauses != null) {
      List<Concrete.ClassFieldImpl> list = new ArrayList<>();
      if (!arguments.isEmpty()) {
        Concrete.Expression arg = (Concrete.Expression) arguments.get(0).getExpression();
        list.add(new Concrete.ClassFieldImpl(arg.getData(), Prelude.ARRAY_ELEMENTS_TYPE.getReferable(), makeLam(arg), null));
      }
      if (arguments.size() >= 2) {
        Concrete.Expression arg = (Concrete.Expression) arguments.get(1).getExpression();
        list.add(new Concrete.ClassFieldImpl(arg.getData(), Prelude.ARRAY_LENGTH.getReferable(), arg, null));
      }
      if (arguments.size() >= 3) {
        Concrete.Expression arg = (Concrete.Expression) arguments.get(2).getExpression();
        list.add(new Concrete.ClassFieldImpl(arg.getData(), Prelude.ARRAY_AT.getReferable(), arg, null));
      }
      if (coclauses != null) {
        if (!(coclauses instanceof Concrete.Coclauses)) {
          throw new IllegalStateException();
        }
        list.addAll(((Concrete.Coclauses) coclauses).getCoclauseList());
      }
      return Concrete.ClassExtExpression.make(data, fun, new Concrete.Coclauses(data, list));
    }

    List<Concrete.Argument> args = new ArrayList<>(arguments.size());
    args.add(new Concrete.Argument(((Concrete.Argument) arguments.get(1)).expression, false));
    args.add(new Concrete.Argument(makeLam(((Concrete.Argument) arguments.get(0)).expression), true));
    for (int i = 2; i < arguments.size(); i++) {
      args.add((Concrete.Argument) arguments.get(i));
    }
    return Concrete.AppExpression.make(data, fun, args);
  }
}
