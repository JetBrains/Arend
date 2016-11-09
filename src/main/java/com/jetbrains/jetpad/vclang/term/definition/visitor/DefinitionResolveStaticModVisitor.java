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
  public Void visitClassField(Abstract.ClassField def, Boolean isStaticDefault) {
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

    return null;
  }

  @Override
  public Void visitImplement(Abstract.Implementation def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT) {
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);
    }
    return null;
  }

  @Override
  public Void visitClassView(Abstract.ClassView def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT) {
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);
    }
    return null;
  }

  @Override
  public Void visitClassViewField(Abstract.ClassViewField def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT) {
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);
    }
    return null;
  }

  @Override
  public Void visitClassViewInstance(Abstract.ClassViewInstance def, Boolean isStaticDefault) {
    if (def.getParentStatement().getStaticMod() == Abstract.DefineStatement.StaticMod.DEFAULT) {
      myStaticModListener.resolveStaticMod(def.getParentStatement(), isStaticDefault);
    }
    return null;
  }
}
