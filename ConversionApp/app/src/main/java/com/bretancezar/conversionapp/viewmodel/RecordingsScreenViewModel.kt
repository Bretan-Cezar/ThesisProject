package com.bretancezar.conversionapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class RecordingsScreenViewModel @Inject constructor(application: Application): ViewModel() {

    private val _applicationContext = application


}