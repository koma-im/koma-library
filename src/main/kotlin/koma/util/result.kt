package koma.util

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS", "NOTHING_TO_INLINE", "DEPRECATION")
inline class KResult<out T, out E: Any> @PublishedApi internal constructor(
        @PublishedApi
        @Deprecated("internal value")
        internal val _value: Any?
){
    val isSuccess: Boolean get() = _value !is _Failure<*>
    val isFailure: Boolean get() = _value is _Failure<*>

    /**
     * Returns the encapsulated value if this instance represents [success][KResult.isSuccess] or `null`
     * if it is [failure][KResult.isFailure].
     *
     * This function is shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    @Suppress("UNCHECKED_CAST")
    inline fun getOrNull(): T? =
            when {
                isFailure -> null
                else -> _value as T
            }

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    fun failureOrNull(): E? {
        val e = when (_value) {
            is _Failure<*> -> _value.exception as E
            else -> null
        }
        return e
    }

    /**
     * Returns a string `Success(v)` if this instance represents [success][KResult.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    override fun toString(): String =
            when (_value) {
                is _Failure<*> -> _value.toString() // "Failure($exception)"
                else -> "Success($_value)"
            }

    /**
     * Companion object for [KResult] class that contains its constructor functions
     * [success] and [failure].
     */
    companion object {
        /**
         * Returns an instance that encapsulates the given [value] as successful value.
         */
        @Suppress("NOTHING_TO_INLINE")
        inline fun <T, E: Any> success(value: T): KResult<T, E> = KResult(value)
        inline fun <T, E: Any> of(value: T): KResult<T, E> = KResult(value)

        /**
         * Returns an instance that encapsulates the given [exception] as failure.
         */
        inline fun <T, E: Any> failure(e: E): KResult<T, E> = KResult(createFailure(e))
        inline fun <T, E: Any> error(e: E): KResult<T, E> = KResult(createFailure(e))
    }

    internal class _Failure<E>(
            @JvmField
            val exception: E
    ) {
        override fun equals(other: Any?): Boolean = other is _Failure<*> && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Failure($exception)"
    }
}

/**
 * Creates an instance of internal marker [KResult.Failure] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
internal fun<E> createFailure(exception: E): Any =
        KResult._Failure(exception)

/**
 * Returns the encapsulated value if this instance represents [success][KResult.isSuccess] or the
 * result of [onFailure] function for encapsulated exception if it is [failure][KResult.isFailure].
 *
 * Note, that an exception thrown by [onFailure] function is rethrown by this function.
 *
 * This function is shorthand for `fold(onSuccess = { it }, onFailure = onFailure)` (see [fold]).
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
inline infix fun <R, T : R, E: Any> KResult<T, E>.getOr(onFailure: (E) -> R): R {
    return if (isFailure) {
        onFailure(failureOrThrow())
    } else {
        getOrThrow()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun <R: Any, T, E: R> KResult<T, E>.getFailureOr(onSuccess: (T) -> R): R {
    return if (isSuccess) {
        onSuccess(getOrThrow())
    } else {
        failureOrThrow()
    }
}

/**
 * Returns the encapsulated value if this instance represents [success][KResult.isSuccess] or the
 * [defaultValue] if it is [failure][KResult.isFailure].
 *
 * This function is shorthand for `getOrElse { defaultValue }` (see [getOrElse]).
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <R, T : R, E: Any> KResult<T, E>.getOrDefault(defaultValue: R): R {
    if (isFailure) return defaultValue
    return _value as T
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <T, E: Any> KResult<T, E>.getOrThrow(): T {
    if (isFailure) {
        val e = failureOrNull()
        if (e is Throwable) throw e
        throw Throwable("KResult is Failure $e")
    }
    return _value as T
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <T, E: Any> KResult<T, E>.failureOrThrow(): E {
    failureOrNull()?.let {
        return it
    }
    throw Throwable("KResult is Success ${this.getOrNull()}")
}

/**
 * Returns the the result of [onSuccess] for encapsulated value if this instance represents [success][KResult.isSuccess]
 * or the result of [onFailure] function for encapsulated exception if it is [failure][KResult.isFailure].
 *
 * Note, that an exception thrown by [onSuccess] or by [onFailure] function is rethrown by this function.
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <R, T, E: Any> KResult<T, E>.fold(
        onSuccess: (T) -> R,
        onFailure: (E) -> R
): R {
    return when (val exception = failureOrNull()) {
        null -> onSuccess(_value as T)
        else -> onFailure(exception)
    }
}

// transformation

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated value
 * if this instance represents [success][KResult.isSuccess] or the
 * original encapsulated exception if it is [failure][KResult.isFailure].
 *
 * Note, that an exception thrown by [transform] function is rethrown by this function.
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <R, T, E: Any> KResult<T, E>.map(transform: (T) -> R): KResult<R, E> {
    return when {
        isSuccess -> KResult.success(transform(_value as T))
        else -> KResult.failure(_value as E)
    }
}

inline fun <R: Any, T, E: Any> KResult<T, E>.mapErr(transform: (E) -> R): KResult<T, R> {
    return when {
        isFailure -> KResult.failure(transform(failureOrThrow()))
        else -> KResult.success(getOrThrow())
    }
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <R, T, E: Any> KResult<T, E>.flatMap(transform: (value: T) -> KResult<R, E>): KResult<R, E> {
    return when {
        isSuccess -> transform(_value as T)
        else -> KResult(_value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to encapsulated failure
 * if this instance represents [failure][KResult.isFailure] or the
 * original encapsulated value if it is [success][KResult.isSuccess].
 *
 * Note, that an exception thrown by [transform] function is rethrown by this function.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T : R, E: Any> KResult<T, E>.recover(transform: (E) -> R): KResult<R, E> {
    return when (val exception = failureOrNull()) {
        null -> this
        else -> KResult.success(transform(exception))
    }
}

/**
 * Performs the given [action] on encapsulated exception if this instance represents [failure][KResult.isFailure].
 * Returns the original `KResult` unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E: Any> KResult<T, E>.onFailure(action: (exception: E) -> Unit): KResult<T, E> {
    failureOrNull()?.let { action(it) }
    return this
}

/**
 * Performs the given [action] on encapsulated value if this instance represents [success][KResult.isSuccess].
 * Returns the original `KResult` unchanged.
 */
@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "DEPRECATION")
inline fun <T, E: Any> KResult<T, E>.onSuccess(action: (value: T) -> Unit): KResult<T, E> {
    if (isSuccess) action(_value as T)
    return this
}
