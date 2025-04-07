package com.example.fibraappkt.ui.wifi

import android.content.Context
import android.net.wifi.ScanResult
import androidx.core.content.ContextCompat
import com.example.fibraappkt.R
import kotlin.math.absoluteValue

class ChannelDataAdapter(private val context: Context) {

    // Dados de canal para redes 2.4GHz (canais 1-14)
    private val channels24GHz = (1..14).associateWith { mutableListOf<ScanResult>() }

    // Dados de canal para redes 5GHz (canais 36-165)
    private val channels5GHz = listOf(36, 40, 44, 48, 52, 56, 60, 64,
        100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
        149, 153, 157, 161, 165)
        .associateWith { mutableListOf<ScanResult>() }

    // Estrutura para armazenar informações de rating de cada canal
    data class ChannelRating(
        val channel: Int,
        val score: Float,
        val starRating: Int, // 1-10 stars
        val networks: List<ScanResult>
    )

    fun processResults(results: List<ScanResult>) {
        // Limpar dados anteriores
        channels24GHz.values.forEach { it.clear() }
        channels5GHz.values.forEach { it.clear() }

        // Classificar resultados por canal
        results.forEach { result ->
            val channel = getChannelFromFrequency(result.frequency)
            when {
                channel in 1..14 -> channels24GHz[channel]?.add(result)
                channel in channels5GHz.keys -> channels5GHz[channel]?.add(result)
            }
        }
    }

    fun getChannelRatings(isFrequency5GHz: Boolean): List<ChannelRating> {
        val channelsMap = if (isFrequency5GHz) channels5GHz else channels24GHz

        val ratings = channelsMap.map { (channel, networks) ->
            val score = if (networks.isEmpty()) 0f else calculateChannelScore(channel, networks)
            // Converter a pontuação para uma escala de 1-10 estrelas
            val starRating = (score / 10).toInt().coerceIn(1, 10)

            ChannelRating(
                channel = channel,
                score = score,
                starRating = starRating,
                networks = networks
            )
        }

        // Ordenar canais por pontuação (melhores primeiro)
        return ratings.sortedByDescending { it.score }
    }

    fun getBestChannels(isFrequency5GHz: Boolean, limit: Int = 3): List<Int> {
        return getChannelRatings(isFrequency5GHz)
            .filter { it.networks.isNotEmpty() } // Apenas canais com redes
            .take(limit)
            .map { it.channel }
    }

    // Calcular pontuação do canal baseado na força dos sinais e interferências
    private fun calculateChannelScore(targetChannel: Int, networks: List<ScanResult>): Float {
        var score = 0f

        // Pontuação pela força dos sinais no canal
        networks.forEach { network ->
            // Normalizar o nível (de -100dBm a 0dBm para 0-100)
            score += (network.level + 100) / 100f * 100f
        }

        // Fator de penalidade por interferência de canais adjacentes
        val channelRanges = when {
            targetChannel in 1..14 -> channels24GHz
            else -> channels5GHz
        }

        // Canais adjacentes causam interferência
        channelRanges.forEach { (channel, channelNetworks) ->
            if (channel != targetChannel && channelNetworks.isNotEmpty()) {
                // Calcular sobreposição baseada na distância entre canais
                // Maior penalidade para canais mais próximos
                val distance = (channel - targetChannel).absoluteValue
                val penalty = when {
                    distance <= 4 -> (5 - distance) * 5f  // Maior penalidade para canais mais próximos
                    else -> 0f  // Sem penalidade para canais distantes
                }

                // Aplicar penalidade baseada na força do sinal das redes interferentes
                channelNetworks.forEach { network ->
                    val normalizedSignal = (network.level + 100) / 100f * 100f
                    score -= (normalizedSignal * penalty / 10f)
                }
            }
        }

        // Garantir que a pontuação não fique negativa
        return score.coerceAtLeast(0f)
    }

    private fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency >= 2412 && frequency <= 2484 -> (frequency - 2412) / 5 + 1
            frequency >= 5170 && frequency <= 5825 -> when {
                // Mapeamento específico para canais 5GHz
                frequency in 5170..5250 -> (frequency - 5170) / 5 + 34
                frequency in 5250..5330 -> (frequency - 5250) / 5 + 50
                frequency in 5490..5730 -> (frequency - 5490) / 5 + 100
                frequency in 5735..5835 -> (frequency - 5735) / 5 + 149
                else -> 0
            }
            else -> 0
        }
    }
}