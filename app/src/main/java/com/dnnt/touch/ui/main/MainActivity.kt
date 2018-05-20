package com.dnnt.touch.ui.main

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dnnt.touch.MyApplication
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import com.dnnt.touch.R
import com.dnnt.touch.been.IMMsg
import com.dnnt.touch.been.User
import com.dnnt.touch.netty.MsgHandler
import com.dnnt.touch.netty.NettyService
import com.dnnt.touch.receiver.NetworkReceiver
import com.dnnt.touch.ui.base.BaseActivity
import com.dnnt.touch.ui.changepassword.ChangePwdActivity
import com.dnnt.touch.ui.login.LoginActivity
import com.dnnt.touch.ui.main.contact.ContactFragment
import com.dnnt.touch.ui.main.message.LatestChatFragment
import com.dnnt.touch.util.*
import dagger.Lazy
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_add_friend.view.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.io.File
import javax.inject.Inject

class MainActivity : BaseActivity<MainViewModel>(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        val TAG = "MainActivity"
    }

    @Inject lateinit var latestChatFragmentProvider: Lazy<LatestChatFragment>
    @Inject lateinit var contactFragmentProvider: Lazy<ContactFragment>
    @Inject lateinit var networkReceiver: NetworkReceiver
    private lateinit var netChangeListener: NetworkReceiver.NetworkChangeListener

    override fun init() {
        if(NetworkReceiver.isNetUsable()){
            startService(Intent(this,NettyService::class.java))
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)

        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        val fragmentList = listOf<Lazy<out DaggerFragment>>(latestChatFragmentProvider, contactFragmentProvider)
        val pagerAdapter = MainPagerAdapter(supportFragmentManager,fragmentList)
        view_pager.adapter = pagerAdapter

        launch(UI){
            if (MyApplication.mUser != null){
                //TODO Have a better solutions?(user_head may not have init)
                while (user_head == null)   delay(100)
                Glide.with(this@MainActivity).load(BASE_URL + MyApplication.mUser?.headUrl).into(user_head)
                user_name.text = MyApplication.mUser?.userName ?: ""
                user_head.setOnClickListener{
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        openAlbum()
                    } else {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE)
                    }
                }

            }
        }

    }

    @Inject fun addListener(context: Context){
        netChangeListener = object : NetworkReceiver.NetworkChangeListener{
            override fun networkChanged(status: Int) {
                if (status > NO_NETWORK){
                    if(MyApplication.mUser != null){
                        startService(Intent(context,NettyService::class.java))
                    }
                }else{
                    stopService(Intent(context,NettyService::class.java))
                }
            }
        }
        networkReceiver.addListener(netChangeListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum()
            } else {
                toast(R.string.permission_denied)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_SET_HEAD_REQ && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null){
                Glide.with(this)
                    .asBitmap()
                    .listener(object :  RequestListener<Bitmap> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                        override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (resource != null){
                                logi(TAG,"res width: ${resource.width}")
                                logi(TAG,"res height: ${resource.height}")
                                val headCache = File(cacheDir.path + "/head.png")
                                mViewModel.updateHead(resource, headCache)
                            }
                            return false
                        }
                    })
                    .load(uri)
                    .into(user_head)
            }
        }
    }

    private fun openAlbum() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent, ACTIVITY_SET_HEAD_REQ)
    }

    override fun getLayoutId() = R.layout.activity_main

    @Inject override fun setViewModel(viewModel: MainViewModel) {
        mViewModel = viewModel
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }else{
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        logi(TAG,"onDestroya")
        stopService(Intent(this,NettyService::class.java))
        super.onDestroy()
        networkReceiver.removeListener(netChangeListener)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.add_friend -> handleAddFriend()
            R.id.change_password -> {
//                startActivityForResult(Intent(this,ChangePwdActivity::class.java), ACTIVITY_CHANGE_PWD_REQ)
                startActivity(ChangePwdActivity::class.java)
            }
            R.id.log_out -> {
                MyApplication.mUser = null
                startActivity(Intent(this,LoginActivity::class.java))
                this.finish()
            }
            R.id.quit -> {
                super.onBackPressed()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }



    private fun handleAddFriend(){
        val view = View.inflate(this,R.layout.dialog_add_friend,null)
        AlertDialog.Builder(this)
            .setView(view)
            .create()
            .show()
        with(view){
            btn_add_friend.setOnClickListener {
                val nameOrPhone = name_or_phone.text.toString()
                when {
                    isNameLegal(nameOrPhone) || nameOrPhone.matches(Regex("\\d{11}")) -> {
                        val user = MyApplication.mUser as User
                        val id = user.id
                        if(nameOrPhone == user.userName || nameOrPhone == user.phone){
                            toast(R.string.can_not_add_yourself)
                            return@setOnClickListener
                        }
                        MsgHandler.sendMsg(IMMsg(from = id,msg = nameOrPhone,type = TYPE_ADD_FRIEND))
                    }
                    else -> toast(R.string.user_not_exist)
                }
            }
        }
    }


}
