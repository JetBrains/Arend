package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.TypedBinding;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Tele;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Universe;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;

public class CheckTypeVisitor implements AbstractDefinitionVisitor<List<Abstract.Binding>, Definition> {
  private final Map<String, Abstract.Definition> myGlobalContext;
  private final List<TypeCheckingError> myErrors;

  public CheckTypeVisitor(Map<String, Abstract.Definition> myGlobalContext, List<TypeCheckingError> myErrors) {
    this.myGlobalContext = myGlobalContext;
    this.myErrors = myErrors;
  }

  @Override
  public Abstract.Definition visitFunction(Abstract.FunctionDefinition def, List<Abstract.Binding> localContext) {
    List<Abstract.TelescopeArgument> arguments = new ArrayList<>(def.getArguments().size());
    int origSize = localContext.size();
    for (Abstract.TelescopeArgument argument : def.getArguments()) {
      com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor.OKResult result = argument.getType().checkType(myGlobalContext, localContext, Universe(), myErrors);
      if (result == null) {
        trimToSize(localContext, origSize);
        return null;
      }

      arguments.add(Tele(argument.getExplicit(), argument.getNames(), result.expression));
      for (String name : argument.getNames()) {
        localContext.add(new TypedBinding(name, result.expression));
      }
    }

    Abstract.Expression expectedType;
    if (def.getResultType() != null) {
      com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor.OKResult typeResult = def.getResultType().checkType(myGlobalContext, localContext, Universe(), myErrors);
      if (typeResult == null) {
        trimToSize(localContext, origSize);
        return null;
      }
      expectedType = typeResult.expression;
    } else {
      expectedType = null;
    }

    com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor.OKResult termResult = def.getTerm().checkType(myGlobalContext, localContext, expectedType, myErrors);
    trimToSize(localContext, origSize);
    return termResult == null ? null : new FunctionDefinition(def.getID(), def.getName(), def.getPrecedence(), def.getFixity(), arguments, termResult.type, def.getArrow(), termResult.expression);
  }
}
