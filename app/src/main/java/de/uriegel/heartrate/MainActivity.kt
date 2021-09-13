package de.uriegel.heartrate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import de.uriegel.activityextensions.ActivityRequest
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val preferences = getSharedPreferences("default", MODE_PRIVATE)
        preferences?.getString(HEARTRATE_ADDRESS, null)?.let {
            heartRateAddress = it
            btnHeartRate.isEnabled = true
        }
        preferences?.getString(BIKE_ADDRESS, null)?.let {
            bikeAddress = it
            btnBike.isEnabled = true
        }

        launch {
            val result = activityRequest.checkAndAccessPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            if (result.any { !it.value }) {
                Toast.makeText(this@MainActivity, "Kein Zugriff auf den Standort", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val backgroundResult = activityRequest.checkAndAccessPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                if (backgroundResult.any { !it.value }) {
                    Toast.makeText(this@MainActivity, "Kein ständiger Zugriff auf den Standort", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
            }

            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val res = activityRequest.launch(enableBtIntent)
            }
        }
    }

    fun onScanHeartRate(view: View) {
        launch {
            heartRateAddress = scan(BluetoothLeService.HEART_RATE_UUID)
            btnHeartRate.isEnabled = heartRateAddress != null
            val preferences = getSharedPreferences("default", MODE_PRIVATE)
            preferences?.edit()?.putString(HEARTRATE_ADDRESS, heartRateAddress)?.commit()
        }
    }

    fun onScanBike(view: View) {
        launch {
            bikeAddress = scan(BluetoothLeService.BIKE_UUID)
            btnBike.isEnabled = bikeAddress != null
            val preferences = getSharedPreferences("default", MODE_PRIVATE)
            preferences?.edit()?.putString(BIKE_ADDRESS, bikeAddress)?.commit()
        }
    }

    fun startHeartRate(view: View) {
        val intent = Intent(this, HeartRateActivity::class.java)
        startActivity(intent)
    }

    private suspend fun scan(uuid: String): String? {
        val intent = Intent(this@MainActivity, DevicesActivity::class.java)
        intent.putExtra("UUID", uuid)
        val result = activityRequest.launch(intent)
        return result.data?.getStringExtra(DevicesActivity.RESULT_DEVICE)
    }

    companion object {
        val HEARTRATE_ADDRESS = "HEARTRATE_ADDRESS"
        val BIKE_ADDRESS = "BIKE_ADDRESS"
    }

    override val coroutineContext = Dispatchers.Main

    private var heartRateAddress: String? = null
    private var bikeAddress: String? = null
    private val activityRequest = ActivityRequest(this)
}

