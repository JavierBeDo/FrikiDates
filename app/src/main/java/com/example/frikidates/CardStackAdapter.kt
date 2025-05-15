import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frikidates.Profile
import com.example.frikidates.R

class CardStackAdapter(private val profiles: List<Profile>) :
    RecyclerView.Adapter<CardStackAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProfile: ImageView = view.findViewById(R.id.image_profile)
        val nameText: TextView = view.findViewById(R.id.name_text)
        val ageText: TextView = view.findViewById(R.id.age_text)
        val locationText: TextView = view.findViewById(R.id.location_text)
        val compatibilityText: TextView = view.findViewById(R.id.compatibility_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = profiles[position]
        holder.imageProfile.setImageResource(profile.imageResId)
        holder.nameText.text = profile.name
        holder.ageText.text = "Edad: ${profile.age}"
        holder.locationText.text = profile.location
        holder.compatibilityText.text = profile.compatibility
    }

    override fun getItemCount(): Int = profiles.size
}
