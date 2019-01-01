package com.esperantajvortaroj.app

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.esperantajvortaroj.app.db.AppDatabase
import com.esperantajvortaroj.app.db.SearchHistory
import com.esperantajvortaroj.app.db.SearchHistoryDao

class SearchHistoryViewModel(application: Application) : AndroidViewModel(application) {
    val searchHistoryDao: SearchHistoryDao = AppDatabase.getInstance(application).searchHistoryDao()
    val allHistory: LiveData<List<SearchHistory>>

    init {
        allHistory = searchHistoryDao.getAll()
    }

    fun insert(searchHistory: SearchHistory) {
        searchHistoryDao.insertOne(searchHistory)
    }

    fun deleteOlderEntries() {
        searchHistoryDao.deleteOldestEntries()
    }

    fun updateLast(word: String) {
        searchHistoryDao.updateLast(word)
    }

    fun deleteOne(entry: SearchHistory) {
        searchHistoryDao.deleteOne(entry)
    }
}