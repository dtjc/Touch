package com.dnnt.touch.di

import android.content.Context
import com.dnnt.touch.BuildConfig
import com.dnnt.touch.MyApplication
import com.dnnt.touch.network.NetService
import com.dnnt.touch.receiver.NetworkReceiver
import com.dnnt.touch.util.BASE_URL
import com.dnnt.touch.util.MyScheduler
import com.dnnt.touch.util.NetworkNotAvailableException
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * Created by dnnt on 17-12-23.
 */
@Module
class AppModule {

    @Provides
    fun context(application: MyApplication): Context = application

    @Provides
    @Singleton
    fun provideCachedThreadPool(): ExecutorService = Executors.newCachedThreadPool()

    @Provides
    @Singleton
    fun provideRetrofit(executorService: ExecutorService, scheduler: MyScheduler) : Retrofit{
        val clientBuild = OkHttpClient.Builder()
                .addInterceptor{
                    if (!NetworkReceiver.isNetUsable()){
                        throw NetworkNotAvailableException()
                    }
                    return@addInterceptor it.proceed(it.request())
                }
                .dispatcher(Dispatcher(executorService))
        if (BuildConfig.DEBUG){
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            clientBuild.addNetworkInterceptor(loggingInterceptor)
        }
        val okHttpClient = clientBuild.build()
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(scheduler))
                .build()
    }

    @Provides
    fun provideNetService(retrofit: Retrofit): NetService = retrofit.create(NetService::class.java)

}