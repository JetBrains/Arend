package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;
import java.util.Set;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class Constructor extends Definition implements Abstract.Constructor {
  private List<TypeArgument> myArguments;
  private int myIndex;

  public Constructor(int index, String name, DataDefinition parent, Precedence precedence, Fixity fixity) {
    super(name, parent, precedence, fixity);
    myIndex = index;
  }

  public Constructor(int index, String name, DataDefinition parent, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> arguments) {
    super(name, parent, precedence, fixity);
    setUniverse(universe);
    hasErrors(false);
    myArguments = arguments;
    myIndex = index;
  }

  @Override
  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  public void setArguments(List<TypeArgument> arguments) {
    myArguments = arguments;
  }

  @Override
  public DataDefinition getDataType() {
    return (DataDefinition) super.getParent();
  }

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  @Override
  public Set<Definition> getDependencies() {
    return getParent().getDependencies();
  }

  @Override
  public void setDependencies(Set<Definition> dependencies) {
    throw new IllegalStateException();
  }

  @Override
  public Expression getType() {
    Expression resultType = DefCall(getParent());
    int numberOfVars = numberOfVariables(myArguments);
    if (getDataType().getParameters() != null) {
      for (int i = numberOfVariables(getDataType().getParameters()) - 1, j = 0; i >= 0; ++j) {
        if (getDataType().getParameters().get(j) instanceof TelescopeArgument) {
          for (String ignored : ((TelescopeArgument) getDataType().getParameters().get(j)).getNames()) {
            resultType = Apps(resultType, new ArgumentExpression(Index(i-- + numberOfVars), getDataType().getParameters().get(j).getExplicit(), !getDataType().getParameters().get(j).getExplicit()));
          }
        } else {
          resultType = Apps(resultType, new ArgumentExpression(Index(i-- + numberOfVars), getDataType().getParameters().get(j).getExplicit(), !getDataType().getParameters().get(j).getExplicit()));
        }
      }
    }

    return myArguments.isEmpty() ? resultType : Pi(myArguments, resultType);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConstructor(this, params);
  }
}
