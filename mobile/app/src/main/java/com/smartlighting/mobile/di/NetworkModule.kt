package com.smartlighting.mobile.di

import android.content.Context
import com.smartlighting.mobile.data.api.ApiService
import com.smartlighting.mobile.data.api.AuthInterceptor
import com.smartlighting.mobile.data.local.PersistentCookieJar
import com.smartlighting.mobile.data.local.TokenManager
import com.smartlighting.mobile.data.repository.LightingRepository
import com.smartlighting.mobile.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provides OkHttpClient with auth and logging interceptors
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: AuthInterceptor,
        persistentCookieJar: PersistentCookieJar
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .cookieJar(persistentCookieJar) // Use persistent cookie jar
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideLightingRepository(
        apiService: ApiService,
        tokenManager: TokenManager,
        persistentCookieJar: PersistentCookieJar
    ): LightingRepository {
        return LightingRepository(apiService, tokenManager, persistentCookieJar)
    }
}
