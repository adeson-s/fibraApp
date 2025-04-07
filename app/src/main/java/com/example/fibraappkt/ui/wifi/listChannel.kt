package com.example.fibraappkt.ui.wifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fibraappkt.R

class ChannelRatingAdapter : RecyclerView.Adapter<ChannelRatingAdapter.ViewHolder>() {

    private var channelRatings: List<ChannelDataAdapter.ChannelRating> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChannelNumber: TextView = view.findViewById(R.id.tvChannelNumber)
        val ivStars: Array<ImageView> = Array(10) { i ->
            view.findViewById(
                when (i) {
                    0 -> R.id.ivStar1
                    1 -> R.id.ivStar2
                    2 -> R.id.ivStar3
                    3 -> R.id.ivStar4
                    4 -> R.id.ivStar5
                    5 -> R.id.ivStar6
                    6 -> R.id.ivStar7
                    7 -> R.id.ivStar8
                    8 -> R.id.ivStar9
                    else -> R.id.ivStar10
                }
            )
        }
        val tvNetworkCount: TextView = view.findViewById(R.id.tvNetworkCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_rating, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channelRating = channelRatings[position]

        holder.tvChannelNumber.text = channelRating.channel.toString()

        // Configurar estrelas
        for (i in 0 until 10) {
            val isFilled = i < channelRating.starRating
            holder.ivStars[i].setImageResource(
                if (isFilled) R.drawable.ic_star_filled else R.drawable.ic_star_empty
            )

            // Colorir estrelas com base na classificação
            holder.ivStars[i].setColorFilter(
                when {
                    !isFilled -> holder.itemView.context.getColor(R.color.gray)
                    channelRating.starRating >= 8 -> holder.itemView.context.getColor(R.color.green)
                    channelRating.starRating >= 5 -> holder.itemView.context.getColor(R.color.yellow)
                    else -> holder.itemView.context.getColor(R.color.red)
                }
            )
        }

        // Mostrar número de redes no canal
        val networkCount = channelRating.networks.size
        holder.tvNetworkCount.text = if (networkCount > 0) "$networkCount redes" else "Sem redes"
    }

    override fun getItemCount() = channelRatings.size

    fun updateRatings(ratings: List<ChannelDataAdapter.ChannelRating>) {
        channelRatings = ratings
        notifyDataSetChanged()
    }
}