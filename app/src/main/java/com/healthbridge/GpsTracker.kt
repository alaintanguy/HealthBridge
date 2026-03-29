import android.content.Context
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.FormBody

class GpsTracker(private val context: Context) {

    val PUSHOVER_TOKEN = "YOUR_APP_TOKEN"
    val MICHEL_USER_KEY = "MICHEL_USER_KEY"

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun startTracking() {
        val request = LocationRequest.create().apply {
            interval = 60000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                sendToMichel(loc.latitude, loc.longitude)
            }
        }, null)
    }

    fun sendToMichel(lat: Double, lon: Double) {
        val client = OkHttpClient()
        val body = FormBody.Builder()
            .add("token", PUSHOVER_TOKEN)
            .add("user", MICHEL_USER_KEY)
            .add("message", "📍 Alain's location: $lat, $lon")
            .add("url", "https://maps.google.com/?q=$lat,$lon")
            .add("url_title", "Open Map")
            .build()

        val request = Request.Builder()
            .url("https://api.pushover.net/1/messages.json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {}
            override fun onFailure(call: Call, e: java.io.IOException) {}
        })
    }
}
