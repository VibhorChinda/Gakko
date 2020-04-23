package com.benrostudios.gakko.ui.classroom.classroomdisplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.benrostudios.gakko.data.repository.ClassroomRepository


@Suppress("UNCHECKED_CAST")
class ClassroomDisplayViewModelFactory(
    private val classroomRepository: ClassroomRepository
): ViewModelProvider.NewInstanceFactory(){
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ClassroomDisplayViewModel(classroomRepository) as T
    }
}