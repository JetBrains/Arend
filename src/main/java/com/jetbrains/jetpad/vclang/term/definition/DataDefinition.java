package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Universe;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class DataDefinition extends Definition {
  private final List<Constructor> myConstructors;
  private final List<TypeArgument> myParameters;

  protected DataDefinition(int id, String name, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(id, name, precedence, fixity, universe);
    myParameters = parameters;
    myConstructors = constructors;
  }

  public DataDefinition(String name, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(name, precedence, fixity, universe);
    myParameters = parameters;
    myConstructors = constructors;
  }

  public DataDefinition(String name, Fixity fixity, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(name, fixity, universe);
    myParameters = parameters;
    myConstructors = constructors;
  }

  public DataDefinition(String name, Universe universe, List<TypeArgument> parameters, List<Constructor> constructors) {
    super(name, universe);
    myParameters = parameters;
    myConstructors = constructors;
  }

  public List<TypeArgument> getParameters() {
    return myParameters;
  }

  public TypeArgument getParameter(int index) {
    return myParameters.get(index);
  }

  public List<Constructor> getConstructors() {
    return myConstructors;
  }

  public Constructor getConstructor(int index) {
    return myConstructors.get(index);
  }

  @Override
  public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
    builder.append("\\function\n");
    if (getFixity() == Fixity.PREFIX) {
      builder.append(getName());
    } else {
      builder.append('(').append(getName()).append(')');
    }
    for (TypeArgument parameter : myParameters) {
      builder.append(' ');
      parameter.prettyPrint(builder, names, Abstract.VarExpression.PREC);
    }
    if (getUniverse() != null) {
      builder.append(" : ").append(getUniverse());
    }
    for (Constructor constructor : myConstructors) {
      builder.append("\n    | ");
      constructor.prettyPrint(builder, names, (byte) 0);
    }
    removeFromList(names, myParameters);
  }

  @Override
  public DataDefinition checkTypes(Map<String, Definition> globalContext, List<Binding> localContext, List<TypeCheckingError> errors) {
    List<TypeArgument> parameters = new ArrayList<>(myParameters.size());
    int origSize = localContext.size();
    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (TypeArgument parameter : myParameters) {
      CheckTypeVisitor.OKResult result = parameter.getType().checkType(globalContext, localContext, Universe(), errors);
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      if (parameter instanceof TelescopeArgument) {
        parameters.add(Tele(parameter.getExplicit(), ((TelescopeArgument) parameter).getNames(), result.expression));
        for (String name : ((TelescopeArgument) parameter).getNames()) {
          localContext.add(new TypedBinding(name, result.expression));
        }
      } else {
        parameters.add(TypeArg(parameter.getExplicit(), result.expression));
        localContext.add(new TypedBinding(null, result.expression));
      }
    }

    List<Constructor> constructors = new ArrayList<>(myConstructors.size());
    for (Constructor constructor : myConstructors) {
      Constructor newConstructor = constructor.checkTypes(globalContext, localContext, errors);
      if (newConstructor == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      Universe maxUniverse = universe.max(newConstructor.getUniverse());
      if (maxUniverse == null) {
        String msg = "Universe " + newConstructor.getUniverse() + " of constructor " + newConstructor.getName() + " is not comparable to universe " + universe + " of previous constructors";
        errors.add(new TypeCheckingError(msg, null));
        trimToSize(localContext, origSize);
        return null;
      }
      universe = maxUniverse;

      constructors.add(newConstructor);
    }

    trimToSize(localContext, origSize);
    if (getUniverse() != null && !universe.lessOrEquals(getUniverse())) {
      errors.add(new TypeMismatchError(new UniverseExpression(getUniverse()), new UniverseExpression(universe), null));
      return null;
    }

    return new DataDefinition(myID, getName(), getPrecedence(), getFixity(), getUniverse() != null ? getUniverse() : universe, parameters, constructors);
  }

  @Override
  public Expression getType() {
    Expression resultType = new UniverseExpression(getUniverse());
    return myParameters.isEmpty() ? resultType : Pi(myParameters, resultType);
  }
}
