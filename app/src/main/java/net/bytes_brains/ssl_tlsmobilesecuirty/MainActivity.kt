package net.bytes_brains.ssl_tlsmobilesecuirty

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.bytes_brains.ssl_tlsmobilesecuirty.data.remote.RetrofitProvider

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        invokeTLSApiWithRetrofitProvider()
    }

    private fun invokeTLSApiWithRetrofitProvider() {
        val retrofitProvider = RetrofitProvider()


        val api = retrofitProvider.initApiService(this)
        lifecycleScope.launch {
            try {
                Log.e("API RESULT", "invokeTLSApiWithRetrofitProvider: ${api.getNews()}")
            } catch (e: Exception) {
                Log.e("TAG", "invokeTLSApiWithRetrofitProvider: ERROR occurred= ${e.message}")
                e.printStackTrace()
            }
        }
    }
}