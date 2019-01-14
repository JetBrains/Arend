<h1 id="records">Records<a class="headerlink" href="#records" title="Permanent link">&para;</a></h1>

A _record_ is a [sigma type](/language-reference/expressions/sigma) with named projections.
The basic syntax looks like this:

```arend
\record R (p_1 : A_1) ... (p_n : A_n) {
  | f_1 : B_1
  ...
  | f_k : B_k
}
```

You can also write `\field f_i : B_i` instead of `| f_i : B_i`, but there is a difference between these notations, which will be discussed [here](#fields-and-properties).

The type `A_i` can depend on variables `p_1`, ... `p_{i-1}`.
The type `B_i` can depend on variables `p_1`, ... `p_n`, `f_1`, ... `f_{i-1}`.
We call `f_1`, ... `f_k` _fields_ of `R`.
Records do not have parameters, so `p_1`, ... `p_n` are also fields of `R`.
The only difference between `p_i` and `f_j` is that `f_j` are visible in the scope of `R` and `p_i` are not:

```arend
\func test1 => f_1
\func test2 => R.f_1
\func test3 => R.p_1

-- p_1 is not in scope in the following function:
-- \func test4 => p_1
```

The record `R` is equivalent to the sigma type `\Sigma (p_1 : A_1) ... (p_n : A_n) (f_1 : B_1) ... (f_k : B_k)`.
To create an instance of this type you can use one of the options listed below, which give the same result.
See [this section](/language-reference/expressions/class-ext) for more information about these constructions.

```arend
\func r1 => \new R a_1 ... a_n { | f_1 => b_1 ... | f_k => b_k }
\func r2 => \new R { | p_1 => a_1 ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r3 => \new R a_1 ... a_n b_1 ... b_k
\func r4 => \new R a_1 ... a_i { | p_{i+1} => a_{i+1} ... | p_n => a_n | f_1 => b_1 ... | f_k => b_k }
\func r5 => \new R a_1 ... a_n b_1 ... b_i { f_{i+1} => b_{i+1} ... | f_k => b_k }
```

You can also define the same function using [copattern matching](/language-reference/definitions/functions/#copattern-matching):
```arend
\func r6 : R \cowith
  | p_1 => a_1
  ...
  | p_n => a_n
  | f_1 => b_1
  ...
  | f_k => b_k
```

Fields are projections:
```arend
\func test5 (x : R) => R.p_1 {x}
\func test6 (x : R) => f_1 {x}
```

You can also use the following syntax:
```arend
\func test5' (x : R) => x.p_1
\func test6' (x : R) => x.f_1
```

This syntax is allowed only when the expression before `.` is a variable with an explicitly specified type which is a record.
If `x : R` and `f` is a field of `R`, then `x.f` is equivalent to `R.f {x}`.

## Fields and properties

Some fields can be marked as a _property_.
To do this, you need to use the keyword `\property` instead of `\field`:

```arend
\record NegativeInt {
  \field x : Int
  \property isNeg : x < 0
}
```

The type of a property must be a proposition.
The field defined as `\field f : A` is not marked as a property and a field defined as `| f : A` is marked as a property if `A` is a proposition.
When you define a field as `\property f : A`, the typechecker produces an error message if `A` is not a proposition.

Properties do not evaluate.
Thus, they are related to fields in the same way as [lemmas](/language-reference/definitions/functions/#lemmas) are related to functions.
For example, consider the following function:

```arend
\func test (x : Int) (p : x < 0) => isNeg {\new NegativeInt x p}
```

Then `test x p` evaluates to `p` if `isNeg` is not a property and does not evaluate if it is.

## Extensions

TODO

## This

TODO
