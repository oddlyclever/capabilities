package com.oddlyclever.jsonlogic

/**
 * Thrown when a rule references an operator that is not registered — the only structural
 * error a JSONLogic rule can contain. All other malformed input (wrong argument counts,
 * mismatched types) degrades according to JavaScript coercion rules instead of throwing,
 * matching the reference implementation.
 */
class JsonLogicException(message: String) : RuntimeException(message)
