package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.visitor.CheckTypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.removeFromList;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class DataDefinition extends Definition {
  private final List<Constructor> myConstructors;
  private final List<TypeArgument> myParameters;
  private final Integer myUniverseLevel;

  public DataDefinition(String name, Precedence precedence, Fixity fixity, List<TypeArgument> parameters, Integer universeLevel, List<Constructor> constructors) {
    super(name, precedence, fixity);
    myParameters = parameters;
    myUniverseLevel = universeLevel;
    myConstructors = constructors;
  }

  public DataDefinition(String name, Fixity fixity, List<TypeArgument> parameters, Integer universeLevel, List<Constructor> constructors) {
    super(name, fixity);
    myParameters = parameters;
    myUniverseLevel = universeLevel;
    myConstructors = constructors;
  }

  public DataDefinition(String name, List<TypeArgument> parameters, Integer universeLevel, List<Constructor> constructors) {
    super(name);
    myParameters = parameters;
    myUniverseLevel = universeLevel;
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
    if (myUniverseLevel != null) {
      builder.append(" : ");
      Universe(myUniverseLevel).prettyPrint(builder, names, (byte) 0);
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
    for (TypeArgument parameter : myParameters) {
      CheckTypeVisitor.OKResult result = parameter.getType().checkType(globalContext, localContext, Universe(-1), errors);
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
    }

    trimToSize(localContext, origSize);
    return new DataDefinition(getName(), getPrecedence(), getFixity(), parameters, myUniverseLevel, constructors);
  }

  @Override
  public Expression getType() {
    return Pi(myParameters, Universe(myUniverseLevel == null ? -1 : myUniverseLevel));
  }
}
