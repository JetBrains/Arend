package org.arend.naming.reference;

import org.arend.ext.reference.DataContainer;
import org.arend.ext.reference.Precedence;
import org.arend.naming.resolving.visitor.TypeClassReferenceExtractVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.AccessModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConcreteLocatedReferable extends LocatedReferableImpl implements DataContainer, TypedReferable {
  private final Object myData;
  private final String myAliasName;
  private final Precedence myAliasPrecedence;
  private Concrete.ReferableDefinition myDefinition;
  private String myDescription = "";

  public ConcreteLocatedReferable(Object data, AccessModifier accessModifier, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, LocatedReferable parent, Kind kind) {
    super(accessModifier, precedence, name, parent, kind);
    myData = data;
    myAliasName = aliasName;
    myAliasPrecedence = aliasPrecedence;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  @Override
  public @Nullable String getAliasName() {
    return myAliasName;
  }

  @Override
  public @NotNull Precedence getAliasPrecedence() {
    return myAliasPrecedence;
  }

  public Concrete.ReferableDefinition getDefinition() {
    return myDefinition;
  }

  @Override
  public @NotNull String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  public @NotNull TCDefReferable getTypecheckable() {
    return myDefinition == null ? this : myDefinition.getRelatedDefinition().getData();
  }

  public void setDefinition(Concrete.ReferableDefinition definition) {
    assert myDefinition == null;
    myDefinition = definition;
  }

  @Nullable
  @Override
  public ClassReferable getTypeClassReference() {
    return myDefinition == null ? null : myDefinition.accept(new TypeClassReferenceExtractVisitor(), null);
  }

  @Override
  public @Nullable Referable getBodyReference(TypeClassReferenceExtractVisitor visitor) {
    return myDefinition instanceof Concrete.FunctionDefinition function && function.getBody() instanceof Concrete.TermFunctionBody ? TypeClassReferenceExtractVisitor.getTypeReference(function.getBody().getTerm(), false) : null;
  }

  public List<ParameterReferable> makeParameterReferableList() {
    LocatedReferable parent = getLocatedReferableParent();
    if (!(parent instanceof ConcreteLocatedReferable)) return Collections.emptyList();
    Concrete.ReferableDefinition def = ((ConcreteLocatedReferable) parent).getDefinition();
    if (def == null) return Collections.emptyList();
    List<? extends Concrete.Parameter> parameters = def.getParameters();
    if (parameters.isEmpty()) return Collections.emptyList();

    Set<String> eliminated = new HashSet<>();
    Concrete.FunctionBody body = def instanceof Concrete.BaseFunctionDefinition ? ((Concrete.BaseFunctionDefinition) def).getBody() : null;
    if (body instanceof Concrete.ElimFunctionBody) {
      if (body.getEliminatedReferences().isEmpty()) return Collections.emptyList();
      for (Concrete.ReferenceExpression reference : body.getEliminatedReferences()) {
        eliminated.add(reference.getReferent().getRefName());
      }
    }

    List<ParameterReferable> result = new ArrayList<>();
    int i = 0;
    for (Concrete.Parameter parameter : parameters) {
      for (Referable referable : parameter.getReferableList()) {
        if (referable != null && !eliminated.contains(referable.getRefName())) {
          result.add(new ParameterReferable((ConcreteLocatedReferable) parent, i, referable, TypeClassReferenceExtractVisitor.getTypeReferenceExpression(parameter.getType(), true)));
        }
        i++;
      }
    }
    return result;
  }
}
