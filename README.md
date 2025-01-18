# Translator - 翻译器

---

此模组作为游戏内的翻译工具，旨在翻译游戏中各种元素中出现的文字，同时不会更改世界数据。它可以翻译聊天栏、计分板、BOSS 血条、标题、工具提示框等位置的文字。

## 主要特性
+ 非侵入式翻译：&nbsp;确保所有功能都不会修改游戏世界的数据。
+ 快捷键：
    - 按&nbsp;U&nbsp;键打开翻译选项；
    - 按&nbsp;O&nbsp;键启动屏幕识别。

+ 游戏内命令：

  - `/transconfig`：翻译器的配置命令，包含以下子命令：
      + `translator <翻译器>` [参数]：选择翻译服务。某些服务可能需要初次使用时进行配置，不同服务所需的参数各不相同；
      + `language <源语言> <目标语言>`：设置翻译的源语言和目标语言；
      + `clearcache`：清理翻译缓存和失败缓存。

  - `/translate <文本>`：翻译提供的文本的命令；
  - `/translate-re <文本>`：反向翻译命令，即从目标语言翻译回源语言。

## 支持的翻译服务
模组内置了百度翻译和有道翻译，用户需要自行从相应平台获取 API 密钥后，使用命令配置对应翻译器以使用这些服务。

### 效果图
*翻译选项界面*  
![翻译器 (Translator)-第1张图片](https://i.mcmod.cn/editor/upload/20241204/1733292192_72788_WhlH.webp)  
*原文*  
![翻译器 (Translator)-第2张图片](https://i.mcmod.cn/editor/upload/20241204/1733292211_72788_yuHd.webp)  
*翻译后*  
![翻译器 (Translator)-第3张图片](https://i.mcmod.cn/editor/upload/20241204/1733292244_72788_MeMg.webp)  
*屏幕识别后*  
![翻译器 (Translator)-第4张图片](https://i.mcmod.cn/editor/upload/20241204/1733292783_72788_WYVs.webp)  
*物品提示框*  
![翻译器 (Translator)-第5张图片](https://i.mcmod.cn/editor/upload/20241204/1733292275_72788_pRqL.webp)  
*书本*  
![翻译器 (Translator)-第6张图片](https://i.mcmod.cn/editor/upload/20241204/1733292327_72788_LvtC.webp)

---

## 特别提醒
界面翻译选项可用于翻译其他模组的屏幕界面，但是可能会引发崩溃。
