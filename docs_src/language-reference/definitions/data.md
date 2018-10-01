<h1 id="data">Data Definitions<a class="headerlink" href="#data" title="Permanent link">&para;</a></h1>

Data definitions generalize allow us to define (higher) inductive types.
Each data definition has several constructors.
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

## Syntax

The basic syntax of a data definition looks like this:

```arend
\data D p_1 ... p_n
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

where `p_1`, ... `p_n`, `p^1_1`, ... `p^m_{k_m}` are either named or unnamed [parameters](/language-reference/definitions/parameters).
There are several extensions of this syntax which we will discuss later.
Each row `con_i p^i_1 ... p^i_{k_i}` defines a constructor `con_i` with the specified parameters.
Parameters `p_1`, ... `p_n` are parameters of the data type `D`, but they also become implicit parameters of constructors.

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
Levels `h` and `p` are calculated automatically, but you can also specify explicitly the level of a data type:

```arend
\data D p_1 ... p_n : \h-Type p
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

If the actual type of `D` does not fit into the specified levels, the typechecker will generate an error message.

## Inductive definitions

A data definition can be recursive, that is `D` may appear in parameters `p^1_1`, ... `p^m_{k_m}` (but not in `p_1`, ... `p_n`).
Such recursive definitions are called _inductive data types_.
They have one restriction: recursive calls to `D` may occur only in strictly positive positions.
The set of such positions is defined inductively:

* `D` occurs only in strictly positive positions in `D a_1 ... a_n` if it does not occur in `a_1`, ... `a_n`.
* `D` occurs only in strictly positive positions in `\Pi (x : A) -> B` if it occurs only in strictly positive positions in `B` and does not occur in `A`.
* `D` occurs only in strictly positive positions in `Path (\lam i => B) b b'` if it occurs only in strictly positive positions in `B` and does not occur in `b` and `b'`.
* `D` occurs only in strictly positive positions in any other expression if it does not occur in it.

## Truncation

You can truncate data types to a specified homotopy level which is less than its actual level.
To do this, specify explicitly the type of a data definition and write keyword `\truncated` before the definition:

```arend
\truncated \data D p_1 ... p_n : \h-Type p
  | con_1 p^1_1 ... p^1_{k_1}
  ...
  | con_m p^m_1 ... p^m_{k_m}
```

If the actual predicative level of `D` is greater than `p`, the typechecker will generate an error message, but `h` can be any number.
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

The data type `Exists` defines a proposition 'There is an `a : A` such that `B a`'.
The function `extract` which extracts a natural number from the proof that there exists a natural number which equals to 3 is incorrect,
but functions `existsSuc` and `existsEq` which construct another proofs are accepted.

A truncated data type is (provably) equivalent to the truncation of the untrancated version of this data type.
So, this is simply a syntax sugar that allows you to define functions over a truncated data type more easily.

## Induction-induction and induction-recursion

Two or more data types can be mutually recursive.
This is called _induction-induction_.
Inductive-inductive definitions also must be strictly positive.
That is, recursive calls to the definition itself and to other recursive definitions may occur only in strictly positive positions.

Data types may also be mutually recusrive with functions.
This is called _induction-recursion_.
Strict positivity and termination checkers work as usual for such definitions.

## Varying number of constructors

Sometimes we need to define a data type which has different constructors depending on its parameters.
The classical example is the data type of lists of fixed length.
The data type `Vec A 0` has only one constructor `nil`, the empty list.
The data type `Vec A (suc n)` also has one constructor `cons`, a non-empty list.
We can define such a data type by 'pattern-matching':

```arend
\data Vec (A : \Type) (n : Nat) \elim n
  | 0 => nil
  | suc n => cons A (Vec A n)
```

The general syntax is similar to the syntax of functions defined by pattern-matching.
After the list of parameters we can write either `\elim vars` or `\with` followed by a list of clause.

```arend
\data D p_1 ... p_n \with
  | t_1, ... t_n => con_1 p^1_1 ... p^1_{k_1}
  ...
  | t_1', ... t_n' => con_m p^m_1 ... p^m_{k_m}
```

Each clause has a list of patterns, followed by `=>`, followed by a constructor definition.
The order of clauses does not matter.
If a clause matches the arguments of a defcall `D a_1 ... a_n`, then the corresponding constructor is added to this data type.
For example, we can define the following data type:

```arend
\data Bool | true | false

\data T (b : Bool) \with
  | true => con1
  | true => con2
```

Data type `T true` has two constructors: `con1` and `con2`.
Data type `T false` is empty.
We can also define several constructors in a single clause as follows:

```arend
\data T (b : Bool) \with
  | true => {
    | con1
    | con2
  }
```

This definition is equivalent to the previous one.
