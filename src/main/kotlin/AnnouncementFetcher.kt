import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object AnnouncementFetcher {
    private const val API_URL = "https://www.browndust2.com/api/newsData_tw.json"

    // 缓存 JSON 数据
    private var cachedJson: String? = null

    // 定义 JSON 实例
    private val json = Json { ignoreUnknownKeys = true } // 忽略未知字段，防止字段缺失时报错

    // 下载 JSON 文件
    suspend fun downloadJson(): String {
        return withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val randomSuffix = Random().nextInt(900) + 100 // 随机三位数
            val requestUrl = "$API_URL?v=$timestamp$randomSuffix"

            val connection = URL(requestUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000 // 10 秒超时
            connection.readTimeout = 10_000

            try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }

    // 根据 ID 查询公告
    suspend fun fetchAnnouncementById(id: Int): Announcement? {
        if (cachedJson == null) {
            cachedJson = downloadJson()
        }

        // 使用 Json 实例解析 JSON 数据
        val response = json.decodeFromString<AnnouncementsResponse>(cachedJson!!)
        return response.data.firstOrNull { it.id == id }
    }
}