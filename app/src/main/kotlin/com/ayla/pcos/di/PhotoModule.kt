package com.ayla.pcos.di

import com.ayla.pcos.data.photos.KeystorePhotoKeyProvider
import com.ayla.pcos.data.photos.PhotoKeyProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Fotoğraf anahtarı (ADR-0010): üretimde Keystore sarmalı; testler yazılımsal anahtarla depoyu doğrudan kurar. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoModule {
    @Binds
    abstract fun bindPhotoKeyProvider(impl: KeystorePhotoKeyProvider): PhotoKeyProvider
}
