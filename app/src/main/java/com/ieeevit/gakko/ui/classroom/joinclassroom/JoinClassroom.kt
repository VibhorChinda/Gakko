package com.ieeevit.gakko.ui.classroom.joinclassroom

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

import com.ieeevit.gakko.R
import com.ieeevit.gakko.internal.Utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.join_classroom_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class JoinClassroom : BottomSheetDialogFragment(), KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: JoinClassroomViewModelFactory by instance()
    private val utils: Utils by instance()

    companion object {
        fun newInstance() = JoinClassroom()
    }

    private lateinit var viewModel: JoinClassroomViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.join_classroom_fragment, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel =
            ViewModelProvider(this, viewModelFactory).get(JoinClassroomViewModel::class.java)
        usr_join_classroom_btn.setOnClickListener {
            validate()
        }
    }

    private fun validate() {
        val classCode = join_class_code_input.text.toString()
        var validation = true
        if (classCode.isEmpty()) {
            join_class_code_input.error = "Class Code cannot by empty"
            validation = false
        }
        if (validation) {
            this.isCancelable = false
            join_progress.visibility = View.VISIBLE
            checkEligibility(classCode)
        }
    }

    private fun checkEligibility(classCode: String)  = viewLifecycleOwner.lifecycleScope.launch{
        Log.d("JoinClassroom","hello")
        viewModel._user_classroom_ids.observe(viewLifecycleOwner, Observer {
            Log.d("JoinClassroom","The recieved list is $it")
            var eligible = true
            if(it.contains(classCode)){
                Toast.makeText(context,"You are already part of this class",Toast.LENGTH_SHORT).show()
                eligible = false
            }
            Log.d("JoinClassroom","The eligibility is $eligible")
            if(eligible){
                Log.d("JoinClassroom","You are eligible")
                classroomValidation()
                joinClassroom(classCode)
            }else{
                dismiss()
            }
        })
    }

    private fun joinClassroom(classCode: String) = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.joinClass(classCode)
    }


    private fun classroomValidation() = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.classExistenceResponse.observe(viewLifecycleOwner, Observer {
            if(!it){
                Toast.makeText(context,"This Classroom doesn't exists", Toast.LENGTH_SHORT).show()
                dismiss()
            }else{
                joinClassroomResponseListener()
            }
        })
    }

    private fun joinClassroomResponseListener() = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.usrJoinClassroomResponse.observe(viewLifecycleOwner, Observer {
            if(it){
                Toast.makeText(context,"You have been enrolled into this class successfully!", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(context,"You request to join this classroom has been sent!",Toast.LENGTH_LONG).show()
            }
            dismiss()
        })


    }




}
