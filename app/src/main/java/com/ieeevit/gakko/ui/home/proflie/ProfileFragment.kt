package com.ieeevit.gakko.ui.home.proflie

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.ieeevit.gakko.R
import com.ieeevit.gakko.data.models.User
import com.ieeevit.gakko.internal.AvatarConstants
import com.ieeevit.gakko.internal.AvatarGenerator
import com.ieeevit.gakko.internal.Utils
import com.ieeevit.gakko.ui.auth.AuthActivity
import com.ieeevit.gakko.ui.base.ScopedFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.profile_fragment.*
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import java.io.File

class ProfileFragment : ScopedFragment(), KodeinAware {

    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: ProfileViewModelFactory by instance()
    private val utils: Utils by instance()
    private lateinit var navController: NavController
    private lateinit var uri: Uri
    private var imageName: String? = null
    private var uriString: String? = null
    private lateinit var userObject: User


    companion object {
        fun newInstance() = ProfileFragment()
        const val PICK_IMG_CODE = 11
    }

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.profile_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ProfileViewModel::class.java)
        var storageReference: StorageReference = FirebaseStorage.getInstance().reference
        val profilePicUploader = storageReference.child("dp/${utils.retrieveMobile()}/dp.jpg")
        fetchUser()

        var options: RequestOptions = RequestOptions()
            .error(AvatarGenerator.avatarImage(requireContext(), 200, AvatarConstants.CIRCLE, utils.retrieveCurrentUserName()!!))
            .placeholder(AvatarGenerator.avatarImage(requireContext(), 200, AvatarConstants.CIRCLE, utils.retrieveCurrentUserName()!!))
            .circleCrop()

        if(utils.retrieveProfilePic().isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicUploader)
                .apply(options)
                .placeholder(AvatarGenerator.avatarImage(requireContext(), 200, AvatarConstants.CIRCLE, utils.retrieveCurrentUserName()!!))
                .into(proflie_fragment_user_picture_image_view)
        }else {
            Glide.with(this)
                .load(utils.retrieveProfilePic())
                .apply(options)
                .placeholder(AvatarGenerator.avatarImage(requireContext(), 200, AvatarConstants.CIRCLE, utils.retrieveCurrentUserName()!!))
                .into(proflie_fragment_user_picture_image_view)
        }

        proflie_fragment_user_picture_image_view.setOnClickListener {
            getImage()
        }

        update_profile_button.setOnClickListener {
           validate()
        }

        logout_button.setOnClickListener {
            logout()
        }

        profile_fragment_name_edit_icon.setOnClickListener {
            profile_name_input.isEnabled = true
            profile_name_input.requestFocus()
        }

        profile_fragment_name_edit_icon3.setOnClickListener {
            profile_display_name_input.isEnabled = true
            profile_display_name_input.requestFocus()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        back_image_icon.setOnClickListener { Navigation.findNavController(view).popBackStack() }
    }

    private fun validate(){
        var name = profile_name_input.text.toString()
        var displayname = profile_display_name_input.text.toString()
        var validation = true
        if(name.isNullOrEmpty()){
            profile_name_input.error = "Enter Valid Name"
            validation = false
        }
        if(displayname.isNullOrEmpty()){
            profile_display_name_input.error = "Enter valid display name"
            validation = false
        }
        if(validation){
            updateProfilePic(name ,displayname)
            utils.saveCurrentUserName(name)
            profile_progress.visibility = View.VISIBLE
        }

    }

    private fun fetchUser() = launch{
        viewModel.fetchUser(utils.retrieveMobile()?:"")
        viewModel.userObject.observe(viewLifecycleOwner, Observer {
            userObject = it
            profile_name_input.setText(it.name)
            profile_display_name_input.setText(it.displayName)
        })
    }

    private fun updateProfilePic(name: String, displayname: String) = launch {
        var imageUploadTask = false
        if(!uriString.isNullOrEmpty()){
            imageUploadTask = true
            viewModel.uploadProfilePic(uri, utils.retrieveMobile() ?: "")
        }
        if(imageUploadTask){
            viewModel.response.observe(viewLifecycleOwner, Observer {
                if(it){
                    profileUpdater(name , displayname)
                }else{
                    Toast.makeText(context,"There was a error uploading your picture!", Toast.LENGTH_SHORT).show()
                    profile_progress.visibility = View.GONE
                }
            })
        }else{
           profileUpdater(name , displayname)
        }

    }

    private fun profileUpdater(name: String,displayname: String) = launch {
        viewModel.updateProfile(name,displayname,utils.retrieveMobile()?: "")
        Toast.makeText(context,"Your Profile has been updated!",Toast.LENGTH_SHORT).show()
        profile_progress.visibility = View.GONE
        profile_name_input.isEnabled = false
        profile_name_input.isEnabled = false
    }

    private fun logout() = launch{
        utils.nukeSharedPrefs()
        val intent = Intent(context, AuthActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun getImage() {
        val intent = Intent()
        intent.type = "image/jpeg"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMG_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMG_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            if (data.data != null) {
                uri = data.data!!
                uriString = uri.toString()
                val myFile = File(uriString!!)
                imageName = null

                if (uriString!!.startsWith("content://")) {
                    var cursor: Cursor? = null
                    try {
                        cursor = activity?.contentResolver?.query(uri, null, null, null, null)
                        if (cursor != null && cursor.moveToFirst()) {
                            imageName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        }
                    } finally {
                        cursor?.close();
                    }
                }
                else if (uriString!!.startsWith("file://")) {
                    imageName = myFile.name;
                }
                Glide.with(this)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(proflie_fragment_user_picture_image_view)
            }
        }
        else {
            if(imageName.isNullOrEmpty())
                Toast.makeText(requireContext(), "No file chosen", Toast.LENGTH_LONG).show()
        }
    }



}
