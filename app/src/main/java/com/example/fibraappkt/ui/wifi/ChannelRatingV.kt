package com.example.fibraappkt.ui.wifi

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.fibraappkt.R

class ChannelRatingViewd @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val channelNumber: TextView
    private val ratingStars: LinearLayout
    private val accessPointCount: TextView

    init {
        orientation = HORIZONTAL

        // Inflar o layout
        LayoutInflater.from(context).inflate(R.layout.view_channel_rating, this, true)

        // Obter referências
        channelNumber = findViewById(R.id.textChannelNumber)
        ratingStars = findViewById(R.id.layoutStars)
        accessPointCount = findViewById(R.id.textAccessPointCount)
    }

    fun setChannelData(channel: Int, rating: Int, apCount: Int) {
        channelNumber.text = channel.toString()
        accessPointCount.text = apCount.toString()

        // Limpar estrelas anteriores
        ratingStars.removeAllViews()

        // Adicionar estrelas com base na classificação (valor de 0-10)
        val maxRating = 10

        for (i in 1..maxRating) {
            val star = ImageView(context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )

                // Definir a cor da estrela baseada na classificação
                val colorResId = when {
                    i <= rating && rating >= 8 -> R.color.starExcellent // Amarelo para excelente (8-10)
                    i <= rating && rating >= 5 -> R.color.starGood      // Verde para bom (5-7)
                    i <= rating -> R.color.starFair                     // Laranja para regular (1-4)
                    else -> R.color.starInactive                        // Cinza para inativo
                }

                setImageResource(R.drawable.ic_star)
                setColorFilter(ContextCompat.getColor(context, colorResId))

                // Adicionar um pequeno espaçamento entre as estrelas
                setPadding(0, 0, 2, 0)
            }

            ratingStars.addView(star)
        }
    }
}