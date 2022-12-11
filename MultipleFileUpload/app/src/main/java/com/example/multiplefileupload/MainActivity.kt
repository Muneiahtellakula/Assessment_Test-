package com.example.multiplefileupload

import android.Manifest
//import com.example.multiplefileupload.FileUploader.uploadFiles
import androidx.appcompat.app.AppCompatActivity
import android.app.ProgressDialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.provider.MediaStore
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.View
import com.example.multiplefileupload.FileUploader.FileUploaderCallback
import android.widget.Toast
import java.io.File
import java.lang.Exception
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    var files = ArrayList<String>()
    private var pDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pDialog = ProgressDialog(this)
        findViewById<View>(R.id.btnSelectFiles).setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    2
                )
            } else {
                val intent = Intent()
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && null != data) {
            if (data.clipData != null) {
                val count =
                    data.clipData!!.itemCount //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    getImageFilePath(imageUri)
                }
            }
            if (files.size > 0) {
                uploadFiles()
            }
        }
    }

    fun getImageFilePath(uri: Uri) {
        val file = File(uri.path)
        val filePath = file.path.split(":").toTypedArray()
        val image_id = filePath[filePath.size - 1]
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            MediaStore.Images.Media._ID + " = ? ",
            arrayOf(image_id),
            null
        )
        if (cursor != null) {
            cursor.moveToFirst()
            @SuppressLint("Range") val imagePath =
                cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            files.add(imagePath)
            cursor.close()
        }
    }

    fun uploadFiles() {
        val filesToUpload = arrayOfNulls<File>(files.size)
        for (i in files.indices) {
            filesToUpload[i] = File(files[i])
        }
        showProgress("Uploading media ...")
        val fileUploader = FileUploader()
        fileUploader.uploadFiles("/task/", "file", filesToUpload, object : FileUploaderCallback {
            override fun onError() {
                hideProgress()
            }

            override fun onFinish(responses: Array<String?>?) {
                hideProgress()
                for (i in responses!!.indices) {
                    val str = responses[i]
                    Log.e("RESPONSE $i", responses[i]!!)
                }
                Toast.makeText(applicationContext, "success", Toast.LENGTH_SHORT).show()
            }

            override fun onProgressUpdate(currentpercent: Int, totalpercent: Int, filenumber: Int) {
                updateProgress(totalpercent, "Uploading file $filenumber", "")
                Log.e("Progress Status", "$currentpercent $totalpercent $filenumber")
            }
        })
    }

    fun updateProgress(`val`: Int, title: String?, msg: String?) {
        pDialog!!.setTitle(title)
        pDialog!!.setMessage(msg)
        pDialog!!.progress = `val`
    }

    fun showProgress(str: String?) {
        try {
            pDialog!!.setCancelable(false)
            pDialog!!.setTitle("Please wait")
            pDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            pDialog!!.max = 100 // Progress Dialog Max Value
            pDialog!!.setMessage(str)
            if (pDialog!!.isShowing) pDialog!!.dismiss()
            pDialog!!.show()
        } catch (e: Exception) {
        }
    }

    fun hideProgress() {
        try {
            if (pDialog!!.isShowing) pDialog!!.dismiss()
        } catch (e: Exception) {
        }
    }
}