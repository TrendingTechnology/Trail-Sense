package com.kylecorry.trail_sense.weather

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.util.*

object PressureHistoryRepository: Observable() {

    private const val FILE_NAME = "pressure.csv"
    private val keepDuration: Duration = Duration.ofHours(48)

    private var readings: MutableList<PressureReading> = mutableListOf()

    private var loaded = false

    fun getAll(context: Context): List<PressureReading> {
        if (!loaded){
            loadFromFile(context)
        }
        return readings
    }

    fun add(context: Context, reading: PressureReading): PressureReading {
        if (!loaded){
            loadFromFile(context)
        }
        readings.add(reading)
        removeOldReadings()
        saveToFile(context)
        setChanged()
        notifyObservers()
        return reading
    }

    private fun removeOldReadings(){
        readings.removeIf { Duration.between(it.time, Instant.now()) > keepDuration }
    }

    private fun loadFromFile(context: Context) {
        val readings = mutableListOf<PressureReading>()
        if (!context.getFileStreamPath(FILE_NAME).exists()) return
        context.openFileInput(FILE_NAME).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val splitLine = line.split(",")
                val time = splitLine[0].toLong()
                val pressure = splitLine[1].toFloat()
                val altitude = splitLine[2].toDouble()
                readings.add(PressureReading(Instant.ofEpochMilli(time), pressure, altitude))
            }
        }
        loaded = true
        this.readings = readings
        removeOldReadings()
        setChanged()
        notifyObservers()
    }

    private fun saveToFile(context: Context){
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            val output = readings.joinToString("\n") { reading ->
                "${reading.time.toEpochMilli()},${reading.pressure},${reading.altitude}"
            }
            it.write(output.toByteArray())
        }
    }

}