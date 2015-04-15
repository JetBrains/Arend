package com.jetbrains.jetpad.vclang.editor.expr;

import com.jetbrains.jetpad.vclang.model.Position;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.action.CellActions;
import jetbrains.jetpad.cell.completion.Completion;
import jetbrains.jetpad.cell.trait.CellTrait;
import jetbrains.jetpad.cell.trait.CellTraitPropertySpec;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.CompletionSupplier;
import jetbrains.jetpad.completion.SimpleCompletionItem;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperProcessor;
import jetbrains.jetpad.model.composite.Composites;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.model.expr.Model.AppExpression;
import static com.jetbrains.jetpad.vclang.model.expr.Model.Expression;

public class SideTransformMapperProcessor implements MapperProcessor<Expression, Cell> {
  private static final SideTransformMapperProcessor INSTANCE = new SideTransformMapperProcessor();

  private SideTransformMapperProcessor() {}

  @Override
  public void process(final Mapper<? extends Expression, ? extends Cell> mapper) {
    final Cell cell = mapper.getTarget();
    final Expression expr = mapper.getSource();
    final WrapperContext ctx = expr.getContext();

    Cell firstFocusable = Composites.firstFocusable(cell);
    Cell lastFocusable = Composites.lastFocusable(cell);

    if (lastFocusable != null) {
      lastFocusable.addTrait(new CellTrait() {
        @Override
        public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
          if (spec == Completion.RIGHT_TRANSFORM) {
            return new CompletionSupplier() {
              @Override
              public List<CompletionItem> get(CompletionParameters cp) {
                List<CompletionItem> result = new ArrayList<>();
                result.add(new SimpleCompletionItem("") {
                  @Override
                  public Runnable complete(String text) {
                    AppExpression appExpr = new AppExpression(ctx);
                    Mapper<?, ?> parentMapper = mapper.getParent();
                    if (expr.position() == Position.APP_ARG) {
                      AppExpression parentExpr = ((AppExpression) expr.parent().get());
                      parentMapper = parentMapper.getParent();
                      parentExpr.replaceWith(appExpr);
                      appExpr.function().set(parentExpr);
                    } else {
                      expr.replaceWith(appExpr);
                      appExpr.function().set(expr);
                    }

                    AppExpressionMapper appExprMapper = (AppExpressionMapper) parentMapper.getDescendantMapper(appExpr);
                    return CellActions.toFirstFocusable(appExprMapper.getTarget().argument);
                  }
                });
                return result;
              }
            };
          }
          return super.get(cell, spec);
        }
      });
    }

    if (firstFocusable != null) {
      firstFocusable.addTrait(new CellTrait() {
        @Override
        public Object get(Cell cell, CellTraitPropertySpec<?> spec) {
          if (spec == Completion.LEFT_TRANSFORM) {
            return new CompletionSupplier() {
              @Override
              public List<CompletionItem> get(CompletionParameters cp) {
                List<CompletionItem> result = new ArrayList<>();
                result.add(new SimpleCompletionItem("") {
                  @Override
                  public Runnable complete(String text) {
                    AppExpression appExpr = new AppExpression(ctx);
                    Mapper<?, ?> parent = mapper.getParent();
                    boolean inAppArg = expr.position() == Position.APP_ARG;
                    if (inAppArg) {
                      AppExpression parentExpr = ((AppExpression) expr.parent().get());
                      Expression function = parentExpr.getFunction();
                      parentExpr.function().set(appExpr);
                      appExpr.function().set(function);
                    } else {
                      expr.replaceWith(appExpr);
                      appExpr.argument().set(expr);
                    }

                    AppExpressionMapper.Cell appExprCell = ((AppExpressionMapper) parent.getDescendantMapper(appExpr)).getTarget();
                    return CellActions.toFirstFocusable(inAppArg ? appExprCell.argument : appExprCell.function);
                  }
                });
                return result;
              }
            };
          }
          return super.get(cell, spec);
        }
      });
    }
  }

  public static SideTransformMapperProcessor getInstance() {
    return INSTANCE;
  }
}
