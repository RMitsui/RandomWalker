package jp.ac.titech.itpro.sdl.randomwalker

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var tag = MapsActivity::class.java.name
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private var NowLat :Double= 0.0
    private var NowLng :Double= 0.0
    private val LatOne = 0.011
    private val LngOne = 0.009
    private var Times = 1.0
    private var rp = DoubleArray(22)
    private lateinit var gAPIKey :String
    private var string: String = ""
    private var peaceful = true
    private var encodedpl :String = ""
    companion object{
        const val RANDOMPOINTS = "RandomWalker.RandomPoints"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val gAPIKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        locationRequest.setExpirationDuration(1000L)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult?.lastLocation ?: return
                textView.text = "緯度:${location.latitude},経度:${location.longitude}"
                NowLat = location.latitude
                NowLng = location.longitude
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)

        bt_makeroute.setOnClickListener {
            Log.d(tag, "makeLocationRequest")
            //現在位置の緯度経度を取得
            val newLocationRequest = LocationRequest().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
            locationRequest.setExpirationDuration(1000L)
            fusedLocationProviderClient.requestLocationUpdates(
                newLocationRequest,
                locationCallback,
                null
            )
        }


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }
}
