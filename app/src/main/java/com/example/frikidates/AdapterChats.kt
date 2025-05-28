import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.frikidates.R
import com.example.frikidates.HolderChats
import java.util.Date
import java.util.Locale
import com.bumptech.glide.Glide
import com.example.frikidates.firebase.FirebaseRepository
import de.hdodenhof.circleimageview.CircleImageView


class AdapterChats(private val chatList: MutableList<HolderChats>, private val onClick: (HolderChats) -> Unit):
    RecyclerView.Adapter<AdapterChats.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.nombreMensaje)
        val tvLastMessage: TextView = itemView.findViewById(R.id.mensajeMensaje)
        val tvTime: TextView = itemView.findViewById(R.id.horaMensaje)
        val ivPhoto: CircleImageView = itemView.findViewById(R.id.fotoPerfilMensaje)

        fun bind(chat: HolderChats) {
            tvUsername.text = chat.username
            tvLastMessage.text = chat.lastMessage

// Formatear hora
            val date = Date(chat.timestamp)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            tvTime.text = sdf.format(date)

// Cargar imagen usando el repositorio
            FirebaseRepository.getFirstProfileImageUrl(
                chat.userId,
                onSuccess = { uri ->
                    Glide.with(itemView.context)
                        .load(uri)
                        .placeholder(R.drawable.default_avatar)
                        .circleCrop()
                        .into(ivPhoto)
                },
                onFailure = {
                    ivPhoto.setImageResource(R.drawable.default_avatar)
                }
            )

            itemView.setOnClickListener { onClick(chat) }

        }
    }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_chats, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size
}
