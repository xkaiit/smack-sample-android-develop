package com.infineon.smack.sample.overview

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.infineon.smack.sample.R
import com.infineon.smack.sample.databinding.MainActivityBinding
import com.infineon.smack.sdk.SmackSdk
import com.infineon.smack.sdk.application.lock.Lock
import com.infineon.smack.sdk.smack.charge.ChargeLevel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: MainActivityBinding

    @VisibleForTesting
    @Inject
    lateinit var smackSdk: SmackSdk

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTitle(R.string.app_title)
        binding.resetButton.setOnClickListener { viewModel.resetCount() }
        binding.firmwareSwitch.setOnCheckedChangeListener { _, checked ->
            viewModel.setFirmwareToggle(checked)
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect(::updateViewState)
            }
        }
        smackSdk.onCreate(this)


        binding.BtnOpen.setOnClickListener {
            TODO("这里需要实现NFC开锁功能")
            // 1、根据开发文档中的介绍调用：smackSdk.lockApi.unlock(lock, lockKey)实现开锁功能
            // 2、调用：smackSdk.lockApi.unlock(lock, lockKey) 一直拿不到lock、lockKEY的值，导致功能未实现

            // 测试开锁的代码
            testLock()
        }
        binding.BtnClose.setOnClickListener {
            TODO("这里需要实现NFC关锁功能")
            // 1、根据开发文档中的介绍调用：smackSdk.lockApi.lock(lock, lockKey)实现开锁功能
            // 2、调用：smackSdk.lockApi.lock(lock, lockKey) 一直拿不到lock、lockKEY的值，导致功能未实现

        }

    }

    private fun showAlert(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        smackSdk.isEnabled = false
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setNeutralButton(R.string.ok) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .setOnDismissListener {
                    smackSdk.isEnabled = true
                }
                .show()
        }
    }

    @SuppressLint("MissingSuperCall")
    public override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        smackSdk.onNewIntent(intent)
    }

    private fun updateViewState(state: MainViewState) = with(binding) {
        showConnection(state.isConnected)
        sentValueTextView.text = state.sentBytesCount.toString()
        receivedValueTextView.text = state.receivedBytesCount.toString()
        divergentValueTextView.text = state.divergentBytesCount.toString()
        firmwareText.isVisible = state.showMissingFirmwareText
        if (state.alertTitle != null && state.alertMessage != null) {
            showAlert(state.alertTitle, state.alertMessage)
        }
    }

    private fun MainActivityBinding.showConnection(isConnected: Boolean) {
        connectedImageView.isVisible = isConnected
        connectedTextView.isVisible = isConnected
        disconnectedImageView.isVisible = !isConnected
        disconnectedTextView.isVisible = !isConnected
    }



    private var cacheKey: Lock?=null

    /**
     * 自己写的测试代码---测试开锁功能
     *
     * 锁硬件设备信息：
     * 锁名：abc
     * 锁的序列号：144498164762820893
     * 初始化密钥：0123456789abcdef
     * 开锁密码：123456
     *
     */
    private fun testLock() {
        val lockApi = smackSdk.lockApi

        // 测试拿到数据的情况
        lockApi.getLock()?.let {
            if (it != null){
                Log.i("BtnOpen", "it的信息==不等于null===111111111=====$it")
                it.map {lock ->
                    // 代码一直没有进入这里，导致拿不到lock，
                    Log.i("BtnOpen", "打印日志22222222: $lock")
                    if (lock != null){
                        Log.i("BtnOpen", "锁的信息=========="+lock.lockId+"_"+lock.isNew+"_")
                    } else{
                        Log.i("BtnOpen", "lock == null")
                    }
                }
            } else {
                Log.i("BtnOpen", "it == null")
            }

        }



        lockApi.getLock().map { lock ->
            // 代码一直没有进入这里，导致拿不到lock，也就拿不到lockKey
            if (lock != null) {
                val lockKey = lockApi.validatePassword(
                    lock,
                    "abc",
                    System.currentTimeMillis() / 1000,
                    "123456"
                )
                cacheKey = lock
                Log.i("BtnOpen", "进入了map==========" + lock.lockId+"_" + lock.isNew + "_")


                // 初始化会话
                lockApi.initializeSession(
                    lock,
                    "abc",
                    System.currentTimeMillis() / 1000,
                    lockKey
                )
                do {
                    val chargeLevel: ChargeLevel = lockApi.getChargeLevel(lock, lockKey)
                } while (!chargeLevel.isFullyCharged)

                lockApi.unlock(lock, lockKey)

            }
        }
        Log.i("BtnOpen", "锁的信息=========="+ (cacheKey?.lockId ?: null) +"_"+ (cacheKey?.isNew ?: null) +"_")


    }




}
