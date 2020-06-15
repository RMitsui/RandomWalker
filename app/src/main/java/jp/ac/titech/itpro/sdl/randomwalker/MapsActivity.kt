package jp.ac.titech.itpro.sdl.randomwalker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.eclipsesource.json.Json
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
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
    private lateinit var mMap: GoogleMap
    private var cb = arrayOfNulls<Int>(8)
    private var writepl : Polyline?= null
    private var NowLat :Double= 0.0
    private var NowLng :Double= 0.0
    private val LatOne = 0.011
    private val LngOne = 0.009
    private var Times = 1.0
    private var rp = DoubleArray(18)
    private var string: String = ""
    private var peaceful = true
    private var encodedpl :String = ""
    private var vdir = 0.0
    private var hdir = 0.0
    private var south = 0.0
    private var north = 0.0
    private var east = 0.0
    private var west = 0.0
    private var url : String? = null
    lateinit var menu :Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        supportActionBar?.title = "RandomWalker"
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

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
                val location = locationResult.lastLocation ?: return
                val loctext = "緯度:${location.latitude},経度:${location.longitude}"
                textView.text = loctext
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
            val direction = sharedPreferences.getString("directions","")
            when(direction){
                "X" -> {
                    vdir = 0.5;hdir = 0.5
                }
                "E" -> {
                    vdir = 0.5;hdir = 0.0
                }
                "SE" -> {
                    vdir = 1.0;hdir = 0.0
                }
                "S" -> {
                    vdir = 1.0;hdir = 0.5
                }
                "SW" -> {
                    vdir = 1.0;hdir = 1.0
                }
                "W" -> {
                    vdir = 0.5;hdir = 1.0
                }
                "NW" -> {
                    vdir = 0.0;hdir = 1.0
                }
                "N" -> {
                    vdir = 0.0;hdir = 0.5
                }
                "NE" -> {
                    vdir = 0.0;hdir = 0.0
                }
                "R" -> {
                    vdir = ThreadLocalRandom.current().nextDouble(0.0,1.0)
                    hdir = ThreadLocalRandom.current().nextDouble(0.0,1.0)
                }
            }
            Times = sharedPreferences.getString("times","1.0")!!.toDouble()
            Times /= 55.0
            peaceful = sharedPreferences.getBoolean("chaos",false)
            //TODO:半径Nmの中の8点をランダムで取得(選べるようにする)
            if(NowLat == 0.0 || NowLng == 0.0)return@setOnClickListener
            rp[0] = NowLat
            rp[1] = NowLng
            for(i in 1..8){
                val thr = ThreadLocalRandom.current()
                val randomDoubleLat = (thr.nextDouble(0.0,1.0)-vdir) * LatOne * Times
                val randomDoubleLng = (thr.nextDouble(0.0,1.0)-hdir) * LngOne * Times
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
            string += if(!peaceful){
                "waypoints=optimize:true|"
            }else{
                "waypoints="
            }

            for(i in 1..8){
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

        iv_data.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                Log.d(tag, "textchanged!!")
                Log.d(tag, "encodedpl:$encodedpl")
                val overview = PolyUtil.decode(encodedpl)
                writepl?.remove()
                writepl = mMap.addPolyline(PolylineOptions().addAll(overview).color(Color.RED))

                val bounds = LatLngBounds.builder().include(LatLng(south,west)).include(LatLng(north,east)).build()
                val width = resources.displayMetrics.widthPixels
                val height = resources.displayMetrics.heightPixels
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,width,height,150))

                var url = "https://www.google.com/maps/dir/?api=1"
                url += "&origin=" + rp[0].toString() + "," + rp[1].toString() + "&destination=" + rp[0].toString() + "," + rp[1].toString()
                url += "&travelmode=walking"
                url += "&waypoints="
                if(!peaceful){
                    for ((i,l) in cb.withIndex()){
                        val k = l?.plus(1)
                        if(k != null){
                            url += rp[k*2].toString() + "," + rp[k*2+1].toString()
                            if(i != 7){
                                url += "|"
                            }
                        }
                    }
                }else{
                    for(i in 1..8){
                        url += rp[i*2].toString() + "," + rp[i*2+1].toString()
                        if(i != 8){
                            url += "|"
                        }
                    }
                }
                Log.d(tag,url)
                menu.getItem(0).isVisible = true
                menu.getItem(1).isVisible = true
                gurl.text=url
                gurl.visibility = INVISIBLE
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
        menuInflater.inflate(R.menu.menu_main, menu)
        if (menu != null) {
            this.menu = menu
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val optintent = Intent(this, SettingsActivity::class.java)
        when(item.itemId){
            R.id.addButton -> startActivity(optintent)
            R.id.outputButton -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("",gurl.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(applicationContext,"URLをコピーしました。",Toast.LENGTH_LONG).show()
            }
            R.id.gmapButton -> {
                val mapintent = Intent()
                mapintent.action = Intent.ACTION_VIEW
                mapintent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity")
                mapintent.data = Uri.parse(gurl.text as String?)
                startActivity(mapintent)
            }
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
                tv_distance.text = "距離: "+String.format("%.1f", dist / 1000.0f) + "km"
                tv_time.text = "時間: "+String.format("%.1f", time / 60.0f) + "分"

                val comb = result.get("routes").asArray()[0].asObject().get("waypoint_order").asArray()

                if(!comb.isEmpty){
                    for ((i,r) in comb.withIndex()){
                        cb[i] = r.asInt()
                    }
                }
                for(i in 0..7){
                    Log.d(tag, "order:${cb[i]}")
                }
                val news = result.get("routes").asArray()[0].asObject().get("bounds").asObject()
                north = news.get("northeast").asObject().get("lat").asDouble()
                east = news.get("northeast").asObject().get("lng").asDouble()
                south = news.get("southwest").asObject().get("lat").asDouble()
                west = news.get("southwest").asObject().get("lng").asDouble()

                iv_data.setText(encodedpl)
            }
        }
    }
}
