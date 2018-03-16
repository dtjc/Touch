package com.dnnt.touch.ui.main

import com.dnnt.touch.di.ActivityScoped
import com.dnnt.touch.di.FragmentScoped
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by dnnt on 18-3-10.
 */
@Module
abstract class MainModule {
    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun messageFragment(): MessageFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun contactFragment(): ContactFragment
}