package koma.util

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Suppress("NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS", "NOTHING_TO_INLINE", "DEPRECATION")
inline class KResult<out T, out E: Any> @PublishedApi internal constructor(
        @PublishedApi
        @Deprecated("internal value")
        internal val _value: Any?
) {
    val isSuccess: Boolean get() = _value !is _Failure<*>
    val isFailure: Boolean get() = _value is _Failure<*>

    /**
     * Returns the encapsulated value if this instance represents [success][KResult.isSuccess] or `null`
     * if it is [failure][KResult.isFailure].
     *
     * This function is shorthand for `getOrElse { null }` (see [getOrElse]) or
     * `fold(onSuccess = { it }, onFailure = { null })` (see [fold]).
     */
    inline fun getOrNull(): T?  {
        return when {
            isFailure -> null
            else -> {
                @Suppress("UNCHECKED_CAST")
                _value as T
            }
        }
    }

    /**
     * Returns the encapsulated exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     *
     * This function is shorthand for `fold(onSuccess = { null }, onFailure = { it })` (see [fold]).
     */
    @Suppress("NOTHING_TO_INLINE")
    fun failureOrNull(): E? {
        val e: E? = when (_value) {
            is _Failure<*> -> {
                @Suppress("UNCHECKED_CAST")
                val v = _value as _Failure<E>
                val f: E = v.getFailure()
                f
            }
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
                is _Failure<*> -> "Failure(${failureOrNull()})"
                else -> "Success($_value)"
            }

    operator fun component1(): T? = getOrNull()
    operator fun component2(): E? = failureOrNull()
    operator fun component3(): KResult<T, E> = this
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
        @Suppress("use success")
        inline fun <T, E: Any> of(value: T): KResult<T, E> = KResult(value)

        /**
         * Returns an instance that encapsulates the given [exception] as failure.
         */
        inline fun <T, E: Any> failure(e: E): KResult<T, E> = KResult(createFailure(e))
        @Suppress("use failure")
        inline fun <T, E: Any> error(e: E): KResult<T, E> = KResult(createFailure(e))
    }

    internal class _Failure<E: Any>(
            val exception: E
    ) {
        init {
            if (exception is _Failure<*>) {
                System.err.println("Invalid _Failure")
                val e = Exception("_Failure can't nested: $exception")
                e.printStackTrace()
                throw e
            }
        }
        fun getFailure(): E {
            val e = exception
            return e
        }
        override fun equals(other: Any?): Boolean = other is _Failure<*> && exception == other.exception
        override fun hashCode(): Int = exception.hashCode()
        override fun toString(): String = "Internal_Failure($exception)"
    }
}

/**
 * Creates an instance of internal marker [KResult.Failure] class to
 * make sure that this class is not exposed in ABI.
 */
@PublishedApi
internal fun<E: Any> createFailure(exception: E): Any {
    return KResult._Failure(exception)
}

/**
 * Returns the encapsulated value if this instance represents [success][KResult.isSuccess] or the
 * result of [onFailure] function for encapsulated exception if it is [failure][KResult.isFailure].
 *
 * Note, that an exception thrown by [onFailure] function is rethrown by this function.
 *
 * Non-local return after resuming from an OkHttp async call may result in an error
 * So it's currently forbidden
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun <R, T : R, E: Any> KResult<T, E>.getOr(crossinline onFailure: (E) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return if (isFailure) {
        onFailure(failureOrThrow())
    } else {
        getOrThrow()
    }
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun <R: Any, T, E: R> KResult<T, E>.getFailureOr(crossinline onSuccess: (T) -> R): R {
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
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T : R, E: Any> KResult<T, E>.getOrDefault(defaultValue: R): R {
    if (isFailure) return defaultValue
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    return _value as T
}

@Suppress("NOTHING_TO_INLINE")
inline fun <T, E: Any> KResult<T, E>.getOrThrow(): T {
    if (isFailure) {
        val e = failureOrNull()
        if (e is Throwable) throw e
        throw Throwable("KResult is Failure $e")
    }
    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val r = _value as T
    return r
}

@Suppress("NOTHING_TO_INLINE")
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
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T, E: Any> KResult<T, E>.fold(
        crossinline onSuccess: (T) -> R,
        crossinline onFailure: (E) -> R
): R {
    return when (val exception = failureOrNull()) {
        null -> {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            onSuccess(_value as T)
        }
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
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T, E: Any> KResult<T, E>.map(crossinline transform: (T) -> R): KResult<R, E> {
    return when {
        isSuccess -> {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            KResult.success(transform(_value as T))
        }
        else -> KResult.failure(failureOrThrow())
    }
}

inline fun <R: Any, T, E: Any> KResult<T, E>.mapFailure(crossinline transform: (E) -> R): KResult<T, R> {
    return when {
        isFailure -> KResult.failure(transform(failureOrThrow()))
        else -> KResult.success(getOrThrow())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun <R, T, E: Any> KResult<T, E>.flatMap(crossinline transform: (value: T) -> KResult<R, E>): KResult<R, E> {
    return when {
        isSuccess -> {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            transform(_value as T)
        }
        else -> {
            @Suppress("DEPRECATION")
            KResult(_value)
        }
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
inline fun <R, T : R, E: Any> KResult<T, E>.recover(crossinline transform: (E) -> R): KResult<R, E> {
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
inline fun <T, E: Any> KResult<T, E>.onFailure(crossinline action: (exception: E) -> Unit): KResult<T, E> {
    failureOrNull()?.let { action(it) }
    return this
}

/**
 * Performs the given [action] on encapsulated value if this instance represents [success][KResult.isSuccess].
 * Returns the original `KResult` unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T, E: Any> KResult<T, E>.onSuccess(crossinline action: (value: T) -> Unit): KResult<T, E> {
    if (isSuccess) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        action(_value as T)
    }
    return this
}


inline fun <R> runCatch(block: () -> R): KResult<R, Throwable> {
    return try {
        KResult.success(block())
    } catch (e: Throwable) {
        KResult.failure(e)
    }
}

/**
 * this is an extension method in order to make sure T is not nullable
 */
fun<T: Any, E: Any> KResult<T, E>.testFailure(value: T?, error: E?): Boolean {
    contract {
        returns(false) implies (value != null)
        returns(true) implies (error != null)
    }
    return error != null
}