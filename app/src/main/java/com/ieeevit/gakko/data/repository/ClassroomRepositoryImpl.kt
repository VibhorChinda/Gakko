package com.ieeevit.gakko.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ieeevit.gakko.data.models.Classroom
import com.ieeevit.gakko.data.network.response.GetClassroomIdResponse
import com.ieeevit.gakko.data.network.service.CreateClassroomService
import com.ieeevit.gakko.internal.Utils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.ieeevit.gakko.data.models.User

class ClassroomRepositoryImpl(
    private val utils: Utils,
    private val createClassroomService: CreateClassroomService
) : ClassroomRepository {

    private var _fetchedClassroomId = MutableLiveData<GetClassroomIdResponse>()
    private val _classrooms = MutableLiveData<List<Classroom>>()
    private val _classroomIds = MutableLiveData<List<String>>()
    private val _join_classroom_response = MutableLiveData<Boolean>()
    private val _classroomExistenceResponse = MutableLiveData<Boolean>()
    private lateinit var databaseReference: DatabaseReference
    private var classroomIds = mutableListOf<String>()
    private var classList = mutableListOf<Classroom>()
    private val teachersList = utils.retrieveTeachersList() ?: mutableSetOf<String>()
    private val _exitClassroomStatus = MutableLiveData<Boolean>()


    override val classroomExistenceResponse: LiveData<Boolean>
        get() = _classroomExistenceResponse


    override val classrooms: LiveData<List<Classroom>>
        get() = _classrooms


    override val createClassroomId: LiveData<GetClassroomIdResponse>
        get() = _fetchedClassroomId

    override val userClassroomIds: LiveData<List<String>>
        get() = _classroomIds

    override val joinClassroomResponse: LiveData<Boolean>
        get() = _join_classroom_response

    override suspend fun getClassrooms() {
        databaseReference =
            Firebase.database.getReference("/users/${utils.retrieveMobile()}/classrooms")
        val classroomFetcher = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                classroomIds = mutableListOf()
                if (p0.exists()) {
                    classList = mutableListOf()
                    for (x in p0.children) {
                        classroomIds.add(x.value.toString())
                        classroomLoader(x.value.toString())
                        Log.d("ClassroomRepo", "Posted Logic")
                    }
                    _classroomIds.postValue(classroomIds)
                } else {
                    _classroomIds.postValue(emptyList())
                    _classrooms.postValue(emptyList())
                    Log.d("ClassroomRepo", "Posted EmptyList")
                }

            }
        }
        databaseReference.addValueEventListener(classroomFetcher)
    }

    override suspend fun fetchClassroomId() {
        val fetchClassroomId = createClassroomService.getClassroomId()
        _fetchedClassroomId.postValue(fetchClassroomId.body())
    }

    override suspend fun createClassroom(classroom: Classroom) {
        databaseReference = Firebase.database.reference
        databaseReference.child("classrooms").child(classroom.classroomID).setValue(classroom)
        updateUserClassroom(classroom.classroomID)
    }

    override suspend fun joinClassroom(classCode: String) {
        databaseReference = Firebase.database.getReference("/classrooms/$classCode")
        Log.d("ClassroomRepo", "The JOIN id is $classCode")
        val joinClassroomFetcher = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                val classroom = p0.getValue(Classroom::class.java)
                if (p0.exists()) {
                    _classroomExistenceResponse.postValue(true)
                    Log.d("ClassroomRepo", "Class does exists")
                    if (classroom?.privacy == false) {
                        databaseReference = Firebase.database.getReference("/classrooms/$classCode")
                        val students: MutableList<String> = classroom.students.toMutableList()
                            ?: mutableListOf(utils.retrieveMobile()!!)
                        if (students.isEmpty()) {
                            students.add(utils.retrieveMobile()!!)
                            databaseReference.child("students").setValue(students)
                        } else {
                            students.add(utils.retrieveMobile()!!)
                            databaseReference.child("students").setValue(students)
                        }
                        updateUserClassroom(classCode)
                        _join_classroom_response.postValue(true)
                    } else if (classroom?.privacy == true) {
                        var requests: MutableList<Map<String, String>> =
                            classroom.requests.toMutableList()
                        val unixTime = System.currentTimeMillis() / 1000L
                        requests.add(mapOf(Pair(utils.retrieveMobile() ?: "", unixTime.toString())))
                        databaseReference = Firebase.database.getReference("/classrooms/$classCode")
                        databaseReference.child("requests").setValue(requests)
                        _join_classroom_response.postValue(false)
                    }
                } else {
                    _classroomExistenceResponse.postValue(false)
                }
            }
        }
        databaseReference.addListenerForSingleValueEvent(joinClassroomFetcher)
    }


    fun classroomLoader(ids: String) {
        databaseReference = Firebase.database.getReference("/classrooms/$ids")
        Log.d("classroom fetcher", ids)

        val classroomLoader = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                var validation = true
                val classroom = p0.getValue(Classroom::class.java)
                Log.d("classroom fetcher", classroom.toString())
                if (classroom?.teachers?.contains(utils.retrieveMobile()) == true && classroom.privacy) {
                    teachersList.add(classroom.classroomID)
                    utils.saveTeacherList(teachersList)
                    Log.d("classroom fetcher", "teacher of: ${classroom.classroomID}")
                }
                classList.forEach {
                    if (it.classroomID == classroom!!.classroomID) {
                        if (it.courseCode == classroom.courseCode) {
                            validation = false
                        }
                    }
                }
                if(validation){
                    classList.add(classroom!!)
                    _classrooms.postValue(classList)
                }


            }
        }
        databaseReference.addListenerForSingleValueEvent(classroomLoader)
    }

    fun updateUserClassroom(classCode: String) {
        databaseReference = Firebase.database.getReference("/users/${utils.retrieveMobile()}")
        classroomIds.add(classCode)
        databaseReference.child("classrooms").setValue(classroomIds)
    }

    override suspend fun exitClassroom(classCode: String) {
        databaseReference = Firebase.database.getReference("/users/${utils.retrieveMobile()}")
        classroomIds.remove(classCode)
        databaseReference.child("classrooms").setValue(classroomIds)
        exitClassroomOperation(classCode)
    }

    private fun exitClassroomOperation(classCode: String) {
        databaseReference = Firebase.database.getReference("/classrooms/$classCode")
        val exitClassroomExecutor = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                _exitClassroomStatus.postValue(false)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val classroom = p0.getValue(Classroom::class.java)
                if (p0.exists()) {
                    databaseReference = Firebase.database.getReference("/classrooms/$classCode")
                    val students: MutableList<String> = classroom?.students?.toMutableList()
                        ?: mutableListOf()
                    if (classroom?.teachers?.contains(utils.retrieveMobile()) == true) {
                        if (students.isEmpty()) {
                            databaseReference.removeValue()
                            _exitClassroomStatus.postValue(true)
                        } else {
                            bulkClassRemover(classCode, students)
                        }
                    } else {

                        students.remove(utils.retrieveMobile()!!)
                        databaseReference.child("students").setValue(students)
                        _exitClassroomStatus.postValue(true)
                    }
                } else {
                    _exitClassroomStatus.postValue(false)
                }
            }
        }
        databaseReference.addListenerForSingleValueEvent(exitClassroomExecutor)
    }

    private fun bulkClassRemover(classCode: String, students: List<String>) {
        for (x in students) {
            databaseReference = Firebase.database.getReference("/users/$x")
            val classroomFetcher = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("Not yet implemented")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.exists()) {
                        val user: User? = p0.getValue(User::class.java)
                        var classrooms = user?.classrooms?.toMutableList() ?: mutableListOf()
                        classrooms.remove(classCode)
                        databaseReference.child("classrooms").setValue(classrooms)
                    }
                }

            }
            databaseReference.addListenerForSingleValueEvent(classroomFetcher)
        }
        _exitClassroomStatus.postValue(true)
    }

    override val classroomExitStatus: LiveData<Boolean>
        get() = _exitClassroomStatus
}