import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val id: Int,
    val attributes: Attributes
)

@Serializable
data class Attributes(
    val subject: String,
    val createdAt: String,
    val publishedAt: String,
    val NewContent: String? // 公告内容，可为空
)

@Serializable
data class AnnouncementsResponse(
    val data: List<Announcement> // 公告列表
)