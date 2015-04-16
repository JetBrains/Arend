package com.jetbrains.jetpad.vclang.model;

import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.otmodel.wrapper.WrapperContext;

import static com.jetbrains.jetpad.vclang.model.expr.Model.*;

public class Example {
  public static ModuleMapper create() {
    final WrapperContext ctx = new WrapperContext();
    ctx.addNodeWrapperFactory(new NodeWrapperFactory());

    Module module = new Module(ctx);

    FunctionDefinition def = new FunctionDefinition(ctx);
    module.definitions.add(def);
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

    final ModuleMapper mapper = new ModuleMapper(module);
    mapper.attachRoot();
    return mapper;
  }
}
