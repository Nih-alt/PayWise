package com.nihal.paywise.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryVisual(
    val icon: ImageVector,
    val color: Color,
    val containerColor: Color
)

object CategoryVisuals {

    // Define a palette of soft colors that work well in both light and dark themes
    private val Red = Color(0xFFE57373)
    private val Pink = Color(0xFFF06292)
    private val Purple = Color(0xFFBA68C8)
    private val DeepPurple = Color(0xFF9575CD)
    private val Indigo = Color(0xFF7986CB)
    private val Blue = Color(0xFF64B5F6)
    private val Cyan = Color(0xFF4DD0E1)
    private val Teal = Color(0xFF4DB6AC)
    private val Green = Color(0xFF81C784)
    private val Lime = Color(0xFFDCE775)
    private val Yellow = Color(0xFFFFD54F)
    private val Orange = Color(0xFFFFB74D)
    private val DeepOrange = Color(0xFFFF8A65)
    private val Brown = Color(0xFFA1887F)
    private val BlueGrey = Color(0xFF90A4AE)

    fun getVisual(categoryName: String): CategoryVisual {
        val normalizedName = categoryName.trim().lowercase()

        val (icon, baseColor) = when {
            // Food & Dining
            normalizedName.contains("food") || normalizedName.contains("eat") -> Icons.Default.Fastfood to Orange
            normalizedName.contains("dining") || normalizedName.contains("restaurant") || normalizedName.contains("dinner") || normalizedName.contains("lunch") -> Icons.Default.Restaurant to Orange
            normalizedName.contains("cafe") || normalizedName.contains("coffee") -> Icons.Default.LocalCafe to Brown
            normalizedName.contains("grocery") || normalizedName.contains("groceries") || normalizedName.contains("supermarket") -> Icons.Default.LocalGroceryStore to Green

            // Transport
            normalizedName.contains("transport") || normalizedName.contains("commute") || normalizedName.contains("bus") || normalizedName.contains("metro") -> Icons.Default.Commute to Blue
            normalizedName.contains("fuel") || normalizedName.contains("gas") || normalizedName.contains("petrol") -> Icons.Default.DirectionsCar to Blue
            normalizedName.contains("taxi") || normalizedName.contains("cab") || normalizedName.contains("uber") || normalizedName.contains("ola") -> Icons.Default.DirectionsCar to Yellow
            normalizedName.contains("train") || normalizedName.contains("rail") -> Icons.Default.Train to Blue
            normalizedName.contains("flight") || normalizedName.contains("travel") || normalizedName.contains("trip") -> Icons.Default.Flight to Cyan

            // Shopping
            normalizedName.contains("shopping") || normalizedName.contains("buy") -> Icons.Default.ShoppingBag to Pink
            normalizedName.contains("clothes") || normalizedName.contains("clothing") || normalizedName.contains("fashion") -> Icons.Default.Checkroom to Pink
            normalizedName.contains("electronics") || normalizedName.contains("gadget") -> Icons.Default.PhoneAndroid to DeepPurple
            normalizedName.contains("cart") -> Icons.Default.ShoppingCart to Pink

            // Bills & Utilities
            normalizedName.contains("bill") || normalizedName.contains("utility") -> Icons.Default.Receipt to Red
            normalizedName.contains("rent") || normalizedName.contains("house") || normalizedName.contains("home") -> Icons.Default.Home to Indigo
            normalizedName.contains("electricity") || normalizedName.contains("power") -> Icons.Default.Category to Yellow // Flash icon ideally
            normalizedName.contains("internet") || normalizedName.contains("wifi") || normalizedName.contains("broadband") -> Icons.Default.Wifi to Cyan
            normalizedName.contains("phone") || normalizedName.contains("mobile") || normalizedName.contains("recharge") -> Icons.Default.PhoneAndroid to Blue
            normalizedName.contains("tax") -> Icons.Default.ReceiptLong to Red

            // Health
            normalizedName.contains("health") || normalizedName.contains("medical") || normalizedName.contains("doctor") || normalizedName.contains("pharmacy") -> Icons.Default.LocalHospital to Red
            normalizedName.contains("insurance") -> Icons.Default.HealthAndSafety to Green

            // Entertainment
            normalizedName.contains("movie") || normalizedName.contains("cinema") || normalizedName.contains("film") -> Icons.Default.Movie to DeepPurple
            normalizedName.contains("game") || normalizedName.contains("gaming") -> Icons.Default.SportsEsports to DeepPurple
            normalizedName.contains("subscription") || normalizedName.contains("netflix") || normalizedName.contains("prime") -> Icons.Default.Movie to Red

            // Education
            normalizedName.contains("education") || normalizedName.contains("school") || normalizedName.contains("college") || normalizedName.contains("course") || normalizedName.contains("book") -> Icons.Default.School to BlueGrey

            // Income / Financial
            normalizedName.contains("salary") || normalizedName.contains("income") || normalizedName.contains("wage") -> Icons.Default.AttachMoney to Green
            normalizedName.contains("investment") || normalizedName.contains("stock") || normalizedName.contains("mutual fund") -> Icons.Default.AccountBalance to Green
            normalizedName.contains("bank") -> Icons.Default.AccountBalance to BlueGrey

            // Other
            normalizedName.contains("pet") || normalizedName.contains("dog") || normalizedName.contains("cat") -> Icons.Default.Pets to Brown
            normalizedName.contains("work") || normalizedName.contains("office") -> Icons.Default.Work to BlueGrey

            else -> Icons.Default.Category to getColorHash(categoryName)
        }

        return CategoryVisual(
            icon = icon,
            color = baseColor,
            containerColor = baseColor.copy(alpha = 0.15f)
        )
    }

    private fun getColorHash(name: String): Color {
        val colors = listOf(
            Red, Pink, Purple, DeepPurple, Indigo, Blue, Cyan, Teal, Green, Lime, Yellow, Orange, DeepOrange, Brown, BlueGrey
        )
        val index = kotlin.math.abs(name.hashCode()) % colors.size
        return colors[index]
    }
}
