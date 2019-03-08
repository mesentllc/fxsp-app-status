package com.fedex.smartpost.utilities.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public class CommonUtils {
	private static final Log logger = LogFactory.getLog(CommonUtils.class);

	public static String getFilename(File file) {
		String filename = file.getAbsolutePath();
		return filename.substring(filename.lastIndexOf("\\") + 1);
	}

	public static boolean foundProjectRoot(File[] files) {
		boolean pomFound = false;
		for (File file : files) {
			String filename = file.getName();
			if (filename.equals("pom.xml") || filename.equals("build.gradle")) {
				pomFound = true;
				break;
			}
		}
		return pomFound;
	}

	public static String getProjectType(File rootDir) {
		File[] files = rootDir.listFiles();
		for (File file : files) {
			switch (file.getName()) {
				case "pom.xml":
					return "maven";
				case "build.gradle":
					return "gradle";
			}
		}
		return "invalid";
	}


}
