package org.arend.term.prettyprint;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Definition;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.context.CoreParameter;
import org.arend.term.concrete.Concrete;
import org.arend.util.Pair;

import java.util.Iterator;
import java.util.ListIterator;

public class ArgumentMappingIterator implements Iterator<Pair<CoreParameter, ConcreteArgument>> {
    private DependentLink coreParameter;
    private final ListIterator<Concrete.Argument> argumentsIterator;

    public ArgumentMappingIterator(Definition definition, Concrete.AppExpression call) {
        coreParameter = definition.getParameters();
        argumentsIterator = call.getArguments().listIterator();
    }

    @Override
    public boolean hasNext() {
        return coreParameter.hasNext() || argumentsIterator.hasNext();
    }

    @Override
    public Pair<CoreParameter, ConcreteArgument> next() {
        var currentParameter = coreParameter;
        coreParameter = coreParameter.hasNext() ? coreParameter.getNext() : coreParameter;
        var currentArgument = argumentsIterator.hasNext() ? argumentsIterator.next() : null;
        if (currentParameter instanceof EmptyDependentLink) {
            return new Pair<>(null, currentArgument);
        }
        if (currentArgument == null) {
            return new Pair<>(currentParameter, null);
        }
        if (currentParameter.isExplicit() == currentArgument.isExplicit()) {
            return new Pair<>(currentParameter, currentArgument);
        } else {
            argumentsIterator.previous();
            return new Pair<>(currentParameter, null);
        }
    }
}
