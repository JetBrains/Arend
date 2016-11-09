package com.jetbrains.jetpad.vclang.typechecking.order;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.AbstractDefinitionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CollectDefCallsVisitor;

import java.util.*;

public class DefinitionGetDepsVisitor implements AbstractDefinitionVisitor<Boolean, Set<Abstract.Definition>> {
  private final Queue<Abstract.Definition> myOthers;
  private final Map<Abstract.Definition, List<Abstract.Definition>> myClassToNonStatics;

  public DefinitionGetDepsVisitor(Queue<Abstract.Definition> myOthers, Map<Abstract.Definition, List<Abstract.Definition>> classToNonStatics) {
    this.myOthers = myOthers;
    myClassToNonStatics = classToNonStatics;
  }

  @Override
  public Set<Abstract.Definition> visitFunction(Abstract.FunctionDefinition def, Boolean isStatic) {
    Set<Abstract.Definition> result = new HashSet<>();
    visitStatements(result, def, def.getStatements(), isStatic);
    if (isStatic) {
      return result;
    }

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(new CollectDefCallsVisitor(result), null);
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(new CollectDefCallsVisitor(result), null);
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      term.accept(new CollectDefCallsVisitor(result), null);
    }

    return result;
  }

  @Override
  public Set<Abstract.Definition> visitClassField(Abstract.ClassField def, Boolean params) {
    throw new IllegalStateException();
  }

  private void visitClassField(Set<Abstract.Definition> result, Abstract.ClassField def) {
    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(new CollectDefCallsVisitor(result), null);
      }
    }

    Abstract.Expression resultType = def.getResultType();
    if (resultType != null) {
      resultType.accept(new CollectDefCallsVisitor(result), null);
    }
  }

  @Override
  public Set<Abstract.Definition> visitData(Abstract.DataDefinition def, Boolean isStatic) {
    if (isStatic)
      return Collections.emptySet();

    Set<Abstract.Definition> result = new HashSet<>();

    for (Abstract.TypeArgument param : def.getParameters()) {
      param.getType().accept(new CollectDefCallsVisitor(result), null);
    }

    for (Abstract.Constructor constructor : def.getConstructors()) {
      visitConstructor(result, constructor);
    }

    return result;
  }

  @Override
  public Set<Abstract.Definition> visitConstructor(Abstract.Constructor def, Boolean isStatic) {
    throw new IllegalStateException();
  }

  private void visitConstructor(Set<Abstract.Definition> result,  Abstract.Constructor def) {
    for (Abstract.TypeArgument arg : def.getArguments()) {
      arg.getType().accept(new CollectDefCallsVisitor(result), null);
    }
    if (def.getDataType().getConditions() != null) {
      for (Abstract.Condition cond : def.getDataType().getConditions()) {
        if (cond.getConstructorName().equals(def.getName())) {
          cond.getTerm().accept(new CollectDefCallsVisitor(result), null);
        }
      }
    }
  }

  public void visitStatements(Set<Abstract.Definition> result, Abstract.Definition parent, Collection<? extends Abstract.Statement> statements, boolean isStatic) {
    Set<Abstract.Definition> nonStatic = !isStatic && parent instanceof Abstract.ClassDefinition ? new HashSet<Abstract.Definition>() : null;
    for (Abstract.Statement statement : statements) {
      if (statement instanceof Abstract.DefineStatement) {
        Abstract.DefineStatement defineStatement = (Abstract.DefineStatement) statement;
        if (isStatic) {
          if (defineStatement.getStaticMod() == Abstract.DefineStatement.StaticMod.STATIC || parent instanceof Abstract.FunctionDefinition) {
            result.add(defineStatement.getDefinition());
            result.addAll(defineStatement.getDefinition().accept(
                new DefinitionGetDepsVisitor(myOthers, null), true
            ));
          }
        } else {
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.ClassField) {
            visitClassField(result, (Abstract.ClassField) ((Abstract.DefineStatement) statement).getDefinition());
          } else
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.Implementation) {
            visitImplement(result, (Abstract.Implementation) ((Abstract.DefineStatement) statement).getDefinition());
          } else
          if (((Abstract.DefineStatement) statement).getDefinition() instanceof Abstract.ClassView) {
            // TODO: I'm not sure what to do here.
            ((Abstract.ClassView) ((Abstract.DefineStatement) statement).getDefinition()).getUnderlyingClassDefCall().accept(new CollectDefCallsVisitor(result), null);
          } else {
            myOthers.add(defineStatement.getDefinition());
            if (parent instanceof Abstract.ClassDefinition && ((Abstract.DefineStatement) statement).getStaticMod() != Abstract.DefineStatement.StaticMod.STATIC) {
              nonStatic.add(defineStatement.getDefinition());
              nonStatic.addAll(((Abstract.DefineStatement) statement).getDefinition().accept(new DefinitionGetDepsVisitor(myOthers, null), true));
            }
          }
        }
      }
    }
    if (nonStatic != null) {
      myClassToNonStatics.put(parent, new ArrayList<>(nonStatic));
    }
  }

  @Override
  public Set<Abstract.Definition> visitClass(Abstract.ClassDefinition def, Boolean isStatic) {
    Set<Abstract.Definition> result = new HashSet<>();
    for (Abstract.SuperClass superClass : def.getSuperClasses()) {
      superClass.getSuperClass().accept(new CollectDefCallsVisitor(result), null);
    }
    visitStatements(result, def, def.getStatements(), isStatic);
    return result;
  }

  @Override
  public Set<Abstract.Definition> visitImplement(Abstract.Implementation def, Boolean isStatic) {
    throw new IllegalStateException();
  }

  @Override
  public Set<Abstract.Definition> visitClassView(Abstract.ClassView def, Boolean params) {
    return null;
  }

  @Override
  public Set<Abstract.Definition> visitClassViewField(Abstract.ClassViewField def, Boolean params) {
    return null;
  }

  @Override
  public Set<Abstract.Definition> visitClassViewInstance(Abstract.ClassViewInstance def, Boolean params) {
    Set<Abstract.Definition> result = new HashSet<>();

    for (Abstract.Argument arg : def.getArguments()) {
      if (arg instanceof Abstract.TypeArgument) {
        ((Abstract.TypeArgument) arg).getType().accept(new CollectDefCallsVisitor(result), null);
      }
    }

    Abstract.Expression term = def.getTerm();
    if (term != null) {
      term.accept(new CollectDefCallsVisitor(result), null);
    }

    return result;
  }

  private void visitImplement(Set<Abstract.Definition> result, Abstract.Implementation def) {
    def.getImplementation().accept(new CollectDefCallsVisitor(result), null);
  }
}
