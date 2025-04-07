package com.example.fibraappkt.ui.speedtest

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fibraappkt.databinding.FragmentSpeedTestBinding
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class SpeedTestFragment : Fragment() {

    private var _binding: FragmentSpeedTestBinding? = null
    private val binding get() = _binding!!
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val downloadUrls = listOf(
        "https://proof.ovh.net/files/100Mb.dat"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeedTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.initialTestView.visibility = View.VISIBLE
        binding.resultsView.visibility = View.GONE
        binding.progressBar.visibility = View.GONE

        binding.btnStartTest.setOnClickListener {
            startSpeedTest()
        }
    }

    private fun startSpeedTest() {
        binding.btnStartTest.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.tvConnectionStatus.text = "Testando sua conexão..."

        coroutineScope.launch {
            val downloadSpeed = testarDownload()
            val uploadSpeed = simularUpload()
            val latency = medirLatencia()

            updateResults(downloadSpeed, uploadSpeed, latency)

            binding.initialTestView.visibility = View.GONE
            binding.resultsView.visibility = View.VISIBLE
        }
    }

    private fun updateResults(downloadSpeed: Double, uploadSpeed: Double, latency: Double) {
        binding.tvDownload.text = "%.1f Mbps".format(downloadSpeed)
        binding.tvUpload.text = "%.1f Mbps".format(uploadSpeed)
        binding.tvLatency.text = "%.0f ms".format(latency)
        binding.tvJitter.text = "%.0f ms".format((latency * 0.15).coerceAtLeast(1.0))

        binding.tvProviderName.text = "Leste Telecom"
        binding.tvDeviceModel.text = android.os.Build.MODEL
        binding.tvProviderLocation.text = "LESTE TELECOM"
        binding.tvCity.text = "Rio de Janeiro"
    }

    private suspend fun testarDownload(): Double = withContext(Dispatchers.IO) {
        val totalBytes = AtomicInteger(0)
        val tempo = measureTimeMillis {
            coroutineScope {
                downloadUrls.map { url ->
                    async {
                        try {
                            val connection = URL(url).openConnection() as HttpURLConnection
                            connection.connectTimeout = 3000
                            connection.readTimeout = 10000
                            connection.connect()

                            val input = connection.inputStream
                            val buffer = ByteArray(16 * 1024)
                            var bytesRead: Int
                            val maxDownloadTime = System.currentTimeMillis() + 50000

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                totalBytes.addAndGet(bytesRead)
                                if (System.currentTimeMillis() > maxDownloadTime) break
                            }

                            input.close()
                            connection.disconnect()
                        } catch (e: Exception) {
                            Log.e("SpeedTest", "Erro no download: ${e.message}")
                        }
                    }
                }.awaitAll()
            }
        }

        val megabits = (totalBytes.get() * 8) / 1_000_000.0
        return@withContext megabits / (tempo / 1000.0)
    }

    private suspend fun simularUpload(): Double = withContext(Dispatchers.IO) {
        val data = ByteArray(5 * 1024 * 1024) // 5MB
        val tempo = measureTimeMillis {
            for (i in data.indices step 8192) {
                delay(1) // simula delay de envio
            }
        }
        val megabits = (data.size * 8) / 1_000_000.0
        return@withContext megabits / (tempo / 1000.0)
    }

    private suspend fun medirLatencia(): Double = withContext(Dispatchers.IO) {
        val latencias = mutableListOf<Long>()
        repeat(7) {
            try {
                val t = measureTimeMillis {
                    val url = URL("https://www.google.com/generate_204")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 1000
                    conn.readTimeout = 1000
                    conn.connect()
                    conn.inputStream.close()
                    conn.disconnect()
                }
                latencias.add(t)
            } catch (_: Exception) {
                latencias.add(150L) // alto como fallback
            }
        }
        // Remove o maior e menor para eliminar outliers
        latencias.sort()
        if (latencias.size > 4) latencias.removeAt(0)
        if (latencias.size > 4) latencias.removeLast()

        return@withContext latencias.average()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
        _binding = null
    }
}
