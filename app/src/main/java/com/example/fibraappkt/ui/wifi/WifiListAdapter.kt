package com.example.fibraappkt.ui.wifi

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fibraappkt.R
import kotlin.math.abs

class WifiListAdapter : RecyclerView.Adapter<WifiListAdapter.WifiViewHolder>() {

    private var wifiList: List<ScanResult> = emptyList()
    private var expandedPosition = -1

    fun updateList(newList: List<ScanResult>) {
        wifiList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi, parent, false)
        return WifiViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        val wifi = wifiList[position]
        holder.bind(wifi, position == expandedPosition)

        holder.itemView.setOnClickListener {
            val wasExpanded = expandedPosition == position
            expandedPosition = if (wasExpanded) -1 else position
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = wifiList.size

    class WifiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val signalIcon: ImageView = itemView.findViewById(R.id.signalIcon)
        private val ssidText: TextView = itemView.findViewById(R.id.ssidTextView)
        private val signalText: TextView = itemView.findViewById(R.id.signalTextView)
        private val channelText: TextView = itemView.findViewById(R.id.channelTextView)
        private val frequencyText: TextView = itemView.findViewById(R.id.frequencyText)
        private val distanceText: TextView = itemView.findViewById(R.id.distanceText)
        private val securityText: TextView = itemView.findViewById(R.id.securityText)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val detailsLayout: View = itemView.findViewById(R.id.detailsLayout)

        fun bind(scanResult: ScanResult, isExpanded: Boolean) {
            val ssid = scanResult.SSID.ifEmpty { "<Sem nome>" }
            ssidText.text = ssid

            val signal = scanResult.level
            signalText.text = "${signal} dBm"

            // Configurar ícone de sinal baseado na força
            when {
                signal > -50 -> signalIcon.setImageResource(R.drawable.ic_signal_full)
                signal > -70 -> signalIcon.setImageResource(R.drawable.ic_signal_good)
                signal > -80 -> signalIcon.setImageResource(R.drawable.ic_signal_medium)
                else -> signalIcon.setImageResource(R.drawable.ic_signal_weak)
            }

            // Calculando o canal Wi-Fi baseado na frequência
            val frequency = scanResult.frequency
            val channel = getChannelFromFrequency(frequency)

            channelText.text = "CH ${channel}"
            frequencyText.text = "${frequency} MHz"

            // Calculando distância aproximada (fórmula simplificada)
            val distance = calculateDistance(scanResult.level, frequency)
            distanceText.text = String.format("%.1fm", distance)

            // Informações de segurança
            val capabilities = scanResult.capabilities
            securityText.text = getSecurityType(capabilities)

            // Expandir ou colapsar detalhes
            expandIcon.rotation = if (isExpanded) 180f else 0f
            detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        private fun getChannelFromFrequency(frequency: Int): Int {
            return when {
                frequency >= 2412 && frequency <= 2484 -> (frequency - 2412) / 5 + 1
                frequency >= 5170 && frequency <= 5825 -> (frequency - 5170) / 5 + 34
                else -> 0
            }
        }

        private fun calculateDistance(signalLevel: Int, frequency: Int): Double {
            // Fórmula simplificada para estimar distância baseada no RSSI
            // Nota: esta é uma aproximação; a precisão varia com o ambiente
            val freqInMhz = frequency / 1000.0
            val exp = (27.55 - (20 * Math.log10(freqInMhz)) + abs(signalLevel)) / 20.0
            return Math.pow(1.0, exp)
        }

        private fun getSecurityType(capabilities: String): String {
            return when {
                capabilities.contains("WPA3") -> "[WPA3]"
                capabilities.contains("WPA2-PSK") -> "[WPA2-PSK]"
                capabilities.contains("WPA-PSK") -> "[WPA-PSK]"
                capabilities.contains("WEP") -> "[WEP]"
                else -> "[OPEN]"
            }
        }
    }
}