package com.example.myagorawebapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    // 动态请求权限（麦克风）
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // WebView 会在 onPermissionRequest 中继续执行（如果用户授予系统权限）
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.mediaPlaybackRequiresUserGesture = false
        ws.allowFileAccess = true
        ws.allowContentAccess = true
        ws.setSupportMultipleWindows(true)
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    // 简化处理：对来自页面的权限请求全部允许（开发/测试使用）。
                    // 生产环境请检查 origin，防止任意网页滥用权限。
                    val needsAudio = request.resources.any { it.contains("AUDIO_CAPTURE") }
                    if (needsAudio && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // 请求 RECORD_AUDIO 运行时权限
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    try {
                        request.grant(request.resources)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    
        // 需要申请的权限列表
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )
    private val PERMISSION_REQUEST_CODE = 100
    private val BATTERY_OPTIMIZATION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查并申请权限
        if (!checkAllPermissionsGranted()) {
            requestRequiredPermissions()
        } else {
            requestIgnoreBatteryOptimization()
            startBackgroundService()
        }
    }

    // 检查所有权限是否授予
    private fun checkAllPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 申请权限
    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    // 权限申请结果回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                requestIgnoreBatteryOptimization()
                startBackgroundService()
                Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show()
            } else {
                // 提示用户手动开启权限
                AlertDialog.Builder(this)
                    .setTitle("权限申请失败")
                    .setMessage("需要授予录音、电话状态等权限才能正常使用听说功能，请手动开启")
                    .setPositiveButton("去设置") { _, _ -> openAppPermissionSettings() }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    // 申请忽略电池优化
    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(PowerManager::class.java)
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
            }
        }
    }

    // 启动后台服务
    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundKeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // 打开应用权限设置
    private fun openAppPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    // 电池优化申请结果回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BATTERY_OPTIMIZATION_REQUEST_CODE) {
            val powerManager = getSystemService(PowerManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(packageName)
                val msg = if (isIgnored) {
                    "电池优化已关闭，应用可长期后台运行"
                } else {
                    "未关闭电池优化，应用后台运行可能受限"
                }
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
 

        // 加载本地页面（注意：我把 index.html 放在 assets 根目录）
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}