<h1 id="functions">Functions<a class="headerlink" href="#functions" title="Permanent link">&para;</a></h1>

Functions in Arend are functions in the mathematical sense.
They can have arbitrary arity.
In particular, constants in Arend are just functions of arity 0.
<!--
 A definition of a function consists of its name, a signature which consists of a list of parameters and (possibly) a result type, and a body which is an expression that describes the behaviour of the function.
-->
A definition of a function `f` consists of the signature of `f` followed by the body of `f`.
The full syntax of function signatures is as follows:   

```arend
\func f p_1 ... p_n : T
``` 
where `f` is the name of the function, `p_1`, ... `p_n` are named [parameters](/language-reference/definitions/parameters) and
`T` is the result type. In some cases specification `: T` of the result type can be omitted depending on the 
definition of function body.
<!--The syntax of function body depends on whether the function is defined by pattern matching.
, where `f` is the name of the function, `p_1`, ... `p_n` are named [parameters](/language-reference/definitions/parameters), and `e` is an expression which denotes the result of the function.
You can also specify the result type of the function by writing `\func f p_1 ... p_n : T => e`, where `T` is an expression which denotes the result type.
In this case, `e` must have type `T`.
Often the typechecker can infer the result type, so usually you don't have to specify it explicitly.
-->
There are several ways to define the body of a function. These ways are described below.   

## Non-recursive definitions

A non-recursive function can be defined simply by specifying an expression for the result of the function: 
```arend
\func f p_1 ... p_n : T => e
```
where `e` is an expression, which must be
of type `T` if it is specified. In such definitions the result type `: T` can often be omitted as the typechecker 
can usually infer it from `e`.

For example, to define the identity function on type `A`, write the following code:

```arend
\func id (x : A) => x
```

A function with three parameters that returns the second one can be defined as follows:

```arend
\func second (x : A) (y : B) (z : C) => y
```

You can explicitly specify the result types of these functions.
The definitions above are equivalent to the following definitions:

```arend
\func id (x : A) : A => x
\func second (x : A) (y : B) (z : C) : B => y
```

Parameters of a function may appear in its body and in its result type.

## Pattern matching

Functions can be defined by pattern matching.
If `D` is a [data type](/language-reference/definitions/data) with constructors `con1` and `con2 Nat`.
Then you can define a function which maps `D` to natural numbers in such a way that `con1` is mapped to `0` and `con2 n` is mapped to `suc n`:

```arend
\func f (d : D) : Nat
  | con1 => 0
  | con2 n => suc n
```

The result type of a function defined by pattern matching must be specified explicitly.
The general form of function definition by pattern matching is

<!--
```arend
\func f (x_1 : T_1) ... (x_n : T_n) : R
  | p^1_1, ... p^1_n => e_1
  ...
  | p^k_1, ... p^k_n => e_k
``` -->

```arend
\func f (x_1 : T_1) ... (x_n : T_n) : R
  | clause_1
  ...
  | clause_k
```

where each `clause_i` is of the form

```arend
p^i_1, ... p^i_n => e_i
```

where `p^i_j` is a pattern of type `T_i` and `e_i` is an expression of type `R[p^i_1/x_1, ... p^i_n/x_n]` (see [this section](/language-reference/expressions) for the discussion of the substitution operation and types of expressions).
Note that variables `x_1`, ... `x_n` are not visible in expressions `e_i`.
If a pattern `p^i_j` contains a variable `x` as a subpattern of type `T`, then this variable may appear in expression `e_i` and it will have the type `T`.
If some of the parameters of `f` are implicit, corresponding patterns must be either omitted or
specified explicitly by surrounding them in `{ }`.

Equivalently, the definition above can be written using the keyword `\with`:
 ```arend 
 \func f p_1 ... p_n : R \with { | clause_1 ... | clause_k } 
 ``` 

A pattern of type `T` can have one of the following forms:

* A variable. If this variable is not used anywhere, its name can be replaced with `_`.
* `con s_1 ... s_m`, where `con (y_1 : A_1) ... (y_m : A_m)` is a constructor of a data type `D` and `s_1` ... `s_m` are patterns.
  In this case, `T` must be equal to `D` and pattern `s_i` must have type `A_i[s_1/y_1, ... s_{i-1}/y_{i-1}]`.
  If some of the parameters of `con` are implicit, corresponding patterns must be omitted.
  They can be specified explicitly by surrounding them in `{ }`.
* `(s_1, ... s_m)`, where `s_1` ... `s_m` are patterns.
  In this case, `T` must be either a [Sigma type](/language-reference/expressions/sigma) with parameters `(y_1 : A_1) ... (y_m : A_m)` or a [class](/language-reference/definitions/classes) (or a [record](/language-reference/definitions/records)) with fields `y_1 : A_1`, ... `y_m : A_m`.
  The pattern `s_i` will have type `A_i[s_1/y_1, ... s_{i-1}/y_{i-1}]`.
  If `m` equals to 0, then `T` also may be a data type without constructors.
  In this case, the right hand side `=> e_i` of the clause in which such a pattern appears must be omitted.

Also, a constructor or a tuple pattern may be an _as-pattern_.
This means that there might be an expressions of the form `\as x : E` after the pattern, where `x` is a variable and `E` is its type which can be omitted.
Then `x` is equivalent to this pattern.

