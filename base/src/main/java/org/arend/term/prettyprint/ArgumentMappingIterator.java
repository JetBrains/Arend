package org.arend.term.prettyprint;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.UntypedDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.definition.FunctionDefinition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.PiExpression;
import org.arend.core.expr.type.Type;
import org.arend.core.subst.LevelPair;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.context.CoreParameter;
import org.arend.term.concrete.Concrete;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.ListIterator;

public class ArgumentMappingIterator implements Iterator<Pair<CoreParameter, ConcreteArgument>> {
    private final Iterator<DependentLink> coreParameter;
    private final ListIterator<Concrete.Argument> argumentsIterator;

    public ArgumentMappingIterator(Definition definition, Concrete.AppExpression call) {
        coreParameter = getParameters(definition);
        argumentsIterator = call.getArguments().listIterator();
    }

    @Override
    public boolean hasNext() {
        return coreParameter.hasNext() || argumentsIterator.hasNext();
    }

    @Override
    public Pair<CoreParameter, ConcreteArgument> next() {
        var currentParameter = coreParameter.hasNext() ? coreParameter.next() : null;
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

    private Iterator<DependentLink> getParameters(@NotNull Definition definition) {
        if (definition instanceof FunctionDefinition) {
            return new Iterator<>() {
                DependentLink param = definition.getParameters();

                @Override
                public boolean hasNext() {
                    return param.hasNext();
                }

                @Override
                public DependentLink next() {
                    var current = param;
                    param = param.getNext();
                    return current;
                }
            };
        } else if (definition instanceof ClassField) {
            var type = ((ClassField) definition).getType(LevelPair.STD);
            return new Iterator<>() {
                Expression piExpression = type;

                @Override
                public boolean hasNext() {
                    return piExpression instanceof PiExpression;
                }

                @Override
                public DependentLink next() {
                    var current = ((PiExpression) piExpression).getParameters();
                    piExpression = ((PiExpression) piExpression).getCodomain();
                    return current;
                }
            };
        } else if (definition instanceof Constructor) {
            var datatype = ((Constructor) definition).getDataType();
            return new Iterator<>() {
                DependentLink datatypeParams = datatype.getParameters();
                DependentLink constructorParams = definition.getParameters();

                @Override
                public boolean hasNext() {
                    return datatypeParams.hasNext() || constructorParams.hasNext();
                }

                @Override
                public DependentLink next() {
                    DependentLink currentLink;
                    if (datatypeParams.hasNext()) {
                        var currentDatatypeParams = datatypeParams;
                        currentLink = new UntypedDependentLink(currentDatatypeParams.getName()) {
                            @Override
                            public Type getType() {
                                return currentDatatypeParams.getType();
                            }

                            @Override
                            public boolean isExplicit() {
                                return false;
                            }
                        };
                        datatypeParams = datatypeParams.getNext();
                    } else {
                        currentLink = constructorParams;
                        constructorParams = constructorParams.getNext();
                    }
                    return currentLink;
                }
            };
        }
        throw new AssertionError("No other definitions should appear");
    }
}
