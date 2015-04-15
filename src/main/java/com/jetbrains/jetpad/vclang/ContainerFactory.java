package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import com.jetbrains.jetpad.vclang.editor.error.ErrorListMapper;
import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;
import jetbrains.jetpad.projectional.util.RootController;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public class ContainerFactory {
  private static CellContainer MAIN_CONTAINER;
  private static CellContainer ERRORS_CONTAINER;
  private static ModuleMapper MAIN_ROOT_MAPPER;
  private static ErrorListMapper ERRORS_ROOT_MAPPER;

  static {
    Module m = createModel();
    MAIN_ROOT_MAPPER = new ModuleMapper(m);
    MAIN_ROOT_MAPPER.attachRoot();

    MAIN_CONTAINER = new CellContainer();
    MAIN_CONTAINER.root.children().add(MAIN_ROOT_MAPPER.getTarget());
    RootController.install(MAIN_CONTAINER);
  }

  static {
    ERRORS_ROOT_MAPPER = new ErrorListMapper();
    ERRORS_ROOT_MAPPER.attachRoot();

    ERRORS_CONTAINER = new CellContainer();
    ERRORS_CONTAINER.root.children().add(ERRORS_ROOT_MAPPER.getTarget());
    RootController.install(ERRORS_CONTAINER);
  }

  public static CellContainer getMainContainer() {
    return MAIN_CONTAINER;
  }

  public static CellContainer getErrorsContainer() {
    return ERRORS_CONTAINER;
  }

  public static ModuleMapper getMainRootMapper() {
    return MAIN_ROOT_MAPPER;
  }

  public static ErrorListMapper getErrorsRootMapper() {
    return ERRORS_ROOT_MAPPER;
  }

  private static Module createModel() {
    final WrapperContext ctx = new WrapperContext();
    Module result = new Module(ctx);
    FunctionDefinition def = new FunctionDefinition(ctx);
    result.definitions.add(def);
    LamExpression expr1 = new LamExpression(ctx);
    def.term().set(expr1);
    NameArgument arg1 = new NameArgument(ctx);
    expr1.getArguments().add(arg1);
    arg1.isExplicit().set(true);
    arg1.name().set("x");
    VarExpression expr2 = new VarExpression(ctx);
    expr1.body().set(expr2);
    expr2.name().set("x");
    PiExpression expr3 = new PiExpression(ctx);
    def.resultType().set(expr3);
    TypeArgument arg2 = new TypeArgument(ctx);
    expr3.getArguments().add(arg2);
    arg2.isExplicit().set(true);
    arg2.type().set(new NatExpression(ctx));
    return result;
  }
}
