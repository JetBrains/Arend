package com.jetbrains.jetpad.vclang.serialization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.definition.ResolvedName;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class SerializeVisitor implements ExpressionVisitor<Void> {
  private int myErrors = 0;
  private final DefNamesIndicies myDefNamesIndicies;
  private final Set<ResolvedName> myModuleDependencies;
  private final ByteArrayOutputStream myStream;
  private final DataOutputStream myDataStream;

  public SerializeVisitor(DefNamesIndicies definitionsIndices, Set<ResolvedName> moduleDependencies, ByteArrayOutputStream stream, DataOutputStream dataStream) {
    myDefNamesIndicies = definitionsIndices;
    myModuleDependencies = moduleDependencies;
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

  public Set<ResolvedName> getMyModuleDependencies() {
    return myModuleDependencies;
  }

  @Override
  public Void visitApp(AppExpression expr) {
    myStream.write(1);
    expr.getFunction().accept(this);
    try {
      myDataStream.writeBoolean(expr.getArgument().isExplicit());
      myDataStream.writeBoolean(expr.getArgument().isHidden());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    expr.getArgument().getExpression().accept(this);
    return null;
  }

  @Override
  public Void visitDefCall(DefCallExpression expr) {
    myStream.write(2);
    int index = myDefNamesIndicies.getDefNameIndex(expr.getDefinition().getResolvedName(), false);
    ResolvedName module = ModuleSerialization.getModule(expr.getDefinition().getResolvedName());
    if (!module.equals(Prelude.PRELUDE.getResolvedName())) {
      myModuleDependencies.add(module);
    }
    try {
      myDataStream.writeBoolean(expr.getExpression() != null);
      if (expr.getExpression() != null) {
        expr.getExpression().accept(this);
      }
      myDataStream.writeInt(index);
      myDataStream.writeInt(expr.getParameters() == null ? 0 : expr.getParameters().size());
      if (expr.getParameters() != null) {
        for (Expression parameter : expr.getParameters()) {
          parameter.accept(this);
        }
      }
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
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr) {
    myStream.write(5);
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    expr.getCodomain().accept(this);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr) {
    myStream.write(6);
    try {
      ModuleSerialization.writeUniverse(myDataStream, expr.getUniverse());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitInferHole(InferHoleExpression expr) {
    throw new IllegalStateException();
  }

  @Override
  public Void visitError(ErrorExpression expr) {
    ++myErrors;
    myStream.write(9);
    try {
      myDataStream.writeBoolean(expr.getExpr() != null);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    if (expr.getExpr() != null) {
      expr.getExpr().accept(this);
    }
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr) {
    myStream.write(10);
    try {
      myDataStream.writeInt(expr.getFields().size());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    for (Expression field : expr.getFields()) {
      field.accept(this);
    }
    visitSigma(expr.getType());
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr) {
    myStream.write(11);
    try {
      ModuleSerialization.writeArguments(this, expr.getArguments());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitElim(ElimExpression expr) {
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
      clause.getExpression().accept(this);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitProj(ProjExpression expr) {
    myStream.write(14);
    expr.getExpression().accept(this);
    try {
      myDataStream.writeInt(expr.getField());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitClassExt(ClassExtExpression expr) {
    myStream.write(15);
    try {
      expr.getBaseClassExpression().accept(this);
      myDataStream.writeInt(expr.getDefinitionsMap().size());
      for (Map.Entry<FunctionDefinition, OverriddenDefinition> entry : expr.getDefinitionsMap().entrySet()) {
        myDataStream.writeInt(myDefNamesIndicies.getDefNameIndex(entry.getKey().getResolvedName(), false));
        myErrors += ModuleSerialization.serializeDefinition(this, entry.getValue());
      }
      ModuleSerialization.writeUniverse(myDataStream, expr.getUniverse());
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr) {
    myStream.write(16);
    expr.getExpression().accept(this);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression) {
    myStream.write(17);
    try {
      myDataStream.writeInt(letExpression.getClauses().size());
      for (LetClause letClause : letExpression.getClauses()) {
        visitLetClause(letClause);
      }
      letExpression.getExpression().accept(this);
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
        clause.getResultType().accept(this);
      }
      myDataStream.writeBoolean(clause.getArrow() == Abstract.Definition.Arrow.RIGHT);
      clause.getTerm().accept(this);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }
}
