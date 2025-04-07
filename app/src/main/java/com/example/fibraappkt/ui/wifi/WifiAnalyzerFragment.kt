package com.example.fibraappkt.ui.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fibraappkt.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButtonToggleGroup

class WifiAnalyzerFragment : Fragment() {

    private lateinit var wifiManager: WifiManager
    private lateinit var wifiListAdapter: WifiListAdapter
    private lateinit var channelDataAdapter: ChannelDataAdapter
    private lateinit var channelRatingAdapter: ChannelRatingAdapter
    private lateinit var chart: LineChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var btnToggleGroup: MaterialButtonToggleGroup
    private lateinit var btn24GHz: Button
    private lateinit var btn5GHz: Button
    private lateinit var tvBestChannels: TextView

    private var isFrequency5GHz = false

    private val handler = Handler(Looper.getMainLooper())
    private val scanInterval = 5000L // 5 segundos
    private var isScanning = false

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val success = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
            if (success) {
                showResults()
            } else {
                Toast.makeText(context, "Erro ao escanear WiFi", Toast.LENGTH_SHORT).show()
            }

            // Continuar escaneando se estiver no modo de escaneamento contínuo
            if (isScanning) {
                handler.postDelayed({ scanWifi() }, scanInterval)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wifi_analyzer, container, false)

        tabLayout = view.findViewById(R.id.tabLayout)
        chart = view.findViewById(R.id.chartWifi)
        recyclerView = view.findViewById(R.id.recyclerViewWifi)
        btnToggleGroup = view.findViewById(R.id.toggleButtonGroup)
        btn24GHz = view.findViewById(R.id.btn24GHz)
        btn5GHz = view.findViewById(R.id.btn5GHz)
        tvBestChannels = view.findViewById(R.id.tvBestChannels)

        setupUI()

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        channelDataAdapter = ChannelDataAdapter(requireContext())
        channelRatingAdapter = ChannelRatingAdapter()

        checkPermissionsAndScan()

