package com.gamer.data.mpcserver.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Java类操作工具类
 */
public class ClazzUtil {

	/**
	 * 获取某个类的实现类
	 */
	public static List<Class<?>> getAllAssignedClass(Class<?> cls) throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		for (Class<?> c : getClasses(cls)) {
			if (cls.isAssignableFrom(c) && !cls.equals(c)) {
				classes.add(c);
			}
		}
		return classes;
	}

	/**
	 * 获取某个类的同包下的所有其他类
	 */
	public static List<Class<?>> getAllClassExceptPackageClass(Class<?> packageClass) throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		for (Class<?> c : getClasses(packageClass)) {
			if (!packageClass.equals(c)) {
				classes.add(c);
			}
		}
		return classes;
	}

	/**
	 * 读取指定类所在包下所有类
	 */
	public static List<Class<?>> getClasses(Class<?> packageClass) throws Exception {
		return getClasses(packageClass.getPackage().getName(), packageClass);
	}

	/**
	 * 读取某个包下所有类
	 */
	public static List<Class<?>> getClasses(String pk) throws Exception {
		return getClasses(pk, null);
	}

	/**
	 * 读取某个包下所有类
	 */
	public static List<Class<?>> getClasses(String pk, Class<?> cls) throws Exception {
		String path = pk.replace('.', '/');
		URL url;
		if (cls != null) {
			url = cls.getClassLoader().getResource(path);
		} else {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			url = classloader.getResource(path);
		}
		if (url == null) {
			throw new Exception("url get error！" + path);
		}
		String protocol = url.getProtocol();
		if ("file".equals(protocol)) { // 适用于class文件
			return getClasses(new File(url.getFile()), pk);
		} else if ("jar".equals(protocol)) { // 适用于jar包
			JarFile jarFile = ((JarURLConnection)url.openConnection()).getJarFile();
			return getClassesFromJarFile(jarFile, pk);
		} else {
			throw new Exception("未识别的文件协议！" + protocol);
		}
	}

	public static List<Class<?>> getClassesFromJarFile(JarFile jarFile, String include) throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = (JarEntry)entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class")) {
				name = name.substring(0, name.length() - 6).replaceAll("/", ".");
				if (!name.contains(include)) {
					continue;
				}
				classes.add(Class.forName(name, false, Thread.currentThread().getContextClassLoader()));
			}
		}
		return classes;
	}

	/**
	 * 从jar包中获取指定文件名的txt文件，返回文件名和内容
	 *
	 * @param jarFile
	 *            jar文件对象
	 * @param fileName
	 *            指定的文件名（如果为null或空字符串则返回所有txt文件）
	 * @return Map，key为文件名，value为文件内容
	 * @throws Exception
	 *             读取文件时可能抛出的异常
	 */
	public static Map<String, String> getTxtFilesWithContentFromJarFile(JarFile jarFile, String fileName)
		throws Exception {
		Map<String, String> txtFilesWithContent = new HashMap<>();
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = (JarEntry)entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".txt")) {
				// 如果指定了文件名，则只处理匹配的文件
				if (fileName != null && !fileName.isEmpty()) {
					// 支持完整路径匹配或仅文件名匹配
					if (!name.equals(fileName) && !name.endsWith("/" + fileName)) {
						continue;
					}
				}
				// 读取文件内容
				InputStream inputStream = jarFile.getInputStream(entry);
				StringBuilder content = new StringBuilder();
				try (BufferedReader reader =
					new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						content.append(line).append("\n");
					}
				}
				// 移除最后一个换行符
				if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
					content.setLength(content.length() - 1);
				}
				txtFilesWithContent.put(name, content.toString());
			}
		}
		return txtFilesWithContent;
	}

	// 根据路径获取
	public static List<Class<?>> getClasses(File dir, String pk) throws ClassNotFoundException {
		List<Class<?>> classes = new ArrayList<>();
		if (!dir.exists()) {
			return classes;
		}
		File[] files = dir.listFiles();
		if(files == null){
			return classes;
		}
		for (File f : files) {
			if (f.isDirectory()) {
				classes.addAll(getClasses(f, pk + "." + f.getName()));
			}
			String name = f.getName();
			//排除
			if (name.endsWith(".class") && !name.contains("$")) {
				String className = pk + "." + name.substring(0, name.length() - 6);
				classes.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
			}
		}
		return classes;
	}
}