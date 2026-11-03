package com.coverit.app.pdf

data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {

    val height get() = bottom - top

    fun union(other: Box) = Box(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom),
    )

    fun intersects(other: Box) =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun padded(amount: Float) = Box(left - amount, top - amount, right + amount, bottom + amount)
}
