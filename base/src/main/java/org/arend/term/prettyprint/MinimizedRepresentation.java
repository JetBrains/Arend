package org.arend.term.prettyprint;

import org.arend.core.context.binding.Binding;
import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.expr.DefCallExpression;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.expr.visitor.ExpressionVisitor;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.concrete.ConcreteFactory;
import org.arend.ext.error.GeneralError;
import org.arend.ext.prettyprinting.DefinitionRenamer;
import org.arend.ext.prettyprinting.PrettyPrinterConfig;
import org.arend.ext.prettyprinting.PrettyPrinterFlag;
import org.arend.ext.reference.ArendRef;
import org.arend.ext.variable.Variable;
import org.arend.extImpl.ConcreteFactoryImpl;
import org.arend.naming.reference.LocalReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.concrete.BaseConcreteExpressionVisitor;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.GoalError;
import org.arend.typechecking.error.local.inference.ArgInferenceError;
import org.arend.typechecking.error.local.inference.FunctionArgInferenceError;
import org.arend.typechecking.error.local.inference.InstanceInferenceError;
import org.arend.typechecking.instance.pool.GlobalInstancePool;
import org.arend.typechecking.instance.pool.LocalInstancePool;
import org.arend.typechecking.instance.provider.InstanceProvider;
import org.arend.typechecking.visitor.CheckTypeVisitor;
import org.arend.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

final public class MinimizedRepresentation {
    private MinimizedRepresentation() {
    }

    /**
     * Converts {@code expressionToPrint} to concrete, inserting as little additional information (like implicit arguments)
     * as possible. Resulting concrete expression is intended to be type checkable.
     */
    public static @NotNull Concrete.Expression generateMinimizedRepresentation(
            @NotNull Expression expressionToPrint,
            @Nullable InstanceProvider instanceProvider,
            @Nullable DefinitionRenamer definitionRenamer) {
        PrettyPrinterConfig verboseConfig;
        PrettyPrinterConfig emptyConfig;
        {
            var configs = getConfigs(definitionRenamer);
            verboseConfig = configs.proj1;
            emptyConfig = configs.proj2;
        }
        var verboseRepresentation = ToAbstractVisitor.convertExtended(expressionToPrint, verboseConfig, null);
        Map<Variable, LocalReferable> referableMapping = verboseRepresentation.getFreeVariables();
        var emptyRepresentation = ToAbstractVisitor.convertExtended(expressionToPrint, emptyConfig, verboseRepresentation.getFreeVariables());
        for (Variable binding : verboseRepresentation.getFreeVariables().keySet().toArray(new Variable[0])) {
            if (binding instanceof Binding && ((Binding) binding).getTypeExpr() != null) {
                ToAbstractVisitor.convertExtended(((Binding) binding).getTypeExpr(), verboseConfig, verboseRepresentation.getFreeVariables());
            }
        }
        List<GeneralError> errorsCollector = new ArrayList<>();
        Map<ArendRef, Expression> refToType = new LinkedHashMap<>();
        ExpressionVisitor<Void, Void> resolveVariablesVisitor = new VoidExpressionVisitor<>() {
            @Override
            public Void visitReference(ReferenceExpression expr, Void params) {
                if (referableMapping.containsKey(expr.getBinding())) {
                    expr.getType().accept(this, null);
                    refToType.putIfAbsent(referableMapping.get(expr.getBinding()), expr.getBinding().getTypeExpr());
                }
                return null;
            }
        };
        expressionToPrint.accept(resolveVariablesVisitor, null);


        var concreteFactory = new ConcreteFactoryImpl(null);
        List<Concrete.LetClause> clauses =
                refToType
                        .entrySet()
                        .stream()
                        .map(entry -> (Concrete.LetClause) concreteFactory.letClause(
                                entry.getKey(),
                                List.of(),
                                ToAbstractVisitor.convertExtended(entry.getValue(), verboseConfig, referableMapping).getConvertedExpression(),
                                concreteFactory.goal())
                        )
                        .collect(Collectors.toList());

        var checkTypeVisitor = new CheckTypeVisitor(error -> {
            if (!(error instanceof GoalError))
                errorsCollector.add(error);
        }, null, null);
        checkTypeVisitor.setInstancePool(new GlobalInstancePool(instanceProvider, checkTypeVisitor, new LocalInstancePool(checkTypeVisitor)));

        int limit = 50;
        while (true) {
            var result = tryFixError(checkTypeVisitor, verboseRepresentation.getConvertedExpression(), emptyRepresentation.getConvertedExpression(), clauses, concreteFactory, errorsCollector);
            if (result) {
                return emptyRepresentation.getConvertedExpression();
            }
            --limit;
            if (limit == 0) {
                throw new AssertionError("Minimization of expression (" + expressionToPrint + ") is likely diverged. Please report it to maintainers.");
            }
        }
    }

