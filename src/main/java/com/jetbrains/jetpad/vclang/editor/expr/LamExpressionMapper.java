package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalRoleSynchronizer;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.cell.Utils.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class LamExpressionMapper extends Mapper<LamExpression, LamExpressionMapper.Cell> {
  public LamExpressionMapper(LamExpression source) {
    super(source, new LamExpressionMapper.Cell());
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    conf.add(forPropsTwoWay(getSource().variable, getTarget().variable.text()));

    ProjectionalRoleSynchronizer<Node, Expression> bodySynchronizer = ProjectionalSynchronizers.<Node, Expression>forSingleRole(this, getSource().body, getTarget().body, ExpressionMapperFactory.getInstance());
    bodySynchronizer.setPlaceholderText("<term>");
    bodySynchronizer.setCompletion(ExpressionCompletion.getGlobalInstance());
    conf.add(bodySynchronizer);
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell body = noDelete(indent());

    public Cell() {
      CellFactory.to(this,
          text("λ"),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          text("=>"),
          space(),
          body);

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
