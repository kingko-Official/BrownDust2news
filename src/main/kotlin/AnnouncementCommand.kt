import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import org.jsoup.Jsoup // 用于解析 HTML 内容
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
    suspend fun CommandSender.handle(id: Int) {
        sendMessage("正在查询公告 ID 为 $id 的内容，请稍候...")

        val announcement = runBlocking { AnnouncementFetcher.fetchAnnouncementById(id) }

        if (announcement != null) {
            val title = announcement.attributes.subject
            val rawContent = announcement.attributes.NewContent
            val publishDate = announcement.attributes.publishedAt

            // 移除 HTML 标签并格式化内容
            val plainContent = Jsoup.parse(rawContent ?: "无内容").text()

            // 转换日期格式
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
            val formattedDate = formatter.format(Instant.parse(publishDate))

            // 发送格式化消息
            sendMessage(
                """
                标题: $title
                内容: $plainContent
                发布日期: $formattedDate
                """.trimIndent()
            )
        } else {
            sendMessage("未找到 ID 为 $id 的公告。请确认输入是否正确。")
        }
    }
}