package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.ClassField;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.BaseExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class SerializeVisitor extends BaseExpressionVisitor<Void, Void> {
  private int myErrors = 0;
  private final DefNamesIndicies myDefNamesIndicies;
  private final ByteArrayOutputStream myStream;
  private final DataOutputStream myDataStream;

  public SerializeVisitor(DefNamesIndicies definitionsIndices, ByteArrayOutputStream stream, DataOutputStream dataStream) {
    myDefNamesIndicies = definitionsIndices;
    myStream = stream;
    myDataStream = dataStream;
  }

  public int getErrors() {
    return myErrors;
  }

  public DataOutputStream getDataStream() {
    return myDataStream;
  }

  public DefNamesIndicies getDefinitionsIndices() {
    return myDefNamesIndicies;
  }

  @Override
  public Void visitApp(AppExpression expr, Void params) {
    myStream.write(1);
    expr.getFunction().accept(this, null);
    try {
      myDataStream.writeBoolean(expr.getArgument().isExplicit());
      myDataStream.writeBoolean(expr.getArgument().isHidden());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Void params) {
    myStream.write(2);
    try {
      myDataStream.writeInt(myDefNamesIndicies.getDefNameIndex(expr.getDefinition().getResolvedName(), false));
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitConCall(ConCallExpression expr, Void params) {
    myStream.write(3);
    int index = myDefNamesIndicies.getDefNameIndex(expr.getDefinition().getResolvedName(), false);
    try {
      myDataStream.writeInt(index);
      myDataStream.writeInt(expr.getParameters().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Expression parameter : expr.getParameters()) {
      parameter.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitClassCall(ClassCallExpression expr, Void params) {
    myStream.write(4);
    int index = myDefNamesIndicies.getDefNameIndex(expr.getDefinition().getResolvedName(), false);
    try {
      myDataStream.writeInt(index);
      myDataStream.writeInt(expr.getImplementStatements().size());
      for (Map.Entry<ClassField, ClassCallExpression.ImplementStatement> elem : expr.getImplementStatements().entrySet()) {
        myDataStream.writeInt(myDefNamesIndicies.getDefNameIndex(elem.getKey().getResolvedName(), true));

        Expression type = elem.getValue().type;
        myDataStream.writeBoolean(type != null);
        if (type != null) {
          type.accept(this, null);
        }

        Expression term = elem.getValue().term;
        myDataStream.writeBoolean(term != null);
        if (term != null) {
          term.accept(this, null);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitIndex(IndexExpression expr, Void params) {
    myStream.write(5);
    try {
      myDataStream.writeInt(expr.getIndex());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Void params) {
    myStream.write(6);
    expr.getBody().accept(this, null);
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Void params) {
    myStream.write(7);
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    expr.getCodomain().accept(this, null);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Void params) {
    myStream.write(8);
    try {
      ModuleSerialization.writeUniverse(myDataStream, expr.getUniverse());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitInferHole(InferHoleExpression expr, Void params) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitError(ErrorExpression expr, Void params) {
    ++myErrors;
    myStream.write(9);
    try {
      myDataStream.writeBoolean(expr.getExpr() != null);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Void params) {
    myStream.write(10);
    try {
      myDataStream.writeInt(expr.getFields().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Expression field : expr.getFields()) {
      field.accept(this, null);
    }
    visitSigma(expr.getType(), null);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Void params) {
    myStream.write(11);
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitElim(ElimExpression expr, Void params) {
    myStream.write(12);
    try {
      myDataStream.writeInt(expr.getExpressions().size());
      for (IndexExpression var : expr.getExpressions())
        myDataStream.writeInt(var.getIndex());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    try {
      myDataStream.writeInt(expr.getClauses().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Clause clause : expr.getClauses()) {
      visitClause(clause);
    }
    return null;
  }

  private void visitPattern(Pattern pattern) {
    try {
      myDataStream.writeBoolean(pattern.getExplicit());
      if (pattern instanceof NamePattern)
        myDataStream.writeInt(0);
      else if (pattern instanceof Abstract.AnyConstructorPattern)
        myDataStream.writeInt(1);
      else if (pattern instanceof Abstract.ConstructorPattern)
        myDataStream.writeInt(2);
      if (pattern instanceof NamePattern) {
        myDataStream.writeBoolean(((NamePattern) pattern).getName() != null);
        if (((NamePattern) pattern).getName() != null)
          myDataStream.writeUTF(((NamePattern) pattern).getName());
      } else if (pattern instanceof ConstructorPattern) {
        Constructor constructor = ((ConstructorPattern) pattern).getConstructor();
        myDataStream.writeInt(myDefNamesIndicies.getDefNameIndex(new ResolvedName(constructor.getParentNamespace(), constructor.getName()), false));
        myDataStream.writeInt(((ConstructorPattern) pattern).getPatterns().size());
        for (Pattern nestedPattern : ((ConstructorPattern) pattern).getPatterns()) {
          visitPattern(nestedPattern);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void visitClause(Clause clause) {
    try {
      myDataStream.writeInt(clause.getPatterns().size());
      for (Pattern pattern : clause.getPatterns())
        visitPattern(pattern);
      myDataStream.writeBoolean(clause.getArrow() == Abstract.Definition.Arrow.RIGHT);
      clause.getExpression().accept(this, null);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitProj(ProjExpression expr, Void params) {
    myStream.write(13);
    expr.getExpression().accept(this, null);
    try {
      myDataStream.writeInt(expr.getField());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Void params) {
    myStream.write(14);
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Void params) {
    myStream.write(15);
    try {
      myDataStream.writeInt(letExpression.getClauses().size());
      for (LetClause letClause : letExpression.getClauses()) {
        visitLetClause(letClause);
      }
      letExpression.getExpression().accept(this, null);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  private void visitLetClause(LetClause clause) {
    try {
      myDataStream.writeUTF(clause.getName().name);
      ModuleSerialization.writeArguments(this, clause.getArguments());
      myDataStream.writeBoolean(clause.getResultType() != null);
      if (clause.getResultType() != null) {
        clause.getResultType().accept(this, null);
      }
      myDataStream.writeBoolean(clause.getArrow() == Abstract.Definition.Arrow.RIGHT);
      clause.getTerm().accept(this, null);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }
}
