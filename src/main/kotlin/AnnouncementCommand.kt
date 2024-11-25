import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import org.jsoup.Jsoup // 用于解析 HTML 内容
import java.io.InputStream
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AnnouncementCommand : SimpleCommand(
    AnnouncementPlugin,
    primaryName = "announcement", // 主命令
    secondaryNames = arrayOf("查询公告", "公告查询"),
    description = "查询指定公告 ID 的内容"
) {
    @Handler
    suspend fun CommandSenderOnMessage<*>.handle(id: Int, type: String? = null) {
        sendMessage("正在查询公告 ID 为 $id 的内容，请稍候...")

        // 查询公告
        val announcement = runBlocking { AnnouncementFetcher.fetchAnnouncementById(id) }

        if (announcement != null) {
            if (type == "tu") {
                // 如果用户请求图片，只发送图片
                sendImages(announcement)
            } else {
                // 默认处理：发送标题、内容和图片
                sendTextAndImages(announcement)
            }
        } else {
            sendMessage("未找到 ID 为 $id 的公告。请确认输入是否正确。")
        }
    }

    // 发送公告的文本和图片
    private suspend fun CommandSenderOnMessage<*>.sendTextAndImages(announcement: Announcement) {
        val title = announcement.attributes.subject
        val rawContent = announcement.attributes.NewContent
        val publishDate = announcement.attributes.publishedAt

        // 移除 HTML 标签但保留换行
        val document = Jsoup.parse(rawContent ?: "无内容")
        val paragraphs = document.select("p")
        val plainContent = paragraphs.joinToString("\n\n") { it.wholeText() }

        // 提取图片 URL
        val imageUrls = document.select("img").map { it.attr("src") }

        // 转换日期格式
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        val formattedDate = formatter.format(Instant.parse(publishDate))

        // 构建消息链
        val messageChain = buildMessageChain {
            +("标题: $title\n")
            +("内容: \n$plainContent\n")
            +("发布日期: $formattedDate\n")

            // 添加图片
            for (imageUrl in imageUrls) {
                try {
                    val inputStream: InputStream = URL(imageUrl).openStream()
                    val externalResource = inputStream.toExternalResource()
                    val image = subject?.uploadImage(externalResource)
                    +image!!
                    externalResource.close()
                } catch (e: Exception) {
                    +("无法加载图片: $imageUrl")
                }
            }
        }

        // 发送消息
        sendMessage(messageChain)
    }

    // 只发送公告中的图片
    private suspend fun CommandSenderOnMessage<*>.sendImages(announcement: Announcement) {
        val rawContent = announcement.attributes.NewContent
        // 使用 Jsoup 解析 HTML 内容，提取图片 URL
        val document = Jsoup.parse(rawContent ?: "无内容")
        val imageUrls = document.select("img").map { it.attr("src") }

        // 发送图片
        val messageChain = buildMessageChain {
            for (imageUrl in imageUrls) {
                try {
                    val inputStream: InputStream = URL(imageUrl).openStream()
                    val externalResource = inputStream.toExternalResource()
                    val image = subject?.uploadImage(externalResource)
                    +image!!
                    externalResource.close()
                } catch (e: Exception) {
                    +("无法加载图片: $imageUrl")
                }
            }
        }

        sendMessage(messageChain)
    }
}
