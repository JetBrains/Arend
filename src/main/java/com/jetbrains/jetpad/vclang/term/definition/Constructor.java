package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.numberOfVariables;

public class Constructor extends Definition implements Abstract.Constructor {
  private DataDefinition myDataType;
  private final List<TypeArgument> myArguments;
  private final int myIndex;

  public Constructor(int index, String name, ClassDefinition parent, Precedence precedence, Fixity fixity, Universe universe, List<TypeArgument> arguments, DataDefinition dataType) {
    super(name, parent, precedence, fixity, universe);
    myArguments = arguments;
    myDataType = dataType;
    myIndex = index;
  }

  @Override
  public List<TypeArgument> getArguments() {
    return myArguments;
  }

  @Override
  public TypeArgument getArgument(int index) {
    return myArguments.get(index);
  }

  @Override
  public DataDefinition getDataType() {
    return myDataType;
  }

  public void setDataType(DataDefinition dataType) {
    myDataType = dataType;
  }

  public int getIndex() {
    return myIndex;
  }

  @Override
  public Expression getType() {
    Expression resultType = DefCall(myDataType);
    int numberOfVars = numberOfVariables(myArguments);
    for (int i = numberOfVariables(myDataType.getParameters()) - 1, j = 0; i >= 0; ++j) {
      if (myDataType.getParameter(j) instanceof TelescopeArgument) {
        for (String ignored : ((TelescopeArgument) myDataType.getParameter(j)).getNames()) {
          resultType = Apps(resultType, new ArgumentExpression(Index(i-- + numberOfVars), myDataType.getParameter(j).getExplicit(), !myDataType.getParameter(j).getExplicit()));
        }
      } else {
        resultType = Apps(resultType, new ArgumentExpression(Index(i-- + numberOfVars), myDataType.getParameter(j).getExplicit(), !myDataType.getParameter(j).getExplicit()));
      }
    }
    return myArguments.isEmpty() ? resultType : Pi(myArguments, resultType);
  }

  @Override
  public <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitConstructor(this, params);
  }
}
