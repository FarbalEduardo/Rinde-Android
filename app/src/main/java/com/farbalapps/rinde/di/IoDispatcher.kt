package com.farbalapps.rinde.di

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@kotlin.annotation.Retention(RUNTIME)
@kotlin.annotation.Target(FIELD, FUNCTION, VALUE_PARAMETER)
annotation class IoDispatcher
