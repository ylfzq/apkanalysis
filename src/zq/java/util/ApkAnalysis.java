package zq.java.util;


import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import sun.security.pkcs.PKCS7;
import sun.security.pkcs.ParsingException;


@SuppressWarnings("restriction")
public class ApkAnalysis implements Closeable {


	public static void main(String[] args) {

		SystemArgs sysArgs = new SystemArgs(args);

		String arg0 = sysArgs.next();
		if (null == arg0 || "-help".equals(arg0) || "-h".equals(arg0)) {
			printHelp();
			return;
		} else if ("-env".equals(arg0)) {
			printAllProperty();
			return;
		} else if (!SystemArgs.isExistedFile(arg0)) {
			printErrorMessage("文件不存在：" + arg0);
			return;
		}

		ApkAnalysis apkAnalysis = ApkAnalysis.getApkReader(arg0);
		if (apkAnalysis == null) {
			printErrorMessage("apk文件不合法");
			return;
		} else if (!sysArgs.hasMore()) {
			printErrorMessage(apkAnalysis.toString());
			return;
		}

		while (sysArgs.hasMoreFunctions()) {
			String funArg = sysArgs.nextFunction();
			try {
				if ("-extract".equals(funArg)) {
					long size =
						apkAnalysis.getFileInApk(
							sysArgs.nextParamOrThrow(),
							new FileOutputStream(sysArgs.nextParamOrThrow()));
					if (size > 0)
						System.out.println("抽取成功，文件大小为：" + size);
					else
						System.out.println("抽取失败");
				} else if ("-versionCode".equals(funArg)) {
					System.out.println(apkAnalysis.getVersionCode());
				} else if ("-versionName".equals(funArg)) {
					System.out.println(apkAnalysis.getVersionName());
				} else if ("-packageName".equals(funArg)) {
					System.out.println(apkAnalysis.getPackageName());
				} else if ("-certs".equals(funArg)) {
					Integer index = null;
					String param = sysArgs.nextParam();
					if (SystemArgs.isNumber(param)) {
						index = Integer.parseInt(param);
						param = sysArgs.nextParam();
					}
					System.out.println(apkAnalysis.getCertsHandled(
						apkAnalysis.getDeclaredCertificates(),
						index,
						param));
				} else if ("-verify".equals(funArg)) {
					List<String> list = apkAnalysis.verifyCertificates();
					if (list.size() == 0) {
						System.out.println("apk签名验证通过");
					} else {
						System.out.println("校验签名未通过的文件数：" + list.size());
						System.out.println(list);
					}
				} else if ("-permissions".equals(funArg)) {
					System.out.println(apkAnalysis.getPermissions());
				} else if ("-features".equals(funArg)) {
					System.out.println(apkAnalysis.getFeatures());
				} else if ("-activities".equals(funArg)) {
					System.out.println(apkAnalysis.getActivities("detail".equals(sysArgs.nextParam())));
				} else if ("-services".equals(funArg)) {
					System.out.println(apkAnalysis.getServices("detail".equals(sysArgs.nextParam())));
				} else if ("-receivers".equals(funArg)) {
					System.out.println(apkAnalysis.getReceivers("detail".equals(sysArgs.nextParam())));
				} else if ("-content".equals(funArg)) {
					System.out.println(apkAnalysis.getContent(
						sysArgs.nextParamOrThrow().split("/"),
						sysArgs.nextParam()));
				} else {
					printErrorMessage("无效的命令：" + funArg);
					break;
				}
			} catch (NoSuchElementException e) {
				printErrorMessage("命令缺少必要的参数");
			} catch (Exception e) {
				printErrorMessage(e.toString());
			}
		}
		apkAnalysis.close();
	}


