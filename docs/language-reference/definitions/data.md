<h1 id="data">Inductive types<a class="headerlink" href="#data" title="Permanent link">&para;</a></h1>

Inductive and [higher inductive](/language-reference/definitions/hits) types are represented
by data definitions.
The basic syntax of a data definition looks like this:

```arend
\data D p_1 ... p_n
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

where `p_1`, ... `p_n`, `p^1_1`, ... `p^m_{k_m}` are either named or unnamed [parameters](/language-reference/definitions/parameters).
There are several extensions of this syntax which are discussed further in this module and in the module on 
[HITs](/language-reference/definitions/hits).
Each row `con_i p^i_1 ... p^i_{k_i}` defines a constructor `con_i` with the specified parameters.
Parameters `p_1`, ... `p_n` are parameters of the data type `D`, but they also become implicit parameters of
the constructors.

Let `A`, ... `F` be some types and let `a`, ... `f` be terms of the corresponding types.
Consider the following example:

```arend
\data Data A B
  | cons {C} D E
  | cons'

\func s1 => Data a b
\func s2 => cons d e
\func s2' => cons {a} d e
\func s2'' => cons {a} {b} {c} d e
\func s3 => cons'
\func s3' => cons' {a}
\func s3'' => cons' {a} {b}
```

Constructor `cons` has three implicit parameters of types `A`, `B`, and `C` and two explicit parameters of types `D` and `E`.
Constructor `cons'` has only two implicit parameters of types `A` and `B`.

The type of a [defcall](/language-reference/expressions/#defcalls) `con_i {a_1} ... {a_n} b_1 ... b_{k_i}` is `D a_1 ... a_n`.
The type of a defcall `D a_1 ... a_n` is of the form [\h-Type p](/language-reference/expressions/universes).
Levels `h` and `p` are inferred automatically and may depend on levels of `a_1,...a_n`.
Alternatively, these levels can be fixed and specified explicitly in the definition of a data type:

```arend
\data D p_1 ... p_n : \h-Type p
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

If the actual type of `D` does not always fit into the specified levels, the typechecker will generate an error message.

Constructors belong to the [module](/language-reference/definitions/modules) associated to the data definition, but they are also visible in the module in which the data type is defined:

```arend
\data D | con1 | con2

\func f => con1
\func g => con2
\func f' => D.con1
\func g' => D.con2
```

In the example above, we defined a data type `D` with two constructors `con1` and `con2`.
Functions `f` and `f'` (as well as `g` and `g'`) are equivalent.

## Inductive definitions

A data definition can be recursive, that is `D` may appear in parameters `p^1_1`, ... `p^m_{k_m}` (but not in `p_1`, ... `p_n`).
Such recursive definitions are called _inductive data types_.
There is one restriction for such definitions: recursive calls to `D` may occur only in _strictly positive_ positions.
The set of strictly positive positions is defined inductively:

* `D` occurs only in strictly positive positions in `D a_1 ... a_n` if it does not occur in `a_1`, ... `a_n`.
* `D` occurs only in strictly positive positions in `\Pi (x : A) -> B` if it occurs only in strictly positive positions in `B` and does not occur in `A`.
* `D` occurs only in strictly positive positions in `Path (\lam i => B) b b'` if it occurs only in strictly positive positions in `B` and does not occur in `b` and `b'`.
* `D` occurs only in strictly positive positions in any other expression if it does not occur in it.

## Truncation

Data types can be truncated to a specified homotopy level, which is less than its actual level.
This is done by specifying explicitly the type of a data definition and writing the keyword `\truncated` before the definition:

```arend
\truncated \data D p_1 ... p_n : \h-Type p
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

If the actual predicative level of `D` is greater than `p`, the typechecker will generate an error message, whereas `h` can be any number.
Such a data type can be eliminated only into types of the same homotopy level.
Consider the following example:

```arend
\truncated \data Exists (A : \Type) (B : A -> \Type) : \Prop
  | witness (a : A) (B a)

{-
-- This function will not typecheck.
\func extract (p : Exists (n : Nat) (n = 3)) : Nat
  | witness a b => a
-}

\func existsSuc (p : Exists (n : Nat) (n = 3)) : Exists (n : Nat) (suc n = 4)
  | witness n p => witness (suc n) (path (\lam i => suc (p @ i)))


\func existsEq (p : Exists (n : Nat) (n = 3)) : 0 = 0
  | witness n p => path (\lam _ => 0)
```

The data type `Exists` defines a proposition of the form 'There is an `a : A` such that `B a`'. Note that a function like
`extract`, which extracts `n : Nat` such that `n=3` out of a proof of `Exists (n:Nat) (n=3)`, is not valid
as its result type `Nat` is of homotopy level of a set (h=0), which is greater than the homotopy level of a 
proposition (h=-1). Two other functions `existsSuc` and `existsEq` in the example above are correct as 
their result types, `Exists (n : Nat) (suc n = 4)` and `0=0` respectively, are propositions.

A truncated data type is (provably) equivalent to the truncation of the untruncated version of this data type.
Thus, this is simply a syntactic sugar that allows to define functions over a truncated data type more easily.

## Induction-induction and induction-recursion

Two or more data types can be mutually recursive.
This is called _induction-induction_.
Just as simply inductive definitions, inductive-inductive definitions also must satisfy a strict positivity condition.
Namely, recursive calls to the definition itself and to other recursive definitions may occur only in strictly positive
positions.

Data types may also be mutually recursive with functions.
This is called _induction-recursion_.
Strict positivity and termination checkers work as usual for such definitions.

## Varying number of constructors

Sometimes there might be a need to define a data type, which has different constructors depending on its parameters.
The classical example is the data type of lists of fixed length.
The data type `Vec A 0` has only one constructor `nil`, the empty list.
The data type `Vec A (suc n)` also has one constructor `cons`, a non-empty list.
Such a data type can be defined by 'pattern-matching':

```arend
\data Vec (A : \Type) (n : Nat) \elim n
  | 0 => nil
  | suc n => cons A (Vec A n)
```

The general syntax is similar to the syntax of functions defined by 
[pattern-matching](/language-reference/definitions/functions).
Either `\elim vars` or `\with` constructs can be used with the only difference that 
`\elim vars` allows to match on a proper subset of parameters of data type.

```arend
\data D p_1 ... p_n \with
  | t^1_1, ... t^1_n => con_1 p^1_1 ... p^1_{k_1}
  ...
  | t^m_1, ... t^m_n => con_m p^m_1 ... p^m_{k_m}
```

Each clause starts a list of patterns, followed by `=>`, followed by a constructor definition.
The order of clauses does not matter.
If a clause matches the arguments of a defcall `D a_1 ... a_n`, then the corresponding constructor is added to this data type.
For example, one can define the following data type:

```arend
\data Bool | true | false

\data T (b : Bool) \with
  | true => con1
  | true => con2
```

Data type `T true` has two constructors: `con1` and `con2`.
Data type `T false` is empty.
It is also possible to define several constructors in a single clause as follows:

```arend
\data T (b : Bool) \with
  | true => {
    | con1
    | con2
  }
```

This definition is equivalent to the previous one.

