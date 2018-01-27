package com.dnnt.touch.di

import com.dnnt.touch.ui.login.LoginActivity
import com.dnnt.touch.ui.login.LoginModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by dnnt on 17-12-23.
 */
@Module
abstract class ActivityBindingModule {
    @ActivityScoped
    @ContributesAndroidInjector(modules = [LoginModule::class])
    internal abstract fun loginActivity(): LoginActivity
}