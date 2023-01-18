Dissonance provides only a single method, `diff`, which takes a `left` and
`right` parameter, both sequences of the same type, and generates a sequence of
the edits required to transform the left sequence into the right sequence. Each
element of the resultant sequence is either a `Keep` value, corresponding to a
value in both the left and right sequences, an `Ins` value (for _insertions_)
which exists only in the right sequence, or a `Del` value (for _deletions_)
which exists only in the left sequence.

The naming of these enumeration cases corresponds to a translation of the left
sequence into the right sequence, but could describe a translation from the
right sequence to the left if the roles are reversed. The `Diff#flip` method
can automatically reverse the translation.

Each of the three possible `Change` cases, `Ins`, `Del` and `Keep` includes the
relevant value, as well as the indices of that value in each sequence it exists
in: for `Ins`, the right; for `Del`, the left, and for `Keep` both a `left` and
`right` index.

### Custom equality

By default, elements of the left and right sequences will be considered _the
same_ (producing `Keep` values) if they are equal according to Java's universal
equality method, `AnyRef#equals`. However, other forms of equality (or
similarity) may exist, and it may be useful to consider two elements to be _the
same_, even if they are not equal. A common example would be if they had the
same ID, even if their content is different. That would introduce the
possibility to do deeper comparisons on those values.

The `diff` method takes an optional third parameter, `cmp`, of type `(T, T) ->
Boolean` which determines whether two elements are _the same_ for the purposes
of the diff.