Now, let us discuss how expressions of the form `f a_1 ... a_n` evaluate (see [this section](/language-reference/expressions/#evaluation) for the definition of the reduction and evaluation relations).
To reduce an expression `E = f a_1 ... a_n`, we first evaluate expressions `a_1`, ... `a_n` and match them with the patterns in the definition of `f` left to right, top to bottom.
If all patterns `p^i_1`, ... `p^i_n` matches with `a_1`, ... `a_n` for some i, then `E` reduces to `e_i[b_1/y_1, ... b_k/y_k]`,
where `y_1`, ... `y_k` are variables that appear in `p^i_1`, ... `p^i_n` and `b_1`, ... `b_k` are subexpressions of `a_1`, ... `a_n` corresponding to these variables.
If some argument cannot be matched with a pattern `con s_1 ... s_m` because it is of the form `con' ...` for some constructor `con'` different from `con`, then the evaluator skips the clause with this patterns and tries the next one.
If some argument cannot be matched with a pattern because it is not a constructor, then `E` does not reduce.
If none of the clauses match with arguments, then `E` also does not reduce.
Variables and patterns of the form `(s_1, ... s_m)` match with any expression.

Let us consider an example.
Let `B` be a data type with two constructors `T` and `F`.
Consider the following function:

```arend
\func g (b b' : B) : Nat
  | T, _ => 0
  | _, T => 1
  | _, _ => 2
```

Let `x` be a variable and let `e` be an arbitrary expression.
If the first argument of `g a_1 a_2` is `T`, then the expression reduces to `0`, if it is `x`, then expression does not reduce since the first pattern fails to match with `x`.
If the first argument is `F`, then the evaluator tries to match the second argument:

```arend
g T e => 0
g x e -- does not reduce
g F T => 1
g F F => 2
g F x -- does not reduce
```

Note that patterns are matched left to right, top to bottom and not the other way around.
This means that even if a funcall matches the first clause, it may not evaluate.
For example, consider the following definition:

```arend
\func \infix 4 < (n m : Nat) : Bool
  | _, 0 => false
  | 0, suc _ => true
  | suc n, suc m => n < m
```

The funcall `n < 0` does not evaluate since it matches the first argument first, but funcalls `0 < 0` and `suc n < 0` both evaluate to `false`.

Sometimes you need to write a clause in which one of the parameters is a data type without constructors.
You can write pattern `()` which is called in this case _the absurd pattern_.
In this case, you must omit the right hand side of the clause.
For example, to define a function from the empty data type you can write:

```arend
\data Empty

\func absurd {A : \Type} (e : Empty) : A
  | ()
```

You can often (but not always) omit the clause with an abusrd pattern completely.
For example, you can define function `absurd` as follows:

```arend
\func absurd {A : \Type} (e : Empty) : A
```

## Elim

It is often true that we only need to pattern match on a single parameter of a function (or a few parameters), but the function has much more parameters.
Then we need to repeat parameters on which we do not pattern match in each clause, which is inconvenient.
In this case, we can use the `\elim` construction:

```arend
\func f (x_1 : A_1) ... (x_n : A_n) : R \elim x_{i_1}, ... x_{i_m}
  | p^1_1, ... p^1_m => e_1
  ...
  | p^k_1, ... p^k_m => e_k
```

where i\_1, ... i\_m are integers such that 1 ≤ i\_1 < ... < i\_m ≤ n.
In this case, parameters `x_{i_1}`, ... `x_{i_m}` are _eliminated_ and are not visible in expressions `e_1`, ... `e_k`.
Other parameters of `f` are still visible in these expressions.
Note that it does not matter whether a parameter `x_i` is explicit or implicit when it is eliminated; the corresponding pattern is always explicit.

As an example, consider the following function which chooses one of its arguments depending on the value of its other argument:

```arend
\func if (b : B) (t e : X) : X \elim b
  | T => t
  | F => e
```

## Recursive functions

Functions defined by pattern matching can be recursive.
That is, if `f` is a function as described above, then a reference to `f` may occur inside expressions `e_1`, ... `e_k`.
Every function in Arend is a total function.
Thus, not every recursive definition is allowed.
In order for such a definition to be valid, the recursion must be _structural_.
This roughly means that the arguments to recursive calls of `f` must be subexpressions of the arguments to the function itself.

Function may also be mutually recursive.
That is, we can have several functions which refer to each other.
In this case, there must be a linear order on the set of these functions `f_1`, ... `f_n` such that the signature of `f_i` refers only to previous functions.
The bodies of the functions may refer to each other as long as the whole recursive system is structural.

## Copattern matching

If the result type of a function is a [record](/language-reference/definitions/records) or a [class](/language-reference/definitions/classes), then a function can also be define by _copattern matching_ which has the following syntax:

```arend
\func f (x_1 : A_1) ... (x_n : A_n) : C \cowith
  | c_1
  ...
  | c_k
```

where `c_1`, ... `c_k` are _coclauses_.
A coclause is a pair consisting of a field `g` of `C` and an expression `e` written `g => e`.
Such a function has the same semantics as a definition of an instance, that is it is equivalent to the following definition:

```arend
\func f (x_1 : A_1) ... (x_n : A_n) => \new C {
  | c_1
  ...
  | c_k
}
```

See [this section](/language-reference/expressions/class-ext) for the description of the involved constructions.

## Lemmas

A lemma is a function that returns a proposition and does not evaluate.
To define a lemma use the keyword `\lemma` instead of `\func`.
If the result type of a lemma does not belong to `\Prop`, but is provably a proposition, you can use the keywords [\level](/language-reference/definitions/level/#level-of-a-type) to define a lemma with this result type.
The fact that lemmas do not evaluate may greatly improve performance of typechecking if their proofs are too lengthy.
