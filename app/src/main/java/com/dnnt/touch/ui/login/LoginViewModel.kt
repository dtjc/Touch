package com.dnnt.touch.ui.login

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.dnnt.touch.ui.base.BaseViewModel
import com.dnnt.touch.MyApplication
import com.dnnt.touch.R
import com.dnnt.touch.base.SingleLiveEvent
import com.dnnt.touch.di.ActivityScoped
import com.dnnt.touch.network.NetService
import com.dnnt.touch.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

/**
 * Created by dnnt on 18-1-26.
 */
@ActivityScoped
class LoginViewModel @Inject constructor(): BaseViewModel() {

//    @Inject lateinit var mScheduler: MyScheduler
    @Inject lateinit var mNetService: NetService
    val mLoginEvent = SingleLiveEvent<Void>()
    val mLoading = MutableLiveData<Boolean>()

    fun login(nameOrPhone: String, password: String){
//        mNetService.getTest("asdfasf").subscribe({},{_,_ -> })
        when {
            nameOrPhone.length !in NAME_MIN_LENGTH..NAME_MAX_LENGTH -> toast(R.string.name_or_phone_wrong)
            password.length !in PWD_MIN_LEN..PWD_MAX_LEN -> toast(R.string.wrong_password)
            else -> {
                val map = hashMapOf(Pair(NAME_OR_PHONE,nameOrPhone), Pair(PASSWORD,password))
                mLoading.value = true
                mNetService.login(map)
//                        .delay(500,TimeUnit.MILLISECONDS,mScheduler)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally{ mLoading.value = false }
                        .subscribe({
                            val user = it.obj
                            if (user == null){
                                toast(R.string.unknown_error)
                                return@subscribe
                            }
                            MyApplication.mUser = user
                            MyApplication.mUser?.nickname = MyApplication.mUser?.userName
                            MyApplication.mToken = it.msg
                            mLoginEvent.call()
                            val sharedPre = MyApplication.mContext.getSharedPreferences(PRE_NAME, Context.MODE_PRIVATE)
                            val id = sharedPre.getLong(ID,0)
                            if (id <= 0 || id != user.id){
                                map[TOKEN] = it.msg
                                map[HEAD_URL] = user.headUrl
                                map[PHONE] = user.phone ?: ""
                                map[USER_NAME] = user.userName
                                val editor = sharedPre.edit()
                                map.forEach {
                                    editor.putString(it.key,it.value)
                                }
                                editor.putLong(ID,user.id)
                                editor.commit()
                            }

                        }, {_,_ ->
                            toast(R.string.login_fail)
                        })
            }
        }
    }
}





