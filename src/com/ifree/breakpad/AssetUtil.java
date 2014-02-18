package com.ifree.breakpad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.res.AssetManager;

public class AssetUtil {
	private AssetManager asm;
	
	class ExtractJob extends Thread{
		private volatile boolean needCacnel= false;
		private boolean override;
		private String from;
		private String to;
		private List<ActionCallback<File>> successHandlers;
		private List<ActionCallback<Throwable>> failureHandlers;
		public ExtractJob(String assetPath,String to,boolean override){
			this.override=override;
			this.from=assetPath;
			this.to=to;
			this.successHandlers=new ArrayList<ActionCallback<File>>();
			this.failureHandlers=new ArrayList<ActionCallback<Throwable>>();
		}
		
		public ExtractJob done(ActionCallback<File> callback){
			this.successHandlers.add(callback);
			return this;
		}
		
		public ExtractJob fail(ActionCallback<Throwable> callback){
			this.failureHandlers.add(callback);
			return this;
		}
				
		
		public void cancel(){
			this.needCacnel=true;
		}
		
		
		public boolean deleteDirectory(File directory) {
			if(this.needCacnel)
				return false;
		    if(directory.exists() && !directory.isFile()){
		        File[] files = directory.listFiles();
		        if(null!=files){
		            for(int i=0; i<files.length; i++) {
		                if(files[i].isDirectory()) {
		                    deleteDirectory(files[i]);
		                }
		                else {
		                    files[i].delete();
		                }
		            }
		        }
		    }
		    return(directory.delete());
		}
		
		/**
		 * bulk copy
		 * this may be a bit slow, you might need to prepare a file list to copy
		 * @param asset
		 */
		public void copyFile(String asset){
			if(this.needCacnel)
				return;
			File outfile=new File(this.to,asset);
			/*if(outfile.exists() && this.override) // too rude?
				this.deleteDirectory(outfile);*/
			try {
				String[] files=asm.list(asset);
				if(files.length>0){//it's a directory
					for(String f:files){
						copyFile(f);
					}
				}else{//it's a file
					InputStream is=asm.open(asset);			
					if(!outfile.getParentFile().exists())
						outfile.getParentFile().mkdirs();
					if(outfile.exists() && this.override)
						outfile.delete();
					FileOutputStream fos=new FileOutputStream(outfile);
					copyFile(is,fos);
					is.close();
		        	is = null;
		        	fos.flush();
		        	fos.close();
		        	fos = null;
				}
			} catch (IOException e) {
				handleFailure(e);
			} 
			handleDone(new File(this.to));
		}
		
		public void copyFile(InputStream in, OutputStream out) throws IOException {
			if(this.needCacnel)
				return;
		    byte[] buffer = new byte[1024];
		    int read;
		    while((read = in.read(buffer)) != -1){
		      out.write(buffer, 0, read);
		    }
		}
		
		private void handleFailure(Throwable e){
			for(ActionCallback<Throwable> callback : this.failureHandlers){
				callback.handle(e);
			}
		}
		
		private void handleDone(File dest){
			for(ActionCallback<File> callback: this.successHandlers){
				callback.handle(dest);
			}
		}

		@Override
		public void run() {
			if(this.override){//a bit rude, so don't store any thing in symbol directory.
				deleteDirectory(new File(this.to));
			}
			this.copyFile(from);			
		}
	}
	
	public interface ActionCallback<T>{
		public void handle(T result);
	}
	
	/**
	 * an android asset util.
	 * 
	 * new AssetUtil(asm).extract("xx",condition).done(new ActionCallback(){...}).fail(new ActionCallback(){...}).start();
	 * @param asm
	 */
	public AssetUtil(AssetManager asm){
		this.asm=asm;
	}
	
	
	public ExtractJob extract(String assetPath,String diskPath){
		return new ExtractJob(assetPath,diskPath,true);
	}
	
	public ExtractJob extract(String assetPath,String diskPath,boolean override){
		return new ExtractJob(assetPath,diskPath,override);
	}
	
}
