# KLog
基于xlog修改的本地日志库

默认无加密，封装了日志相关的工具方法，方便快捷。兼容性好，不会像xlog那样有些手机打开就崩，谁用谁知道。

## 引入

```gradle
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
    implementation 'com.github.EspoirX:KLog:vX.X.X'
}
```
[![](https://jitpack.io/v/EspoirX/KLog.svg)](https://jitpack.io/#EspoirX/KLog)

## 初始化

调用 LogService 相关的类进行初始化：

``` kotlin
val logService = LogService()

fun init(application: Application) {
    RuntimeInfo.sAppContext = application
    logService.config()
        .processTag(BuildConfig.NAME)
        .logPath(getLogPath())
        .apply()
    logService.init()
}

private fun getLogPath(): String {
    return StorageUtils.getCacheDir(RuntimeInfo.sAppContext, "logs")
}
```

processTag 是log 文件的前缀，可以不填， logPath 是 log 文件的本地路径。

## 生成日志压缩包

提供了一个方便的方法生成日志压缩包，可以直接用来上传之类的操作：

```kotlin
fun submitLog() {
    MainScope().launch(Dispatchers.IO) {
        logService.submitLog(listener = object : OnSubmitLogListener {
            override fun onSubmitLog(zipFilePath: String) {
               //zipFilePath 是文件压缩包
            }
        })
    }
}
```

也可以自己操作，具体方法在 LogService 类中

## log 工具类
工具类是 KLog，使用方法和平常的 Log 使用无区别:

```kotlin
KLog.i("TAG", "我是log。。。")
```

 下面是日志文件的一些截图，乱码是因为我编码有问题，遇到这种情况可以用vs code 这种编辑器查看，一样的

 <a href="https://sm.ms/image/1esoSCLiQBt5cTX" target="_blank"><img src="https://s2.loli.net/2023/09/14/1esoSCLiQBt5cTX.png" width="300"></a>
<a href="https://sm.ms/image/7oCuBU4zMViyIJx" target="_blank"><img src="https://s2.loli.net/2023/09/14/7oCuBU4zMViyIJx.png" width="300"></a>
<a href="https://sm.ms/image/6H5nihYArwsoJuk" target="_blank"><img src="https://s2.loli.net/2023/09/14/6H5nihYArwsoJuk.png" width="200"></a>
