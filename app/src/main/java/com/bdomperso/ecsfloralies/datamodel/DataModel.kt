package com.bdomperso.ecsfloralies.datamodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DataModel(jsonTxt: String): ViewModel() {

    private val _buildings: MutableLiveData<List<Building>>

    private val _filteredStages: MutableLiveData<List<Stage>> = MutableLiveData()
    private val _filteredApartments: MutableLiveData<List<Apartment>> = MutableLiveData()
    private val _filteredApartment: MutableLiveData<Apartment> = MutableLiveData()
    private val _filename = MutableLiveData<String>()

    init {
        val jsonData = Json.decodeFromString<Residence>(jsonTxt)
        _buildings = MutableLiveData(jsonData.buildings)
    }

    val buildings = Transformations.map(_buildings) { buildingList ->
        buildingList.map { building -> building.name }
    }

    var filteredStages = Transformations.map(_filteredStages) { stages ->
        stages.map { it.level }
    }

    val filteredApartments = Transformations.map(_filteredApartments) { apartments ->
        apartments.map { it.type }
    }

    val filteredCounters = Transformations.map(_filteredApartment) { apartment ->
        listOf(apartment.counter1, apartment.counter2)
    }

    val filename: LiveData<String> = _filename

    var selectedBuilding: Any = "A"
        set(value) {
            field = value
            println("building: $value")
            val filtered = _buildings.value?.first { it.name == selectedBuilding }?.stages
            _filteredStages.value = filtered
            _filename.value = composeFilename()
            filtered?.forEach { println("building: ${it.level}") }
        }

    var selectedStage: Any = 0
        set(value) {
            field = value
            println("stage: $value")
            val filtered = _filteredStages.value?.first { it.level == selectedStage}?.apartments
            _filteredApartments.value = filtered
            _filename.value = composeFilename()
            filtered?.forEach { println("apartment:${it.type}") }
        }

    var selectedApartment: Any = ""
        set(value) {
            field = value
            println("apartment: $value")
            _filteredApartment.value = _filteredApartments.value?.first { it.type == selectedApartment }
            _filename.value = composeFilename()
        }

    var selectedCounter: Any = ""
        set(value) {
            field = value
            _filename.value = composeFilename()
            println("counter: $value")
        }

    private fun composeFilename(): String {
        val building = this.selectedBuilding as String
        val stage = this.selectedStage as Int
        val apartment = this.selectedApartment as String
        val counter = this.selectedCounter as String
        return  "${building}_ET${stage}_${apartment}_${counter}.jpg"
    }

    private fun printAll() {
        _buildings.value?.forEach { building ->
            println("Bat: ${building.name}")
            building.stages.forEach{ stage ->
                println("  Stage: ${stage.level}")
                stage.apartments.forEach{ apartment ->
                    println("    Apartment: ${apartment.type}")
                    println("      Cpt1: ${apartment.counter1}")
                    println("      Cpt2: ${apartment.counter2}")
                }
            }
        }
    }
}
