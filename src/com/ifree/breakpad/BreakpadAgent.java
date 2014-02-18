package com.ifree.breakpad;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ifree.breakpad.AssetUtil.ActionCallback;

import android.content.res.AssetManager;
import android.util.Log;

public class BreakpadAgent {

	/**
	 * native code warning: you can't create more than one JVM. you can't share
	 * JNIEnv between threads, so you need to share JVM
	 * */

	static{
		System.loadLibrary("BreakpadHelper");
	}
	
	private static final String TAG = "BreadpadAgent::report";

	private String symbolDir;
	private String dumpDir;
	private AssetManager asm;
	private AssetUtil.ActionCallback<Throwable> backtraceCallback;

	class FlurryProcessor implements ProcessReader.ReaderLineProcessor {

		private List<StackTraceElement> mStackTrace;

		public FlurryProcessor() {
			mStackTrace = new ArrayList<StackTraceElement>();
		}

		@Override
		public boolean process(String line) {
			String label = "[stackwalk stack trace]::";
			String cls = "";
			String method = "";
			String file = "";
			int lineNum = 0;
			if (line.startsWith(label)) {
				String rest[] = line.substring(25).split("\\|");
				// Log.d(TAG,line.substring(25));
				if (rest.length == 4) {
					cls = rest[0];
					method = rest[1];
					file = rest[2];
					lineNum = Integer.parseInt(rest[3]);
				}

			}
			mStackTrace.add(new StackTraceElement(cls, method, file, lineNum));
			return true;
		}

		@Override
		public void endProcess() {
			Throwable jniCrash = new Throwable(TAG);
			jniCrash.setStackTrace(mStackTrace
					.toArray(new StackTraceElement[mStackTrace.size()]));

			execCallback(jniCrash);			
			// DeleteDumpFile(); TODO:remove it when release
			
			Log.e(TAG, "jni crash logging", jniCrash);
		}

	}

	public BreakpadAgent(AssetManager asm, String symdir, String dumpDir) {
		this.setSymbolDir(symdir);
		this.setDumpDir(dumpDir);
		this.setAssetManager(asm);
		//if use native
		nativeInit();
	}
	
	public native void nativeInit();

	/**
	 * get backtrace from native code
	 * */
	public native void getBacktrace(String dumpFile,String symbolPath);

	/**
	 * get backtrace from cross compiled minidump_stackwalk
	 * 
	 * @param stackwalk_path
	 *            minidump_stackwalk path in asset dir
	 * @param rootPath
	 *            the path I will extract symbol files to, and maybe you clean
	 *            up symbol files when update to new version
	 * */
	public void getBacktrace(final String stackwalk_path,final String exec_path, final String rootPath) {
		final boolean override = false;
		
		new AssetUtil(asm).extract(stackwalk_path, exec_path, override).done(
				new ActionCallback<File>() {

					@Override
					public void handle(File result) {
						// when minidump_stackwalk extract success,extract
						// symbols
						final File stackwalk=new File(exec_path,stackwalk_path);
						new AssetUtil(asm).extract(symbolDir, rootPath,
								override).done(new ActionCallback<File>() {

							@Override
							public void handle(File result) {
								try {
									if (!stackwalk.canExecute())
										stackwalk.setExecutable(true);
									String dump=findFirstDump();
									if(dump==null)
										throw new IOException("dump file not found");
									//futuretask?
									Process proc = Runtime.getRuntime().exec(
											stackwalk.getAbsolutePath() + " "
													+ dump + " "
													+ getSymbolDir(), null,
													stackwalk.getParentFile());
									ProcessReader out = new ProcessReader(proc);
									ProcessReader err = new ProcessReader(proc
											.getErrorStream());
									
									out.addProcessor(new FlurryProcessor());
									err.addProcessor(new ProcessReader.ReaderLineProcessor() {

										@Override
										public boolean process(String line) {
											System.out.print( line);// you must handle process's error stream, or android will hang
											return true;
										}

										@Override
										public void endProcess() {
											// pass
										}

									});
									out.start();
									err.start();
									int status = proc.waitFor();
									Log.d(TAG, "process exit with status: "
											+ status);
								} catch (IOException e) {
									e.printStackTrace();
									Log.e(TAG, e.getMessage(), e);
								} catch (InterruptedException e) {
									e.printStackTrace();
									Log.e(TAG, e.getMessage(), e);
								}

							}

						}).start();
					}

				}).start();
	}
	
	
	/**
	 * handle dump result
	 * @param jniCrash
	 */
	private void execCallback(Throwable jniCrash) {
		if(backtraceCallback!=null){
			backtraceCallback.handle(jniCrash);
		}
	}
	
	public String findFirstDump() {
		File dumpf = new File(dumpDir);
		File files[] = dumpf.listFiles();
		if(files==null)
			return null;
		if (files.length > 0) {// assume no nested directories
			return files[0].getAbsolutePath();
		}
		return null;
	}

	/**
	 * @return the symbolDir
	 */
	public String getSymbolDir() {
		return symbolDir;
	}

	/**
	 * @param symbolDir
	 *            the symbolDir to set
	 */
	public void setSymbolDir(String symbolDir) {
		this.symbolDir = symbolDir;
	}

	/**
	 * @return the dumpDir
	 */
	public String getDumpDir() {
		return dumpDir;
	}

	/**
	 * @param dumpDir
	 *            the dumpDir to set
	 */
	public void setDumpDir(String dumpDir) {
		this.dumpDir = dumpDir;
	}

	/**
	 * @return the asm
	 */
	public AssetManager getAssetManager() {
		return asm;
	}

	/**
	 * @param asm
	 *            the asm to set
	 */
	public void setAssetManager(AssetManager asm) {
		this.asm = asm;
	}

	/**
	 * @return the backtraceCallback
	 */
	public AssetUtil.ActionCallback<Throwable> getBacktraceCallback() {
		return backtraceCallback;
	}

	/**
	 * @param backtraceCallback the backtraceCallback to set
	 */
	public void setBacktraceCallback(AssetUtil.ActionCallback<Throwable> backtraceCallback) {
		this.backtraceCallback = backtraceCallback;
	}

	
}