        return view
    }

    private fun setupUI() {
        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        wifiListAdapter = WifiListAdapter()
        recyclerView.adapter = wifiListAdapter

        // Configurar o gráfico
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = true
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f

        // Configurar toggle button para frequência
        btnToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isFrequency5GHz = (checkedId == R.id.btn5GHz)
                updateChannelRatingView()
            }
        }

        // Configurar as tabs
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { // Access Points
                        recyclerView.adapter = wifiListAdapter
                        recyclerView.visibility = View.VISIBLE
                        chart.visibility = View.GONE
                        btnToggleGroup.visibility = View.GONE
                        tvBestChannels.visibility = View.GONE
                    }
                    1 -> { // Channel Rating
                        recyclerView.adapter = channelRatingAdapter
                        recyclerView.visibility = View.VISIBLE
                        chart.visibility = View.GONE
                        btnToggleGroup.visibility = View.VISIBLE
                        tvBestChannels.visibility = View.VISIBLE
                        updateChannelRatingView()
                    }
                    2 -> { // Channel Graph
                        showChannelGraph()
                        recyclerView.visibility = View.GONE
                        chart.visibility = View.VISIBLE
                        btnToggleGroup.visibility = View.GONE
                        tvBestChannels.visibility = View.GONE
                    }
                    3 -> { // Time Graph
                        showTimeGraph()
                        recyclerView.visibility = View.GONE
                        chart.visibility = View.VISIBLE
                        btnToggleGroup.visibility = View.GONE
                        tvBestChannels.visibility = View.GONE
                        // Iniciar escaneamento contínuo
                        startContinuousScan()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                if (tab.position == 3) {
                    // Parar escaneamento contínuo quando sair da tab Time Graph
                    stopContinuousScan()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Recarregar dados
                scanWifi()
            }
        })
    }

    private fun updateChannelRatingView() {
        val channelRatings = channelDataAdapter.getChannelRatings(isFrequency5GHz)
        channelRatingAdapter.updateRatings(channelRatings)

        // Atualizar texto de melhores canais
        val bestChannels = channelDataAdapter.getBestChannels(isFrequency5GHz)
        if (bestChannels.isNotEmpty()) {
            tvBestChannels.text = "Melhores Canais: ${bestChannels.joinToString(", ")}"
        } else {
            tvBestChannels.text = "Sem canais disponíveis"
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        )

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), missing.toTypedArray(), 1001)
        } else {
            scanWifi()
        }
    }

    private fun scanWifi() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(context, "Wi-Fi está desativado, ativando...", Toast.LENGTH_SHORT).show()
            wifiManager.isWifiEnabled = true
        }

        requireContext().registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
    }

    private fun showResults() {
        val results = wifiManager.scanResults

        // Ordenar por força do sinal (mais forte primeiro)
        val sortedResults = results.sortedByDescending { it.level }

        wifiListAdapter.updateList(sortedResults)

        // Processar resultados para o adaptador de canal
        channelDataAdapter.processResults(results)

        // Atualizar a visão atual se visível
        when (tabLayout.selectedTabPosition) {
            1 -> updateChannelRatingView()
            2 -> showChannelGraph()
            3 -> updateTimeGraph(sortedResults)
        }
    }

    private fun showChannelGraph() {
        val results = wifiManager.scanResults

        // Agrupar redes por canal
        val channelData = mutableMapOf<Int, MutableList<ScanResult>>()

        results.forEach { result ->
            val channel = getChannelFromFrequency(result.frequency)
            if (!channelData.containsKey(channel)) {
                channelData[channel] = mutableListOf()
            }
            channelData[channel]?.add(result)
        }

        // Criar dataset para cada canal
        val dataSets = mutableListOf<LineDataSet>()
        val colors = listOf(
            R.color.teal_700,
            R.color.purple_500,
            R.color.primaryColor,
            R.color.secondaryColor,
            R.color.errorColor
        )

        channelData.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
            val channel = entry.key
            val networks = entry.value

            val entries = networks.mapIndexed { i, result ->
                // Normalizar de -100dBm para gráfico
                Entry(channel.toFloat(), (result.level + 100).toFloat())
            }

            val colorIndex = index % colors.size
            val color = ContextCompat.getColor(requireContext(), colors[colorIndex])

            val dataSet = LineDataSet(entries, "Canal $channel").apply {
                this.color = color
                setDrawCircles(true)
                setCircleColor(color)
                lineWidth = 2f
                valueTextSize = 10f
            }

            dataSets.add(dataSet)
        }

        chart.data = LineData(dataSets as List<ILineDataSet>)
        chart.description = Description().apply {
            text = "Canais Wi-Fi"
            textSize = 14f
        }
        chart.invalidate()
    }

    private val timeGraphData = mutableMapOf<String, MutableList<Entry>>()
    private var timeGraphX = 0f

    private fun updateTimeGraph(results: List<ScanResult>) {
        // Adicionar novos pontos para redes existentes
        results.forEach { result ->
            val ssid = result.SSID.ifEmpty { result.BSSID }
            if (!timeGraphData.containsKey(ssid)) {
                timeGraphData[ssid] = mutableListOf()
            }

            // Adicionar novo ponto de dados
            timeGraphData[ssid]?.add(Entry(timeGraphX, result.level.toFloat()))

            // Manter apenas os últimos 20 pontos por rede
            if ((timeGraphData[ssid]?.size ?: 0) > 20) {
                timeGraphData[ssid]?.removeAt(0)
            }
        }

        timeGraphX += 1f

        // Criar datasets
        val dataSets = mutableListOf<LineDataSet>()
        val colors = listOf(
            R.color.teal_700,
            R.color.purple_500,
            R.color.primaryColor,
            R.color.secondaryColor,
            R.color.errorColor
        )

        timeGraphData.entries.forEachIndexed { index, entry ->
            val ssid = entry.key
            val data = entry.value

            val colorIndex = index % colors.size
            val color = ContextCompat.getColor(requireContext(), colors[colorIndex])

            val dataSet = LineDataSet(data, ssid).apply {
                this.color = color
                setDrawCircles(true)
                setCircleColor(color)
                lineWidth = 2f
                valueTextSize = 10f
                // Opcional: Apenas mostrar valores no último ponto
                setDrawValues(false)
            }

            dataSets.add(dataSet)
        }

        chart.data = LineData(dataSets as List<ILineDataSet>)

        chart.description = Description().apply {
            text = "Sinal Wi-Fi ao Longo do Tempo"
            textSize = 14f
        }
        chart.animateX(300)
        chart.invalidate()
    }

    private fun showTimeGraph() {
        // Configurar o gráfico para exibição ao longo do tempo
        chart.xAxis.valueFormatter = null
        chart.xAxis.granularity = 1f
        chart.axisLeft.axisMinimum = -100f
        chart.axisLeft.axisMaximum = 0f
        chart.axisLeft.axisLineWidth = 2f

        // Iniciar com dados atuais
        val results = wifiManager.scanResults
        if (results.isNotEmpty()) {
            updateTimeGraph(results)
        }
    }

    private fun startContinuousScan() {
        isScanning = true
        handler.post { scanWifi() }
    }

    private fun stopContinuousScan() {
        isScanning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency >= 2412 && frequency <= 2484 -> (frequency - 2412) / 5 + 1
            frequency >= 5170 && frequency <= 5825 -> (frequency - 5170) / 5 + 34
            else -> 0
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            scanWifi()
        } else {
            Toast.makeText(context, "Permissões necessárias para escanear redes Wi-Fi", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Registrar receiver quando o fragment estiver visível
        requireContext().registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        // Escanear redes imediatamente
        scanWifi()
    }

    override fun onPause() {
        super.onPause()
        // Parar escaneamento contínuo e desregistrar receiver
        stopContinuousScan()
        try {
            requireContext().unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Receptor já foi desregistrado ou não estava registrado
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopContinuousScan()
    }
}