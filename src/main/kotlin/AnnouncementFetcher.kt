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
    private var cachedEtag: String? = null
    private var cachedLastModified: String? = null

    // 定义 JSON 实例
    private val json = Json { ignoreUnknownKeys = true } // 忽略未知字段，防止字段缺失时报错

    // 检查内容是否更新
    private suspend fun isContentUpdated(): Boolean {
        return withContext(Dispatchers.IO) {
            val connection = URL(API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD" // 只请求响应头
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            try {
                connection.connect()
                val newEtag = connection.getHeaderField("ETag")
                val newLastModified = connection.getHeaderField("Last-Modified")

                // 判断是否有新内容
                val isUpdated = (newEtag != null && newEtag != cachedEtag) ||
                    (newLastModified != null && newLastModified != cachedLastModified)

                // 更新标识符
                if (isUpdated) {
                    cachedEtag = newEtag
                    cachedLastModified = newLastModified
                }

                isUpdated
            } finally {
                connection.disconnect()
            }
        }
    }

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
        if (cachedJson == null|| isContentUpdated()) {
            cachedJson = downloadJson()
        }

        // 使用 Json 实例解析 JSON 数据
        val response = json.decodeFromString<AnnouncementsResponse>(cachedJson!!)
        return response.data.firstOrNull { it.id == id }
    }
}