package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.editor.util.Validators;
import com.jetbrains.jetpad.vclang.model.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.cell.TextCell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.indent.IndentCell;
import jetbrains.jetpad.cell.text.TextEditing;
import jetbrains.jetpad.cell.util.CellFactory;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.cell.ProjectionalSynchronizers;

import static com.jetbrains.jetpad.vclang.editor.Synchronizers.forExpression;
import static com.jetbrains.jetpad.vclang.editor.util.Cells.noDelete;
import static jetbrains.jetpad.cell.util.CellFactory.*;
import static jetbrains.jetpad.mapper.Synchronizers.forPropsTwoWay;

public class PiExpressionMapper extends Mapper<PiExpression, PiExpressionMapper.Cell> {
  public PiExpressionMapper(PiExpression source) {
    super(source, new PiExpressionMapper.Cell(source.position().prec() > Abstract.PiExpression.PREC));
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);
    conf.add(forPropsTwoWay(getSource().domain().get().name(), getTarget().variable.text()));
    conf.add(forExpression(this, getSource().domain().get().type(), getTarget().domain, "<dom>", ExpressionCompletion.getInstance()));
    conf.add(forExpression(this, getSource().codomain(), getTarget().codomain, "<cod>", ExpressionCompletion.getInstance()));
  }

  public static class Cell extends IndentCell {
    public final TextCell variable = noDelete(new TextCell());
    public final jetbrains.jetpad.cell.Cell domain = noDelete(indent());
    public final jetbrains.jetpad.cell.Cell codomain = noDelete(indent());

    public Cell(boolean parens) {
      if (parens) children().add(label("("));
      CellFactory.to(this,
          text("("),
          variable,
          placeHolder(variable, "<no name>"),
          space(),
          text(":"),
          space(),
          domain,
          text(")"),
          space(),
          text("->"),
          space(),
          codomain);
      if (parens) children().add(label(")"));

      focusable().set(true);
      variable.addTrait(TextEditing.validTextEditing(Validators.identifier()));
      set(ProjectionalSynchronizers.ON_CREATE, CellActions.toCell(variable));
    }
  }
}
