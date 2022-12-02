package com.bdomperso.ecsfloralies.datamodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DataModel(jsonTxt: String): ViewModel() {

    private val TAG = "DataModel"

    private val _buildings: MutableLiveData<List<Building>>

    private val _filteredStages: MutableLiveData<List<Stage>> = MutableLiveData()
    private val _filteredApartments: MutableLiveData<List<Apartment>> = MutableLiveData()
    private val _filteredApartment: MutableLiveData<Apartment> = MutableLiveData()
    private val _filename = MutableLiveData<String>()
    private val _imageUri = MutableLiveData<Uri>()

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

    val imageUri: LiveData<Uri> = _imageUri

    var image: String = ""
        set(value) { _imageUri.value = Uri.parse(value) }

    var selectedBuilding: Any = "A"
        set(value) {
            field = value
            Log.i(TAG, "selectedBuilding: $value")
            val filtered = _buildings.value?.first { it.name == selectedBuilding }?.stages
            _filteredStages.value = filtered!!
            _filename.value = composeFilename()
        }

    var selectedStage: Any = 0
        set(value) {
            field = value
            Log.i(TAG, "selectedStage: $value")
            val filtered = _filteredStages.value?.first { it.level == selectedStage }?.apartments
            _filteredApartments.value = filtered!!
            _filename.value = composeFilename()
        }

    var selectedApartment: Any = ""
        set(value) {
            field = value
            Log.i(TAG, "selectedApartment: $value")
            _filteredApartment.value = _filteredApartments.value?.first { it.type == selectedApartment }
            _filename.value = composeFilename()
        }

    var selectedCounter: Any = ""
        set(value) {
            field = value
            Log.i(TAG, "selectedCounter: $value")
            _filename.value = composeFilename()
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
            building.stages.forEach{ stage ->
                stage.apartments.forEach{ apartment ->
                    println("${building.name}\t${stage.level}\t${apartment.type}\t${apartment.counter1}\t${apartment.counter2}")
                }
            }
        }
    }
}
