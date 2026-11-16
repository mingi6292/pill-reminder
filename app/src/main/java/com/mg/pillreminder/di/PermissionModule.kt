package com.mg.pillreminder.di

import com.mg.pillreminder.domain.permission.PermissionChecker
import com.mg.pillreminder.permission.AndroidPermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface PermissionModule {

    @Binds
    fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker
}