	private static String getDuration(Date date1, Date date2) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		return String.format("[%s, %s]", sdf.format(date1), sdf.format(date2));
	}


	private static void printHelp() {

		System.out.print("Apk分析工具 v1.0.7\t编译时JDK版本：1.6.0_33\t");
		System.out.println("当前JRE版本：" + System.getProperty("java.version"));
		System.out.println("作者：周骞\t发布日期：2015-01-08");
		System.out.println("---------------------------------------------------");
		System.out.println("ApkAnalysis <filePath> [-versionCode] [-versionName] [-packageName]...");
		System.out.println("可用的选项：");
		System.out.println("\t-versionCode\t版本号");
		System.out.println("\t-versionName\t版本名称，如1.0.3");
		System.out.println("\t-packageName\tApk包名");
		System.out.println("\t-certs [index] [MD5|SHA1|issuer|subject|validity]\t获取证书的信息");
		System.out.println("\t-verify\t校验apk内文件的签名，并列出未通过校验的文件");
		System.out.println("\t-permissions\t获取apk所需的权限");
		System.out.println("\t-features\t获取apk所需的特性");
		System.out.println("\t-activities [detail]\t获取apk所含的Activity");
		System.out.println("\t-services [detail]\t获取apk所含的Service");
		System.out.println("\t-receivers [detail]\t获取apk所含的静态Receiver");
		System.out.println("\t-content <path> [name]\t获取AndroidManifest.xml中的内容");
		System.out.println("\t-extract <pathInApk> <savePath>\t抽取apk中的文件");
		System.out.println("\t-h[elp]\t显示此帮助信息");
		System.out.println("---------------------------------------------------");
		System.out.println("如在程序中引用本包，方法如下：");
		System.out.println("\tApkAnalysis apkAnalysis = ApkAnalysis.getApkReader(apkFilePath);");
		System.out.println("需要判断apkAnalysis是否为null，为null表示读取失败，不为null时即可调用getXX()获取数据");
	}


	public static void printAllProperty() {

		final String[] keys =
			new String[] {
				"java.version^Java 运行时环境版本", "java.vendor^Java 运行时环境供应商",
				"java.vendor.url^Java 供应商的 URL", "java.home^Java 安装目录",
				"java.vm.specification.version^Java 虚拟机规范版本",
				"java.vm.specification.vendor^Java 虚拟机规范供应商",
				"java.vm.specification.name^Java 虚拟机规范名称",
				"java.vm.version^Java 虚拟机实现版本", "java.vm.vendor^Java 虚拟机实现供应商",
				"java.vm.name^Java 虚拟机实现名称",
				"java.specification.version^Java 运行时环境规范版本",
				"java.specification.vendor^Java 运行时环境规范供应商",
				"java.specification.name^Java 运行时环境规范名称",
				"java.class.version^Java 类格式版本号f", "java.class.path^Java 类路径",
				"java.library.path^加载库时搜索的路径列表", "java.io.tmpdir^默认的临时文件路径",
				"java.compiler^要使用的 JIT 编译器的名称", "java.ext.dirs^一个或多个扩展目录的路径",
				"os.name^操作系统的名称", "os.arch^操作系统的架构", "os.version^操作系统的版本",
				"file.separator^文件分隔符", "path.separator^路径分隔符",
				"line.separator^行分隔符", "user.name^用户的账户名称", "user.home^用户的主目录",
				"user.dir^用户的当前工作目录"
			};

		for (String key : keys) {
			String[] tmp = key.split("\\^");
			System.out.println(String.format(
				"%s\t%s\t%s",
				tmp[0],
				tmp[1],
				System.getProperty(tmp[0])));
		}
	}


	private static void printErrorMessage(String errorMsg) {

		System.out.println(errorMsg);
		System.out.println("--如需获取帮助，请输入-help或-h");
	}


	public static ApkAnalysis getApkReader(String apkFile) {

		try {
			return new ApkAnalysis(new JarFile(new File(apkFile)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static final String COMMON_ICON_PATH = "res/drawable-hdpi/icon.png";

	private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
	private static final String CERT_FILE_NAME = "META-INF/CERT.RSA";
	private static final String INVALID_APK = "this apk is invalid";

	private JarFile mJarFile;
	private AXmlNode mManifestData = null;


	private ApkAnalysis(JarFile apkFile) throws IOException {

		mJarFile = apkFile;
		JarEntry jarEntry = mJarFile.getJarEntry(MANIFEST_FILE_NAME);
		if (jarEntry == null)
			throw new IOException(INVALID_APK);

		mManifestData =
			AXmlNode.createNodeTree(mJarFile.getInputStream(jarEntry));
		if (mManifestData == null)
			throw new IOException(INVALID_APK);
	}


	public String getPackageName() {

		return mManifestData.getAttribute(new String[] {
			"manifest"
		}, "package");
	}


	public String getVersionCode() {

		return mManifestData.getAttribute(new String[] {
			"manifest"
		}, "versionCode");
	}


	public String getVersionName() {

		return mManifestData.getAttribute(new String[] {
			"manifest",
		}, "versionName");
	}


	public List<String> verifyCertificates() {

		List<String> list = new LinkedList<String>();
		X509Certificate[] declaredCerts = getDeclaredCertificates();
		try {
			Enumeration<JarEntry> entries = mJarFile.entries();
			CHECKED:
			while (entries.hasMoreElements()) {
				JarEntry jarEntry = entries.nextElement();
				if (!jarEntry.getName().startsWith("META-INF/")) {
					Certificate[] certs = getCertificatesFromJarEntry(jarEntry);
					if (certs != null) {
						for (Certificate cert : certs) {
							for (X509Certificate x509 : declaredCerts) {
								if (x509.getPublicKey().equals(
									cert.getPublicKey()))
									continue CHECKED;
							}
						}
						list.add(jarEntry.getName());
					} else {
						list.add("*#NotSigned#*" + jarEntry.getName());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}


	public Object getCertsHandled(X509Certificate[] certs, Integer index,
		String param) throws NoSuchAlgorithmException,
		CertificateEncodingException {

		if (index != null) {
			if (param == null)
				return certs[index];
			else if (param.matches("MD5|SHA1")) {
				MessageDigest messageDigest = null;
				messageDigest = MessageDigest.getInstance(param);
				messageDigest.update(certs[index].getEncoded());
				return toHexString(messageDigest.digest());
			} else if ("issuer".equals(param))
				return certs[index].getIssuerDN();
			else if ("subject".equals(param))
				return certs[index].getSubjectDN();
			else if ("validity".equals(param))
				return getDuration(
					certs[index].getNotBefore(),
					certs[index].getNotAfter());
		} else {
			List<String> list = new ArrayList<String>(certs.length);
			if (param == null)
				return Arrays.toString(certs);
			else if (param.toUpperCase().matches("MD5|SHA1")) {
				MessageDigest messageDigest = null;
				messageDigest = MessageDigest.getInstance(param);
				for (X509Certificate cert : certs) {
					messageDigest.reset();
					messageDigest.update(cert.getEncoded());
					list.add(toHexString(messageDigest.digest()));
				}
				return list;
			} else if ("issuer".equals(param)) {
				for (X509Certificate cert : certs) {
					list.add(cert.getIssuerDN().toString());
				}
				return list;
			} else if ("subject".equals(param)) {
				for (X509Certificate cert : certs) {
					list.add(cert.getSubjectDN().toString());
				}
				return list;
			} else if ("validity".equals(param)) {
				for (X509Certificate cert : certs) {
					list.add(getDuration(
						cert.getNotBefore(),
						cert.getNotAfter()));
				}
				return list;
			}
		}
		return "未知的参数：" + param;
	}


	public X509Certificate[] getDeclaredCertificates() {

		JarEntry je = mJarFile.getJarEntry(CERT_FILE_NAME);
		if (je != null) {
			try {
				PKCS7 pkcs7 = new PKCS7(mJarFile.getInputStream(je));
				return pkcs7.getCertificates();
			} catch (ParsingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new X509Certificate[0];
	}


	public Certificate[] getCertificatesFromJarEntry(String fileName) {

		JarEntry je = mJarFile.getJarEntry(fileName);
		if (je == null)
			return null;
		return getCertificatesFromJarEntry(je);
	}


	private Certificate[] getCertificatesFromJarEntry(JarEntry je) {

		try {
			byte[] readBuffer = new byte[8192];
			InputStream is = mJarFile.getInputStream(je);
			while (is.read(readBuffer, 0, readBuffer.length) != -1)
				;
			is.close();
			return je.getCertificates();
		} catch (IOException e) {
			return null;
		}
	}


	public List<String> getContent(String[] path, String name) {

		if (name == null)
			return mManifestData.getAXmlNodesDetail(path);
		else
			return mManifestData.getAttributes(path, name);
	}


	public List<String> getPermissions() {

		return mManifestData.getAttributes(new String[] {
			"manifest", "uses-permission"
		}, "name");
	}


	public List<String> getFeatures() {

		return mManifestData.getAttributes(new String[] {
			"manifest", "uses-feature"
		}, "name");
	}


	public List<String> getActivities(boolean detail) {

		return getContent(new String[] {
			"manifest", "application", "activity"
		}, detail ? null : "name");
	}


	public List<String> getServices(boolean detail) {

		return getContent(new String[] {
			"manifest", "application", "service"
		}, detail ? null : "name");
	}


	public List<String> getReceivers(boolean detail) {

		return getContent(new String[] {
			"manifest", "application", "receiver"
		}, detail ? null : "name");
	}


	public long getFileInApk(String pathInApk, OutputStream out) {

		try {
			JarEntry jarEntry = mJarFile.getJarEntry(pathInApk);
			if (jarEntry == null) {
				out.close();
				return 0;
			}
			InputStream in = mJarFile.getInputStream(jarEntry);
			byte[] buffer = new byte[8192];
			long count = 0;
			int length = 0;
			while ((length = in.read(buffer, 0, buffer.length)) > 0) {
				out.write(buffer, 0, length);
				count += length;
			}
			out.close();
			in.close();
			return count;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}


	@Override
	public void close() {

		try {
			mJarFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public String toString() {

		return mManifestData.toString();
	}


	private static final String HEX_CHARS = "0123456789ABCDEF";


	public static String toHexString(byte[] bytes) {

		StringBuilder sb = new StringBuilder();
		for (int b : bytes) {
			sb.append(HEX_CHARS.charAt((b & 0xf0) >> 4));
			sb.append(HEX_CHARS.charAt(b & 0x0f));
		}
		return sb.toString();
	}

}
