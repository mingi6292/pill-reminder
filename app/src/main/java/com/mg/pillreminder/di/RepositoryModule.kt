package com.mg.pillreminder.di

import com.mg.pillreminder.data.repository.MedicineRepositoryImpl
import com.mg.pillreminder.data.repository.ReminderRepositoryImpl
import com.mg.pillreminder.domain.repository.MedicineRepository
import com.mg.pillreminder.domain.repository.ReminderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository

    @Binds
    fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
