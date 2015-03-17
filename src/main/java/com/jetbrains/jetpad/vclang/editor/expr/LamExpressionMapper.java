package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.base.Validators;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;
import jetbrains.jetpad.values.Color;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class LamExpressionMapper extends ExpressionMapper<LamExpression, LamExpressionMapper.Cell> {
  public LamExpressionMapper(LamExpression source) {
    super(source, new LamExpressionMapper.Cell(source.position().prec() > Abstract.LamExpression.PREC));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsTwoWay(getSource().variable(), getTarget().variable.text()));
    conf.add(forExpression(this, getSource().body(), getTarget().body, "<term>"));
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell body = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(label("("));

      TextCell cell = label("=>");
      cell.background().set(Color.TRANSPARENT);
      background().set(Color.LIGHT_BLUE); // Doesn't do anything.

      CellFactory.to(this,
          label("λ"),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          cell,
          space(),
          body);
      if (parens) children().add(label(")"));

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
