package com.benrostudios.gakko.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.benrostudios.gakko.data.models.Material

interface MaterialsRepository {
    val downloadURL: LiveData<String>
    suspend fun uploadMaterial(uri: Uri, phoneNumber: String, displayName: String)
    suspend fun uploadMaterialInformation(material: Material, classNumber: String)
}