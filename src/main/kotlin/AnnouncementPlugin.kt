
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription

import net.mamoe.mirai.utils.info


object AnnouncementPlugin : JavaPlugin(
    JvmPluginDescription(
        id = "com.kingko.announcementplugin",
        name = "BrownDust2 Announcement Plugin",
        version = "0.1.1"
    ) {
        author("KingKo")
        info("查询棕色尘埃2的公告内容")
    }
) {
    override fun onEnable() {
        logger.info { "公告查询插件已启用！" }

        // 注册命令
        AnnouncementCommand.register()
    }
}
