package com.android.challenge.hilt

import com.android.challenge.business.JellyApi
import com.android.challenge.business.JellyRepository
import com.android.challenge.business.JellyRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.jellyjelly.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideJellyApi(retrofit: Retrofit): JellyApi {
        return retrofit.create(JellyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJellyRepository(api: JellyApi): JellyRepository {
        return JellyRepositoryImpl(api)
    }
}
