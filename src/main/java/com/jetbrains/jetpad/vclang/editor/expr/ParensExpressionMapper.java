package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.ParensExpression;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.cell.Utils.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.indent;
import static jetbrains.jetpad.cell.util.CellFactory.text;

public class ParensExpressionMapper extends Mapper<ParensExpression, ParensExpressionMapper.Cell> {

  public ParensExpressionMapper(ParensExpression source) {
    super(source, new Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    ProjectionalRoleSynchronizer<Node, Expression> exprSynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().expr, getTarget().expr, ExpressionMapperFactory.getInstance());
    exprSynchronizer.setPlaceholderText("<expr>");
    exprSynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(exprSynchronizer);
  }

  public static class Cell extends IndentCell {
    public final jetbrains.jetpad.cell.Cell expr = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          text("("),
          expr,
          text(")"));

      focusable().set(true);
      // set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(expr));
    }
  }
}
