package jp.ac.titech.itpro.sdl.randomwalker

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.VISIBLE
import androidx.core.widget.doOnTextChanged
import androidx.preference.PreferenceFragmentCompat
import com.eclipsesource.json.Json
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
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import jp.ac.titech.itpro.sdl.randomwalkerpast.HttpUtil
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var tag = MapsActivity::class.java.name
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    private lateinit var newLocationResult: LocationResult
    private lateinit var mMap: GoogleMap
    private var cb = arrayOfNulls<Int>(8)
    private var writepl : Polyline?= null
    private var NowLat :Double= 0.0
    private var NowLng :Double= 0.0
    private val LatOne = 0.011
    private val LngOne = 0.009
    private var Times = 1.0
    private var rp = DoubleArray(18)
    private lateinit var gAPIKey :String
    private var string: String = ""
    private var peaceful = true
    private var encodedpl :String = ""
    private var vdir = 0.0
    private var hdir = 0.0
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

        supportActionBar?.title = "RandomWalker"

        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val gAPIKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")

        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val locationRequest = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        //locationRequest.setExpirationDuration(1000L)

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(tag, "onLocationResult")
                val location = locationResult?.lastLocation ?: return
                textView.text = "緯度:${location.latitude},経度:${location.longitude}"
                NowLat = location.latitude
                NowLng = location.longitude
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(NowLat,NowLng),16.0f))
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
            newLocationRequest.setExpirationDuration(1000L)
            fusedLocationProviderClient.requestLocationUpdates(
                newLocationRequest,
                locationCallback,
                null
            )
            //TODO:半径Nmの中の8点をランダムで取得(選べるようにする)
            if(NowLat == 0.0 || NowLng == 0.0)return@setOnClickListener
            rp[0] = NowLat
            rp[1] = NowLng
            for(i in 1..8){
                val randomDoubleLat = (ThreadLocalRandom.current().nextDouble(0.0,1.0)-vdir) * LatOne * Times
                val randomDoubleLng = (ThreadLocalRandom.current().nextDouble(0.0,1.0)-hdir) * LngOne * Times
                var rLat = NowLat + randomDoubleLat
                var rLng = NowLng + randomDoubleLng
                rLat = round(rLat*10000000)/10000000
                rLng = round(rLng*10000000)/10000000
                rp[i*2] = rLat
                rp[i*2+1] = rLng
                //Log.d(tag,"${randomDoubleLat},${randomDoubleLng}")
                //Log.d(tag,"${rLat},${rLng}")
            }
            string = "https://maps.googleapis.com/maps/api/directions/json?"
            string+= "origin=${rp[0]},${rp[1]}&"
            string+= "destination=${rp[0]},${rp[1]}&"
            string+= "mode=walking&"
            string += if(peaceful){
                "waypoints=optimize:true|"
            }else{
                "waypoints="
            }

            for(i in 1..8){
                if(!peaceful){
                    string +="via:"
                }
                string += "${rp[i*2]},${rp[i*2+1]}"
                string += if(i != 8){
                    "|"
                }else{
                    "&"
                }
            }
            string+= "key=${gAPIKey}"
            Log.d(tag,string)
            gmapapicall()


        }

        bt_gotomap.setOnClickListener{
            var url = "https://www.google.com/maps/dir/?api=1"
            url += "&origin=" + rp[0].toString() + "," + rp[1].toString() + "&destination=" + rp[0].toString() + "," + rp[1].toString()
            url += "&travelmode=walking"
            url += "&waypoints="
            for ((i,l) in cb.withIndex()){
                val k = l?.plus(1)
                if(k != null){
                    url += rp[k*2].toString() + "," + rp[k*2+1].toString()
                    if(i != 7){
                        url += "|"
                    }
                }
            }
            Log.d(tag,url)

            var mapintent = Intent()
            mapintent.action = Intent.ACTION_VIEW
            mapintent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
            mapintent.data = Uri.parse(url)
            startActivity(mapintent);
        }

        iv_data.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                Log.d(tag, "textchanged!!")
                Log.d(tag, "encodedpl:$encodedpl")
                var overview = PolyUtil.decode(encodedpl)
                writepl?.remove()
                writepl = mMap.addPolyline(PolylineOptions().addAll(overview).color(Color.RED))
                //Log.d(tag,writepl.toString())
                bt_gotomap.visibility = VISIBLE

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(tag,"onMapReady")
        mMap = googleMap
        val lc = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(tag, "onLocationResult in onMapReady")
                val location = locationResult?.lastLocation ?: return
                textView.text = "緯度:${location.latitude},経度:${location.longitude}"
                NowLat = location.latitude
                NowLng = location.longitude
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(NowLat,NowLng),16.0f))
            }
        }
        val lr = LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        lr.setExpirationDuration(1000L)
        fusedLocationProviderClient.requestLocationUpdates(lr, lc, null)

        var nowin = LatLng(NowLat, NowLng)

        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isTiltGesturesEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }
        mMap.isMyLocationEnabled = true

        val zoomValue = 15.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nowin, zoomValue))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val optintent = Intent(this, SettingsActivity::class.java)
        when(item.itemId){
            R.id.addButton -> startActivity(optintent)
        }
        return true
    }

    fun gmapapicall() = GlobalScope.launch(Dispatchers.Main) {
        val http = HttpUtil()
        //Mainスレッドでネットワーク関連処理を実行するとエラーになるためBackgroundで実行
        async(Dispatchers.Default) { http.httpGet(string) }.await().let {
            val result = Json.parse(it).asObject()
            val status = result.get("status").asString()
            if(status == "OK") {
                encodedpl =
                    result.get("routes").asArray()[0].asObject().get("overview_polyline").asObject()
                        .get("points").asString()

                val routes = result.get("routes").asArray()[0].asObject().get("legs").asArray()
                var dist = 0
                var time = 0
                for (route in routes) {
                    dist += route.asObject().get("distance").asObject().get("value").asInt()
                    time += route.asObject().get("duration").asObject().get("value").asInt()
                }
                Log.d(tag, "dist:" + String.format("%.1f", dist / 1000.0f) + "km")
                Log.d(tag, "time:" + String.format("%.1f", time / 60.0f) + "min")
                tv_distance.text = "距離"+String.format("%.1f", dist / 1000.0f) + "km"
                tv_time.text = "時間"+String.format("%.1f", time / 60.0f) + "分"

                var comb = result.get("routes").asArray()[0].asObject().get("waypoint_order").asArray()

                if(!comb.isEmpty){
                    for ((i,r) in comb.withIndex()){
                        cb[i] = r.asInt()
                    }
                }
                for(i in 0..7){
                    Log.d(tag, "order:${cb[i]}")
                }
                iv_data.setText(encodedpl)
            }
        }
    }
}