    private static Pair<PrettyPrinterConfig, PrettyPrinterConfig> getConfigs(DefinitionRenamer definitionRenamer) {
        return new Pair<>(new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.of(PrettyPrinterFlag.SHOW_TYPES_IN_LAM, PrettyPrinterFlag.SHOW_CASE_RESULT_TYPE, PrettyPrinterFlag.SHOW_CON_PARAMS, PrettyPrinterFlag.SHOW_BIN_OP_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_COERCE_DEFINITIONS, PrettyPrinterFlag.SHOW_GLOBAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_IMPLICIT_ARGS, PrettyPrinterFlag.SHOW_LOCAL_FIELD_INSTANCE, PrettyPrinterFlag.SHOW_TUPLE_TYPE);
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        }, new PrettyPrinterConfig() {
            @Override
            public @NotNull EnumSet<PrettyPrinterFlag> getExpressionFlags() {
                return EnumSet.noneOf(PrettyPrinterFlag.class);
            }

            @Override
            public @Nullable DefinitionRenamer getDefinitionRenamer() {
                return definitionRenamer;
            }
        });
    }

    private static boolean tryFixError(CheckTypeVisitor checkTypeVisitor, Concrete.Expression completeConcrete, Concrete.Expression minimizedConcrete, List<Concrete.LetClause> clauses, ConcreteFactoryImpl concreteFactory, List<GeneralError> errorsCollector) {
        var finalizedConcrete = (Concrete.LetExpression) concreteFactory.letExpr(false, false, clauses, minimizedConcrete);
        checkTypeVisitor.finalCheckExpr(finalizedConcrete, null);
        if (!errorsCollector.isEmpty()) {
            minimizedConcrete.accept(new ErrorFixingConcreteExpressionVisitor(errorsCollector, concreteFactory), completeConcrete);
            return false;
        } else {
            return true;
        }
    }
}

class ErrorFixingConcreteExpressionVisitor extends BaseConcreteExpressionVisitor<Concrete.SourceNode> {

    private final List<GeneralError> myErrors;
    private final ConcreteFactory myFactory;

    public ErrorFixingConcreteExpressionVisitor(List<GeneralError> myErrors, ConcreteFactory myFactory) {
        this.myErrors = myErrors;
        this.myFactory = myFactory;
    }

    private <T extends Concrete.Parameter> void visitParameters(List<T> actualParameters, List<T> completeParameters) {
        for (int i = 0; i < actualParameters.size(); ++i) {
            var param = actualParameters.get(i);
            if (param instanceof Concrete.TypeParameter) {
                ((Concrete.TypeParameter) param).type.accept(this, ((Concrete.TypeParameter) completeParameters.get(i)).type);
            }
        }
    }

    @Override
    public Concrete.Expression visitPi(Concrete.PiExpression expr, Concrete.SourceNode params) {
        var verboseExpr = (Concrete.PiExpression) params;
        visitParameters(expr.getParameters(), verboseExpr.getParameters());
        expr.codomain.accept(this, verboseExpr.codomain);
        return expr;
    }

    @Override
    public Concrete.Expression visitLam(Concrete.LamExpression expr, Concrete.SourceNode params) {
        var verboseExpr = (Concrete.LamExpression) params;
        visitParameters(expr.getParameters(), verboseExpr.getParameters());
        expr.body.accept(this, verboseExpr.body);
        return expr;
    }

    @Override
    public Concrete.Expression visitApp(Concrete.AppExpression expr, Concrete.SourceNode params) {
        Concrete.AppExpression verboseExpr = (Concrete.AppExpression) params;
        if (expr.getFunction() instanceof Concrete.ReferenceExpression) {
            var errorList = myErrors.stream().filter(err -> err.getCauseSourceNode() == expr.getFunction()).collect(Collectors.toList());
            if (!errorList.isEmpty()) {
                GeneralError mostImportantError = findMostImportantError(errorList);
                myErrors.clear();
                var fixed = fixError(expr, verboseExpr, mostImportantError);
                expr.getArguments().clear();
                expr.setFunction(fixed);
                return expr;
            }
        }
        var allArguments = verboseExpr.getArguments().iterator();
        for (var argument : expr.getArguments()) {
            var currentArgument = allArguments.next();
            while (currentArgument.isExplicit() != argument.isExplicit()) {
                currentArgument = allArguments.next();
            }
            argument.expression = argument.expression.accept(this, currentArgument.expression);
        }

        expr.setFunction(expr.getFunction().accept(this, verboseExpr.getFunction()));

        return expr;
    }

