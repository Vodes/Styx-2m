package moe.styx.styx2m.misc

inline fun Float.ifInvalid(newValue: Float): Float {
    if (this.isNaN() || this.isInfinite())
        return newValue
    return this
}