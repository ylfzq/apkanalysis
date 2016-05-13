# apkanalysis
An apk analysis tool, written in Java. No external dependencies. Using it as a library or a command line tool. 

这个工具前后用了好多天的时间查阅资料并不断修改才完成。本工具可以用于读取apk包的大量信息，无其他依赖。可以直接通过命令行运行，也可以当作架包使用。  
命令行方式使用举例：
获取AndroidManifest.xml文件中定义的versionCode：
```
java -jar ApkAnalysis.jar “apk路径” -versionCode
```
获取apk证书详情：
```
java -jar ApkAnalysis.jar “apk路径” -certs
```
获取apk证书中的第一条的详情：
```
java -jar ApkAnalysis.jar “apk路径” -certs 0
```
获取证书摘要（`百度`、`高德地图`等API中需要的那个SHA1）：
```
java -jar ApkAnalysis.jar “apk路径” -certs 0 SHA1
```
获取apk发布者信息：
```
java -jar ApkAnalysis.jar “apk路径” -certs 0 issuer
```
获取apk声明的权限：
```
java -jar ApkAnalysis.jar “apk路径” -permissions
```
当作为架包使用时，通过
```
ApkAnalysis apkAnalysis = ApkAnalysis.getApkReader(apkFilePath);
```
获取到ApkAnalysis的实例，然后就调用对应方法读取即可。相信都会使用自动补全等功能吧？那个会告诉你有哪些可用的方法，这里不例举了。

输入
`java -jar ApkAnalysis.jar -help`会给出如下提示，请慢慢研究。如果好用，请star表示支持。
<pre>
Apk分析工具 v1.0.7	编译时JDK版本：1.6.0_33	当前JRE版本：1.6.0_33
作者：周骞	发布日期：2015-01-08
---------------------------------------------------
ApkAnalysis <filePath> [-versionCode] [-versionName] [-packageName]...
可用的选项：
	-versionCode	版本号
	-versionName	版本名称，如1.0.3
	-packageName	Apk包名
	-certs [index] [MD5|SHA1|issuer|subject|validity]	获取证书的信息
	-verify	校验apk内文件的签名，并列出未通过校验的文件
	-permissions	获取apk所需的权限
	-features	获取apk所需的特性
	-activities [detail]	获取apk所含的Activity
	-services [detail]	获取apk所含的Service
	-receivers [detail]	获取apk所含的静态Receiver
	-content <path> [name]	获取AndroidManifest.xml中的内容
	-extract <pathInApk> <savePath>	抽取apk中的文件
	-h[elp]	显示此帮助信息
</pre>
如在程序中引用本包，方法如下：
```
	ApkAnalysis apkAnalysis = ApkAnalysis.getApkReader(apkFilePath);
```
需要判断apkAnalysis是否为null，为null表示读取失败，不为null时即可调用getXX()获取数据