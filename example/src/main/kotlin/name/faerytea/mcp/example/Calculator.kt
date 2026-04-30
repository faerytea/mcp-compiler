package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.SafeTool

enum class Op {
    ADD, MUL, MAX, MIN
}

/**
 * Apply operation to list of numbers
 */
@SafeTool
fun aggregate(
    @Description("Numbers")
    ints: IntArray,
    @Description("Operations")
    op: Op,
) = when (op) {
    Op.ADD -> ints.sum()
    Op.MUL -> {
        var res = 1
        for (e in ints) res *= e
        res
    }
    Op.MAX -> ints.max()
    Op.MIN -> ints.min()
}.toString()