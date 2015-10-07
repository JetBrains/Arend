package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.Name;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class SigmaExpression extends Expression implements Abstract.SigmaExpression {
  private final List<TypeArgument> myArguments;

  public SigmaExpression(List<TypeArgument> arguments) {
    myArguments = arguments;
  }

  @Override
  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitSigma(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    int origSize = context.size();
    for (TypeArgument argument : myArguments) {
      Expression type = argument.getType().getType(context);
      if (!(type instanceof UniverseExpression)) return null;
      universe = universe.max(((UniverseExpression) type).getUniverse());
      if (universe == null) return null;

      if (argument instanceof TelescopeArgument) {
        for (String name : ((TelescopeArgument) argument).getNames()) {
          context.add(new TypedBinding(name, argument.getType()));
        }
      } else {
        context.add(new TypedBinding((Name) null, argument.getType()));
      }
    }

    trimToSize(context, origSize);
    return new UniverseExpression(universe);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitSigma(this, params);
  }
}
