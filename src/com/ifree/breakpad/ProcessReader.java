package com.ifree.breakpad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessReader extends Thread {

	public interface ErrorListener {
		void onError(Throwable ex);
	}

	public interface ReaderLineProcessor {
		boolean process(String line);

		void endProcess();
	}

	InputStream is;
	private List<ReaderLineProcessor> processors;
	private ErrorListener errorListener;

	public ProcessReader(InputStream is) {
		this.is = is;
		processors = new ArrayList<ReaderLineProcessor>();
	}

	public ProcessReader(Process proc) {
		this(proc.getInputStream());// default process input stream
	}

	public void addProcessor(ReaderLineProcessor p) {
		processors.add(p);
	}

	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
	}

	public void run() {
		try {
			InputStreamReader inStreamReader = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(inStreamReader);
			String line = null;
			while ((line = br.readLine()) != null) {
				for (int i = 0, il = processors.size(); i < il; i++) {
					if (!processors.get(i).process(line))// if failed to process
															// quit
						return;
				}
			}
			for (int i = 0, il = processors.size(); i < il; i++) {
				processors.get(i).endProcess();
			}
		} catch (IOException ex) {
			if (errorListener != null)
				errorListener.onError(ex);
		}
	}
}
