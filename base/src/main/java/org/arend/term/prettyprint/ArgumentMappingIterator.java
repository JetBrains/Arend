package org.arend.term.prettyprint;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.UntypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.expr.type.Type;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.context.CoreParameter;
import org.arend.term.concrete.Concrete;
import org.arend.ext.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArgumentMappingIterator implements Iterator<Pair<CoreParameter, ConcreteArgument>> {
    private final Iterator<DependentLink> coreParameterIterator;
    private final ListIterator<Concrete.Argument> argumentsIterator;

    public ArgumentMappingIterator(Definition definition, Concrete.AppExpression call) {
        coreParameterIterator = getParameters(definition).iterator();
        argumentsIterator = call.getArguments().listIterator();
    }

    @Override
    public boolean hasNext() {
        return coreParameterIterator.hasNext() || argumentsIterator.hasNext();
    }

    @Override
    public Pair<@Nullable CoreParameter, @Nullable ConcreteArgument> next() {
        var currentParameter = coreParameterIterator.hasNext() ? coreParameterIterator.next() : null;
        var currentArgument = argumentsIterator.hasNext() ? argumentsIterator.next() : null;
        if (currentParameter == null) {
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

    private List<DependentLink> linkToList(DependentLink link) {
        ArrayList<DependentLink> list = new ArrayList<>();
        while (link.hasNext()) {
            list.add(link);
            link = link.getNext();
        }
        return list;
    }

    private Iterable<DependentLink> getParameters(@NotNull Definition definition) {
        if (definition instanceof ClassField) {
            Expression type = ((ClassField) definition).getType(definition.makeIdLevels());
            ArrayList<DependentLink> parameters = new ArrayList<>();
            do {
                parameters.add(((PiExpression) type).getParameters());
                type = ((PiExpression) type).getCodomain();
            } while (type instanceof PiExpression);
            return parameters;
        } else if (definition instanceof Constructor) {
            var datatype = ((Constructor) definition).getDataType();
            return Stream.concat(linkToList(datatype.getParameters()).stream().map(link -> new UntypedDependentLink(link.getName()) {
                @Override
                public Type getType() {
                    return link.getType();
                }

                @Override
                public boolean isExplicit() {
                    return false;
                }
            }), linkToList(definition.getParameters()).stream()).collect(Collectors.toList());
        } else {
            return linkToList(definition.getParameters());
        }
    }
}
