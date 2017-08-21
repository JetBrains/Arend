package com.jetbrains.jetpad.vclang.term;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {}

  // Parameters

  public interface Parameter extends SourceNode {
    boolean getExplicit();
  }

  public interface NameParameter extends Parameter, ReferableSourceNode {
  }

  public interface TypeParameter extends Parameter {
    @Nonnull Concrete.Expression getType();
  }

  public interface TelescopeParameter extends TypeParameter {
    @Nonnull List<? extends ReferableSourceNode> getReferableList();
  }

  // Definitions

  public interface ReferableSourceNode extends SourceNode {
    @Nullable default String getName() {
      return toString();
    }
  }

  public interface GlobalReferableSourceNode extends ReferableSourceNode {
  }

  public static Collection<? extends Parameter> getParameters(Abstract.Definition definition) {
    if (definition instanceof Abstract.FunctionDefinition) {
      return ((FunctionDefinition) definition).getParameters();
    }
    if (definition instanceof Abstract.DataDefinition) {
      return ((DataDefinition) definition).getParameters();
    }
    if (definition instanceof Abstract.Constructor) {
      return ((Constructor) definition).getParameters();
    }
    return null;
  }

  public interface Definition extends GlobalReferableSourceNode {
    @Nonnull Precedence getPrecedence();
    @Nullable Definition getParentDefinition();
    boolean isStatic();
    <P, R> R accept(AbstractDefinitionVisitor<? super P, ? extends R> visitor, P params);
  }

  public interface ClassField extends Definition {
    @Nonnull Concrete.Expression getResultType();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface Implementation extends Definition {
    @Nonnull ClassField getImplementedField();
    @Nonnull Concrete.Expression getImplementation();

    @Override
    ClassDefinition getParentDefinition();
  }

  public interface DefinitionCollection {
    @Nonnull Collection<? extends Definition> getGlobalDefinitions();
  }

  public interface FunctionBody extends SourceNode {}

  public interface TermFunctionBody extends FunctionBody {
    @Nonnull Concrete.Expression getTerm();
  }

  public interface ElimBody extends SourceNode {
    @Nonnull List<? extends Concrete.ReferenceExpression> getEliminatedReferences();
    @Nonnull List<? extends Concrete.FunctionClause> getClauses();
  }

  public interface ElimFunctionBody extends FunctionBody, ElimBody {
  }

  public interface FunctionDefinition extends Definition, DefinitionCollection {
    @Nonnull FunctionBody getBody();
    @Nonnull List<? extends Parameter> getParameters();
    @Nullable Concrete.Expression getResultType();
  }

  public interface DataDefinition extends Definition {
    @Nonnull List<? extends TypeParameter> getParameters();
    @Nullable List<? extends Concrete.ReferenceExpression> getEliminatedReferences();
    @Nonnull List<? extends Concrete.ConstructorClause> getConstructorClauses();
    boolean isTruncated();
    @Nullable Concrete.UniverseExpression getUniverse();
  }

  public interface Constructor extends Definition, ElimBody {
    @Nonnull DataDefinition getDataType();
    @Nonnull List<? extends TypeParameter> getParameters();
  }

  public interface SuperClass extends SourceNode {
    @Nonnull Concrete.Expression getSuperClass();
  }

  public interface ClassDefinition extends Definition, DefinitionCollection {
    @Nonnull List<? extends TypeParameter> getPolyParameters();
    @Nonnull Collection<? extends SuperClass> getSuperClasses();
    @Nonnull Collection<? extends ClassField> getFields();
    @Nonnull Collection<? extends Implementation> getImplementations();
    @Nonnull Collection<? extends Definition> getInstanceDefinitions();
  }

  // ClassViews

  public interface ClassView extends Definition {
    @Nonnull Concrete.ReferenceExpression getUnderlyingClassReference();
    @Nonnull String getClassifyingFieldName();
    @Nullable ClassField getClassifyingField();
    @Nonnull List<? extends ClassViewField> getFields();
  }

  public interface ClassViewField extends Definition {
    @Nonnull String getUnderlyingFieldName();
    @Nullable ClassField getUnderlyingField();
    @Nonnull ClassView getOwnView();
  }

  public interface ClassViewInstance extends Definition {
    boolean isDefault();
    @Nonnull List<? extends Parameter> getParameters();
    @Nonnull Concrete.ReferenceExpression getClassView();
    @Nonnull GlobalReferableSourceNode getClassifyingDefinition();
    @Nonnull Collection<? extends Concrete.ClassFieldImpl> getClassFieldImpls();
  }

  // Patterns

  public interface Pattern extends SourceNode {
    byte PREC = 11;
    boolean isExplicit();
  }

  public interface NamePattern extends Pattern, ReferableSourceNode {
  }

  public interface ConstructorPattern extends Pattern, Concrete.PatternContainer {
    @Nonnull String getConstructorName();
    @Nullable Abstract.Constructor getConstructor();

    @Nonnull
    @Override
    List<? extends Pattern> getPatterns();
  }

  public interface EmptyPattern extends Pattern {}


  public static class Precedence {
    public enum Associativity { LEFT_ASSOC, RIGHT_ASSOC, NON_ASSOC }

    public static final Precedence DEFAULT = new Precedence(Associativity.RIGHT_ASSOC, (byte) 10);

    public final @Nonnull Associativity associativity;
    public final byte priority;

    public Precedence(@Nonnull Associativity associativity, byte priority) {
      this.associativity = associativity;
      this.priority = priority;
    }

    @Override
    public String toString() {
      String result = "infix";
      if (associativity == Associativity.LEFT_ASSOC) {
        result += "l";
      }
      if (associativity == Associativity.RIGHT_ASSOC) {
        result += "r";
      }
      return result + " " + priority;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Precedence that = (Precedence) o;
      return priority == that.priority && associativity == that.associativity;
    }

    @Override
    public int hashCode() {
      return  31 * associativity.hashCode() + (int) priority;
    }
  }
}
