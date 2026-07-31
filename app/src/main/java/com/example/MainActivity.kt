package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.local.FirewallDatabase
import com.example.data.repository.FirewallRepositoryImpl
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FirewallViewModel

class MainActivity : ComponentActivity() {

  private val repository by lazy {
    val db = FirewallDatabase.getDatabase(applicationContext)
    FirewallRepositoryImpl(
      applicationContext,
      db.ruleFilterDao(),
      db.appRuleDao(),
      db.networkLogDao()
    )
  }

  private val viewModel: FirewallViewModel by viewModels {
    FirewallViewModel.Factory(repository, applicationContext)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