    private static GeneralError findMostImportantError(List<GeneralError> errors) {
        return errors
                .stream()
                .filter(err -> err instanceof InstanceInferenceError)
                .findFirst()
                .orElse(errors
                        .stream()
                        .filter(err -> err instanceof ArgInferenceError)
                        .findFirst()
                        .orElse(errors.get(0))
                );
    }

    Concrete.AppExpression fixError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, GeneralError error) {
        if (error instanceof InstanceInferenceError) {
            return fixInstanceInferenceError(incomplete, complete, (InstanceInferenceError) error);
        } else if (error instanceof FunctionArgInferenceError) {
            return fixFunctionArgInferenceError(incomplete, complete, (FunctionArgInferenceError) error);
        } else {
            return fixUnknownError(incomplete, complete);
        }
    }

    private Concrete.AppExpression fixUnknownError(Concrete.AppExpression incomplete, Concrete.AppExpression complete) {
        var args = new ArrayList<Concrete.Argument>();
        var parentIterator = incomplete.getArguments().iterator();
        var parentProperIterator = complete.getArguments().iterator();
        var currentActualArg = parentIterator.next();
        while (parentProperIterator.hasNext()) {
            var currentProperArg = parentProperIterator.next();
            if (currentActualArg == null || (currentProperArg.isExplicit() == currentActualArg.isExplicit())) {
                args.add(currentActualArg);
                currentActualArg = parentIterator.hasNext() ? parentIterator.next() : null;
            } else {
                args.add(currentProperArg);
            }
        }
        return (Concrete.AppExpression) myFactory.app(incomplete.getFunction(), args);
    }

    private Concrete.AppExpression fixFunctionArgInferenceError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, FunctionArgInferenceError targetError) {
        var definition = targetError.definition;
        var args = new ArrayList<Concrete.Argument>();
        var fullIterator = new ArgumentMappingIterator(definition, complete);
        var incompleteIterator = new ArgumentMappingIterator(definition, incomplete);
        var inserted = false;
        var i = definition instanceof ClassField ? 0 : 1;
        while (fullIterator.hasNext()) {
            var fullArg = fullIterator.next().proj2;
            var incompleteArg = incompleteIterator.next().proj2;
            if (i == targetError.index) {
                args.add((Concrete.Argument) fullArg);
                inserted = true;
            } else if (incompleteArg != null) {
                args.add((Concrete.Argument) incompleteArg);
            } else if (!inserted) {
                args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), fullArg.isExplicit()));
            }
            i += 1;
        }
        return (Concrete.AppExpression) myFactory.app(incomplete.getFunction(), args);
    }

    private Concrete.AppExpression fixInstanceInferenceError(Concrete.AppExpression incomplete, Concrete.AppExpression complete, InstanceInferenceError targetError) {
        var function = (Concrete.ReferenceExpression) incomplete.getFunction();
        var definition = ((TCDefReferable) function.getReferent()).getTypechecked();
        var args = new ArrayList<Concrete.Argument>();
        var fullIterator = new ArgumentMappingIterator(definition, complete);
        var incompleteIterator = new ArgumentMappingIterator(definition, incomplete);
        var inserted = false;
        while (fullIterator.hasNext()) {
            var fullResult = fullIterator.next();
            var incompleteResult = incompleteIterator.next();
            var param = fullResult.proj1;
            if (param instanceof DependentLink && ((DependentLink) param).getType() instanceof DefCallExpression && ((DefCallExpression) ((DependentLink) param).getType()).getDefinition() == targetError.classRef.getTypechecked()) {
                args.add((Concrete.Argument) fullResult.proj2);
                inserted = true;
            } else if (incompleteResult.proj2 != null) {
                args.add((Concrete.Argument) incompleteResult.proj2);
            } else if (!inserted) {
                args.add((Concrete.Argument) myFactory.arg(myFactory.hole(), fullResult.proj2.isExplicit()));
            }
        }
        return (Concrete.AppExpression) myFactory.app(function, args);
    }
}
