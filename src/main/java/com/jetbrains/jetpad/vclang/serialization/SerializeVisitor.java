package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class SerializeVisitor implements ExpressionVisitor<Void> {
  private int myErrors = 0;
  private final DefinitionsIndices myDefinitionsIndices;
  private final ByteArrayOutputStream myStream;
  private final DataOutputStream myDataStream;

  public SerializeVisitor(DefinitionsIndices definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream) {
    myDefinitionsIndices = definitionsIndices;
    myStream = stream;
    myDataStream = dataStream;
  }

  public int getErrors() {
    return myErrors;
  }

  @Override
  public Void visitApp(AppExpression expr) {
    myStream.write(1);
    expr.getFunction().accept(this);
    myStream.write(expr.getArgument().isExplicit() ? 1 : 0);
    myStream.write(expr.getArgument().isHidden() ? 1 : 0);
    expr.getArgument().getExpression().accept(this);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr) {
    myStream.write(2);
    int index = myDefinitionsIndices.getDefinitionIndex(expr.getDefinition());
    try {
      myDataStream.writeInt(index);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitIndex(IndexExpression expr) {
    myStream.write(3);
    try {
      myDataStream.writeInt(expr.getIndex());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr) {
    myStream.write(4);
    expr.getBody().accept(this);
    visitArguments(expr.getArguments());
    return null;
  }

  private void visitArguments(List<? extends Argument> arguments) {
    try {
      myDataStream.writeInt(arguments.size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Argument argument : arguments) {
      visitArgument(argument);
    }
  }

  private void visitArgument(Argument argument) {
    myStream.write(argument.getExplicit() ? 1 : 0);
    try {
      if (argument instanceof TelescopeArgument) {
        myStream.write(0);
        myDataStream.writeInt(((TelescopeArgument) argument).getNames().size());
        for (String name : ((TelescopeArgument) argument).getNames()) {
          myDataStream.writeBytes(name);
        }
        ((TypeArgument) argument).getType().accept(this);
      } else
      if (argument instanceof TypeArgument) {
        myStream.write(1);
        ((TypeArgument) argument).getType().accept(this);
      }
      if (argument instanceof NameArgument) {
        myStream.write(2);
        myDataStream.writeBytes(((NameArgument) argument).getName());
      } else {
        throw new IllegalStateException();
      }
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitPi(PiExpression expr) {
    myStream.write(5);
    visitArguments(expr.getArguments());
    expr.getCodomain().accept(this);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr) {
    myStream.write(6);
    try {
      myDataStream.writeInt(expr.getUniverse().getLevel());
      if (expr.getUniverse() instanceof Universe.Type) {
        myDataStream.writeInt(((Universe.Type) expr.getUniverse()).getTruncated());
      } else {
        throw new IllegalStateException();
      }
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitVar(VarExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitInferHole(InferHoleExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitError(ErrorExpression expr) {
    ++myErrors;
    myStream.write(9);
    myStream.write(expr.getExpr() == null ? 0 : 1);
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr) {
    myStream.write(9);
    try {
      myDataStream.writeInt(expr.getFields().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Expression field : expr.getFields()) {
      field.accept(this);
    }
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr) {
    myStream.write(10);
    visitArguments(expr.getArguments());
    return null;
  }

  @Override
  public Void visitElim(ElimExpression expr) {
    myStream.write(11);
    myStream.write(expr.getElimType() == Abstract.ElimExpression.ElimType.ELIM ? 0 : 1);
    expr.getExpression().accept(this);
    try {
      myDataStream.writeInt(expr.getClauses().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Clause clause : expr.getClauses()) {
      visitClause(clause);
    }
    myStream.write(expr.getOtherwise() == null ? 0 : 1);
    if (expr.getOtherwise() != null) {
      visitClause(expr.getOtherwise());
    }
    return null;
  }

  private void visitClause(Clause clause) {
    try {
      myDataStream.writeInt(myDefinitionsIndices.getDefinitionIndex(clause.getConstructor()));
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    visitArguments(clause.getArguments());
    myStream.write(clause.getArrow() == Abstract.Definition.Arrow.LEFT ? 0 : 1);
    clause.getExpression().accept(this);
  }

  @Override
  public Void visitFieldAcc(FieldAccExpression expr) {
    myStream.write(12);
    expr.getExpression().accept(this);
    try {
      myDataStream.writeInt(myDefinitionsIndices.getDefinitionIndex(expr.getDefinition()));
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }
}
