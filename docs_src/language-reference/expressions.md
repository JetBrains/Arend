<h1 id="expressions">Expressions<a class="headerlink" href="#expressions" title="Permanent link">&para;</a></h1>

An expression denotes a value which may depend on some variables.
The basic example of an expression is simply a variable `x`.
Of course, `x` must be defined somewhere in order for such an expression to make sense.
It can be either a parameter (of a [definition](/language-reference/definitions/parameters), or a [lambda expression](/language-reference/expressions/pi), or a [pi expression](/language-reference/expressions/pi), or a [sigma expression](/language-reference/expressions/sigma)),
a variable define in a [let expression](/language-reference/expressions/let), or a variable defined in a [pattern](/language-reference/definitions/functions/#pattern-matching).

If `e`, `e_1`, ... `e_n` are expressions and `x_1`, ... `x_n` are variables, then we will write `e[e_1/x_1, ... e_n/x_n]` for the _substitution_ operation.
This is a meta-operation, that is it is a function on the set of expressions of the language and not an expression itself.
The expression `e[e_1/x_1, ... e_n/x_n]` is simply `e` in which every occurrence of each of the variables `x_i` is replaced with the expression `e_i`.

## Evaluation

There is a binary relation `=>` on the set of expressions called the _reduction relation_.
If `e_1 => ... => e_n`, we will say that `e_1` _reduces_ to `e_n`.
If there is no `e'` such that `e => e'`, we will say that `e` is a _normal form_.
If `e` reduces to `e'` and `e'` is a normal form, we will say that `e'` is a _normal form_ of `e` and that `e` _evaluates_ to `e'`.
Every expression has a unique normal form.

The relation `=>` is a meta-relation on the set of expressions of the language, that is you cannot refer to it explicitly in the language.
This relation is used by the typechecker to compare expressions.
The typechecker never compares expressions directly.
To compare expressions `e_1` and `e_2`, it first evaluates their normal forms and then compares them.
Since normal forms always exist, the comparison algorithm always terminates, but it is easy to write an expression that does not evaluate in any reasonable time.

## Types

Every expression has a type.
The fact that an expression `e` has type `E` is denoted by `e : E`.
A type is an expression which has type `\Type`.
The expression `\Type` is discussed in [this section](/language-reference/expressions/universes).
Every variable has a type which is specified when the variable is defined (or can be inferred).
An expression of the form `x` has the type of the variable `x`.

The type of an expression usually can be inferred automatically, but sometimes it is useful to specify it explicitly.
An expression of the form `(e : E)` (parentheses are necessary) is equivalent to `e`, but also has an explicit type annotation.
In this expression, `e` must have type `E` and the type of the whole expression is also `E` (since it is equivalent to `e`).

## Defcalls

A _defcall_ is an expression of the form `f a_1 ... a_n`, where `f` is a definition with n parameters
(an exception is classes and records in which case only expressions without arguments are called defcalls, see [this section](/language-reference/expressions/class-ext) for the discussion of such expressions).

If `f` is a definition with parameters `x_1`, ... `x_n` and the result type `R`, then the type of a defcall `f a_1 ... a_n` is `R[a_1/x_1, ... a_n/x_n]`.
If `f` is either a class, a record, a data type, a constructor without conditions, an instance, or a function defined by copattern matching, then `f a_1 ... a_n` is a normal form whenever `a_1`, ... `a_n` are.
If `f` is a function defined as `\func f (x_1 : A_1) ... (x_n : A_n) => e`, then `f a_1 ... a_n` reduces to `e[a_1/x_1, ... a_n/x_n]`.
If `f` is a function defined by pattern matching or a constructor with conditions, then the evaluation of defcalls `f a_1 ... a_n` is described in [this section](/language-reference/definitions/functions/#pattern-matching).
If `f` is an instance or a function defined by copattern matching, then the evaluation of defcalls `f a_1 ... a_n` is described in [this section](/language-reference/definitions/instances).

If `f` has n parameters and k < n, you can write `f a_1 ... a_k` and such an expression is equivalent to `\lam a_{k+1} ... a_n => f a_1 ... a_n`.
