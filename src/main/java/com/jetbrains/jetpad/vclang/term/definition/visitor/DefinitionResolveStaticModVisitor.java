package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.staticmodresolver.StaticModListener;

public class DefinitionResolveStaticModVisitor implements AbstractDefinitionVisitor<Boolean, Void> {
  private StaticModListener myStaticModListener;

  public DefinitionResolveStaticModVisitor(StaticModListener staticModListener) {
    myStaticModListener = staticModListener;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT)
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);

    return null;
  }

  @Override
  public Void visitAbstract(Abstract.ClassViewField def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT)
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);

    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT)
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT)
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);

    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Boolean isStaticDefault) {
    if (def.getParentStatement() != null && def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT)
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);

    boolean curDefaultIsStatic = def.getKind() == Abstract.ClassDefinition.Kind.Module;

    for (Abstract.Statement statement : def.getStatements()) {
      if (statement instanceof Abstract.DefaultStaticStatement) {
        curDefaultIsStatic = ((Abstract.DefaultStaticStatement) statement).isStatic();
      } else if (statement instanceof Abstract.DefineStatement) {
        ((Abstract.DefineStatement)statement).getDefinition().accept(this, curDefaultIsStatic);
      }
    }
    return null;
  }

  @Override
  public Void visitImplement(Abstract.ImplementDefinition def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT) {
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);
    }
    return null;
  }
}
