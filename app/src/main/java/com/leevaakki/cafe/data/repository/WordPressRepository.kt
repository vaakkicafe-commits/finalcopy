package com.leevaakki.cafe.data.repository

import android.util.Log
import com.leevaakki.cafe.models.MenuItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordPressRepository @Inject constructor(
    private val client: HttpClient
) {
    suspend fun fetchMenu(baseUrl: String): List<MenuItem> {
        return try {
            // We use category_name=menu_items. Ensure this slug exists in WP Categories.
            val url = "$baseUrl/wp-json/wp/v2/posts?category_name=menu_items&_embed&per_page=100"
            val response: String = client.get(url).body()
            val jsonArray = Json.parseToJsonElement(response).jsonArray
            
            jsonArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val acf = obj["acf"]?.jsonObject
                    val meta = obj["meta"]?.jsonObject
                    
                    // 1. Robust Price Parsing
                    val priceValue = acf?.get("price")?.jsonPrimitive?.doubleOrNull 
                        ?: meta?.get("price")?.jsonPrimitive?.doubleOrNull
                        ?: obj["price"]?.jsonPrimitive?.doubleOrNull // Check top level too
                        ?: 0.0

                    // 2. Robust Image Parsing
                    val featuredMediaUrl = obj["featured_media_url"]?.jsonPrimitive?.contentOrNull ?: ""
                    
                    // Try to get from _embedded if featured_media_url is missing
                    val embeddedMedia = obj["_embedded"]?.jsonObject?.get("wp:featuredmedia")?.jsonArray?.getOrNull(0)?.jsonObject
                    val sourceUrl = embeddedMedia?.get("source_url")?.jsonPrimitive?.contentOrNull ?: ""
                    
                    // Fallback to searching content for first <img> tag if both above fail
                    val contentHtml = obj["content"]?.jsonObject?.get("rendered")?.jsonPrimitive?.content ?: ""
                    val fallbackImg = if (sourceUrl.isEmpty() && featuredMediaUrl.isEmpty()) {
                         val match = Regex("<img [^>]*src=\"([^\"]+)\"").find(contentHtml)
                         match?.groups?.get(1)?.value ?: ""
                    } else ""

                    val imageUrl = featuredMediaUrl.ifEmpty { sourceUrl.ifEmpty { fallbackImg } }

                    MenuItem(
                        id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        name = obj["title"]?.jsonObject?.get("rendered")?.jsonPrimitive?.content ?: "Untitled",
                        description = obj["excerpt"]?.jsonObject?.get("rendered")?.jsonPrimitive?.content ?: "",
                        price = priceValue,
                        category = "WordPress",
                        imageUrl = imageUrl,
                        isAvailable = true
                    )
                } catch (e: Exception) {
                    Log.e("WordPressRepository", "Error parsing individual item: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("WordPressRepository", "Menu Fetch Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchOffers(baseUrl: String): List<String> {
        return try {
            val response: String = client.get("$baseUrl/wp-json/wp/v2/posts?category_name=offers&_embed").body()
            val jsonArray = Json.parseToJsonElement(response).jsonArray
            
            jsonArray.map { element ->
                val obj = element.jsonObject
                val featuredMediaUrl = obj["featured_media_url"]?.jsonPrimitive?.contentOrNull ?: ""
                val embeddedMedia = obj["_embedded"]?.jsonObject?.get("wp:featuredmedia")?.jsonArray?.getOrNull(0)?.jsonObject
                val sourceUrl = embeddedMedia?.get("source_url")?.jsonPrimitive?.contentOrNull ?: ""
                featuredMediaUrl.ifEmpty { sourceUrl }
            }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("WordPressRepository", "Offers Fetch Error: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchSettings(baseUrl: String): Map<String, String> {
        return try {
            val response: String = client.get("$baseUrl/wp-json/wp/v2/posts?slug=app-settings").body()
            val jsonArray = Json.parseToJsonElement(response).jsonArray
            val settingsMap = mutableMapOf<String, String>()
            
            if (jsonArray.isNotEmpty()) {
                val post = jsonArray[0].jsonObject
                val acf = post["acf"]?.jsonObject
                
                // Location and Social Links
                acf?.let {
                    it["swiggy_url"]?.jsonPrimitive?.contentOrNull?.let { url -> settingsMap["swiggy_url"] = url }
                    it["zomato_url"]?.jsonPrimitive?.contentOrNull?.let { url -> settingsMap["zomato_url"] = url }
                    it["maps_url"]?.jsonPrimitive?.contentOrNull?.let { url -> settingsMap["maps_url"] = url }
                    it["phone_number"]?.jsonPrimitive?.contentOrNull?.let { phone -> settingsMap["phone_number"] = phone }
                    it["whatsapp_number"]?.jsonPrimitive?.contentOrNull?.let { wa -> settingsMap["whatsapp_number"] = wa }
                    it["address"]?.jsonPrimitive?.contentOrNull?.let { addr -> settingsMap["address"] = addr }
                }
            }
            settingsMap
        } catch (e: Exception) {
            Log.e("WordPressRepository", "Settings Fetch Error: ${e.message}")
            emptyMap()
        }
    }
}
