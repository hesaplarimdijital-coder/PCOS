package com.ayla.pcos.di

import com.ayla.pcos.data.health.HealthConnectClientSource
import com.ayla.pcos.data.health.HealthConnectSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Health Connect erişimi; testler sahte kaynakla doğrudan depo kurar. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {
    @Binds
    abstract fun bindHealthConnectSource(impl: HealthConnectClientSource): HealthConnectSource
}
