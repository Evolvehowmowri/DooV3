package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import com.example.data.Category

data class CategoryInfo(
    val name: String,
    val color: Color,
    val icon: ImageVector
)

object CategoryRegistry {
    val staticCategories = listOf(
        CategoryInfo("Health", CategoryHealth, Icons.Default.Favorite),
        CategoryInfo("Work", CategoryWork, Icons.Default.Work),
        CategoryInfo("Personal", CategoryPersonal, Icons.Default.Home),
        CategoryInfo("Mind", CategoryMind, Icons.Default.Psychology),
        CategoryInfo("Creative", CategoryCreative, Icons.Default.Brush),
        CategoryInfo("General", CategoryGeneral, Icons.Default.Star)
    )

    var dynamicCategoriesList: List<Category> = emptyList()

    val categories: List<CategoryInfo>
        get() = if (dynamicCategoriesList.isEmpty()) {
            staticCategories
        } else {
            dynamicCategoriesList.map { it.toCategoryInfo() }
        }

    fun getCategoryInfo(name: String): CategoryInfo {
        val primaryName = name.split(",").firstOrNull()?.trim() ?: "General"
        return categories.firstOrNull { it.name.equals(primaryName, ignoreCase = true) }
            ?: CategoryInfo("General", CategoryGeneral, Icons.Default.Star)
    }
}

fun Category.toCategoryInfo(): CategoryInfo {
    val color = try {
        Color(android.graphics.Color.parseColor(this.colorHex))
    } catch (e: Exception) {
        CategoryGeneral
    }
    val icon = getIconByName(this.iconName)
    return CategoryInfo(this.name, color, icon)
}

fun getIconByName(name: String): ImageVector {
    return when (name) {
        "Favorite" -> Icons.Default.Favorite
        "Work" -> Icons.Default.Work
        "Home" -> Icons.Default.Home
        "Psychology" -> Icons.Default.Psychology
        "Brush" -> Icons.Default.Brush
        "School" -> Icons.Default.School
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "MusicNote" -> Icons.Default.MusicNote
        "Flight" -> Icons.Default.Flight
        "Pets" -> Icons.Default.Pets
        "Restaurant" -> Icons.Default.Restaurant
        "DirectionsRun" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Spa" -> Icons.Default.Spa
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "AttachMoney" -> Icons.Default.AttachMoney
        "Code" -> Icons.Default.Code
        "People" -> Icons.Default.People
        "LocalCafe" -> Icons.Default.LocalCafe
        "Book" -> Icons.Default.Book
        "WaterDrop" -> Icons.Default.WaterDrop
        "Lightbulb" -> Icons.Default.Lightbulb
        "Event" -> Icons.Default.Event
        "CheckCircle" -> Icons.Default.CheckCircle
        "Build" -> Icons.Default.Build
        "PhoneAndroid" -> Icons.Default.PhoneAndroid
        "BarChart" -> Icons.Default.BarChart
        else -> Icons.Default.Star
    }
}
