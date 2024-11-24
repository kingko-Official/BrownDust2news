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
    suspend fun CommandSenderOnMessage<*>.handle(id: Int) {
        sendMessage("正在查询公告 ID 为 $id 的内容，请稍候...")

        val announcement = runBlocking { AnnouncementFetcher.fetchAnnouncementById(id) }

        if (announcement != null) {
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
        } else {
            sendMessage("未找到 ID 为 $id 的公告。请确认输入是否正确。")
        }
    }
}
