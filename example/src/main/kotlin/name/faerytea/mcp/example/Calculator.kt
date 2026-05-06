package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.ResourceAnnotation
import name.faerytea.mcp.annotations.ResourceTemplate
import name.faerytea.mcp.annotations.SafeTool
import java.math.BigInteger

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

val primesCache = ArrayList<BigInteger>(16).apply { add(BigInteger.TWO) }

/**
 * Returns [n]th prime.
 */
@ResourceTemplate(
    "prime://{n}/",
    mimeType = "text/plain",
    annotations = ResourceAnnotation(
        audience = ["user", "assistant"]
    )
)
fun prime(n: Int): String {
    var next = primesCache.last()
    outer@while (n > primesCache.size) {
        next += BigInteger.ONE
        var i = 0
        while (i < primesCache.size) {
            val prime = primesCache[i]
            if (prime * prime > next) break
            if (next.mod(prime) == BigInteger.ZERO) {
                continue@outer
            }
            ++i
        }
        println("New prime: $next")
        primesCache.add(next)
    }
    return primesCache[n - 1].toString().also { println("Returning $it") }
}
