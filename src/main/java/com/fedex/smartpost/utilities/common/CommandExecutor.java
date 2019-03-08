package com.fedex.smartpost.utilities.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class CommandExecutor {
	protected void executeAndLogStdOut(String[] command) throws IOException {
		String output;

		Process process = Runtime.getRuntime().exec(command);
		BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((output = br.readLine()) != null) {
			System.out.println(output);
		}
		br.close();
	}

	protected abstract void build() throws IOException;
}
